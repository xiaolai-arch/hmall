package com.hmall.cart;

import com.hmall.api.client.ItemClient;
import com.hmall.api.config.DefaultFeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

//开启OpenFeign功能，可以扫描到那些feign客户端, 这里可以指定多个client = "ItemClient.Class"
// basePackages = {"com.hmall.api.client"} 扫描客户端的路径
// defaultConfiguration = DefaultFeignConfig.class feign配置类（日志记录）
@EnableFeignClients(basePackages = {"com.hmall.api.client"}, defaultConfiguration = DefaultFeignConfig.class)
@MapperScan("com.hmall.cart.mapper")
@SpringBootApplication
public class CartApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}
