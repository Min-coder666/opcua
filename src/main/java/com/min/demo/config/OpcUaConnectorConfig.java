package com.min.demo.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "opcua")
public class OpcUaConnectorConfig {

    private Server server;
    private Mapping mapping;


    @Data
    public static class Mapping{
        private String deviceNamePattern;
        private String deviceNodePattern;
        private Map<String,String> timeseries;
    }
    @Data
    public static class Server{
        private String name;
        private String url;
        private Integer timeoutInMillis;
        private Integer scanPeriodInMillis;
        private Integer subCheckPeriodInMillis;
        private Boolean disableSubscriptions;
        private Boolean showMap;
        private String security;
    }



}
