package com.tb.gateway;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.tb.gateway.tb.TbClient;
import com.tb.gateway.util.GatewayUtil;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Main {
    private static final Log log = Log.get(Main.class);

    public static void main(String[] args) {
        GatewayUtil.load();
        TbClient.connect();
        TbClient.subscribe();

        Gson gson = new Gson();

        while (true) {
            Map<String, List<Object>> msg = Map.of("test", Collections.singletonList(Map.of(
                    "ts", new Date().getTime(),
                    "values", Map.of(
                            "temperature", 42
                    )
            )));
            TbClient.send(gson.toJson(msg));
            ThreadUtil.sleep(3000);
        }
    }
}
