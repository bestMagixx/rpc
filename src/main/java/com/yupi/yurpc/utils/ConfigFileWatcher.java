package com.yupi.yurpc.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置文件监听器，基于 WatchService
 * 监听 classpath 输出目录下配置文件的 MODIFY 事件
 */
@Slf4j
public class ConfigFileWatcher {

    private static volatile ConfigFileWatcher instance;
    private final WatchService watchService;
    private final Map<String, Runnable> fileCallbacks = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private Thread watchThread;

    private ConfigFileWatcher() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
    }

    public static ConfigFileWatcher getInstance() {
        if (instance == null) {
            synchronized (ConfigFileWatcher.class) {
                if (instance == null) {
                    try { instance = new ConfigFileWatcher(); }
                    catch (IOException e) { throw new RuntimeException("无法创建 WatchService", e); }
                }
            }
        }
        return instance;
    }

    /** 注册文件变更回调 */
    public void register(String fileName, Runnable callback) {
        // 找到配置文件在磁盘上的实际路径（从 classpath URL 解析）
        URL resource = Thread.currentThread().getContextClassLoader().getResource(fileName);
        if (resource == null) return;
        try {
            Path path = Paths.get(resource.toURI());
            Path parentDir = path.getParent();
            String watchFileName = path.getFileName().toString();
            // 注册目录监听（只监听 MODIFY 事件）
            parentDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            fileCallbacks.put(watchFileName, callback);
        } catch (Exception e) {
            log.error("注册文件监听失败: {}", fileName, e);
        }
    }

    /** 启动监听线程 */
    public void start() {
        watchThread = new Thread(() -> {
            while (running) {
                try {
                    WatchKey key = watchService.take();   // 阻塞等待事件
                    for (WatchEvent<?> event : key.pollEvents()) {
                        String changedFile = event.context().toString();
                        Runnable callback = fileCallbacks.get(changedFile);
                        if (callback != null && event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            callback.run();                // 触发重新加载配置
                        }
                    }
                    key.reset();
                } catch (InterruptedException e) {
                    break;  // 线程被中断，退出
                }
            }
        }, "config-file-watcher");
        watchThread.setDaemon(true);  // 守护线程，不阻止 JVM 退出
        watchThread.start();
    }

    /** 停止监听 */
    public void stop() {
        running = false;
        if (watchThread != null) watchThread.interrupt();
        try { watchService.close(); } 
        catch (IOException e) { log.error("关闭 WatchService 失败", e); }
    }
}