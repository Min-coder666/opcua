package com.min.demo.controller;

import com.min.demo.domain.DeviceValue;
import com.min.demo.opcua.OpcuaSubConnector;
import com.min.demo.service.DeviceValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
public class OpcuaController {

    @Autowired
    DeviceValueService deviceValueService;
    @Autowired
    OpcuaSubConnector opcuaSubConnector;

    @GetMapping("/opcuaData")
    public List<DeviceValue> sendData(){
        return deviceValueService.transport();
    }

    @GetMapping("/testSub")
    public void sub(){
        opcuaSubConnector.open();
    }

}
