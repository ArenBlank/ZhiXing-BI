package com.huang.zhixing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.huang.zhixing.mapper")
public class ZhiXingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhiXingApplication.class, args);
    }
}
