package com.bm.bmrpc.springboot.starter.bootstrap;

import cn.hutool.core.bean.BeanException;
import com.bm.bmrpc.springboot.starter.annotation.RpcService;
import com.bm.bmrpc.RpcApplication;
import com.bm.bmrpc.config.RegistryConfig;
import com.bm.bmrpc.config.RpcConfig;
import com.bm.bmrpc.model.ServiceMetaInfo;
import com.bm.bmrpc.registry.LocalRegistry;
import com.bm.bmrpc.registry.Registry;
import com.bm.bmrpc.registry.RegistryFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Rpc服务提供者启动
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class RpcProviderBootstrap implements BeanPostProcessor {

    /**
     * Bean初始化后执行，执行服务
     *
     * @param bean
     * @param beanName
     * @return
     * @throw BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean,String beanName) throws BeanException {
        Class<?> beanClass = bean.getClass();
        RpcService rpcService = beanClass.getAnnotation(RpcService.class);
        if(rpcService != null){
            //需要注册服务
            //1.获取服务基本信息
            Class<?> interfaceClass = rpcService.interfaceClass();
            //默认值处理
            if(interfaceClass == void.class){
                interfaceClass = beanClass.getInterfaces()[0];
            }
            String serviceName = interfaceClass.getName();
            String serviceVersion = rpcService.serviceVersion();
            //2.注册服务
            //本地注册
            LocalRegistry.register(serviceName, beanClass);

            //全局配置
            final RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            //注册服务到注册中心
            RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
            Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceVersion(serviceVersion);
            serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
            serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
            try{
                registry.register(serviceMetaInfo);
            }catch (Exception e){
                throw new RuntimeException(serviceName + " 服务注册失败", e);
            }
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean,beanName);
    }
}
