package com.example.blesensreader;

import java.util.HashMap;

public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String TEMP_MEASUREMENT = "00002a1c-0000-1000-8000-00805f9b34fb";
    public static String HUMIDITY_MEASUREMENT = "00002a6f-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("00000002-0000-0000-FDFD-FDFDFDFDFDFD", "Unknown Service");
        //attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(TEMP_MEASUREMENT, "Temperature Measurement");
        attributes.put(HUMIDITY_MEASUREMENT, "Humidity Measurement");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
