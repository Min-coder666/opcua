package com.min.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.demo.config.DeviceTokenConfig;
import com.min.demo.domain.DeviceValue;
import com.min.demo.domain.MyNode;
import com.min.demo.opcua.InitAttr;
import com.min.demo.opcua.OpcUaClientService;
import com.min.demo.opcua.OpcuaConnector;
import com.min.demo.opcua.OpcuaSubConnector;
import com.min.demo.service.DeviceValueService;
import com.min.demo.service.MqttOpcuaService;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
public class HomeController {

    Logger logger = LoggerFactory.getLogger(getClass());

    private OpcUaClient client = null;
    @Autowired
    private OpcUaClientService opcUaClientService;

    @Autowired
    private MqttOpcuaService mqttOpcuaService;

    @Autowired
    DeviceTokenConfig deviceTokenConfig;

    @Autowired
    OpcuaConnector connector;
    @Autowired
    OpcuaSubConnector subConnector;

    @Autowired
    DeviceValueService deviceValueService;
    private List<MyNode> nodes;

    @GetMapping("/")
    public String homepage(){
        return "opcua_data";
    }

    @GetMapping("/connector")
    @ResponseBody
    public void testConnector(){
         subConnector.open();
    }


    @GetMapping("/getNodes")
    @ResponseBody
    public List<MyNode> getNodes() {
        try {
            if (client == null)
                client = opcUaClientService.createClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.nodes = opcUaClientService.getNodes(client);
        return this.nodes;
    }

    @PostMapping("/connectFirst")
    @ResponseBody
    public void connectFirst(@RequestBody String jsonString){

        ObjectMapper mapper = new ObjectMapper();
        InitAttr attr;
        try {
            if (client == null){
                client = opcUaClientService.createClient();
                logger.info("opcuaClient已创建");
            }
            attr = mapper.readValue(jsonString, InitAttr.class);
            mqttOpcuaService.connectDevice(attr,this.nodes,client);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
