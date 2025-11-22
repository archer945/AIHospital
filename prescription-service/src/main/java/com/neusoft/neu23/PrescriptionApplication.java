package com.neusoft.neu23;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@MapperScan("com.neusoft.neu23.mapper")
@EnableDiscoveryClient
public class PrescriptionApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PrescriptionApplication.class, args);
    }
}