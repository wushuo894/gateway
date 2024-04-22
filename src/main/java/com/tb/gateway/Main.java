package com.tb.gateway;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.tb.gateway.config.ThreadConfig;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.config.Config;
import com.tb.gateway.tb.TbClient;
import com.tb.gateway.utils.GatewayUtil;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Log log = Log.get(Main.class);
    public static final Gson gson = new Gson();

    public static void main(String[] args) {
        GatewayUtil.load();
        TbClient.connect();
        TbClient.subscribe();
        log.info(gson.toJson(Config.GATEWAY_CONFIG));
        Collection<Connector> connectors = Config.CONNECTORS_MAP.values();
        ExecutorService executor = ThreadConfig
                .EXECUTOR;
        for (Connector connector : connectors) {
            executor.submit(() -> {
                try {
                    connector.run();
                } catch (Exception e) {
                    log.error(e, e.getMessage());
                }
            });
        }

        // 三分钟一次 GC
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(3, TimeUnit.MINUTES);
            System.gc();
        });
    }
}
