package com.min.demo.opcua;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitAttr {

    private String deviceName;
    private String[] timeseries;
}
