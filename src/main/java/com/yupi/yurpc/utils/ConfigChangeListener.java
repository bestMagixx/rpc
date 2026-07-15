package com.yupi.yurpc.utils;

/**
 * 配置变更监听器
 */
public interface ConfigChangeListener<T> {
    /**
     * 配置变更时回调
     * @param newConfig 新配置对象
     * @param oldConfig 旧配置对象
     */
    void onChange(T newConfig, T oldConfig);
}