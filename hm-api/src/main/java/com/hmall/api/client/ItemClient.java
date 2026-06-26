package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

// 标注他是一个feign客户端，指定微服务名称
// 这样可以获取该微服务的服务实例列表
// 并基于负载均衡选择一个服务实例
@FeignClient(value = "item-service", configuration = DefaultFeignConfig.class)
public interface ItemClient {

    // 在接口内：编写远程调用的方法；这些方法都可以参考：远程服务接口controller

    // 根据商品id查询商品
    @GetMapping("/items")
    public List<ItemDTO> queryItemByIds(@RequestParam ("ids") Collection<Long> ids);

    @PutMapping("/items/stock/deduct")
    public void deductStock(@RequestBody List<OrderDetailDTO> items);
}
