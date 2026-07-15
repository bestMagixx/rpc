package com.yupi.yurpc.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.serializer.JdkSerializer;
import com.yupi.yurpc.serializer.Serializer;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 服务代理（JDK动态代理）
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
        //指定序列化器
        Serializer serializer = new JdkSerializer();

        //构造请求
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
        try{
            //序列化
            byte[] bodyBytes = serializer.serialize(rpcRequest);
            System.out.printf("发送字节头：%02x %02x\n",bodyBytes[0],bodyBytes[1]);
            //发送请求
            //TODO 注意这里地址被硬编码了（需要使用注册中心和服务发现机制解决）
            try(HttpResponse httpResponse = HttpRequest.post("http://localhost:8080")
                    .body(bodyBytes)
                    .execute()){
                byte[] result = httpResponse.bodyBytes();
                System.out.printf("接收字节头：%02x %02x\n",result[0],result[1]);
                //反序列化
                RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
                return rpcResponse.getData();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
}
