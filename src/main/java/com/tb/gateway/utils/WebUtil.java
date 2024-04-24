package com.tb.gateway.utils;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.server.SimpleServer;
import com.tb.gateway.action.RootAction;
import com.tb.gateway.config.Config;
import com.tb.gateway.config.GatewayConfig;

public class WebUtil {
    public static void start() {
        GatewayConfig gatewayConfig = Config.GATEWAY_CONFIG;
        Integer port = gatewayConfig.getPort();

        ThreadUtil.execute(()->{
            SimpleServer server = new SimpleServer(port);
            server.addAction("/", new RootAction())
                    .start();
        });
    }
}
