package com.tb.gateway;

import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.tb.gateway.entity.Config;
import com.tb.gateway.tb.TbClient;
import com.tb.gateway.util.GatewayUtil;

public class Main {
    private static final Log log = Log.get(Main.class);
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        GatewayUtil.load();
        TbClient.connect();
        TbClient.subscribe();
        log.info(gson.toJson(Config.GATEWAY_CONFIG));
//
//        while (true) {
//            Map<String, List<Object>> msg = Map.of("test", Collections.singletonList(Map.of(
//                    "ts", new Date().getTime(),
//                    "values", Map.of(
//                            "temperature", 42
//                    )
//            )));
//            TbClient.send(gson.toJson(msg));
//            ThreadUtil.sleep(3000);
//        }
    }
}
