package org.example.streams;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.kstream.internals.SlidingWindowedKStreamImpl;

import javax.rmi.CORBA.Tie;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static java.util.Optional.ofNullable;

public class VehicleApp {
    public static void main(String[] args) {
        Properties config = new Properties();

        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "group1");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "172.21.0.2:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        config.put("schema.registry.url", "http://172.21.0.6:8081");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());

//        Creating the serdes
        SpecificAvroSerde<fleet_mgmt_sensors> VehicleInfoSerde = getValueSerde(config);
        SpecificAvroSerde<AvgTemp> AvgTempSerde = getAvgTempSerde(config);
        SpecificAvroSerde<AvgRPM> AvgRPMSerde = getAvgRPMSerde(config);

        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, VehicleInfoSerde.getClass());
        config.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, TemperatureTimestampExtractor.class.getName());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, fleet_mgmt_sensors> vehicleStream = builder.stream("telemetry", Consumed.with(Serdes.String(), VehicleInfoSerde )
                .withTimestampExtractor(new TemperatureTimestampExtractor()));

        /* Printing the data in stream
//        vehicleStream.foreach((key, value) -> {
//            System.out.println("Recieved - key: " + key + ", value: " + value);
//        });*/


//        grouped Stream for temperature
        KGroupedStream<Integer, Integer> groupedStream = vehicleStream.map((key, value) ->
                new KeyValue<>(value.getVehicleId(), value.getEngineTemperature()))
                .groupByKey(Grouped.with(Serdes.Integer(), Serdes.Integer()));

//        Grouped stream for rpm
        KGroupedStream<Integer, Integer> rpmgroupedStream = vehicleStream.map((key, value) ->
                new KeyValue<>(value.getVehicleId(), value.getEngineRotation()))
                .groupByKey(Grouped.with(Serdes.Integer(), Serdes.Integer()));

//        Creating the Tumble window for every 10 minutes
        TimeWindows Window = TimeWindows.of(Duration.ofMinutes(10));

//        Creating the Sliding windwo to the rpm
        SlidingWindows slidingWindows = SlidingWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(1));

//        Tumble window stream to find average in every 10 minutes
        TimeWindowedKStream<Integer, Integer> windowedKStream = groupedStream.windowedBy(Window);

//        Slinding window stream to find the rpm
        SlidingWindowedKStreamImpl<Integer, Integer> slidingWindowedKStream = (SlidingWindowedKStreamImpl<Integer, Integer>) rpmgroupedStream.windowedBy(slidingWindows);

        /*----------------- Average Temperature -------------------------- */

        KTable<Windowed<Integer>, AvgTemp> avgTemperature = windowedKStream
                .aggregate(
                        () -> new AvgTemp(0.0, 0),
                        (key, value, aggregate) -> {
                            aggregate.setTotalTemp(aggregate.getTotalTemp() + value);
                            aggregate.setCount(aggregate.getCount() + 1);
                            return aggregate;
                        },
                        Materialized.with(null, AvgTempSerde)
                );
        final KTable<Windowed<Integer>, Double> AverageTemp = avgTemperature.mapValues(value -> value.getTotalTemp() / value.getCount(), Materialized.with(null, Serdes.Double()));

        /*To print the result */
//        AverageTemp.toStream().foreach((key, value) -> {
//            int vehicleId = key.key();
//            long StartTime = key.window().start();
//            long endTime = key.window().end();
//            System.out.println("Vehicle Id: " + vehicleId + ", Time Window: " + StartTime + " to " + endTime + " , AverageTemperature: " + value);
//        });

        /* ----------------------- Average RPM -------------------------- */

        KTable<Windowed<Integer>, AvgRPM> rpm = slidingWindowedKStream
                .aggregate(
                        () -> new AvgRPM(0.0, 0),
                        (key, value, aggregate) -> {
                            aggregate.setTotalRpm(aggregate.getTotalRpm() + value);
                            return aggregate;
                        },
                        Materialized.with(null, AvgRPMSerde)
                );

        KTable<Windowed<Integer>, AvgRPM> avgRPMKTable = windowedKStream
                .aggregate(
                        () -> new AvgRPM(0.0, 0),
                        (key, rotations, aggregate) -> {
                            aggregate.setTotalRpm(aggregate.getTotalRpm() + rotations);
                            aggregate.setCount(aggregate.getCount() + 1);
                            return aggregate;
                        },
                        Materialized.with(null, AvgRPMSerde)
                );


        final KTable<Windowed<Integer>, Double> rotation_per_minute = rpm.mapValues((key, value) -> {
            long windowStart = key.window().start();
            long windowEnd = key.window().end();
            long duration = windowEnd - windowStart;
            double result = (value.getTotalRpm() / (double) duration) * 60000;
            return result;
        }, Materialized.with(null, Serdes.Double()));

        final KTable<Windowed<Integer>, Double> averageRPM = avgRPMKTable.mapValues(value -> value.getTotalRpm()/ value.getCount(), Materialized.with(null, Serdes.Double()));

        /*Printing the average RPM */
//        averageRPM.toStream().foreach((key , value) -> {
//            int vehicle_id = key.key();
//            Instant startTime = key.window().startTime();
//            Instant endTime = key.window().endTime();
//            System.out.println("Vehicle Id: " + vehicle_id + "Start: " + startTime + "End: " + endTime + "Average RPM : " + value);
//        });

//        Merged the both streams
       KStream<String, Double> mergedStream = AverageTemp
               .toStream()
               .filter((key, value) -> value > 100)
               .map((key, value) -> {
                   String windowTime = key.window().startTime() + "-" + key.window().endTime();
                   String keywithTime = "Average Temperature - " + key.key() + " - " + windowTime;
                   return new KeyValue<>(keywithTime, value);
               })
               .merge(averageRPM.toStream().map((key, value) -> {
                   String windowTime = key.window().startTime() + "-" + key.window().endTime();
                   String keywithTime = "Average RPM - " + key.key() + " - " + windowTime;
                   return new KeyValue<>(keywithTime, value);
               }));

       mergedStream.to("output", Produced.with(Serdes.String(), Serdes.Double()));

        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.cleanUp();
        streams.start();

    }

    private static long parseTimestamp(String timestamp) {
        Instant instant = Instant.parse(timestamp);
        return instant.toEpochMilli();
    }

//    Creating serder for Average Temperature Schema
    public static SpecificAvroSerde<AvgTemp> getAvgTempSerde(Properties envProps) {
        SpecificAvroSerde<AvgTemp> serde = new SpecificAvroSerde<>();
        serde.configure(getSerdeConfig(envProps), false);
        return serde;
    }


    public static Map<String, String> getSerdeConfig(Properties props) {
        final HashMap<String, String> map = new HashMap<>();
        final String schemaURL = props.getProperty(SCHEMA_REGISTRY_URL_CONFIG);
        map.put(SCHEMA_REGISTRY_URL_CONFIG, ofNullable(schemaURL).orElse(""));
        return map;
    }

//    Creating Avro serde to get the vehicle information
    public static SpecificAvroSerde<fleet_mgmt_sensors> getValueSerde(Properties envProps) {
        SpecificAvroSerde<fleet_mgmt_sensors> serde = new SpecificAvroSerde<>();
        serde.configure(getSerdeConfig(envProps), false);
        return serde;
    }

//    Creating Avro serde for Average rpm
    public static SpecificAvroSerde<AvgRPM> getAvgRPMSerde(Properties props) {
        SpecificAvroSerde<AvgRPM> serde = new SpecificAvroSerde<>();
        serde.configure(getSerdeConfig(props), false);
        return serde;
    }

}
