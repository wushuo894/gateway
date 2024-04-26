package com.tb.gateway.connectors.base;

import com.google.gson.JsonObject;
import com.tb.gateway.tb.TbClient;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 设备连接
 */
@Data
@Accessors(chain = true)
public abstract class BaseConnector<T extends BaseConfig> implements Runnable {

    /**
     * 配置文件
     */
    protected T config;

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
    public abstract Object serverSideRpcHandler(JsonObject jsonObject);

    public void telemetry(Map<String, Object> map) {
        String deviceName = config.getDeviceName();
        TbClient.telemetry(deviceName, map);
    }

    public void setConfig(BaseConfig baseConfig) {
        this.config = (T) baseConfig;
    }
}
