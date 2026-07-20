package com.bm.bmrpc.proxy;

import cn.hutool.core.collection.CollUtil;
import com.bm.bmrpc.config.RpcConfig;
import com.bm.bmrpc.model.RpcRequest;
import com.bm.bmrpc.model.RpcResponse;
import com.bm.bmrpc.model.ServiceMetaInfo;
import com.bm.bmrpc.registry.Registry;
import com.bm.bmrpc.registry.RegistryFactory;
import com.bm.bmrpc.RpcApplication;
import com.bm.bmrpc.constant.RpcConstant;
import com.bm.bmrpc.constant.RpcMapKeyConstant;
import com.bm.bmrpc.fault.retry.RetryStrategy;
import com.bm.bmrpc.fault.retry.RetryStrategyFactory;
import com.bm.bmrpc.fault.tolerant.TolerantStrategy;
import com.bm.bmrpc.fault.tolerant.TolerantStrategyFactory;
import com.bm.bmrpc.fault.tolerant.TolerantStrategyKeys;
import com.bm.bmrpc.loadbalancer.LoadBalancerFactory;
import com.bm.bmrpc.serializer.Serializer;
import com.bm.bmrpc.serializer.SerializerFactory;
import com.bm.bmrpc.server.tcp.VertxTcpClient;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务代理（JDK 动态代理）
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class ServiceProxy implements InvocationHandler {

    /**
     * 调用代理
     *
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 指定序列化器
        final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

        // 构造请求
        String serviceName = method.getDeclaringClass().getName();
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
        try {
            // 序列化
            byte[] bodyBytes = serializer.serialize(rpcRequest);
            // 从注册中心获取服务提供者请求地址
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
            List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
            if (CollUtil.isEmpty(serviceMetaInfoList)) {
                throw new RuntimeException("暂无服务地址");
            }
            //使用负载均衡来获取要请求的节点
            Map<String,Object> requestParam = new HashMap<>();
            //使用要调用的方法名作为负载均衡参数
            requestParam.put(RpcMapKeyConstant.LOADBALANCER_PARAM,rpcRequest.getMethodName());
            ServiceMetaInfo selectedServiceMetaInfo = LoadBalancerFactory.getLoadBalancer(RpcApplication.getRpcConfig().getLoadBalancer())
                    .select(requestParam,serviceMetaInfoList);
            //Rpc请求
            //使用重试机制
            RpcResponse rpcResponse;
            try {
                RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(RpcApplication.getRpcConfig().getRetryStrategy());
                //使用封装好的VertxClient发送请求
                rpcResponse = retryStrategy.doRetry(() -> VertxTcpClient.doRequest(rpcRequest,selectedServiceMetaInfo));
            } catch (Exception e) {
                //容错机制
                TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(rpcConfig.getTolerantStrategy());
                if(rpcConfig.getTolerantStrategy().equals(TolerantStrategyKeys.FAIL_OVER)){
                    Map<String,Object> serviceMetaInfoMap = new HashMap<>();
                    Map.Entry<RpcRequest,ServiceMetaInfo> entry = new AbstractMap.SimpleEntry<>(rpcRequest,selectedServiceMetaInfo);
                    serviceMetaInfoMap.put(RpcMapKeyConstant.TOLERANT_PARAM, entry);
                    rpcResponse = tolerantStrategy.doTolerant(serviceMetaInfoMap,e);
                }else {
                    rpcResponse = tolerantStrategy.doTolerant(null, e);
                }
            }
            return rpcResponse.getData();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
