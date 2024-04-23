package com.tb.gateway.connectors.modbus;

import com.tb.gateway.connectors.base.BaseConfig;
import com.tb.gateway.enums.ModbusDataType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class ModbusConfig extends BaseConfig {
    /**
     * 从机地址
     */
    private Integer unitId;

    /**
     * 设备 /dev/ttysWK1
     */
    private String port;

    /**
     * 波特率
     */
    private Integer baudrate;

    /**
     * 数据位
     */
    private Integer databits;

    /**
     * 停止位
     */
    private Integer stopbits;

    /**
     * 校验
     */
    private String parity;

    /**
     * 遥测数据
     */
    private List<ModbusInfo> timeseries;

    /**
     * rpc 数据
     */
    private List<ModbusInfo> rpc;

    /**
     * 共享配置
     */
    private List<ModbusInfo> attributeUpdates;


    @Data
    @Accessors(chain = true)
    public static class ModbusInfo {
        private String tag;
        private ModbusDataType type;
        private Integer functionCode;
        private Integer objectsCount;
        private Integer address;
    }

}
