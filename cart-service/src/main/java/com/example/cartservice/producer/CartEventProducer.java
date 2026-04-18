package com.example.cartservice.producer;

import com.example.cartservice.dto.CartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartEventProducer {

    private static final Logger log = LoggerFactory.getLogger(CartEventProducer.class);
    private static final String TOPIC = "cart-events";

    private final KafkaTemplate<String, CartEvent> kafkaTemplate;

    public CartEventProducer(KafkaTemplate<String, CartEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCartEvent(CartEvent event) {
        log.info("Sending cart event to Kafka: cartId={}, productId={}, quantity={}",
                event.getCartId(), event.getProductId(), event.getQuantity());

        kafkaTemplate.send(TOPIC, event);
    }
}