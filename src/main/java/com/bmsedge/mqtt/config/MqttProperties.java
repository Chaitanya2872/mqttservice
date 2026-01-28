package com.bmsedge.mqtt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MQTT Configuration Properties
 * Binds mqtt.* properties from application.yml
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    private BrokerConfig broker = new BrokerConfig();
    private List<String> topics;
    private int qos = 1;
    private ConnectionConfig connection = new ConnectionConfig();

    @Data
    public static class BrokerConfig {
        private String url;
        private String username;
        private String password;
        private String clientId;
    }

    @Data
    public static class ConnectionConfig {
        private int keepAliveInterval = 60;
        private int connectionTimeout = 30;
        private boolean automaticReconnect = true;
        private boolean cleanSession = false;
    }
}