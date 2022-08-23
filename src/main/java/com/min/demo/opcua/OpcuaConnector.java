package com.min.demo.opcua;

import com.min.demo.config.OpcUaConnectorConfig;
import com.min.demo.domain.DeviceNode;
import com.min.demo.domain.DeviceValue;
import com.min.demo.service.DeviceValueService;
import lombok.Data;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
@Data
public class OpcuaConnector extends Thread{

    Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    OpcUaConnectorConfig opcUaConnectorConfig;

    @Autowired
    DeviceValueService deviceValueService;
    private OpcUaClient client;
    private boolean connected;
    private boolean stopped;
    private List<DeviceValue> dataToSend;
    private Long lastScanTime;
    private List<String> deviceNodePatternList;
    private List<DeviceNode> subscribedList;

    private ConcurrentHashMap<NodeId,DeviceValue> newestData;
    private void createClient(){
        this.initClient();
        if (client!=null){
            client.disconnect();
            client = null;
        }

        String endPointUrl = opcUaConnectorConfig.getServer().getUrl();
        try {
            Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
            Files.createDirectories(securityTempDir);
            if (!Files.exists(securityTempDir)) {
                throw new Exception("unable to create security dir: " + securityTempDir);
            }

            this.client = OpcUaClient.create(endPointUrl,
                    endpoints ->
                            endpoints.stream()
                                    .filter(e -> e.getSecurityPolicyUri().equals(SecurityPolicy.None.getUri()))
                                    .findFirst(),
                    configBuilder ->
                            configBuilder
                                    .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                                    .setApplicationUri("urn:eclipse:milo:examples:client")
                                    //访问方式
                                    .setIdentityProvider(new AnonymousProvider())
                                    .setRequestTimeout(UInteger.valueOf(opcUaConnectorConfig.getServer().getTimeoutInMillis()))
                                    .setKeepAliveTimeout(UInteger.valueOf(5000))
                                    .build()
            );
        } catch (Exception e) {
            logger.error("opcua client 创建失败.",e);
        }
    }



    private void connect(){
        this.createClient();
        while (!this.connected && !this.stopped){
            try {
                this.client.connect().get();
                this.connected = true;
                this.scanDeviceNodesFromConfig();
            }catch (Exception e){
                logger.error("error on connection to OPC-UA server.",e);
            }

        }


    }
    public void open(){
        this.stopped = false;
        this.start();
        logger.info("Starting OPC-UA Connector");
    }

    private void initClient(){
        this.lastScanTime = System.currentTimeMillis();
        this.dataToSend = new LinkedList<>();
    }

    private void scanDeviceNodesFromConfig(){
        String devicePattern = opcUaConnectorConfig.getMapping().getDeviceNodePattern();
        subscribedList = new ArrayList<>();
        try {
            List<? extends UaNode> nodes = client.getAddressSpace().browseNodes(Identifiers.ObjectsFolder);
            for (UaNode node: nodes) {
                if (Pattern.matches(devicePattern,node.getBrowseName().getName())){
                    subscribedList.add(this.subDetails(node));
                }
            }
        } catch (Exception e) {
            logger.error("扫描配置文件节点错误. ",e);
        }

    }

    private DeviceNode subDetails(UaNode node) throws Exception {
        DeviceNode ret = new DeviceNode();
        ret.setNodeId(node.getNodeId());
        ret.setTimeseries(new HashMap<>());
        List<? extends UaNode> nodes = client.getAddressSpace().browseNodes(node.getNodeId());
        Map<String,String> timeseries = opcUaConnectorConfig.getMapping().getTimeseries();
        for (UaNode n: nodes) {
            if (n.getBrowseName().getName().equals(opcUaConnectorConfig.getMapping().getDeviceNamePattern())){
                ret.setName((String) client.readValue(0.0, TimestampsToReturn.Neither, n.getNodeId()).get().getValue().getValue());
            }else if (timeseries.containsValue(n.getBrowseName().getName())){
                   ret.getTimeseries().put(n.getBrowseName().getName(),n.getNodeId());
            }
        }
        return ret;
    }

    private void scanValue(){
        for (DeviceNode node: subscribedList) {
            DeviceValue deviceValue = new DeviceValue();
            deviceValue.setName(node.getName());
            for (Map.Entry<String, NodeId> entry : node.getTimeseries().entrySet()){
                try {
                    deviceValue.setTime(new Date().toString());
                    deviceValue.getTimeseries().put(entry.getKey(),client.readValue(0.0, TimestampsToReturn.Neither, entry.getValue()).get().getValue().getValue());
                } catch (Exception e) {
                    logger.error("读取节点value错误.",e);
                }
            }
            dataToSend.add(deviceValue);
        }
    }

    private void checkConnection(){
        try{
            if (this.client != null){
                client.getAddressSpace().getNode(Identifiers.ObjectsFolder);
            }else {
                this.connected = false;
            }
        }catch (Exception e){
            this.connected =false;
            logger.error(this.getName()+": opcua连接出错.",e);
        }
    }




    @Override
    public void run() {
        super.run();
        Integer scanPeriodInMillis = this.opcUaConnectorConfig.getServer().getScanPeriodInMillis();
        boolean disable = this.opcUaConnectorConfig.getServer().getDisableSubscriptions();
        int count = 0;
        while (!this.stopped){
            try{
                Thread.sleep(1000);
                count++;
                checkConnection();
                if (!connected && !stopped)
                    this.connect();
                else if (!stopped){
                    if (!disable && System.currentTimeMillis() - lastScanTime >= scanPeriodInMillis){
                        lastScanTime = System.currentTimeMillis();
                        logger.info("{} 进行数据扫描. count = {}",new Timestamp(lastScanTime),count);
                        count = 0;
                        scanValue();
                        if (!dataToSend.isEmpty()){
                            deviceValueService.insertAll(dataToSend);
                            dataToSend.clear();
                        }
//                        while (!dataToSend.isEmpty()){
//                            deviceValueService.insertOne(dataToSend.poll());
//                        }

                    }
                }else{
                    this.close();
                }
            }catch (Exception e){
                logger.error("循环扫描出错." ,e);
            }
        }
    }

    private void close(){
        this.stopped = true;
        if (this.client!=null)
            try {
                client.disconnect();
            }catch (Exception e){

            }
        this.connected = false;
        logger.info("{} has been stopped.",this.getName());
    }

//    private void showData(DeviceValue value){
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append(value.getName());
//        stringBuilder.append(" get value in ");
//        stringBuilder.append(new DateTime(value.getTime()));
//        stringBuilder.append(", value is ");
//        for (Map.Entry<String, Object> entry : value.getTimeseries().entrySet()){
//            stringBuilder.append(entry.getKey()+" : "+ entry.getValue()+" ");
//        }
//        System.out.println(stringBuilder);
//    }
}
