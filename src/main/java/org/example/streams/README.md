# Kafka Streams Processing

## Background

A fleet management company plans to leverage Kafka Streams for 2 of its use cases

- Alerting if a vehicle's average temperature or rpm breaches a certain threshold
- Calculating the nearest service center in case there is an alert

There are 2 sensors in every vehicle that emits data to 2 separate kafka topics. The first sensor(location sensor) emits the location(lat, long) of the vehicle every 45 seconds. The second sensor(telemetry sensor) emits the temperature and number of rotations at a given moment every 10 seconds. The vehicle information and the (service center information) is stored in a postgres database in (2) separate tables.


## Setup

1. Setup a datagen source connector with the schema defined in [sensor-schema.json](../../../../avro/org/example/streams/sensor-schema.avsc)
2. Setup the database and the load the database with the [docker-compose.yaml](./docker-compose.yaml). Run
```bash
docker-compose up -d --build
```
Check the database contents with
```bash
docker-compose exec -it postgres psql "postgres://platformatory:plf_password@postgres:5432/plf_training"
```
3. Add kafka and connect to the docker-compose file
4. Create a source connectors for the vehicle and (service_center) tables.


## Requirements

- Calculate the average temperature and rotations per minute(rpm) of the vehicle every 10 minutes
  - Output all calculations where the avg. temp. is greater than 100 and the rpm is greater than 3000. 
  - The output should include the vehicle information as well

### TODO

- Calculate the nearest service center when the above conditions are met.
