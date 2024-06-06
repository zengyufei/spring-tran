package com.example.tran;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
@EnableAsync
@SpringBootApplication
@MapperScan("com.example.tran.mapper")
public class SpringTranApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringTranApplication.class, args);
    }

}
