package com.tb.gateway;

import cn.hutool.log.LogFactory;
import cn.hutool.log.dialect.console.ConsoleColorLogFactory;
import com.tb.gateway.utils.GatewayUtil;
import com.tb.gateway.utils.WebUtil;

public class Main {
    static {
        LogFactory.setCurrentLogFactory(new ConsoleColorLogFactory());
    }

    public static void main(String[] args) {
        GatewayUtil.start();
        WebUtil.start();
    }
}
