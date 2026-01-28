package com.bmsedge.mqtt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MQTT Service Application
 * Main entry point for MQTT data collection service
 */
@SpringBootApplication
public class MqttServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqttServiceApplication.class, args);
        System.out.println("\n" +
                "==============================================\n" +
                "   MQTT Service Started Successfully! \n" +
                "==============================================\n" +
                "   Listening for MQTT messages...\n" +
                "   REST API: http://localhost:8090/api/mqtt-data\n" +
                "==============================================\n");
    }
}