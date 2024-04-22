package com.tb.gateway.entity;

import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.connectors.base.Converter;
import com.tb.gateway.eum.DeviceType;
import lombok.Data;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 设备配置
 */
@Data
@Accessors(chain = true)
public class DeviceConfig implements Serializable {
    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 设备Id
     */
    private String deviceId;
    /**
     * 设备类型
     */
    private DeviceType deviceType;
    /**
     * 文件
     */
    private String fileName;
    /**
     * 设备连接
     */
    @Setter
    private Connector connector;
    /**
     * 数据转换
     */
    private Converter<?, ?> converter;
}
