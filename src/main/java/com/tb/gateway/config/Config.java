package com.tb.gateway.config;

import cn.hutool.core.map.BiMap;
import com.tb.gateway.connectors.base.BaseConfig;
import com.tb.gateway.connectors.base.BaseConnector;

import java.util.HashMap;

public class Config {
    public static GatewayConfig GATEWAY_CONFIG;
    public static ThingsBoardConfig THINGS_BOARD_CONFIG;
    public static BiMap<BaseConfig, BaseConnector<? extends BaseConfig>> CONNECTORS_MAP = new BiMap<>(new HashMap<>());
}
