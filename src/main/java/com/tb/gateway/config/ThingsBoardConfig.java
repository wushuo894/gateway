package com.tb.gateway.config;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class ThingsBoardConfig implements Serializable {
    /**
     * 主机IP
     */
    private String host;
    /**
     * 端口
     */
    private Integer port;
    /**
     * 客户端ID
     */
    private String clientId;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 密码
     */
    private String password;
    /**
     * 超时时间
     */
    private Integer timeout;
}
