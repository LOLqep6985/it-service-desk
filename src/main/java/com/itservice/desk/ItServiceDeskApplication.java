package com.itservice.desk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.itservice.desk.mapper")
@EnableScheduling
public class ItServiceDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItServiceDeskApplication.class, args);
    }
}