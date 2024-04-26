package com.tb.gateway.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.BiMap;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.TypeUtil;
import cn.hutool.log.Log;
import cn.hutool.log.dialect.console.ConsoleLog;
import com.google.gson.Gson;
import com.tb.gateway.config.Config;
import com.tb.gateway.config.GatewayConfig;
import com.tb.gateway.config.ThreadConfig;
import com.tb.gateway.connectors.base.BaseConfig;
import com.tb.gateway.connectors.base.BaseConnector;
import com.tb.gateway.enums.DeviceType;
import com.tb.gateway.tb.TbClient;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 网关工具
 */
public class GatewayUtil {
    public static final Gson gson = new Gson();

    /**
     * 加载
     */
    public static void load() {
        Log log = Log.get(GatewayUtil.class);
        Gson gson = new Gson();

        File configDir = new File("config");
        FileUtil.mkdir(configDir);
        File[] configs = FileUtil.ls(configDir.getAbsolutePath());
        Optional<File> tbGatewayConfig = Arrays.stream(configs)
                .filter(config -> List.of("json", "json5").contains(FileUtil.getSuffix(config)))
                .filter(config -> "tb_gateway".equals(FileUtil.getPrefix(config)))
                .findFirst();
        if (tbGatewayConfig.isEmpty()) {
            log.error("没有找到配置文件 tb_gateway.json");
            System.exit(1);
            return;
        }
        File configFile = tbGatewayConfig.get();

        log.info("load config: {}", configFile.getName());

        GatewayConfig gatewayConfig =
                gson.fromJson(FileUtil.readUtf8String(tbGatewayConfig.get()), GatewayConfig.class);
        List<BaseConfig> baseConfigList = gatewayConfig.getConnectors();

        ConsoleLog.setLevel(gatewayConfig.getLogLevel());

        Config.THINGS_BOARD_CONFIG = gatewayConfig.getThingsboard();
        ThreadConfig thread = ObjUtil.defaultIfNull(gatewayConfig.getThread(), new ThreadConfig());
        thread.init();
        gatewayConfig.setThread(thread);

        BiMap<BaseConfig, BaseConnector<? extends BaseConfig>> connectorsMap = Config.CONNECTORS_MAP;

        baseConfigList = baseConfigList.stream()
                .filter(Objects::nonNull)
                .map(config -> {
                    DeviceType deviceType = config.getDeviceType();
                    Class<?> connectorsClass = deviceType.getConnectorClass();
                    if (Objects.isNull(connectorsClass)) {
                        return config;
                    }

                    log.info("load connectorsClassName: {}", connectorsClass.getName());

                    String s = FileUtil.readUtf8String(new File("config" + File.separator + config.getFileName()));

                    BaseConnector<? extends BaseConfig> baseConnector = (BaseConnector<? extends BaseConfig>) ReflectUtil.newInstance(connectorsClass);

                    Class<?> returnClass =
                            ClassUtil.loadClass(TypeUtil.getGenerics(connectorsClass)[0].getActualTypeArguments()[0].getTypeName());

                    BaseConfig baseConfig = (BaseConfig) gson.fromJson(s, returnClass);
                    baseConfig.setDeviceName(config.getDeviceName());
                    baseConnector.setConfig(baseConfig);

                    connectorsMap.put(baseConfig, baseConnector);
                    return baseConfig;
                }).collect(Collectors.toList());

        Config.GATEWAY_CONFIG = gatewayConfig.setConnectors(baseConfigList);
    }

    /**
     * 启动
     */
    public static void start() {
        Log log = Log.get(GatewayUtil.class);
        GatewayUtil.load();
        TbClient.connect();
        TbClient.subscribe();
        log.info(gson.toJson(Config.GATEWAY_CONFIG));
        Collection<BaseConnector<? extends BaseConfig>> connectors = Config.CONNECTORS_MAP.values();
        ExecutorService executor = ThreadConfig
                .EXECUTOR;
        for (BaseConnector<? extends BaseConfig> connector : connectors) {
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
