package com.bm.bmrpc;

import com.bm.bmrpc.config.RegistryConfig;
import com.bm.bmrpc.config.RpcConfig;
import com.bm.bmrpc.constant.RpcConstant;
import com.bm.bmrpc.registry.Registry;
import com.bm.bmrpc.registry.RegistryFactory;
import com.bm.bmrpc.utils.ConfigUtils;
import com.bm.bmrpc.utils.ConfigChangeListener;
import com.bm.bmrpc.utils.ConfigFileWatcher;
import com.bm.bmrpc.utils.ConfigHolder;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC 框架应用
 * 相当于 holder，存放了项目全局用到的变量。双检锁单例模式实现
 */
@Slf4j
public class RpcApplication {

    private static volatile ConfigHolder<RpcConfig> configHolder;

    /**
     * 框架初始化，支持传入自定义配置
     *
     * @param newRpcConfig
     */
    public static void init(RpcConfig newRpcConfig) {
        configHolder = new ConfigHolder<>(newRpcConfig);
        log.info("rpc init, config = {}", newRpcConfig.toString());
    }

    /**
     * 初始化
     */
    public static void init() {
        try {
            configHolder = ConfigUtils.watchConfig(
                    RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX, "");
            ConfigFileWatcher.getInstance().start();   // ← 关键！启动监听线程
            log.info("rpc init, config = {}", configHolder.getConfig().toString());
        } catch (Exception e) {
            // 配置加载失败，使用默认值
            configHolder = new ConfigHolder<>(new RpcConfig());
        }
        // 注册中心初始化
        RegistryConfig registryConfig = configHolder.getConfig().getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        registry.init(registryConfig);
        log.info("registry init, config = {}", registryConfig);

        //创建并注册Shutdown Hook，JVM退出时执行操作
        Runtime.getRuntime().addShutdownHook(new Thread(registry::destroy));
    }

    /**
     * 获取配置
     *
     * @return
     */
    public static RpcConfig getRpcConfig() {
        if (configHolder == null) {
            synchronized (RpcApplication.class) {
                if (configHolder == null) init();
            }
        }
        return configHolder.getConfig();   // ← 从 ConfigHolder 取，而非直接取 volatile RpcConfig
    }

    /** 注册配置变更监听器 */
    public static void addConfigChangeListener(ConfigChangeListener<RpcConfig> listener) {
        if (configHolder == null) init();
        configHolder.addListener(listener);
    }


}
