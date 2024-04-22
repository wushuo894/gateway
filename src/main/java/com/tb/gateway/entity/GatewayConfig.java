package com.tb.gateway.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 网关配置
 */
@Data
@Accessors(chain = true)
public class GatewayConfig {
    private ThingsBoardConfig thingsBoardConfig;
    private List<DeviceConfig> connectors;
}
