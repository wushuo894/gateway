package com.tb.gateway.connectors.modbus;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.log.Log;
import com.fazecast.jSerialComm.SerialPort;
import com.ghgande.j2mod.modbus.io.BytesInputStream;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.procimg.ObservableRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.enums.ModbusDataType;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public class ModbusConnectors extends Connector {
    private final Log log = Log.get(ModbusConfig.class);

    public static final Map<String, SerialConnection> STRING_SERIAL_CONNECTION_MAP = new HashMap<>();
    public static Gson gson = new Gson();

    /**
     * 运行
     */
    @Override
    public void run() {
        ModbusConfig modbusConfig = (ModbusConfig) baseConfig;
        List<ModbusConfig.ModbusInfo> timeseries = modbusConfig.getTimeseries();
        while (true) {
            for (ModbusConfig.ModbusInfo modbusInfo : timeseries) {
                String tag = modbusInfo.getTag();
                try {
                    Object execute = execute(modbusConfig, modbusInfo);
                    telemetry(Map.of(tag, execute));
                } catch (IOException e) {
                    log.error(e, e.getMessage());
                }
            }
            ThreadUtil.sleep(3000);
        }
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
        ModbusConfig modbusConfig = (ModbusConfig) baseConfig;

        List<ModbusConfig.ModbusInfo> rpc = modbusConfig.getRpc();
        for (ModbusConfig.ModbusInfo modbusInfo : rpc) {
            String tag = modbusInfo.getTag();
            if (!tag.equals(method)) {
                continue;
            }

            try {
                return execute(modbusConfig, modbusInfo, params);
            } catch (IOException e) {
                log.error(e, e.getMessage());
            }
        }

        return null;
    }

    public Object execute(ModbusConfig modbusConfig, ModbusConfig.ModbusInfo modbusInfo) throws IOException {
        return execute(modbusConfig, modbusInfo, null);
    }

    public Object execute(ModbusConfig modbusConfig, ModbusConfig.ModbusInfo modbusInfo, JsonElement params) throws IOException {
        ModbusDataType type = modbusInfo.getType();

        String port = modbusConfig.getPort();
        Integer stopBits = modbusConfig.getStopbits();
        Integer databits = modbusConfig.getDatabits();
        Integer baudrate = modbusConfig.getBaudrate();
        Integer unitId = modbusConfig.getUnitId();
        String parity = modbusConfig.getParity();

        SerialParameters parameters = new SerialParameters();
        parameters.setPortName(port);
        parameters.setBaudRate(baudrate);
        parameters.setDatabits(databits);
        Field field = ReflectUtil.getField(SerialPort.class, parity);
        parameters.setParity((Integer) ReflectUtil.getStaticFieldValue(field));
        parameters.setStopbits(stopBits);

        SerialConnection connection;

        synchronized (STRING_SERIAL_CONNECTION_MAP) {
            String key = gson.toJson(parameters);
            if (STRING_SERIAL_CONNECTION_MAP.containsKey(key)) {
                connection = STRING_SERIAL_CONNECTION_MAP.get(key);
            } else {
                connection = new SerialConnection(parameters);
                STRING_SERIAL_CONNECTION_MAP.put(key, connection);
            }

            if (!connection.isOpen()) {
                connection.open();
            }
        }

        byte[] bytes = new byte[0];

        if (Objects.nonNull(params) && !params.isJsonNull()) {
            if (List.of(ModbusDataType.INT16, ModbusDataType.INT32).contains(type)) {
                bytes = HexUtil.toHex(params.getAsNumber().intValue()).getBytes(StandardCharsets.UTF_8);
            }

            if (ModbusDataType.STRING.equals(type)) {
                bytes = params.getAsString().getBytes(StandardCharsets.UTF_8);
            }
        }

        try {
            // 创建Modbus RTU事务
            ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection);
            Integer address = modbusInfo.getAddress();
            Integer functionCode = modbusInfo.getFunctionCode();
            Integer objectsCount = modbusInfo.getObjectsCount();
            ModbusDataType modbusDataType = modbusInfo.getType();
            ModbusRequest request = ModbusRequest.createModbusRequest(functionCode);
            try {
                ReflectUtil.setFieldValue(request, "reference", address);
            } catch (Exception ignored) {
            }
            try {
                ReflectUtil.setFieldValue(request, "wordCount", objectsCount);
            } catch (Exception ignored) {
            }
            request.setUnitID(unitId);
            request.setHeadless(true);
            if (bytes.length > 0) {
                Register register = new ObservableRegister();
                register.setValue(bytes);
                try {
                    ReflectUtil.invoke(request, "setRegister", register);
                } catch (Exception e) {

                }
            }
            String hexMessage = request.getHexMessage();
            log.info("request: {}", hexMessage);
            transaction.setRequest(request);
            transaction.execute();
            ModbusResponse response = transaction.getResponse();
            byte[] message = response.getMessage();
            log.info("response: {}", response.getHexMessage());
            BiFunction<Byte[], Integer, Object> fun = modbusDataTypeMap.get(modbusDataType);
            return fun.apply(ArrayUtil.wrap(message), objectsCount);
        } catch (Exception e) {
            log.error(e, e.getMessage());
        }
        return null;
    }

    public final Map<ModbusDataType, BiFunction<Byte[], Integer, Object>> modbusDataTypeMap = Map.of(
            ModbusDataType.INT16, (bytes, count) -> {
                Integer length = ModbusDataType.INT16.getLength();
                int i = length * count;
                Byte[] sub = ArrayUtil.sub(bytes, -i, bytes.length);
                String s = HexUtil.encodeHexStr(ArrayUtil.unWrap(sub));
                return HexUtil.hexToInt(s);
            },
            ModbusDataType.INT32, (bytes, count) -> {
                Integer length = ModbusDataType.INT32.getLength();
                int i = length * count;
                Byte[] sub = ArrayUtil.sub(bytes, -i, bytes.length);
                String s = HexUtil.encodeHexStr(ArrayUtil.unWrap(sub));
                return HexUtil.hexToInt(s);
            },
            ModbusDataType.STRING, (bytes, count) -> {
                Integer length = ModbusDataType.STRING.getLength();
                int i = length * count;
                Byte[] sub = ArrayUtil.sub(bytes, -i, bytes.length);
                return HexUtil.encodeHexStr(ArrayUtil.unWrap(sub));
            }
    );

}
