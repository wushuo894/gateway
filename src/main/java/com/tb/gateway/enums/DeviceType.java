package com.tb.gateway.enums;

import com.tb.gateway.connectors.base.BaseConfig;
import com.tb.gateway.connectors.base.BaseConnector;
import com.tb.gateway.connectors.modbus.ModbusConnectors;
import com.tb.gateway.connectors.test.TestConnectors;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 设备类型
 */
@AllArgsConstructor
public enum DeviceType {
    MODBUS(ModbusConnectors.class), TEST(TestConnectors.class);
    @Getter
    private Class<? extends BaseConnector<? extends BaseConfig>> connectorClass;
}
