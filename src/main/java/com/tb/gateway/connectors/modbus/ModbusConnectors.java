package com.tb.gateway.connectors.modbus;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.log.Log;
import com.fazecast.jSerialComm.SerialPort;
import com.ghgande.j2mod.modbus.io.BytesInputStream;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.enums.ModbusDataType;

import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;

public class ModbusConnectors extends Connector {
    private final Log log = Log.get(ModbusConfig.class);

    public static final Map<String, SerialConnection> STRING_SERIAL_CONNECTION_MAP = new HashMap<>();
    public static Gson gson = new Gson();
    public static final Map<Integer, Class<?>> FUNC_CODE_MAP = new HashMap<>();

    /**
     * 运行
     */
    @Override
    public void run() {
        ModbusConfig modbusConfig = (ModbusConfig) baseConfig;
        loadFuncCodeMap();
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

    public synchronized void loadFuncCodeMap() {
        Set<Class<?>> classes = ClassUtil.scanPackage("com.ghgande.j2mod.modbus.msg");
        for (Class<?> aClass : classes) {
            Class<?> superclass = aClass.getSuperclass();
            if (Objects.isNull(superclass)) {
                continue;
            }
            String name = superclass.getName();
            if (!name.equals("com.ghgande.j2mod.modbus.msg.ModbusRequest")) {
                continue;
            }
            Constructor<?> constructor = ReflectUtil.getConstructor(aClass);
            if (Objects.isNull(constructor)) {
                continue;
            }
            if (constructor.getParameterCount() > 0) {
                continue;
            }
            ModbusRequest modbusRequest;
            try {
                modbusRequest = (ModbusRequest) constructor.newInstance();
            } catch (Exception e) {
                log.error(e, e.getMessage());
                continue;
            }
            int functionCode = modbusRequest.getFunctionCode();
            FUNC_CODE_MAP.put(functionCode, aClass);
        }
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

        SerialParameters parameters = new SerialParameters();
        parameters.setPortName(port);
        parameters.setBaudRate(baudrate);
        parameters.setDatabits(databits);
        parameters.setParity(SerialPort.EVEN_PARITY);
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

        if (Objects.nonNull(params)) {
            if (List.of(ModbusDataType.INT16, ModbusDataType.INT32).contains(type)) {
                bytes = new byte[]{params.getAsNumber().byteValue()};
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
            Class<?> aClass = FUNC_CODE_MAP.get(functionCode);
            log.info(aClass.getName());
            ModbusRequest request = (ModbusRequest) ReflectUtil.newInstance(aClass, address, objectsCount);
            request.setUnitID(unitId);
            request.setHeadless(true);
            if (bytes.length > 0) {
                request.readData(new BytesInputStream(bytes));
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
        return "NULL";
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
