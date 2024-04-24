package com.tb.gateway.connectors.modbus;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.procimg.ObservableRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.enums.ModbusDataType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ModbusConnectors extends Connector<ModbusConfig> {
    private final Log log = Log.get(ModbusConfig.class);

    /**
     * 运行
     */
    @Override
    public void run() {
        List<ModbusConfig.ModbusInfo> timeseries = config.getTimeseries();
        do {
            for (ModbusConfig.ModbusInfo modbusInfo : timeseries) {
                String tag = modbusInfo.getTag();
                try {
                    Object execute = execute(config, modbusInfo);
                    telemetry(Map.of(tag, execute));
                } catch (IOException e) {
                    log.error(e, e.getMessage());
                }
            }
            ThreadUtil.sleep(3000);
        } while (true);
    }

    /**
     * TB > gateway 数据
     *
     * @param jsonObject 入参
     * @return 返回
     */
    @Override
    public Object serverSideRpcHandler(JsonObject jsonObject) {
        JsonObject data = jsonObject.get("data").getAsJsonObject();
        String method = data.get("method").getAsString();
        JsonElement params = data.get("params");

        List<ModbusConfig.ModbusInfo> rpc = config.getRpc();
        for (ModbusConfig.ModbusInfo modbusInfo : rpc) {
            String tag = modbusInfo.getTag();
            if (!tag.equals(method)) {
                continue;
            }

            try {
                return execute(config, modbusInfo, params);
            } catch (IOException e) {
                log.error(e, e.getMessage());
                return e.getMessage();
            }
        }

        return "没有对应的方法";
    }

    public Object execute(ModbusConfig modbusConfig, ModbusConfig.ModbusInfo modbusInfo) throws IOException {
        return execute(modbusConfig, modbusInfo, null);
    }

    public Object execute(ModbusConfig modbusConfig, ModbusConfig.ModbusInfo modbusInfo, JsonElement params) throws IOException {
        ModbusDataType type = modbusInfo.getType();

        SerialConnection connection = ModbusUtil.getConnection(modbusConfig);

        Register register = new ObservableRegister();

        if (Objects.nonNull(params) && !params.isJsonNull()) {
            if (List.of(ModbusDataType.INT16, ModbusDataType.INT32).contains(type)) {
                register.setValue(params.getAsNumber().intValue());
            }

            if (ModbusDataType.STRING.equals(type)) {
                byte[] bytes = params.getAsString().getBytes(StandardCharsets.UTF_8);
                register.setValue(bytes);
            }
        }

        try {
            // 创建Modbus RTU事务
            ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection);

            // 发送
            ModbusRequest request = ModbusUtil.createModbusRequest(modbusConfig, modbusInfo, register);
            String hexMessage = request.getHexMessage();
            log.debug("request: {}", hexMessage);
            transaction.setRequest(request);
            transaction.execute();

            // 获取响应结果
            ModbusResponse response = transaction.getResponse();
            byte[] message = response.getMessage();
            log.debug("response: {}", response.getHexMessage());
            return ModbusUtil.toType(modbusInfo, message);
        } catch (Exception e) {
            log.error(e, e.getMessage());
            return e.getMessage();
        }
    }

}
