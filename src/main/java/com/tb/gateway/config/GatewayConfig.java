package com.tb.gateway.config;

import cn.hutool.log.level.Level;
import com.tb.gateway.connectors.base.BaseConfig;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 网关配置
 */
@Data
@Accessors(chain = true)
public class GatewayConfig implements Serializable {
    private String username;
    private String password;
    private Integer port;
    private Boolean web;
    private ThingsBoardConfig thingsboard;
    private List<BaseConfig> connectors;
    private ThreadConfig thread;
    private Level logLevel;
}
