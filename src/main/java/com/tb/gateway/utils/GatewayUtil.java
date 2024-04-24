package com.tb.gateway.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.BiMap;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.TypeUtil;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.tb.gateway.config.Config;
import com.tb.gateway.config.GatewayConfig;
import com.tb.gateway.config.ThreadConfig;
import com.tb.gateway.connectors.base.BaseConfig;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.enums.DeviceType;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

        BiMap<BaseConfig, Connector<? extends BaseConfig>> connectorsMap = Config.CONNECTORS_MAP;

        baseConfigList = baseConfigList.stream()
                .filter(Objects::nonNull)
                .map(config -> {
                    DeviceType deviceType = config.getDeviceType();
                    String name = deviceType.name();
                    Class<?> connectorsClass = deviceType.getConnectorClass();
                    if (Objects.isNull(connectorsClass)) {
                        return config;
                    }

                    log.info("load connectorsClassName: {}", connectorsClass.getName());

                    String s = FileUtil.readUtf8String(new File("config" + File.separator + config.getFileName()));

                    Connector<? extends BaseConfig> connector = (Connector<? extends BaseConfig>) ReflectUtil.newInstance(connectorsClass);

                    Class<?> returnClass =
                            ClassUtil.loadClass(TypeUtil.getGenerics(connectorsClass)[0].getActualTypeArguments()[0].getTypeName());

                    BaseConfig baseConfig = (BaseConfig) gson.fromJson(s, returnClass);
                    baseConfig.setDeviceName(config.getDeviceName());
                    connector.setConfig(baseConfig);

                    connectorsMap.put(baseConfig, connector);
                    return baseConfig;
                }).collect(Collectors.toList());

        Config.GATEWAY_CONFIG = gatewayConfig.setConnectors(baseConfigList);
    }
}
