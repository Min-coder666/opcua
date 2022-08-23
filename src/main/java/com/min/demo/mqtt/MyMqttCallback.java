package com.min.demo.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Map;

public class MyMqttCallback implements MqttCallbackExtended {


    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("=============");
        System.out.println("连接断开");
        System.out.println("=============");

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    @Override
    public void connectComplete(boolean b, String s) {
        System.out.println("=============");
        System.out.println("连接成功");
        System.out.println("=============");
    }


    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,String> map = mapper.readValue("{\"credentialsValue\":\"lqWi5t1TU2QBXY6lcNXm\",\"credentialsType\":\"ACCESS_TOKEN\",\"status\":\"SUCCESS\"}", Map.class);
        System.out.println(map.get("credentialsValue"));
    }
}
