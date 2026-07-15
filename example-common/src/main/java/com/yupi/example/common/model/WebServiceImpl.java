package com.yupi.example.common.model;

import com.yupi.example.common.service.WebService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class WebServiceImpl implements WebService {
    private static String webAddress = null;
    @Override
    public String getWebAddress(){
        if(webAddress != null) return webAddress;

        String propertiesFile = "web.properties";
        Properties properties = new Properties();
        try(InputStream inputStream = Files.newInputStream(Paths.get(propertiesFile))){
            if(inputStream == null){
                System.out.println("file is not exit");
                webAddress = "http://localhost:8080";
            }else{
                properties.load(inputStream);
                webAddress = properties.getProperty("webAddress");
                System.out.println();
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        return  webAddress;
    }
}
