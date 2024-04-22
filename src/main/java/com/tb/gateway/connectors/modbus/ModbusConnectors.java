package com.tb.gateway.connectors.modbus;

import cn.hutool.core.util.HexUtil;
import cn.hutool.log.Log;
import com.fazecast.jSerialComm.SerialPort;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import com.google.gson.JsonObject;
import com.tb.gateway.connectors.base.Connector;
import com.tb.gateway.entity.ModbusConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class ModbusConnectors extends Connector {
    private final Log log = Log.get(ModbusConfig.class);
    private final ModbusConfig modbusConfig = (ModbusConfig) deviceConfig;

    /**
     * 运行
     */
    @Override
    public void run() {
        String port = modbusConfig.getPort();
        Integer stopBits = modbusConfig.getStopbits();
        Integer databits = modbusConfig.getDatabits();
        Integer baudrate = modbusConfig.getBaudrate();
        Integer unitId = modbusConfig.getUnitId();

        SerialParameters parameters = new SerialParameters();
        parameters.setPortName(port);
        parameters.setBaudRate(baudrate);
        parameters.setDatabits(databits);
        parameters.setParity(SerialPort.NO_PARITY);
        parameters.setStopbits(stopBits);

        SerialConnection connection = new SerialConnection(parameters);
        try {
            connection.open();
        } catch (IOException e) {
            log.error(e, e.getMessage());
        }
        List<ModbusConfig.ModbusInfo> timeseries = modbusConfig.getTimeseries();
        for (ModbusConfig.ModbusInfo modbusInfo : timeseries) {
            Integer address = modbusInfo.getAddress();
            Integer objectsCount = modbusInfo.getObjectsCount();
            String tag = modbusInfo.getTag();
            TypeKind type = modbusInfo.getType();
            ReadCoilsRequest request = new ReadCoilsRequest(address, objectsCount);
            request.setUnitID(unitId);
            ModbusResponse response = request.getResponse();
            System.out.println(HexUtil.hexToInt(response.getHexMessage()));
        }
    }

    /**
     * TB > gateway 数据
     *
     * @param jsonObject 入参
     * @return 返回
     */
    @Override
    public MqttMessage serverSideRpcHandler(JsonObject jsonObject) {
        return null;
    }

}
