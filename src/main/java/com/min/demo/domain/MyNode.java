package com.min.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyNode {
    String browserName;
    String nodeId;
    NodeId nodeIdObject;

    public MyNode(UaNode ua){
        this.browserName = ua.getBrowseName().getName();
        this.nodeId  = ua.getNodeId().toParseableString();
        this.nodeIdObject = ua.getNodeId();

    }

}
