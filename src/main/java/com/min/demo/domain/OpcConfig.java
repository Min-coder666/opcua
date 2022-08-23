package com.min.demo.domain;

import lombok.Data;

import java.util.List;

@Data
public class OpcConfig {

    private String name;
    private String url;
    private Integer timeoutInMillis;
    private Integer scanPeriodInMillis;
    private Boolean disableSubscriptions;
    private Integer subCheckPeriodInMillis;
    private Boolean showMap;
    private String security;
    private String type;
    private String deviceNodePattern;
    private String deviceNamePattern;
    private List<KeyAndPath> attributes;
    private List<KeyAndPath> timeseries;



}
