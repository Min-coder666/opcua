package com.min.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.demo.config.DeviceTokenConfig;
import com.min.demo.config.MqttConnectConfig;
import com.min.demo.domain.MyNode;
import com.min.demo.opcua.InitAttr;
import com.min.demo.opcua.OpcUaClientService;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class MqttOpcuaService {

    Logger logger = LoggerFactory.getLogger(getClass());
    private MqttClient uploadClient;
    InitAttr attr;
    @Autowired
    DeviceTokenConfig deviceTokenConfig;
    @Autowired
    MqttConnectConfig mqttConnectConfig;

    @Autowired
    private OpcUaClientService opcUaClientService;

    public void connectDevice(InitAttr attr, List<MyNode> nodes, OpcUaClient client) throws MqttException {
        this.attr = attr;
        String deviceName ="";
        String devicePattern = attr.getDeviceName();
        NodeId nameID;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getBrowserName().equals(devicePattern)){
                nameID = nodes.get(i).getNodeIdObject();
                deviceName = (String) (opcUaClientService.readValue(client,nameID).getValue().getValue());
                break;
            }
        }
        logger.info("deviceName: {}",deviceName);

        String token = "";
        List<DeviceTokenConfig.Config> list = deviceTokenConfig.getConfigs();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(deviceName)){
                token = list.get(i).getToken();
                break;
            }
        }
        if (token.equals("")){
            createDevice(deviceName,client);
        }else {
            logger.info("connected by config token:{}",token);
            connectTB(token,client);
        }
    }

    private void connectTB(String token,OpcUaClient client) throws MqttException {

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(token);
        String uri = "tcp://"+mqttConnectConfig.getHost()+":"+mqttConnectConfig.getPort();

        uploadClient = new MqttClient(uri,"upload");
        uploadClient.connect(options);
        logger.info("thingsboardMqttClient 已连接.Uri:{}, token:{}",uri,token);
        opcUaClientService.run(client,uploadClient,attr);
    }
    private void createDevice(String deviceName,OpcUaClient opcClient) throws MqttException{
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("provision");
        String uri = "tcp://"+mqttConnectConfig.getHost()+":"+mqttConnectConfig.getPort();

        MqttClient client = new MqttClient(uri,"provisionClient");
//        MqttClient client = new MqttClient(uri,"123456",new MemoryPersistence());
        client.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable throwable) {
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

                logger.info("get message from topic:{}" ,s);
                String response = new String(mqttMessage.getPayload());
                logger.info("message: {}",response);

                ObjectMapper mapper = new ObjectMapper();
                Map<String,String> map = mapper.readValue(response,Map.class);
                logger.info("设备已创建，设备token:{}",map.get("credentialsValue"));
                connectTB(map.get("credentialsValue"),opcClient);
                client.close();
                client.disconnect();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        client.connect(options);
        logger.info("mqtt设备配置连接 已连接");

        Map<String,String> map = new HashMap<>();
        map.put("deviceName",deviceName);
        map.put("provisionDeviceKey",mqttConnectConfig.getProvisionDeviceKey());
        map.put("provisionDeviceSecret",mqttConnectConfig.getProvisionDeviceSecret());

        ObjectMapper mapper = new ObjectMapper();
        byte[] json;
        try {
            json = mapper.writeValueAsBytes(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        MqttMessage message = new MqttMessage(json);
        message.setQos(0);
        client.publish("/provision/request",message);
        logger.info("设备声明请求已发送，设备名称：{}",map.get(deviceName));
    }
}


