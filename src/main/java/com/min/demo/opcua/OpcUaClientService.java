package com.min.demo.opcua;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.demo.domain.MyNode;
import com.min.demo.service.MqttClientService;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


@Service
public class OpcUaClientService {
    Logger logger = LoggerFactory.getLogger(getClass());

    private List<MyNode> nodes;

    @Autowired
    private MqttClientService mqttClientService;


    public void run(OpcUaClient client, MqttClient mqttClient, InitAttr attr){
        logger.info("正在监测opuca连接 数据扫描间隔5000ms");
        Map<String,NodeId> nodeIdMap = new HashMap<>();
        String[] timeseries = attr.getTimeseries();
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < timeseries.length; j++) {
                if (nodes.get(i).getBrowserName().equals(timeseries[j]))
                    nodeIdMap.put(timeseries[j],nodes.get(i).getNodeIdObject());
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        Map<String,String> data = new HashMap<>();
        while (true){
            try {

                for (Map.Entry<String, NodeId> entry : nodeIdMap.entrySet()) {
                    String dataNow = String.valueOf(client.readValue(0.0, TimestampsToReturn.Neither, entry.getValue()).get().getValue().getValue());
                    data.put(entry.getKey(),dataNow);
                }

                byte[] json = mapper.writeValueAsBytes(data);
                mqttClientService.upload(json,mqttClient);
                Thread.sleep(5000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<MyNode> getNodes(OpcUaClient opcUaClient) {
        List<MyNode> myNodeList = new ArrayList<>();;
        try {
            opcUaClient.connect().get();
            UaNode uaNode = opcUaClient.getAddressSpace().getNode(new NodeId(2,1));
            List<UaNode> list = new ArrayList<>();
            this.browseNode(opcUaClient,uaNode,list);
            for (UaNode ua: list) {
                myNodeList.add(new MyNode(ua));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

//        Main.readValue(opcUaClient);

        nodes = myNodeList;
        return myNodeList;
     }


    public OpcUaClient createClient() throws Exception {
        //opc ua服务端地址
        final  String endPointUrl = "opc.tcp://0.0.0.0:4840";
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security dir: " + securityTempDir);
        }
        return OpcUaClient.create(endPointUrl,
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
                                .setRequestTimeout(UInteger.valueOf(5000))
                                .build()
        );
    }

    public DataValue readValue(OpcUaClient client ,NodeId nodeId) {
        DataValue value = null;
        try {

            //读取节点数据
            value = client.readValue(0.0, TimestampsToReturn.Neither, nodeId).get();
            //标识符
            String identifierName = String.valueOf(nodeId.getIdentifier());
            System.out.println(identifierName + ": " + String.valueOf(value.getValue().getValue()));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * 遍历树形节点
     *
     * @param client OPC UA客户端
     * @param uaNode 节点
     * @throws Exception
     */
    public void browseNode(OpcUaClient client, UaNode uaNode,List<UaNode> list) throws Exception {
        List<? extends UaNode> nodes;
        if (uaNode == null) {
            nodes = client.getAddressSpace().browseNodes(Identifiers.ObjectsFolder);
        } else {
            list.add(uaNode);
            nodes = client.getAddressSpace().browseNodes(uaNode);
        }
        for (UaNode nd : nodes) {
            //排除系统行性节点，这些系统性节点名称一般都是以"_"开头
            if (Objects.requireNonNull(nd.getBrowseName().getName()).contains("_")) {
                continue;
            }
            browseNode(client, nd,list);
        }
    }

}
