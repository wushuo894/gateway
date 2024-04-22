package com.tb.gateway.connectors.base;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tb.gateway.entity.DeviceConfig;
import com.tb.gateway.tb.TbClient;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 设备连接
 */
@Data
@Accessors(chain = true)
public abstract class Connector implements Runnable {

    /**
     * 配置文件
     */
    protected DeviceConfig deviceConfig;

    /**
     * 数据转换
     */
    protected Converter<?, ?> converter;

    /**
     * 运行
     */
    @Override
    public abstract void run();

    /**
     * TB > gateway 数据
     *
     * @param jsonObject 入参
     * @return 返回
     */
    public abstract MqttMessage serverSideRpcHandler(JsonObject jsonObject);

    public void telemetry(Map<String, Object> map) {
        Gson gson = new Gson();
        String deviceName = deviceConfig.getDeviceName();
        Map<String, List<Object>> msg = Map.of(deviceName, List.of(Map.of(
                "ts", new Date().getTime(),
                "values", map
        )));
        TbClient.telemetry(gson.toJson(msg));
    }

}
