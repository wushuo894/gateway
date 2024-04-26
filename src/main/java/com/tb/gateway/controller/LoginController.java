package com.tb.gateway.controller;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.tb.gateway.annotation.Controller;
import com.tb.gateway.annotation.Auth;
import com.tb.gateway.annotation.Body;
import com.tb.gateway.annotation.PostApi;
import com.tb.gateway.config.Config;
import com.tb.gateway.config.GatewayConfig;
import com.tb.gateway.entity.Result;

@Controller("/")
public class LoginController {
    public static final TimedCache<String, String> CACHE = CacheUtil.newTimedCache(1000 * 60 * 5);

    @PostApi("/login")
    @Auth(false)
    public Result<String> login(@Body GatewayConfig config) {
        String username = config.getUsername();
        String password = config.getPassword();
        password = MD5.create().digestHex(password);

        GatewayConfig gatewayConfig = Config.GATEWAY_CONFIG;
        String configUsername = gatewayConfig.getUsername();
        String configPassword = gatewayConfig.getPassword();
        configPassword = MD5.create().digestHex(configPassword);

        if (StrUtil.equals(configPassword, password) && StrUtil.equals(configUsername, username)) {
            String s = RandomUtil.randomString(64);
            CACHE.put("login", s);
            return Result.success(s);
        }

        return Result.error();
    }

    @PostApi("/test")
    @Auth(true)
    public Result<String> test(@Body String s) {
        return Result.success(s);
    }

}
