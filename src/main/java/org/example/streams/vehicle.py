import psycopg2
import os
import random
from datetime import datetime
import time
from concurrent.futures import ThreadPoolExecutor
from geopy.distance import distance

db_param = {
    "database": os.environ.get("POSTGRES_DB"),
    "user": os.environ.get("POSTGRES_USER"),
    "password": os.environ.get("POSTGRES_PASSWORD"),
    "host": os.environ.get("POSTGRES_HOST"),
    "port": os.environ.get("POSTGRES_PORT")
}

conn = psycopg2.connect(**db_param)

# Create a cursor
cursor = conn.cursor()

# Create the table
cursor.execute('''
CREATE TABLE IF NOT EXISTS public.location (
    vehicle_id INTEGER NOT NULL,
    latitude FLOAT NOT NULL,
    longitude FLOAT NOT NULL,
    "timestamp" INTEGER NOT NULL
    );
''')

conn.commit()

def isPrime(n):
  if n == 2 or n == 3: return True
  if n < 2 or n%2 == 0: return False
  if n < 9: return True
  if n%3 == 0: return False
  r = int(n**0.5)
  # since all primes > 3 are of the form 6n ± 1
  # start with f=5 (which is prime)
  # and test f, f+2 for being prime
  # then loop by 6. 
  f = 5
  while f <= r:
    if n % f == 0: return False
    if n % (f+2) == 0: return False
    f += 6
  return True 

count = [1] * 10
primes = [i for i in range(11,149) if isPrime(i)]
timestamps = [1609459200] * 10

def generate_coordinates(start_lat, start_lon, distance_miles):
    # Generate random values to simulate movement
    delta_lat = random.uniform(-0.1, 0.1)
    delta_lon = random.uniform(-0.1, 0.1)

    # Calculate new coordinates using geopy's distance function
    new_point = distance(miles=distance_miles).destination((start_lat, start_lon), random.uniform(0.0, 359.0))
    new_lat, new_lon = new_point.latitude, new_point.longitude

    return new_lat, new_lon

def simulate_vehicle_movement(vehicle_id):
    # Starting coordinates in the United States (adjust as needed)
    start_lat = random.uniform(24, 49)
    start_lon = random.uniform(-125, -66)

    while True:
        # Generate coordinates every 45 seconds
        random_prime = random.choice(primes)
        if count[vehicle_id-1] % random_prime != 0:
            lat, lon = generate_coordinates(start_lat, start_lon, random.uniform(1, 5))
            print(f"Vehicle {vehicle_id}: Latitude={lat}, Longitude={lon} at timestamp - {timestamps[vehicle_id+1]}")
            # timestamp_datetime = datetime.fromtimestamp(timestamps[vehicle_id])
            cursor.execute('''
            INSERT INTO public.location ("vehicle_id", "latitude", "longitude", timestamp)
            VALUES (%s, %s, %s, %s); ''', (vehicle_id, lat, lon, timestamps[vehicle_id+1]))
            print("Data inserted\n")
            # cursor.execute('''
            # SELECT * FROM public.location;''')
            timestamps[vehicle_id-1]+=45
        else:
            print(f"Vehicle {vehicle_id}: Waiting...")
            timestamps[vehicle_id+1]+=random.uniform(120, 900)

        count[vehicle_id-1]+=1
        conn.commit()
    cursor.close()
    conn.close()


if __name__ == "__main__":
    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = [executor.submit(simulate_vehicle_movement, vehicle) for vehicle in range(1, 11)]
        # Wait for all tasks to complete
        for future in futures:
            future.result()
