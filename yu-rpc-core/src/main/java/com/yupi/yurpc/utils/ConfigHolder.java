package com.yupi.yurpc.utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 配置持有者，支持 volatile 动态更新 + 监听器回调
 */
public class ConfigHolder<T> {

    private volatile T config;
    private final List<ConfigChangeListener<T>> listeners = new CopyOnWriteArrayList<>();

    public ConfigHolder(T initialConfig) {
        this.config = initialConfig;
    }

    public T getConfig() {
        return config;
    }

    /** 文件变更时调用 */
    public void update(T newConfig) {
        T oldConfig = this.config;
        this.config = newConfig;          // volatile 写，对所有线程可见
        // 通知监听器
        for (ConfigChangeListener<T> listener : listeners) {
            listener.onChange(newConfig, oldConfig);
        }
    }

    /** 注册监听器 */
    public void addListener(ConfigChangeListener<T> listener) {
        listeners.add(listener);
    }
}