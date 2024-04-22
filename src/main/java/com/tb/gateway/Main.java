package com.tb.gateway;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.entity.Config;
import com.tb.gateway.tb.TbClient;
import com.tb.gateway.util.GatewayUtil;

import java.util.Collection;

public class Main {
    private static final Log log = Log.get(Main.class);
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        GatewayUtil.load();
        TbClient.connect();
        TbClient.subscribe();
        log.info(gson.toJson(Config.GATEWAY_CONFIG));
        Collection<Connector> connectors = Config.CONNECTORS_MAP.values();
        for (Connector connector : connectors) {
            ThreadUtil
                    .execute(() -> {
                        try {
                            connector.run();
                        } catch (Exception e) {
                            log.error(e, e.getMessage());
                        }
                    });
        }
    }
}
