package com.hmall.api.client.fallback;

import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class ItemClientFallback implements FallbackFactory<ItemClient> {

    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                log.error("查询商品，走了降级");
                return new ArrayList<>();
            }
            /**
             * 扣减库存
             * @param items
             * */
            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                log.error("扣减库存，走了降级");
                throw new RuntimeException("库存服务异常");
            }
        };

    }
}
