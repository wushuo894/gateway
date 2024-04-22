package com.tb.gateway.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.BiMap;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.entity.Config;
import com.tb.gateway.entity.DeviceConfig;
import com.tb.gateway.entity.GatewayConfig;
import com.tb.gateway.eum.DeviceType;

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
        List<DeviceConfig> deviceConfigList = gatewayConfig.getConnectors();

        Config.THINGS_BOARD_CONFIG = gatewayConfig.getThingsboard();

        Map<String, ? extends Class<?>> collect = ClassUtil.scanPackage("com.tb.gateway")
                .stream()
                .collect(Collectors.toMap(c -> c.getSimpleName().toUpperCase(), m -> m));

        BiMap<DeviceConfig, Connector> connectorsMap = Config.CONNECTORS_MAP;

        deviceConfigList = deviceConfigList.stream()
                .map(config -> {
                    DeviceType deviceType = config.getDeviceType();
                    String name = deviceType.name();
                    Class<?> configClass = collect.get((name + "Config").toUpperCase());
                    Class<?> connectorsClass = collect.get((name + "Connectors").toUpperCase());
                    if (Objects.isNull(configClass)) {
                        return config;
                    }

                    String s = FileUtil.readUtf8String(new File("config" + File.separator + config.getFileName()));

                    DeviceConfig deviceConfig = (DeviceConfig) gson.fromJson(s, configClass);
                    Connector connector = (Connector) ReflectUtil.newInstance(connectorsClass);
                    connector.setDeviceConfig(deviceConfig);

                    connectorsMap.put(deviceConfig, connector);
                    return deviceConfig;
                }).collect(Collectors.toList());

        Config.GATEWAY_CONFIG = gatewayConfig.setConnectors(deviceConfigList);
    }
}
