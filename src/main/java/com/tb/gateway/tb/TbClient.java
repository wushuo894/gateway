package com.tb.gateway.tb;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.entity.Config;
import com.tb.gateway.entity.DeviceConfig;
import com.tb.gateway.entity.GatewayConfig;
import com.tb.gateway.entity.ThingsBoardConfig;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Data
@Accessors(chain = true)
public class TbClient {
    private static MqttClient mqttClient;
    private final Gson gson = new Gson();
    private static final Log log = Log.get(TbClient.class);

    public static void connect() {
        ThingsBoardConfig thingsBoardConfig = Config.THINGS_BOARD_CONFIG;
        String host = thingsBoardConfig.getHost();
        Integer port = thingsBoardConfig.getPort();
        String clientId = thingsBoardConfig.getClientId();
        String userName = thingsBoardConfig.getUserName();
        String password = thingsBoardConfig.getPassword();

        String url = StrFormatter.format("tcp://{}:{}", host, port);

        try {
            log.info(url);
            mqttClient = new MqttClient(url, clientId);
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setUserName(userName);
            mqttConnectOptions.setPassword(password.toCharArray());
            mqttClient.connect(mqttConnectOptions);
        } catch (MqttException e) {
            log.error(e, e.getMessage());
        }
    }

    public static void send(String msg) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(msg.getBytes(StandardCharsets.UTF_8));
        try {
            log.info("send: {}", msg);
            mqttClient.publish("v1/gateway/telemetry", mqttMessage);
        } catch (MqttException e) {
            log.error(e, e.getMessage());
        }
    }

    private static final Map<String, Function<JsonObject, MqttMessage>> functionMap =
            Map.of("v1/gateway/rpc", jsonObject -> {
                String device = jsonObject.get("device").getAsString();
                GatewayConfig gatewayConfig = Config.GATEWAY_CONFIG;
                for (DeviceConfig deviceConfig : gatewayConfig.getConnectors()) {
                    String deviceName = deviceConfig.getDeviceName();
                    if (!device.equals(deviceName)) {
                        continue;
                    }
                    Connector connector = deviceConfig.getConnector();
                    return connector.serverSideRpcHandler(jsonObject);
                }
                throw new RuntimeException("不存在的设备: " + device);
            });

    /**
     * 订阅
     */
    public static void subscribe() {
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
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if (!functionMap.containsKey(topic)) {
                    log.info("not subscribe: {}", topic);
                    return;
                }
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(
                        new String(message.getPayload(), StandardCharsets.UTF_8), JsonObject.class
                );
                MqttMessage apply = functionMap.get(topic).apply(jsonObject);
                mqttClient.publish(topic, apply);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    MqttMessage message = token.getMessage();
                    if (Objects.isNull(message)) {
                        return;
                    }
                    String s = message.toString();
                    log.info("msg: {}", s);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
    }


}
