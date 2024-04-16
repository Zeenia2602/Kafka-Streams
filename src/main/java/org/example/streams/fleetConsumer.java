package org.example.streams;
//package org.example.streams;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
//import com.fasterxml.jackson.core.exc.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class fleetConsumer {
//    private static final Logger log = LoggerFactory.getLogger(fleetProducer.class.getSimpleName());

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "172.21.0.2:9092");
//        properties.put(ConsumerConfig.SECURITY_PROVIDERS_CONFIG, "PLAINTEXT");
//        properties.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required    username=\"admin\"    password=\"admin-secret\";");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        properties.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://172.21.0.6:8081");
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

//        properties.put(ConsumerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        KafkaConsumer<String, fleet_mgmt_sensors> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList("telemetry"));

        while (true) {
            final ConsumerRecords<String, fleet_mgmt_sensors> records = consumer.poll(Duration.ofMillis(10));
            for (final ConsumerRecord<String, fleet_mgmt_sensors> record : records) {
//                final String key = record.key();
                final fleet_mgmt_sensors value = record.value();
                System.out.println(value);
            }
        }


    }
}
