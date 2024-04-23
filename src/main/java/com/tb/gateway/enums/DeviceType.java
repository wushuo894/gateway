package com.tb.gateway.enums;

import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.connectors.modbus.ModbusConnectors;
import com.tb.gateway.connectors.test.TestConnectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DeviceType {
    MODBUS(ModbusConnectors.class), TEST(TestConnectors.class);
    private Class<? extends Connector> connectorClass;
}
