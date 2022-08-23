package com.min.demo.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Test {


    public static void main(String[] args) throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("provision");

        MqttClient client = new MqttClient("tcp://127.0.0.1:1883","88887");
        client.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable throwable) {
                System.out.println("=============");
                System.out.println("连接断开");
                System.out.println("=============");
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                System.out.println("=============");
                System.out.println("get message from topic:" + s);
                System.out.println("message: "+new String(mqttMessage.getPayload()));
                System.out.println("=============");
                client.close();
                client.disconnect();

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        client.connect(options);
        System.out.println("已连接");

        Map<String,String> map = new HashMap<>();
        map.put("deviceName","auto_created_java2222");
        map.put("provisionDeviceKey","q7k1l3ps2gy9ehxs90e2");
        map.put("provisionDeviceSecret","1umlwpgri0gz88065baz");

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
        System.out.println("发布消息");


    }
}
