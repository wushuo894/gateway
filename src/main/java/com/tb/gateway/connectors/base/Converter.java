package com.tb.gateway.connectors.base;

/**
 * 数据转换
 *
 * @param <P> 原类型
 * @param <T> 目标类型
 */
public abstract class Converter<P, T> {
    /**
     * 类型转换
     *
     * @param p 原类型
     * @return 目标类型
     */
    public abstract T converter(P p);
}
