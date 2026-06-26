package com.hmall.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import springfox.documentation.spring.web.plugins.DefaultConfiguration;

import java.util.Collection;

@FeignClient(value = "cart-service", configuration = DefaultConfiguration.class)
public interface CartClient {

    // 对应的方法名可以不同：feign主要是根据方法路径，参数进行地址构建的
    @DeleteMapping("/carts")
    public void deleteCartItemByIds(@RequestParam("ids") Collection<Long> ids);

}
