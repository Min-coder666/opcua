package com.min.demo.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "device")
@PropertySource("classpath:device.properties")
public class DeviceTokenConfig {
    private List<Config> configs;
    @Data
    public static class Config{
        String name;
        String token;
    }
}
