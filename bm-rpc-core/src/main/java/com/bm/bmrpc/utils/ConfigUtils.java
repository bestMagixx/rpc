package com.bm.bmrpc.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;
import cn.hutool.setting.yaml.YamlUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置工具类
 */
public class ConfigUtils {

    // 支持的配置格式，按优先级排列
    private static final String[] CONFIG_EXTENSIONS = {".properties", ".yaml", ".yml"};

    /**
     * 加载配置（无环境）
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix) {
        return loadConfig(tClass, prefix, "");
    }

    /**
     * 加载配置（支持环境 + 多格式自动探测）
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix, String environment){
        String baseName = buildBaseName(environment);
        // 依次探测 .properties → .yaml → .yml
        for (String ext : CONFIG_EXTENSIONS) {
            String fileName = baseName + ext;
            if (resourceExists(fileName)) {
                return parseConfig(fileName, ext, tClass, prefix);
            }
        }
        // 全部找不到 → 返回默认实例（与现有 RpcApplication.init() 的 catch 逻辑配合）
        try { return tClass.newInstance(); }
        catch (Exception e) { throw new RuntimeException("无法创建配置实例", e); }
    }

    /**
     * 注册配置监听（文件变更时自动更新对象）
     */
    public static <T> ConfigHolder<T> watchConfig(Class<T> tClass, String prefix, String environment) throws InstantiationException, IllegalAccessException {
        T initialConfig = loadConfig(tClass, prefix, environment);
        ConfigHolder<T> holder = new ConfigHolder<>(initialConfig);
        String baseName = buildBaseName(environment);
        // 为每种可能存在的格式注册监听
        for (String ext : CONFIG_EXTENSIONS) {
            String fileName = baseName + ext;
            if (resourceExists(fileName)) {
                ConfigFileWatcher.getInstance().register(fileName, () -> {
                    T newConfig = null;
                    try {
                        newConfig = parseConfig(fileName, ext, tClass, prefix);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    holder.update(newConfig);
                });
            }
        }
        return holder;
    }

    // ---- 内部方法 ----

    private static String buildBaseName(String environment) {
        if (StrUtil.isNotBlank(environment))
            return "application-" + environment;
        return "application";
    }

    private static boolean resourceExists(String fileName) {
        return Thread.currentThread().getContextClassLoader()
                .getResource(fileName) != null;
    }

    private static <T> T parseConfig(String fileName, String ext, Class<T> tClass, String prefix){
        switch (ext) {
            case ".properties":
                Props props = new Props(fileName);
                props.autoLoad(true);
                return props.toBean(tClass, prefix);
            case ".yaml": case ".yml":
                Map<String, Object> yamlMap = YamlUtil.loadByPath(fileName);
                // 提取 prefix 下的子 Map
                Map<String, Object> flatMap = extractByPrefix(yamlMap, prefix);
                return BeanUtil.toBean(flatMap, tClass);
            default:
                throw new RuntimeException("不支持的配置格式: " + ext);
        }
    }

    private static Map<String, Object> extractByPrefix(Map<String, Object> map, String prefix) {
        // YAML 嵌套结构: {rpc: {name: x}} → 找到 prefix="rpc" 的子Map → {name: x}
        Object sub = map.get(prefix);
        if (sub instanceof Map) return (Map<String, Object>) sub;
        // 如果 prefix 是多级如 "rpc.server"，需要逐层深入
        // 也兼容平铺格式: {rpc.name: x} → 过滤 prefix
        Map<String, Object> flat = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix + ".")) {
                flat.put(key.substring(prefix.length() + 1), entry.getValue());
            }
        }
        return flat;
    }
}
