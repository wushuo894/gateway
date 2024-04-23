package com.tb.gateway.enums;

import com.tb.gateway.connectors.base.BaseConfig;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.connectors.modbus.ModbusConnectors;
import com.tb.gateway.connectors.test.TestConnectors;
import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
public enum DeviceType {
    MODBUS(ModbusConnectors.class), TEST(TestConnectors.class);
    @Getter
    private Class<? extends Connector<? extends BaseConfig>> connectorClass;
}
