package com.yupi.yurpc.loadbalancer;

import com.yupi.yurpc.spi.SpiLoader;

    /**
     *
     * 负载均衡器工厂（工厂模式，用于获取负载均衡器对象）
     * @learn <a href="https://codefather.cn">编程宝典</a>
     * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
     * @from <a href="https://yupi.icu">编程导航知识星球</a>
     */
public class LoadBalancerFactory {

    /**
     * 默认负载均衡器
     */
    private static final LoadBalancer DEFAULT_LOAD_BALANCER = new RoundRobinLoadBalancer();

    /**
     * 获取Rpc内置负载均衡器接口的实现类对象
     * @param loadBalancerKey
     * @return
     */
    public static LoadBalancer getLoadBalancer(String loadBalancerKey){
        return SpiLoader.getInstance(LoadBalancer.class,loadBalancerKey);
    }

    /**
     * 获取用户自定义负载均衡器接口的实现类对象
     * @param tClass
     * @param loadBalancerKey
     * @return T
     */
    public static <T> T getLoadBalancer(T tClass,String loadBalancerKey){
        return SpiLoader.getInstance(tClass.getClass(),loadBalancerKey);
    }
}
