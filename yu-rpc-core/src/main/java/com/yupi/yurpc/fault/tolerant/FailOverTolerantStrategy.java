package com.yupi.yurpc.fault.tolerant;

import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.constant.RpcMapKeyConstant;
import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.registry.Registry;
import com.yupi.yurpc.registry.RegistryFactory;
import com.yupi.yurpc.server.tcp.VertxTcpClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 转移到其他服务节点 - 容错策略
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
@Slf4j
public class FailOverTolerantStrategy implements TolerantStrategy {

    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        // 自行扩展，获取其他服务节点并调用
        Map.Entry<RpcRequest,ServiceMetaInfo> entry = (Map.Entry<RpcRequest,ServiceMetaInfo>) context.get(RpcMapKeyConstant.TOLERANT_PARAM);

        RpcResponse response;
        //获取注册中心
        Registry registry = RegistryFactory.getInstance(RpcApplication.getRpcConfig().getRegistryConfig().getRegistry());
        List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(entry.getValue().getServiceKey());
        for(ServiceMetaInfo serviceMetaInfo1 : serviceMetaInfoList){
            if(!serviceMetaInfo1.equals(entry.getValue())){
                try {
                    response = VertxTcpClient.doRequest(entry.getKey(), serviceMetaInfo1);
                    return response;
                }catch (Exception ignored){
                }
            }
        }
        throw new RuntimeException("服务报错", e);
    }
}
