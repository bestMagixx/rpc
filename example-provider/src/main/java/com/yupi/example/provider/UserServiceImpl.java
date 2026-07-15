package com.yupi.example.provider;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;

public class UserServiceImpl implements UserService {

    public User getUser(User user){
        System.out.println("用户名:" + user.getName());
        return user;
    }
}
