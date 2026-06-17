import firebase_admin
from firebase_admin import credentials, db
import time
import sys

def monitor():
    # Initialize Firebase
    cred = credentials.Certificate(
        r"C:\Users\pavan\OneDrive\Desktop\smart parking\firebase.json"
    )
    
    try:
        firebase_admin.get_app()
    except ValueError:
        firebase_admin.initialize_app(cred, {
            'databaseURL': 'https://smartparking-90d2b-default-rtdb.firebaseio.com/'
        })

    print("--- Firebase Monitor ---")
    print("Watching 'parking_slot'...")

    def on_status_change(event):
        data = event.data
        if not data:
            print("No data.")
            return

        # New Schema: { "box 1": {"status": 0}, ... }
        # 0=Empty, 1=Booked, 2=Filled
        
        counts = {0: 0, 1: 0, 2: 0}
        slot_7_status = "unknown"

        if isinstance(data, dict):
            for key, val_dict in data.items():
                # key is "box N", val_dict is {"status": X}
                if isinstance(val_dict, dict) and 'status' in val_dict:
                    s = val_dict['status']
                    if s in counts:
                        counts[s] += 1
                    
                    if key == "box 7":
                        # Translate for display
                        if s == 0: slot_7_status = "Empty (0)"
                        elif s == 1: slot_7_status = "Booked (1)"
                        elif s == 2: slot_7_status = "Filled (2)"
            
            print(f"\n[LIVE STATUS] Empty(0): {counts[0]} | Booked(1): {counts[1]} | Filled(2): {counts[2]}")
            print(f"    -> Box 7 Status: {slot_7_status}")
            
            # Unauthorized Detection
            # If Filled > Booked, some slots are filled but not booked!
            unauthorized_count = counts[2] - counts[1]  # Approximation (won't be exact if a booked slot is empty)
            # Better: Count specific deviations if we had full map. For now, simple prompt.
            
            if counts[2] > 0:
                 print("\n[ACTION REQUIRED] Found Filled Slots!")
                 print("    -> If a car is parking, you MUST 'book <id>' to confirm it.")
                 print("    -> Otherwise, the system will keep alerting.")
            
            # Print sample free slots
            free = [k for k,v in data.items() if v.get('status') == 0]
            print(f"    -> Open Boxes: {free[:5]}...")

    # Listen streams
    db.reference('parking_slot').listen(on_status_change)
    # db.reference('parking_lot/bookings').listen(on_bookings_change) # Optional now

    print("\nListening for changes... Press Ctrl+C to stop.")
    while True:
        time.sleep(1)

if __name__ == "__main__":
    monitor()
