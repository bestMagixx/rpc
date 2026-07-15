package com.yupi.example.provider;

import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.constant.RpcConstant;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.registry.LocalRegistry;
import com.yupi.yurpc.registry.Registry;
import com.yupi.yurpc.registry.RegistryFactory;
import com.yupi.yurpc.server.HttpServer;
import com.yupi.yurpc.server.VertxHttpServer;

/**
 * 简易服务提供者示例
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class EasyProviderExample {

    public static void main(String[] args) {
        // RPC 框架初始化
        RpcApplication.init();
        
        // 注册服务
        Registry registry = RegistryFactory.getInstance(RpcApplication.getRpcConfig().getRegistryConfig().getRegistry());

        ServiceMetaInfo metaInfo = new ServiceMetaInfo();
        metaInfo.setServiceName("user");
        metaInfo.setServiceHost("127.0.0.1");
        metaInfo.setServiceVersion("1.0");
        metaInfo.setServicePort(1234);

        try {
            registry.register(metaInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        // 启动 web 服务
//        HttpServer httpServer = new VertxHttpServer();
//        httpServer.doStart(RpcApplication.getRpcConfig().getServerPort());
    }
}
