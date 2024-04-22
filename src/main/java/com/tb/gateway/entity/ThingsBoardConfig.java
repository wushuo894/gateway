package com.tb.gateway.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ThingsBoardConfig {
    private String host;
    private Integer port;
    private String clientId;
    private String userName;
    private String password;
}
