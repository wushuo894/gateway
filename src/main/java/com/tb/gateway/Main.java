package com.tb.gateway;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.dialect.console.ConsoleColorLogFactory;
import cn.hutool.log.dialect.console.ConsoleLog;
import cn.hutool.log.level.Level;
import com.tb.gateway.config.Config;
import com.tb.gateway.config.ThreadConfig;
import com.tb.gateway.connectors.base.BaseConfig;
import com.tb.gateway.connectors.base.BaseConnector;
import com.tb.gateway.tb.TbClient;
import com.tb.gateway.utils.GatewayUtil;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    static {
        ConsoleLog.setLevel(Level.INFO);
        LogFactory.setCurrentLogFactory(new ConsoleColorLogFactory());
    }

    private static final Log log = Log.get(Main.class);

    public static void main(String[] args) {
        GatewayUtil.load();
        TbClient.connect();
        TbClient.subscribe();
        Collection<BaseConnector<? extends BaseConfig>> baseConnectors = Config.CONNECTORS_MAP.values();
        ExecutorService executor = ThreadConfig
                .EXECUTOR;
        for (BaseConnector<? extends BaseConfig> baseConnector : baseConnectors) {
            executor.submit(() -> {
                try {
                    baseConnector.run();
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
