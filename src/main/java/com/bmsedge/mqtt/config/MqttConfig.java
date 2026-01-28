package com.bmsedge.mqtt.config;

import com.bmsedge.mqtt.service.MqttDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * MQTT Configuration
 * Configures MQTT client and message handling for HiveMQ Cloud
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MqttConfig {

    private final MqttDataService mqttDataService;
    private final MqttProperties mqttProperties;

    /**
     * MQTT Client Factory with SSL support for HiveMQ Cloud
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        try {
            String brokerUrl = mqttProperties.getBroker().getUrl();
            String username = mqttProperties.getBroker().getUsername();
            String password = mqttProperties.getBroker().getPassword();

            options.setServerURIs(new String[]{brokerUrl});
            options.setKeepAliveInterval(mqttProperties.getConnection().getKeepAliveInterval());
            options.setConnectionTimeout(mqttProperties.getConnection().getConnectionTimeout());
            options.setAutomaticReconnect(mqttProperties.getConnection().isAutomaticReconnect());
            options.setCleanSession(mqttProperties.getConnection().isCleanSession());

            // Set credentials
            if (username != null && !username.trim().isEmpty()) {
                options.setUserName(username);
                log.info("🔑 MQTT Username: {}", username);
            }
            if (password != null && !password.trim().isEmpty()) {
                options.setPassword(password.toCharArray());
                log.info("🔑 MQTT Password: [PROTECTED]");
            }

            // For HiveMQ Cloud SSL - trust all certificates (for development)
            if (brokerUrl.startsWith("ssl://")) {
                log.info("🔐 Configuring SSL for HiveMQ Cloud");

                // Create a trust manager that accepts all certificates
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                options.setSocketFactory(sslContext.getSocketFactory());
            }

            factory.setConnectionOptions(options);

            log.info("✅ MQTT Client Factory configured");
            log.info("📡 Broker: {}", brokerUrl);
            log.info("👤 Username: {}", username);
            log.info("📋 Topics: {}", String.join(", ", mqttProperties.getTopics()));

        } catch (Exception e) {
            log.error("❌ Error configuring MQTT client factory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure MQTT", e);
        }

        return factory;
    }

    /**
     * MQTT Input Channel
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT Message Producer (Subscriber)
     */
    @Bean
    public MessageProducer inbound() {
        String[] topics = mqttProperties.getTopics().toArray(new String[0]);

        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                mqttProperties.getBroker().getClientId(),
                mqttClientFactory(),
                topics
        );

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(mqttProperties.getQos());
        adapter.setOutputChannel(mqttInputChannel());

        log.info("✅ MQTT Subscriber configured");
        log.info("🆔 Client ID: {}", mqttProperties.getBroker().getClientId());
        log.info("📊 QoS: {}", mqttProperties.getQos());

        return adapter;
    }

    /**
     * MQTT Message Handler
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttMessageHandler() {
        return message -> {
            try {
                String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
                String payload = (String) message.getPayload();

                log.info("📨 MQTT Message Received!");
                log.info("📋 Topic: {}", topic);
                log.info("📦 Payload: {}", payload);

                // Process message
                mqttDataService.processMqttMessage(payload);

            } catch (Exception e) {
                log.error("❌ Error handling MQTT message: {}", e.getMessage(), e);
            }
        };
    }
}