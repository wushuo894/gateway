package com.tb.gateway.connectors.modbus;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.log.Log;
import com.fazecast.jSerialComm.SerialPort;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.enums.ModbusDataType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                try {
                    execute(modbusConfig, modbusInfo);
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
        return null;
    }

    public Object execute(ModbusConfig modbusConfig, ModbusConfig.ModbusInfo modbusInfo) throws IOException {
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
            }

            if (!connection.isOpen()) {
                connection.open();
            }
        }


        try {
            // 创建Modbus RTU事务
            ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection);
            Integer address = modbusInfo.getAddress();
            Integer functionCode = modbusInfo.getFunctionCode();
            Integer objectsCount = modbusInfo.getObjectsCount();
            String tag = modbusInfo.getTag();
            ModbusDataType modbusDataType = modbusInfo.getType();
            ModbusRequest request = new ReadMultipleRegistersRequest(address, objectsCount);
            request.setUnitID(unitId);
            request.setHeadless(true);
            String hexMessage = request.getHexMessage();
            log.info(hexMessage);
            transaction.setRequest(request);
            transaction.execute();
            ModbusResponse response = transaction.getResponse();
            byte[] message = response.getMessage();
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
                i = i - (i * 2);
                Byte[] sub = ArrayUtil.sub(bytes, i, bytes.length);
                String s = HexUtil.encodeHexStr(ArrayUtil.unWrap(sub));
                return HexUtil.hexToInt(s);
            },
            ModbusDataType.INT32, (bytes, count) -> {
                Integer length = ModbusDataType.INT32.getLength();
                int i = length * count;
                i = i - (i * 2);
                Byte[] sub = ArrayUtil.sub(bytes, i, bytes.length);
                String s = HexUtil.encodeHexStr(ArrayUtil.unWrap(sub));
                return HexUtil.hexToInt(s);
            },
            ModbusDataType.STRING, (bytes, count) -> {
                Integer length = ModbusDataType.STRING.getLength();
                int i = length * count;
                i = i - (i * 2);
                Byte[] sub = ArrayUtil.sub(bytes, i, bytes.length);
                return HexUtil.encodeHexStr(ArrayUtil.unWrap(sub));
            }
    );

}
