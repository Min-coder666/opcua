package com.min.demo.opcua;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.demo.config.OpcUaConnectorConfig;
import com.min.demo.domain.DeviceNode;
import com.min.demo.domain.DeviceValue;
import com.min.demo.service.DeviceValueService;
import lombok.Data;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

@Component
public class OpcuaSubConnector extends Thread{

    Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    OpcUaConnectorConfig opcUaConnectorConfig;

    @Autowired
    DeviceValueService deviceValueService;
    private OpcUaClient client;
    private boolean connected;
    private boolean stopped;
    private Long lastScanTime;
    private List<ReadValueId> toSubscribeList;
    private UaSubscription subscription;
    private static ConcurrentHashMap<NodeId,DeviceValue> newestData;
    private HashMap<NodeId,ParentMsg> childToParent;

    private SimpleDateFormat simpleDateFormat;
    private static ObjectMapper mapper;

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
        this.toSubscribeList = new ArrayList<>();
        this.childToParent = new HashMap<>();
        this.newestData = new ConcurrentHashMap<>();
        this.simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        mapper = new ObjectMapper();
    }

    private void scanDeviceNodesFromConfig(){
        String devicePattern = opcUaConnectorConfig.getMapping().getDeviceNodePattern();
        try {
            List<? extends UaNode> nodes = client.getAddressSpace().browseNodes(Identifiers.ObjectsFolder);
            for (UaNode node: nodes) {
                if (Pattern.matches(devicePattern,node.getBrowseName().getName())){
                    this.subDetails(node);
                }
            }
            if (subscription == null)
                subscription = client.getSubscriptionManager().createSubscription(500.0).get();
            for (ReadValueId rvi: toSubscribeList) {
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
                        (item, id)-> item.setValueConsumer((it, val) ->{
                            ParentMsg msg = childToParent.get(it.getReadValueId().getNodeId());
                            newestData.get(msg.parent).getTimeseries().put(msg.key,val.getValue().getValue());
                            newestData.get(msg.parent).setTime(simpleDateFormat.format(new Date()));
                        })
                );
            }
        } catch (Exception e) {
            logger.error("扫描配置文件节点错误. ",e);
        }

    }

    private void subDetails(UaNode node) throws Exception {

        NodeId parent = node.getNodeId();
        DeviceValue value = new DeviceValue();
        List<? extends UaNode> nodes = client.getAddressSpace().browseNodes(node.getNodeId());
        Map<String,String> timeseries = opcUaConnectorConfig.getMapping().getTimeseries();
        for (UaNode n: nodes) {
            String browseName = n.getBrowseName().getName();
            if (browseName.equals(opcUaConnectorConfig.getMapping().getDeviceNamePattern())){
                value.setName((String) client.readValue(0.0, TimestampsToReturn.Neither, n.getNodeId()).get().getValue().getValue());
            }else if (timeseries.containsValue(browseName)){
                value.getTimeseries().put(browseName,null);
                childToParent.put(n.getNodeId(),new ParentMsg(parent,browseName));
                toSubscribeList.add(
                        new ReadValueId(n.getNodeId(),
                                AttributeId.Value.uid(),
                                null,
                                QualifiedName.NULL_VALUE));
            }
        }
        newestData.put(parent,value);
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


    public static String getNewestData(){
        String ret = "";
        try {
            ret = mapper.writeValueAsString(new ArrayList<>(newestData.values()));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }


    @Override
    public void run() {
        Integer scanPeriodInMillis = this.opcUaConnectorConfig.getServer().getScanPeriodInMillis();
        boolean disable = this.opcUaConnectorConfig.getServer().getDisableSubscriptions();
        while (!this.stopped){
            try{
                Thread.sleep(1000);
                checkConnection();
                if (!connected && !stopped)
                    this.connect();
                else if (!stopped){
//                    if (!disable && System.currentTimeMillis() - lastScanTime >= scanPeriodInMillis){
//                        lastScanTime = System.currentTimeMillis();
//                        logger.info("{} data is {}",new Timestamp(lastScanTime),getNewestData());
//
//                    }
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

    class ParentMsg{
        NodeId parent;
        String key;

        public ParentMsg(NodeId parent, String key) {
            this.parent = parent;
            this.key = key;
        }
    }

}

