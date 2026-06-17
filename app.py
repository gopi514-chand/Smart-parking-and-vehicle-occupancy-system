import subprocess
import threading
import time
import cv2
import numpy as np
import pickle
import firebase_admin
from firebase_admin import credentials, db
import json


# ------------------ NON-BLOCKING SPEECH ------------------
def speak(text):
    def _run():
        cmd = (
            "Add-Type -AssemblyName System.Speech; "
            f"(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('{text}');"
        )
        subprocess.run(["powershell", "-Command", cmd], shell=True)

    threading.Thread(target=_run, daemon=True).start()


# =========================================================
class Park_classifier:
    def __init__(self, positions_path, rect_width, rect_height):
        self.rect_width = rect_width
        self.rect_height = rect_height

        with open(positions_path, "rb") as f:
            raw_positions = pickle.load(f)

        raw_positions.sort(key=lambda p: p[0])

        columns = []
        if raw_positions:
            current_col = [raw_positions[0]]
            column_threshold = 40

            for pos in raw_positions[1:]:
                if abs(pos[0] - current_col[-1][0]) > column_threshold:
                    columns.append(current_col)
                    current_col = []
                current_col.append(pos)
            columns.append(current_col)

        self.positions = []
        counter = 1
        for col in columns:
            col.sort(key=lambda p: p[1])
            for (x, y) in col:
                self.positions.append((x, y, counter))
                counter += 1

    # -----------------------------------------------------
    def implement_process(self, frame):
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        blur = cv2.GaussianBlur(gray, (3, 3), 1)
        thresh = cv2.adaptiveThreshold(
            blur, 255,
            cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
            cv2.THRESH_BINARY_INV, 25, 16
        )
        median = cv2.medianBlur(thresh, 5)
        kernel = np.ones((3, 3), np.uint8)
        dilate = cv2.dilate(median, kernel, iterations=1)
        return dilate

    def get_slot_at(self, x, y):
        for px, py, slot_no in self.positions:
            if px < x < px + self.rect_width and py < y < py + self.rect_height:
                return slot_no
        return None

    def get_slot_coords(self, slot_no):
        for px, py, s_no in self.positions:
            if s_no == slot_no:
                return (px, py)
        return None

    # -----------------------------------------------------
    def classify(self, image, processed_image, slot_data=None, return_empty_slots=False):
        if slot_data is None:
            slot_data = {}

        empty_slots = []

        for x, y, slot_no in self.positions:
            slot_img = processed_image[y:y+self.rect_height, x:x+self.rect_width]
            non_zero_count = cv2.countNonZero(slot_img)

            is_occupied = non_zero_count >= 900

            s_info = slot_data.get(str(slot_no), {})
            state_label = s_info.get('status', 'none')

            color = (0, 255, 0)
            text = str(slot_no)

            if is_occupied:
                if state_label in ["pending", "booked"]:
                    color = (255, 0, 255)
                    text = "Wait..."
                elif state_label == "authorized":
                    color = (0, 255, 0)
                    text = "OK"
                else:
                    color = (0, 0, 255)
            else:
                if state_label == "booked":
                    color = (0, 255, 255)
                    text = "Booked"
                else:
                    empty_slots.append(slot_no)

            cv2.rectangle(image, (x, y),
                          (x + self.rect_width, y + self.rect_height),
                          color, 2)

            cv2.putText(image, text, (x, y - 5),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5,
                        (255, 255, 255), 2)

        if return_empty_slots:
            return image, empty_slots

        return image


# =========================================================
def demostration():
    rect_width, rect_height = 107, 48
    carp_park_positions_path = "data/source/CarParkPos"
    video_path = "data/source/carPark.mp4"

    # ---------------- FIREBASE ----------------
    cred = credentials.Certificate(
        r"C:\Users\pavan\OneDrive\Desktop\smart parking\firebase.json"
    )
    firebase_admin.initialize_app(cred, {
        'databaseURL': 'https://smartparking-90d2b-default-rtdb.firebaseio.com/'
    })

    empty_slots_ref = db.reference('parking_lot/empty_slots')
    bookings_ref = db.reference('parking_lot/bookings')
    parking_ref = db.reference('parking_slot')

    # ---------------- CLASSIFIER ----------------
    classifier = Park_classifier(carp_park_positions_path, rect_width, rect_height)
    cap = cv2.VideoCapture(video_path)

    state = {
        'empty_slots_ids': set(),
        'mouse_pos': (0, 0),
        'global_last_speak_time': 0
    }

    last_spoken = {}
    first_run = True   # ⭐ IMPORTANT FLAG

    cv2.namedWindow("Parking Status")

    while True:
        ret, frame = cap.read()
        if not ret:
            cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
            continue

        legacy_bookings = bookings_ref.get() or {}
        remote_data = parking_ref.get() or {}

        slot_data = {}
        for k, v in legacy_bookings.items():
            if v:
                slot_data[k] = {'status': 'booked'}

        processed_frame = classifier.implement_process(frame)
        denoted_image, empty_slots = classifier.classify(
            frame, processed_frame, slot_data=slot_data, return_empty_slots=True
        )

        # =====================================================
        # 🔥 FIRST RUN: AUTO APPROVE EXISTING CARS
        # =====================================================
        if first_run:
            print("Initializing existing cars as BOOKED + APPROVED")
            init_data = {}

            for x, y, slot_no in classifier.positions:
                box_key = f"box {slot_no}"
                if slot_no not in empty_slots:
                    init_data[box_key] = {
                        "status": 1,
                        "access_request": "approved"
                    }
                else:
                    init_data[box_key] = {
                        "status": 0,
                        "access_request": "none"
                    }

            parking_ref.update(init_data)
            empty_slots_ref.set(empty_slots)

            first_run = False
            continue
        # =====================================================

        current_bookings = {
            k: True for k, v in slot_data.items()
            if v['status'] in ['booked', 'authorized']
        }

        live_data = {}

        for x, y, slot_no in classifier.positions:
            s_id = str(slot_no)
            box_key = f"box {s_id}"

            current = remote_data.get(box_key, {})
            current_req = current.get("access_request", "none")

            is_booked = s_id in current_bookings
            is_free = slot_no in empty_slots

            status_val = 0
            new_req = current_req

            if not is_free:
                status_val = 2
                if current_req == "approved":
                    status_val = 1
                elif current_req == "denied":
                    now = time.time()
                    if now - state['global_last_speak_time'] > 3:
                        if s_id not in last_spoken or now - last_spoken[s_id] > 10:
                            speak(f"Parking {s_id} rejected. Please vacate.")
                            last_spoken[s_id] = now
                            state['global_last_speak_time'] = now
                else:
                    if current_req == "none":
                        new_req = "pending"

            elif is_booked:
                status_val = 1
                if current_req == "pending":
                    new_req = "approved"
            else:
                status_val = 0
                if current_req != "none":
                    new_req = "none"

            live_data[box_key] = {
                "status": status_val,
                "access_request": new_req
            }

        parking_ref.update(live_data)
        empty_slots_ref.set(empty_slots)

        cv2.imshow("Parking Status", denoted_image)
        if cv2.waitKey(30) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()


# =========================================================
if __name__ == "__main__":
    demostration()
