package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.po.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "trade-service", configuration = DefaultFeignConfig.class)
public interface OrderClient {

    // 修改订单状态
    @PostMapping("/order/updateStatus")
    public void updateById(@RequestBody Order order);

}
