package com.tb.gateway.entity;

import cn.hutool.core.map.BiMap;
import com.tb.gateway.connectors.base.Connector;

import java.util.HashMap;

public class Config {
    public static GatewayConfig GATEWAY_CONFIG;
    public static ThingsBoardConfig THINGS_BOARD_CONFIG;
    public static BiMap<DeviceConfig, Connector> CONNECTORS_MAP = new BiMap<>(new HashMap<>());
}
