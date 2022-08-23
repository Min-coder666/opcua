package com.min.demo.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DeviceValue {

    private String time;
    private String name;
    private Map<String,Object> timeseries;

    public DeviceValue(){
        this.timeseries = new HashMap<>();
    }

}
