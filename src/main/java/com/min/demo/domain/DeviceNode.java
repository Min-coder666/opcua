package com.min.demo.domain;

import lombok.Data;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import java.util.Map;

@Data
public class DeviceNode {
    private NodeId nodeId;
    private String name;
    private Map<String, NodeId> timeseries;
}
