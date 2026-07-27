package com.hmall.common.constants;

public interface MqConstants {
    // ========== 延时队列（订单超时取消）==========
    String DELAY_EXCHANGE = "trade.delay.topic";
    String DELAY_ORDER_QUEUE = "trade.order.delay.queue";
    String DELAY_ORDER_ROUTING_KEY = "order.query";

    // ========== 商品上下架通知 ==========
    String ITEM_EXCHANGE_NAME = "item.topic";    // 交换机名称
    String ITEM_UP_KEY = "item.up";              // 上架 routing key
    String ITEM_DOWN_KEY = "item.down";          // 下架 routing key
}
