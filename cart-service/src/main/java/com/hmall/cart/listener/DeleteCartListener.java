package com.hmall.cart.listener;


import com.hmall.api.dto.CartClearMessage;
import com.hmall.cart.service.ICartService;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class DeleteCartListener {

    private final ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "cart.clear.queue", durable = "true"),
            exchange = @Exchange(value = "trade.topic", type = ExchangeTypes.TOPIC),
            key = "order.create"
    ))
    public void listenerDeleteCart(CartClearMessage cartClearMessage) {
        log.info("删除购物车：{}", cartClearMessage);
        // 1.手动设置用户上下文
        UserContext.setUser(cartClearMessage.getUserId());
        // 2.直接调用本地service清理购物车
        cartService.removeByItemIds(cartClearMessage.getItemIds());
        // 3.清除用户上下文
        UserContext.removeUser();
        log.info("删除购物车完成");

    }
}
