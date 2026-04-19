package com.example.productservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CartEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CartEventConsumer.class);

    @KafkaListener(topics = "cart-topic", groupId = "product-service-group")
    public void consume(String event) {
        log.info("✅ Received cart event in product-service: {}", event);
    }
}