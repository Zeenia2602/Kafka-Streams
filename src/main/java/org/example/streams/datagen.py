import psycopg2
from faker import Faker
from datetime import datetime
import os

# Connect to PostgreSQL database
db_params = {
    "database": os.environ.get("POSTGRES_DB"),
    "user": os.environ.get("POSTGRES_USER"),
    "password": os.environ.get("POSTGRES_PASSWORD"),
    "host": os.environ.get("POSTGRES_HOST"),
    "port": os.environ.get("POSTGRES_PORT")
}

conn = psycopg2.connect(**db_params)
# Create a cursor object to execute SQL queries
cursor = conn.cursor()

# Create the table if it doesn't exist
cursor.execute('''
    CREATE TABLE IF NOT EXISTS public.vehicle (
        id SERIAL PRIMARY KEY,
        "created" timestamp without time zone NOT NULL,
        "modified" timestamp without time zone,
        name character varying(100) NOT NULL,
        category character varying(100) NOT NULL,
        registration_number character varying(100) NOT NULL,
        identification_number character varying(100) NOT NULL
    );
''')

# Commit the changes
conn.commit()

# Generate and insert 200 vehicle details
fake = Faker()

for _ in range(200):
    created_date = fake.date_time_between(start_date="-1y", end_date="now")
    modified_date = fake.date_time_between(start_date=created_date, end_date="now")
    name = fake.name()
    category = fake.word()
    registration_number = fake.license_plate()
    identification_number = fake.vin()

    # Insert data into the table
    cursor.execute('''
        INSERT INTO public.vehicle ("created", "modified", name, category, registration_number, identification_number)
        VALUES (%s, %s, %s, %s, %s, %s);
    ''', (created_date, modified_date, name, category, registration_number, identification_number))

# Commit the changes
conn.commit()

# Close the cursor and connection
cursor.close()
conn.close()

print("Data inserted successfully.")
