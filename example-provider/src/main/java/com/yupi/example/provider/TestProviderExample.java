package com.yupi.example.provider;

import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.constant.RpcConstant;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.registry.LocalRegistry;
import com.yupi.yurpc.registry.Registry;
import com.yupi.yurpc.registry.RegistryFactory;
import com.yupi.yurpc.server.HttpServer;
import com.yupi.yurpc.server.tcp.VertxTcpServer;

/**
 * 测试服务提供者 - 通过 EtcdRegistry 注册服务，测试 TcpServerHandler 消息处理
 * <p>
 * 使用方式：
 * 1. 确保 Etcd 服务器已启动（地址配置在 application.properties 中）
 * 2. 先启动 TestProviderExample（注册服务到 Etcd + 启动 TCP 服务器）
 * 3. 再启动 TestConsumerExample（从 Etcd 发现服务并发起调用）
 * <p>
 * 注册流程：
 * - LocalRegistry：本地注册服务实现类，供 TcpServerHandler 通过反射调用
 * - EtcdRegistry：将服务元信息注册到 Etcd，供消费者服务发现
 */
public class TestProviderExample {

    /**
     * 测试用 TCP 服务端口，避免与 HTTP 服务默认端口 8080 冲突
     */
    private static final int TEST_PORT = 8899;

    public static void main(String[] args) {
        // 初始化 RPC 框架
        // 会从 application.properties 加载配置，包括 Etcd 地址、序列化器等
        // 同时初始化 EtcdRegistry 并连接 Etcd 服务器
        RpcApplication.init();

        // 1. 本地注册：注册服务实现类
        // TcpServerHandler 通过 LocalRegistry.get(serviceName) 获取实现类，再通过反射调用
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 2. 远程注册：将服务元信息注册到 Etcd
        // 消费者通过 EtcdRegistry.serviceDiscovery() 发现此服务地址
        Registry registry = RegistryFactory.getInstance(
                RpcApplication.getRpcConfig().getRegistryConfig().getRegistry());

        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(UserService.class.getName());
        serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
        serviceMetaInfo.setServiceHost("127.0.0.1");
        serviceMetaInfo.setServicePort(TEST_PORT);

        try {
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            System.err.println("服务注册到 Etcd 失败: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // 3. 启动 TCP 服务器
        System.out.println("============================================");
        System.out.println("  TestProviderExample 启动完成");
        System.out.println("============================================");
        System.out.println("服务接口:   " + UserService.class.getName());
        System.out.println("实现类:     " + UserServiceImpl.class.getName());
        System.out.println("服务版本:   " + RpcConstant.DEFAULT_SERVICE_VERSION);
        System.out.println("服务地址:   127.0.0.1:" + TEST_PORT);
        System.out.println("Etcd 注册:  已完成");
        System.out.println("Local注册:  已完成");
        System.out.println("等待消费者调用...");
        System.out.println("============================================");

        HttpServer tcpServer = new VertxTcpServer();
        tcpServer.doStart(TEST_PORT);
    }
}
