import firebase_admin
from firebase_admin import credentials, db
import time
import sys

def simulate_booking():
    # Initialize Firebase (Same credentials)
    cred = credentials.Certificate(
        r"C:\Users\pavan\OneDrive\Desktop\smart parking\firebase.json"
    )
    
    # Check if app is already initialized to avoid error
    try:
        firebase_admin.get_app()
    except ValueError:
        firebase_admin.initialize_app(cred, {
            'databaseURL': 'https://smartparking-90d2b-default-rtdb.firebaseio.com/'
        })

    bookings_ref = db.reference('parking_lot/bookings')
    empty_slots_ref = db.reference('parking_lot/empty_slots')

    print("--- Remote Booking Console ---")
    print("Commands:")
    print("  book <id>   : Book a specific slot (e.g., 'book 5')")
    print("  unbook <id> : Unbook a specific slot (e.g., 'unbook 5')")
    print("  list        : Show statuses and pending requests")
    print("  confirm <id>: Approve a pending request")
    print("  reject <id> : Deny a pending request")
    print("  exit        : Quit this tool")

    # Use parking_slot wrapper
    status_ref = db.reference('parking_slot')

    while True:
        command = input("\nfirebase-remote> ").strip().lower()
        
        if command == "exit":
            break
        
        elif command == "list":
            data = status_ref.get()
            if data:
                empty = []
                booked = []
                filled = [] # Status 2
                pending = [] # Access Request Pending
                
                if isinstance(data, dict):
                    for k, v_dict in data.items():
                        if isinstance(v_dict, dict):
                            s = v_dict.get('status', 0)
                            req = v_dict.get('access_request', 'none')
                            
                            if s == 0: empty.append(k)
                            elif s == 1: booked.append(k)
                            elif s == 2: filled.append(k)
                            
                            if req == "pending":
                                pending.append(k)

                print(f"Available (Status 0): {empty}")
                print(f"Booked (Status 1): {booked}")
                print(f"Filled (Status 2): {filled}")
                print(f"⚠ PENDING REQUESTS: {pending}")
            else:
                print("No status data available yet.")
            
        elif command.startswith("confirm "):
            try:
                slot_id = command.split()[1]
                key = f"box {slot_id}"
                print(f"Approving {key}...")
                status_ref.child(key).update({"access_request": "approved"})
                print("✓ Approved.")
            except IndexError:
                print("Usage: confirm <id>")

        elif command.startswith("reject "):
            try:
                slot_id = command.split()[1]
                key = f"box {slot_id}"
                print(f"Denying {key}...")
                status_ref.child(key).update({"access_request": "denied"})
                print("✓ Denied. Voice alert should trigger.")
            except IndexError:
                print("Usage: reject <id>")

        elif command.startswith("book "):
            try:
                slot_id = command.split()[1]
                key = f"box {slot_id}"
                print(f"Booking {key}...")
                # Set status=1 (Booked) and Auto-Approve
                status_ref.child(key).update({
                    "status": 1, 
                    "access_request": "approved"
                })
                print("✓ Booked & Approved.")
            except IndexError:
                print("Usage: book <id>")

        elif command.startswith("unbook "):
            try:
                slot_id = command.split()[1]
                key = f"box {slot_id}"
                print(f"Canceling booking for {key}...")
                # Reset to 0 and Clear Request
                status_ref.child(key).update({
                    "status": 0,
                    "access_request": "none"
                })
                # Clear legacy as well
                bookings_ref.child(str(slot_id)).delete()
                print(f"✓ Canceled.")
            except IndexError:
                print("Usage: unbook <id>")
        
        else:
            print("Unknown command. Try 'book <id>', 'unbook <id>', 'list', or 'exit'.")

if __name__ == "__main__":
    simulate_booking()
