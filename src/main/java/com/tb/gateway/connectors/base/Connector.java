package com.tb.gateway.connectors.base;

import com.google.gson.JsonObject;
import com.tb.gateway.entity.DeviceConfig;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.paho.client.mqttv3.MqttMessage;

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

}
