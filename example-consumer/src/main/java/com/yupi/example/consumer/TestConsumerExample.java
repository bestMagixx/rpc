package com.yupi.example.consumer;

import cn.hutool.core.collection.CollUtil;
import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.bm.bmrpc.RpcApplication;
import com.bm.bmrpc.constant.RpcConstant;
import com.bm.bmrpc.model.RpcRequest;
import com.bm.bmrpc.model.RpcResponse;
import com.bm.bmrpc.model.ServiceMetaInfo;
import com.bm.bmrpc.registry.Registry;
import com.bm.bmrpc.registry.RegistryFactory;
import com.bm.bmrpc.server.tcp.VertxTcpClient;

import java.util.List;

/**
 * 测试服务消费者 - 通过 EtcdRegistry 发现服务，测试 TcpServerHandler 消息处理
 * <p>
 * 使用方式：
 * 1. 确保 Etcd 服务器已启动（地址配置在 application.properties 中）
 * 2. 先启动 TestProviderExample（注册服务到 Etcd + 启动 TCP 服务器）
 * 3. 再启动 TestConsumerExample（从 Etcd 发现服务并发起调用）
 * <p>
 * 调用流程：
 * - EtcdRegistry.serviceDiscovery() 从 Etcd 获取服务提供者地址
 * - VertxTcpClient.doRequest() 通过 TCP 协议发送请求
 * - 完整链路: ProtocolMessageEncoder → TCP → TcpServerHandler → ProtocolMessageDecoder → RpcResponse
 */
public class TestConsumerExample {

    public static void main(String[] args) {
        try {
            // 初始化 RPC 框架
            // 会从 application.properties 加载配置，包括 Etcd 地址、序列化器等
            // 同时初始化 EtcdRegistry 并连接 Etcd 服务器
            RpcApplication.init();

            // 构造请求参数
            User paramUser = new User();
            paramUser.setName("testUser");

            // 构造 RpcRequest
            // serviceName 使用接口全限定名，需与 Provider 注册时的 serviceName 一致
            RpcRequest rpcRequest = RpcRequest.builder()
                    .serviceName(UserService.class.getName())
                    .methodName("getUser")
                    .parameterTypes(new Class[]{User.class})
                    .args(new Object[]{paramUser})
                    .build();

            // 通过 EtcdRegistry 服务发现获取服务提供者地址
            Registry registry = RegistryFactory.getInstance(
                    RpcApplication.getRpcConfig().getRegistryConfig().getRegistry());

            // 构造查询条件（serviceName + version 组成 serviceKey）
            ServiceMetaInfo queryMetaInfo = new ServiceMetaInfo();
            queryMetaInfo.setServiceName(UserService.class.getName());
            queryMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);

            List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(queryMetaInfo.getServiceKey());

            if (CollUtil.isEmpty(serviceMetaInfoList)) {
                System.err.println("❌ 测试失败: 未从 Etcd 发现服务 " + queryMetaInfo.getServiceKey());
                System.err.println("   请确保 TestProviderExample 已启动并完成注册");
                return;
            }

            // 取第一个服务节点（简单负载均衡）
            ServiceMetaInfo selectedService = serviceMetaInfoList.get(0);

            System.out.println("============================================");
            System.out.println("  TestConsumerExample 启动");
            System.out.println("============================================");
            System.out.println("目标服务:   " + rpcRequest.getServiceName());
            System.out.println("调用方法:   " + rpcRequest.getMethodName());
            System.out.println("请求参数:   name=" + paramUser.getName());
            System.out.println("发现节点:   " + selectedService.getServiceHost() + ":" + selectedService.getServicePort());
            System.out.println("Etcd 发现:  成功 (共 " + serviceMetaInfoList.size() + " 个节点)");
            System.out.println();

            // 通过 VertxTcpClient 发送 TCP 请求
            // 完整协议链路:
            // 请求: ProtocolMessageEncoder.encode → TCP 发送
            // 响应: TCP 接收 → TcpServerHandler 处理 → ProtocolMessageDecoder.decode → RpcResponse
            RpcResponse rpcResponse = VertxTcpClient.doRequest(rpcRequest, selectedService);

            // 处理响应结果
            System.out.println("--- 响应结果 ---");
            if (rpcResponse.getException() != null) {
                System.err.println("调用异常: " + rpcResponse.getMessage());
                rpcResponse.getException().printStackTrace();
            } else if (rpcResponse.getData() != null) {
                User resultUser = (User) rpcResponse.getData();
                System.out.println("响应消息:   " + rpcResponse.getMessage());
                System.out.println("返回数据:   name=" + resultUser.getName());
                System.out.println("数据类型:   " + rpcResponse.getDataType().getName());
                System.out.println();

                // 验证结果
                boolean nameMatch = resultUser.getName().equals(paramUser.getName());
                if (nameMatch) {
                    System.out.println("✅ 测试通过!");
                    System.out.println("   - EtcdRegistry 服务注册/发现正确");
                    System.out.println("   - TcpServerHandler 协议编解码正确");
                    System.out.println("   - 服务反射调用正确");
                    System.out.println("   - 返回值与预期一致: " + paramUser.getName());
                } else {
                    System.err.println("❌ 测试失败: 返回值与预期不一致");
                    System.err.println("   预期: " + paramUser.getName() + ", 实际: " + resultUser.getName());
                }
            } else {
                System.err.println("❌ 测试失败: 返回数据为空");
                System.err.println("响应消息: " + rpcResponse.getMessage());
            }

        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
