package com.bm.bmrpc.springboot.starter.bootstrap;

import com.bm.bmrpc.springboot.starter.annotation.RpcReference;
import com.bm.bmrpc.proxy.ServiceProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

/**
 * Rpc服务提供者启动
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class RpcConsumerBootstrap implements BeanPostProcessor {

    /**
     * Bean 初始化后执行，注入服务
     *
     * @param bean
     * @param beanName
     * @return
     * @throw BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName){
        Class<?> beanClass = bean.getClass();
        // 遍历整个类继承链（兼容 @Configuration 等被 CGLIB 代理的 Bean，
        // 代理子类的 getDeclaredFields() 拿不到父类字段）
        for(Class<?> currentClass = beanClass; currentClass != null; currentClass = currentClass.getSuperclass()){
            Field[] declaredFields = currentClass.getDeclaredFields();
            for(Field field : declaredFields){
                RpcReference rpcReference = field.getAnnotation(RpcReference.class);
                if(rpcReference != null){
                    //为属性生成代理对象
                    Class<?> interfaceClass = rpcReference.interfaceClass();
                    if(interfaceClass == void.class){
                        interfaceClass = field.getType();
                    }
                    field.setAccessible(true);
                    Object proxyObject = ServiceProxyFactory.getProxy(interfaceClass);
                    try{
                        field.set(bean, proxyObject);
                        field.setAccessible(false);
                    }catch (IllegalAccessException e){
                        throw new RuntimeException("为字段注入代理对象失败", e);
                    }
                }
            }
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
