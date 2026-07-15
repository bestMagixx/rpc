package com.yupi.example.common.service;

import com.yupi.example.common.model.User;

/**
 * 用户服务
 */
public interface UserService {
    /**
     * 获取用户
     *
     * @param user
     * @return
     */
    User getUser(User user);

    default int getInt(){
        return 1;
    }
}
