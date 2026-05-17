import requests
import json
import time

BASE_URL = 'http://localhost:8080/api'

# 1. Register a driver
print("Registering driver...")
driver_data = {
    "name": "Test Driver",
    "email": "testdriver@example.com",
    "password": "password",
    "vehicleType": "Car"
}
res = requests.post(f"{BASE_URL}/drivers/register", json=driver_data)
try:
    driver_id = res.json().get('driverId')
    print("Driver ID:", driver_id)
except Exception as e:
    print(res.text)

# 2. Register a passenger
print("Registering passenger...")
passenger_data = {
    "name": "Test Passenger",
    "email": "testpassenger@example.com",
    "password": "password"
}
res = requests.post(f"{BASE_URL}/users/register", json=passenger_data)
try:
    passenger_id = res.json().get('userId')
    print("Passenger ID:", passenger_id)
except Exception as e:
    print(res.text)

# 3. Create a booking
print("Creating booking...")
booking_data = {
    "passengerId": passenger_id,
    "pickupLocation": "A",
    "dropoffLocation": "B",
    "rideType": "Car",
    "fare": 500.0
}
res = requests.post(f"{BASE_URL}/bookings/create", json=booking_data)
try:
    booking_id_str = res.json().get('bookingId') # e.g. B001
    booking_id = int(booking_id_str.replace('B', ''))
    print("Booking ID:", booking_id)
except Exception as e:
    print(res.text)

# 4. Accept booking
print("Accepting booking...")
res = requests.put(f"{BASE_URL}/bookings/{booking_id}/status", json={
    "status": "Accepted",
    "driverId": driver_id
})
print("Accept:", res.text)

# 5. Complete booking
print("Completing booking...")
res = requests.put(f"{BASE_URL}/bookings/{booking_id}/status", json={
    "status": "Completed"
})
print("Complete:", res.text)

# 6. Check driver details
res = requests.get(f"{BASE_URL}/drivers/{driver_id}")
print("Driver stats after completion:", res.text)

# 7. Rate driver
print("Rating driver...")
res = requests.put(f"{BASE_URL}/drivers/{driver_id}/rate", json={
    "rating": 4.0
})
print("Rate:", res.text)

# 8. Check driver details again
res = requests.get(f"{BASE_URL}/drivers/{driver_id}")
print("Driver stats after rating:", res.text)
