package com.tb.gateway.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.BiMap;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.tb.gateway.config.ThreadConfig;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.config.Config;
import com.tb.gateway.connectors.base.BaseConfig;
import com.tb.gateway.config.GatewayConfig;
import com.tb.gateway.enums.DeviceType;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class GatewayUtil {
    private static final Log log = Log.get(GatewayUtil.class);

    public static void load() {
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

        GatewayConfig gatewayConfig =
                gson.fromJson(FileUtil.readUtf8String(tbGatewayConfig.get()), GatewayConfig.class);
        List<BaseConfig> baseConfigList = gatewayConfig.getConnectors();

        Config.THINGS_BOARD_CONFIG = gatewayConfig.getThingsboard();
        ThreadConfig thread = ObjUtil.defaultIfNull(gatewayConfig.getThread(), new ThreadConfig());
        thread.init();
        gatewayConfig.setThread(thread);

        Map<String, ? extends Class<?>> collect = ClassUtil.scanPackage("com.tb.gateway")
                .stream()
                .collect(Collectors.toMap(c -> c.getSimpleName().toUpperCase(), m -> m));

        BiMap<BaseConfig, Connector> connectorsMap = Config.CONNECTORS_MAP;

        baseConfigList = baseConfigList.stream()
                .map(config -> {
                    DeviceType deviceType = config.getDeviceType();
                    String name = deviceType.name();
                    Class<?> configClass = collect.get((name + "Config").toUpperCase());
                    Class<?> connectorsClass = collect.get((name + "Connectors").toUpperCase());
                    if (Objects.isNull(configClass)) {
                        return config;
                    }

                    String s = FileUtil.readUtf8String(new File("config" + File.separator + config.getFileName()));

                    BaseConfig baseConfig = (BaseConfig) gson.fromJson(s, configClass);
                    baseConfig.setDeviceName(config.getDeviceName());
                    Connector connector = (Connector) ReflectUtil.newInstance(connectorsClass);
                    connector.setBaseConfig(baseConfig);

                    connectorsMap.put(baseConfig, connector);
                    return baseConfig;
                }).collect(Collectors.toList());

        Config.GATEWAY_CONFIG = gatewayConfig.setConnectors(baseConfigList);
    }
}
