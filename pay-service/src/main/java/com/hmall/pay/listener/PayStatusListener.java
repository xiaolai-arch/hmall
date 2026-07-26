package com.hmall.pay.listener;

import com.hmall.api.client.OrderClient;
import com.hmall.api.client.PayClient;
import com.hmall.api.dto.MutilDelayMessage;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.api.po.Order;
import com.hmall.common.constants.MqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PayStatusListener {

    private final OrderClient orderClient;
    private final PayClient payClient;
    private final RabbitTemplate rabbitTemplate;

    /**
     * durable = "true" 持久化队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "mark.order.pay.queue", durable = "true"),
            exchange = @Exchange(value = "pay.topic", type = ExchangeTypes.TOPIC),
            key = "pay.status"
    ))
    public void listenPaySuccess(Long orderId) {
        log.info("订单支付成功收到消息，订单id： {}", orderId);
        orderClient.markOrderPaySuccess(orderId);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.DELAY_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(value = MqConstants.DELAY_EXCHANGE, type = ExchangeTypes.TOPIC, delayed = "true"),
            key = MqConstants.DELAY_ORDER_ROUTING_KEY
    ))
    public void listenPayFail(MutilDelayMessage<Long> msg) {
        if (msg == null || msg.getData() == null) {
            log.info("发了个什么垃圾消息？");
            return;
        }

        // 1.查询订单
        Order order = orderClient.getById(msg.getData());
        if (order == null) {
            log.info("订单不存在，订单id: {}", msg.getData());
            return;
        }

        // 2.查询支付状态
        PayOrderDTO payOrderDTO = payClient.getPayOrderByBizOrderNo(msg.getData());

        if (payOrderDTO == null) {
            log.info("支付单不存在，订单id: {}", msg.getData());
            return;
        }
        // 3.判断支付状态
        if (payOrderDTO.getStatus() == 3) {
            // 支付成功
            log.info("支付成功，订单id: {}", msg);
            orderClient.markOrderPaySuccess(msg.getData());
        } else if (payOrderDTO.getStatus() == 2) {
            // 已取消
            log.error("支付订单已经被取消，订单id: {}", msg.getData());
            // TODO: 订单和支付单都取消


        } else {
            // 目前还没支付，判断是否还有延迟时间
            if (msg.hasNext()) {
                // 判断是否还有时间
                // 还有延迟时间，继续发延迟消息
                log.info("还有时间，用户还没支付继续发{}", msg);
                Long delay = msg.popNextTime();
                rabbitTemplate.convertAndSend(MqConstants.DELAY_EXCHANGE, MqConstants.DELAY_ORDER_ROUTING_KEY, msg, new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setDelay(delay.intValue());
                        return message;
                    }
                });
            }else {
                // 没时间了
                log.info("没时间了，所以订单和支付单都取消{}", msg);
                // TODO: 订单和支付单都取消
            }
        }
    }
}
