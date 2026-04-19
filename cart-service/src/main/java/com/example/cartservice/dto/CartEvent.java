package com.example.cartservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CartEvent {

    private Long cartId;
    private Long productId;
    private Integer quantity;

    public CartEvent() {
    }

    public CartEvent(Long cartId, Long productId, Integer quantity) {
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
    }
}