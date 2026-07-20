package com.yupi.example.springboot.provider;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.bm.bmrpc.springboot.starter.annotation.RpcService;
import org.springframework.stereotype.Service;

/**
 * 用户实现类
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
@Service
@RpcService
public class UserServiceImpl implements UserService {

    public User getUser(User user){
        System.out.println("用户名:" + user.getName());
        return user;
    }
}
