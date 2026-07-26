package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.dto.PayOrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "pay-service", configuration = DefaultFeignConfig.class)
public interface PayClient {

    @GetMapping("/pay-orders/bizOrderNo/{bizOrderNo}")
    PayOrderDTO getPayOrderByBizOrderNo(@PathVariable("bizOrderNo") Long bizOrderNo);
}
