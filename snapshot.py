import firebase_admin
from firebase_admin import credentials, db
import json

# Initialize Firebase
cred = credentials.Certificate(r"C:\Users\pavan\OneDrive\Desktop\smart parking\firebase.json")
try:
    firebase_admin.get_app()
except ValueError:
    firebase_admin.initialize_app(cred, {'databaseURL': 'https://smartparking-90d2b-default-rtdb.firebaseio.com/'})

# Get Data
ref = db.reference('parking_lot/slots_status')
data = ref.get()

print("\n--- CURRENT FIREBASE STATE ---")
if data:
    # Normalize list to dict if needed
    if isinstance(data, list):
         data = {str(i): v for i, v in enumerate(data) if v is not None}
    
    # Analyze
    empty = [k for k,v in data.items() if v == 'empty']
    filled = [k for k,v in data.items() if v == 'filled']
    booked = [k for k,v in data.items() if v == 'booked']
    trigger = [k for k,v in data.items() if v == 1 or v == '1']

    print(f"Total Slots Tracking: {len(data)}")
    print(f"Filled (Cars): {len(filled)}")
    print(f"Empty (Free):  {len(empty)}")
    print(f"Booked:        {len(booked)}")
    
    if booked:
        print(f"Booked IDs: {booked}")
    if trigger:
         print(f"TRIGGER DETECTED (Pending Bookings): {trigger}")
else:
    print("Firebase is empty or not reachable.")
