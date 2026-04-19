package com.example.cartservice.producer;

import com.example.cartservice.dto.CartEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartEventProducer {

    private static final String TOPIC = "cart-topic";

    private final KafkaTemplate<String, CartEvent> kafkaTemplate;

    public CartEventProducer(KafkaTemplate<String, CartEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCartEvent(CartEvent event) {
        kafkaTemplate.send(TOPIC, event);
    }
}