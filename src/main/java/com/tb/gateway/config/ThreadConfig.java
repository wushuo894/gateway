package com.tb.gateway.config;

import cn.hutool.core.thread.ExecutorBuilder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Data
@Accessors(chain = true)
public class ThreadConfig implements Serializable {
    /**
     * 初始线程
     */
    private Integer corePoolSize = 4;
    /**
     * 最大线程
     */
    private Integer maxPoolSize = 12;
    /**
     * 最大等待数
     */
    private Integer capacity = 128;

    public static ExecutorService EXECUTOR;

    public void init() {
        EXECUTOR = ExecutorBuilder.create()
                .setCorePoolSize(corePoolSize)
                .setMaxPoolSize(maxPoolSize)
                .setWorkQueue(new LinkedBlockingQueue<>(capacity))
                .build();
    }
}
