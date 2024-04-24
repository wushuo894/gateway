package com.tb.gateway.connectors.base;

import com.tb.gateway.enums.DeviceType;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 设备配置
 */
@Data
@Accessors(chain = true)
public class BaseConfig implements Serializable {
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
    private BaseConnector<? extends BaseConfig> baseConnector;
}
