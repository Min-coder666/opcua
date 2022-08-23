package com.min.demo.netty.websocket;

import com.min.demo.opcua.OpcuaSubConnector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataPushRunner implements Runnable{

    Logger logger = LoggerFactory.getLogger(getClass());


    private boolean stop ;
    private Channel channel;

    public DataPushRunner(){}
    public DataPushRunner(Channel channel) {
        this.stop = false;
        this.channel = channel;
    }

    @Override
    public void run() {
        while (!stop){
            channel.writeAndFlush(new TextWebSocketFrame(OpcuaSubConnector.getNewestData()));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
               logger.error("Error from DataPushRunner",e);
            }
        }
        logger.info("数据推送停止");

    }

    public void stop(){
        this.stop = true;
    }
}
