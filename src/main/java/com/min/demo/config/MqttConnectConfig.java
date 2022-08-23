package com.min.demo.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt")
@PropertySource("classpath:mqtt.properties")
public class MqttConnectConfig {
    String host;
    String port;
    String provisionDeviceKey;
    String provisionDeviceSecret;
}
