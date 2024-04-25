package com.tb.gateway;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import com.google.gson.Gson;

import java.util.Map;

public class Test2 {
    private static final Log LOG = Log.get(Test2.class);

    public static void main(String[] args) {
        Gson gson = new Gson();
        HttpUtil.createServer(8888).addAction("/test", (req, res) -> {
            int i = RandomUtil.randomInt(-1, 1);
            LOG.info(req.getBody());
            System.out.println(i);
            res.write(gson.toJson(Map.of(
                    "code", i,
                    "cmd", 0,
                    "voiceData", ""
            ))).close();
        }).start();
    }
}
