package org.example.streams;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

public class TemperatureTimestampExtractor implements TimestampExtractor {
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long prevTime) {
        return ((fleet_mgmt_sensors)record.value()).getTs().toEpochMilli();
    }
}
