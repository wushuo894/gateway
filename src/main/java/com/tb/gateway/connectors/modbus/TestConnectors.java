package com.tb.gateway.connectors.modbus;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.entity.TestConfig;

import java.util.Date;
import java.util.Map;

public class TestConnectors extends Connector {
    private final Log log = Log.get(TestConnectors.class);
    private final TestConfig modbusConfig = (TestConfig) deviceConfig;

    @Override
    public void run() {
        while (true) {
            telemetry(Map.of(
                    "test", new Date().getTime()
            ));
            ThreadUtil.sleep(3000);
        }
    }

    @Override
    public Object serverSideRpcHandler(JsonObject jsonObject) {
        JsonObject data = jsonObject.get("data").getAsJsonObject();
        String method = data.get("method").getAsString();
        JsonElement params = data.get("params");
        log.info("serverSideRpcHandle: {}", jsonObject);
        return "ok";
    }
}
