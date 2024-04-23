package com.tb.gateway.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.lang.model.type.TypeKind;
import java.lang.reflect.Type;
import java.net.Proxy;

/**
 * Modbus数据类型
 */
@AllArgsConstructor
public enum ModbusDataType {
    INT16(2), INT32(4), STRING(8);
    @Getter
    private Integer length;
}
