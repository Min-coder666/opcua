package com.min.demo;

import com.min.demo.config.DeviceTokenConfig;
import com.min.demo.config.OpcUaConnectorConfig;
import com.min.demo.domain.DeviceNode;
import com.min.demo.domain.DeviceValue;
import com.min.demo.opcua.OpcUaClientService;
import com.min.demo.opcua.OpcuaConnector;
import com.min.demo.opcua.OpcuaSubConnector;
import com.min.demo.service.DeviceValueService;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@SpringBootTest
class DemoApplicationTests {

	@Autowired
	DeviceTokenConfig deviceTokenConfig;
	@Autowired
	OpcUaConnectorConfig opcUaConnectorConfig;

	@Autowired
	OpcUaClientService opcUaClientService;

	@Autowired
	OpcuaConnector opcuaConnector;
	@Autowired
	OpcuaSubConnector opcuaSubConnector;

	@Autowired
	DeviceValueService deviceValueService;
	Logger logger = LoggerFactory.getLogger(getClass());
	@Test
	void contextLoads() {
		opcuaSubConnector.open();
	}

}
