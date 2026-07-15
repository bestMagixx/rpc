package com.yupi.example.common.service;

import com.yupi.example.common.model.WebServiceImpl;

public interface WebService {
    static String webAddress = null;
    /**
     * 获取服务器地址
     */
    String getWebAddress();

    static WebService getService(){
        return new WebServiceImpl();
    }
}
