package com.tb.gateway.tb;

import cn.hutool.core.map.BiMap;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tb.gateway.config.Config;
import com.tb.gateway.config.GatewayConfig;
import com.tb.gateway.config.ThingsBoardConfig;
import com.tb.gateway.config.ThreadConfig;
import com.tb.gateway.connectors.base.BaseConfig;
import com.tb.gateway.connectors.base.BaseConnector;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * TB
 */
@Data
@Accessors(chain = true)
public class TbClient {
    private static MqttClient mqttClient;
    private static final Gson gson = new Gson();
    private static final Map<String, Object> TELEMETRY_MAP = new ConcurrentHashMap<>();

    /**
     * 连接MQTT
     */
    public static void connect() {
        Log log = Log.get(TbClient.class);
        ThingsBoardConfig thingsBoardConfig = Config.THINGS_BOARD_CONFIG;
        String host = thingsBoardConfig.getHost();
        Integer port = thingsBoardConfig.getPort();
        String clientId = StrUtil.blankToDefault(thingsBoardConfig.getClientId(), "");
        String userName = thingsBoardConfig.getUserName();
        String password = thingsBoardConfig.getPassword();
        Integer timeout = ObjUtil.defaultIfNull(thingsBoardConfig.getTimeout(), 3000);

        String url = StrFormatter.format("tcp://{}:{}", host, port);

        try {
            log.info("connect: {}", url);
            mqttClient = new MqttClient(url, clientId);

            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setUserName(userName);
            mqttConnectOptions.setConnectionTimeout(timeout);
            mqttConnectOptions.setMaxReconnectDelay(Integer.MAX_VALUE);

            if (StrUtil.isNotBlank(password)) {
                mqttConnectOptions.setPassword(password.toCharArray());
            }

            mqttClient.connect(mqttConnectOptions);
            log.info("connect ok.");

            RuntimeUtil.addShutdownHook(TbClient::disconnect);

            // 将遥测数据发送出去
            ThreadUtil.execute(() -> {
                while (true) {
                    ThreadUtil.sleep(3000);
                    if (TELEMETRY_MAP.isEmpty()) {
                        continue;
                    }
                    synchronized (TELEMETRY_MAP) {
                        String s = gson.toJson(TELEMETRY_MAP);
                        publish("v1/gateway/telemetry", s);
                        TELEMETRY_MAP.clear();
                    }
                }
            });

        } catch (MqttException e) {
            log.error(e, e.getMessage());
            System.exit(1);
        }
    }

    public static void disconnect() {
        Log log = Log.get(TbClient.class);
        try {
            mqttClient.disconnect();
        } catch (MqttException e) {
            log.error(e, e.getMessage());
        }
    }

    /**
     * 将遥测数据放入队列
     *
     * @param deviceName
     * @param values
     */
    public static void telemetry(String deviceName, Map<String, Object> values) {
        TELEMETRY_MAP.put(deviceName, List.of(Map.of(
                "ts", new Date().getTime(),
                "values", values
        )));
    }

    /**
     * 发送消息
     *
     * @param topic
     * @param msg
     */
    public static void publish(String topic, String msg) {
        Log log = Log.get(TbClient.class);
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(msg.getBytes(StandardCharsets.UTF_8));
        try {
            log.debug("send: {}", msg);
            mqttClient.publish(topic, mqttMessage);
        } catch (MqttException e) {
            log.error(e, e.getMessage());
        }
    }

    /**
     * 处理订阅
     */
    private static final Map<String, Function<JsonObject, String>> functionMap =
            Map.of("v1/gateway/rpc", jsonObject -> {
                Log log = Log.get(TbClient.class);
                log.info("rpc: {}", jsonObject);
                String device = jsonObject.get("device").getAsString();
                GatewayConfig gatewayConfig = Config.GATEWAY_CONFIG;
                BiMap<BaseConfig, BaseConnector<? extends BaseConfig>> connectorsMap = Config.CONNECTORS_MAP;
                for (BaseConfig baseConfig : gatewayConfig.getConnectors()) {
                    String deviceName = baseConfig.getDeviceName();
                    if (!device.equals(deviceName)) {
                        continue;
                    }
                    BaseConnector<? extends BaseConfig> baseConnector = connectorsMap.get(baseConfig);
                    Object o = baseConnector.serverSideRpcHandler(jsonObject);

                    if (Objects.isNull(o)) {
                        continue;
                    }

                    String id = jsonObject.get("data").getAsJsonObject()
                            .get("id").getAsString();

                    Map<String, Object> retData = Map.of(
                            "device", deviceName,
                            "id", id,
                            "data", o
                    );
                    return gson.toJson(retData);
                }
                return null;
            });

    /**
     * 订阅
     */
    public static void subscribe() {
        Log log = Log.get(TbClient.class);
        try {
            String[] topicFilters = ArrayUtil.toArray(functionMap.keySet(), String.class);
            mqttClient.subscribe(topicFilters);
        } catch (MqttException e) {
            log.error(e, e.getMessage());
        }
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.error(cause, cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                if (!functionMap.containsKey(topic)) {
                    log.debug("not subscribe: {}", topic);
                    return;
                }
                ThreadConfig.EXECUTOR.submit(() -> {
                    JsonObject jsonObject = gson.fromJson(
                            new String(message.getPayload(), StandardCharsets.UTF_8), JsonObject.class
                    );
                    try {
                        String apply = functionMap.get(topic).apply(jsonObject);
                        log.debug("callback: {}", apply);
                        message.setPayload(apply.getBytes(StandardCharsets.UTF_8));
                        mqttClient.publish(topic, message);
                    } catch (Exception e) {
                        log.error(e, e.getMessage());
                    }
                });
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    MqttMessage message = token.getMessage();
                    if (Objects.isNull(message)) {
                        return;
                    }
                    String s = message.toString();
                    log.debug("msg: {}", s);
                } catch (MqttException e) {
                    log.error(e, e.getMessage());
                }
            }
        });
    }


}
