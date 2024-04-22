package com.tb.gateway.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class ThingsBoardConfig implements Serializable {
    private String host;
    private Integer port;
    private String clientId;
    private String userName;
    private String password;
    private Integer timeout;
}
