package com.tb.gateway.connectors.modbus;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.tb.gateway.enums.ModbusDataType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ModbusUtil {

    /**
     * 类型转换
     *
     * @param modbusInfo
     * @param bytes
     * @return
     */
    public static Object toType(ModbusConfig.ModbusInfo modbusInfo, byte[] bytes) {
        ModbusDataType modbusDataType = modbusInfo.getType();

        Integer count = modbusInfo.getObjectsCount();
        Integer length = modbusDataType.getLength();

        int i = length * count;
        byte[] sub = ArrayUtil.sub(bytes, -i, bytes.length);

        Map<List<ModbusDataType>, Supplier<Object>> map =
                Map.of(
                        List.of(ModbusDataType.INT16, ModbusDataType.INT32), () -> HexUtil.encodeHexStr(sub),
                        List.of(ModbusDataType.STRING), () -> HexUtil.encodeHexStr(sub)
                );

        for (List<ModbusDataType> modbusDataTypes : map.keySet()) {
            Optional<ModbusDataType> first = modbusDataTypes.stream()
                    .filter(dataType -> !modbusDataType.name().equals(dataType.name()))
                    .findFirst();
            if (first.isEmpty()) {
                continue;
            }
            Supplier<Object> objectSupplier = map.get(modbusDataTypes);
            return objectSupplier.get();
        }

        throw new RuntimeException("未知类型");
    }

    /**
     * 创建对应的 request
     *
     * @param modbusConfig
     * @param modbusInfo
     * @param register
     * @return
     */
    public static ModbusRequest createModbusRequest(ModbusConfig modbusConfig, ModbusConfig.ModbusInfo modbusInfo, Register... register) {
        Integer unitId = modbusConfig.getUnitId();
        Integer address = modbusInfo.getAddress();
        Integer functionCode = modbusInfo.getFunctionCode();
        Integer objectsCount = modbusInfo.getObjectsCount();

        ModbusRequest request;

        switch (functionCode) {
            case Modbus.READ_COILS:
                request = new ReadCoilsRequest(address, objectsCount);
                break;
            case Modbus.READ_INPUT_DISCRETES:
                request = new ReadInputDiscretesRequest(address, objectsCount);
                break;
            case Modbus.READ_MULTIPLE_REGISTERS:
                request = new ReadMultipleRegistersRequest(address, objectsCount);
                break;
            case Modbus.READ_INPUT_REGISTERS:
                request = new ReadInputRegistersRequest(address, objectsCount);
                break;
            case Modbus.WRITE_COIL:
                request = new WriteCoilRequest();
                break;
            case Modbus.WRITE_SINGLE_REGISTER:
                request = new WriteSingleRegisterRequest(address, register[0]);
                break;
            case Modbus.WRITE_MULTIPLE_COILS:
                request = new WriteMultipleCoilsRequest(address, objectsCount);
                break;
            case Modbus.WRITE_MULTIPLE_REGISTERS:
                request = new WriteMultipleRegistersRequest(address, register);
                break;
            case Modbus.READ_EXCEPTION_STATUS:
                request = new ReadExceptionStatusRequest();
                break;
            case Modbus.READ_SERIAL_DIAGNOSTICS:
                request = new ReadSerialDiagnosticsRequest();
                break;
            case Modbus.READ_COMM_EVENT_COUNTER:
                request = new ReadCommEventCounterRequest();
                break;
            case Modbus.READ_COMM_EVENT_LOG:
                request = new ReadCommEventLogRequest();
                break;
            case Modbus.REPORT_SLAVE_ID:
                request = new ReportSlaveIDRequest();
                break;
            case Modbus.READ_FILE_RECORD:
                request = new ReadFileRecordRequest();
                break;
            case Modbus.WRITE_FILE_RECORD:
                request = new WriteFileRecordRequest();
                break;
            case Modbus.MASK_WRITE_REGISTER:
                request = new MaskWriteRegisterRequest();
                break;
            case Modbus.READ_WRITE_MULTIPLE:
                request = new ReadWriteMultipleRequest(unitId);
                break;
            case Modbus.READ_FIFO_QUEUE:
                request = new ReadFIFOQueueRequest();
                break;
            case Modbus.READ_MEI:
                request = new ReadMEIRequest();
                break;
            default:
                request = new IllegalFunctionRequest(functionCode);
                break;
        }
        request.setUnitID(unitId);
        request.setHeadless(true);
        return request;
    }
}
