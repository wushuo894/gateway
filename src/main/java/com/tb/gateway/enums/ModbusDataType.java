package com.tb.gateway.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Modbus数据类型
 */
@AllArgsConstructor
public enum ModbusDataType {
    INT16(2), INT32(4), STRING(8);
    @Getter
    private Integer length;
}
