package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.po.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(value = "trade-service", configuration = DefaultFeignConfig.class)
public interface OrderClient {

    // 根据id查询订单
    @GetMapping("/orders/{id}")
    Order getById(@PathVariable("id") Long id);

    // 修改订单状态为已支付
    @PutMapping("/orders/{orderId}")
    void markOrderPaySuccess(@PathVariable("orderId") Long orderId);

}
