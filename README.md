OPCUA 
-------------

数据读取: Milo框架<br>
前端展示: Netty.WebSocket

## 数据读取
### 配置文件
application.yml
```
opcua:
  server:
    name: OPC-UA Default Server  
    url: opc.tcp://0.0.0.0:4840
    timeoutInMillis: 5000
    scanPeriodInMillis: 1000
    disableSubscriptions: false
    subCheckPeriodInMillis: 100
    showMap: false
    security: Basic123Rsa15
  mapping:
    #设备节点相对路径 默认在 Root/Object/ 下
    deviceNodePattern: Device[0-9]+  
    #设备名字节点相对路径 默认在设备节点下
    deviceNamePattern: Name
    #设备遥测数据节点相对路径 默认在设备节点下
    timeseries: {Temperature: Temperature,Humidity: Humidity,BatteryLevel: BatteryLevel}
```

### 数据订阅

     if (subscription == null)
        subscription = client.getSubscriptionManager()
                                        .createSubscription(500.0)
                                        .get();
     for(ReadValueId rvi: toSubscribeList) {
        subscription.createMonitoredItems(
            TimestampsToReturn.Both,
            newArrayList(new MonitoredItemCreateRequest(
                rvi,
                MonitoringMode.Reporting,
                new MonitoringParameters(
                    subscription.nextClientHandle(),
                        1000.0,     // sampling interval
                        null,       // filter, null means use default
                        uint(10),   // queue size
                        true        // discard oldest
                )
                )),
                // 设置订阅数据发生变更时操作
                (item, id)-> item.setValueConsumer((it, val) ->{
                    ParentMsg msg = childToParent.get(it.getReadValueId().getNodeId());
                    newestData.get(msg.parent).getTimeseries().put(msg.key,val.getValue().getValue());
                    newestData.get(msg.parent).setTime(simpleDateFormat.format(new Date()));
                    })
        );
    }

### 实时数据获取接口
        public static String getNewestData(){
            String ret = "";
            try {
                ret = mapper.writeValueAsString(new ArrayList<>(newestData.values()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ret;
        }

## 数据展示
### Netty服务端
Springboot官方整合的WebSocket是javax.websocket。<br>
Netty性能更好，即采用Netty作为WebSocket服务端，暂未于springboot整合。

    @SpringBootApplication
    public class DemoApplication {
        public static void main(String[] args) {
            SpringApplication.run(DemoApplication.class, args);
            NettyServer server = new NettyServer();
            server.start();
        }
    }

#### 服务端数据推送
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

#### 前端JS
    getData(){
        if (!window.WebSocket){
                alert("当前浏览器不支持websocket");
            }else {
                socket = new WebSocket("ws://127.0.0.1:9998/pushData");
                socket.onopen = this.onopen
                // 设置关闭连接的方法
                socket.onclose = this.onclose
                // 设置接收数据的方法
                socket.onmessage = this.onmessage
            }
        },
        onopen(ev){
            this.ws_status = "已经连接到ws服务器";
        },
        onclose(){
            this.ws_status = "ws服务器连接已断开";
        },
        onmessage(ev){
            this.tableData = JSON.parse(ev.data)
        }
    }



### 问题
 *数据更新 数据格式转换 消耗时间*