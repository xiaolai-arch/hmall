package com.hmall.pay.listener;

import com.hmall.api.client.OrderClient;
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
public class PayStatusListener {

    private final OrderClient orderClient;

    /**
     * durable = "true" 持久化队列
     * */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "mark.order.pay.queue", durable = "true"),
            exchange = @Exchange(value = "pay.topic", type = ExchangeTypes.TOPIC),
            key = "pay.status"
    ))
    public void listenPaySuccess(Long orderId) {
        log.info("订单支付成功收到消息，订单id： {}", orderId);
        orderClient.markOrderPaySuccess(orderId);
    }
}
