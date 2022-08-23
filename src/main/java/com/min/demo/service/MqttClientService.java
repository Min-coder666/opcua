package com.min.demo.service;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MqttClientService {

    Logger logger = LoggerFactory.getLogger(getClass());

    public void upload(byte[] payload, MqttClient mqttClient){
        MqttMessage message = new MqttMessage(payload);
        message.setQos(0);
        try {
            mqttClient.publish("v1/devices/me/telemetry",message);
            logger.info("数据上传请求已发送，数据：{}",new String(payload));
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

    }



    public static void main(String[] args) throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("A1_TEST_TOKEN");
        options.setConnectionTimeout(120);
        options.setKeepAliveInterval(20);

        MqttClient client = new MqttClient("tcp://127.0.0.1:1883","clientID");
        client.connect(options);

        String meg = "{temp:99}";


        MqttMessage message = new MqttMessage(meg.getBytes());
        message.setQos(0);
        client.publish("v1/devices/me/telemetry",message);
        client.disconnect();
        client.close();
    }
}
