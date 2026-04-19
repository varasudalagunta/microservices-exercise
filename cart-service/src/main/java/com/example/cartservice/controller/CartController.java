package com.example.cartservice.controller;

import com.example.cartservice.dto.CartItemRequest;
import com.example.cartservice.model.Cart;
import com.example.cartservice.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public Cart createCart(@RequestBody Cart cart) {
        return cartService.createCart(cart);
    }

    @GetMapping
    public List<Cart> getAllCarts() {
        return cartService.getAllCarts();
    }

    @GetMapping("/{id}")
    public Cart getCartById(@PathVariable Long id) {
        return cartService.getCartById(id);
    }

    @DeleteMapping("/{id}")
    public String deleteCart(@PathVariable Long id) {
        return cartService.deleteCart(id);
    }

    @PostMapping("/{cartId}/items")
    public Cart addItemToCart(@PathVariable Long cartId, @RequestBody CartItemRequest request) {
        return cartService.addItemToCart(cartId, request.getProductId(), request.getQuantity());
    }

    @PostMapping("/{cartId}/products/{productId}/{quantity}")
    public ResponseEntity<Cart> addItemToCartByPath(@PathVariable Long cartId,
                                                    @PathVariable Long productId,
                                                    @PathVariable Integer quantity) {
        System.out.println("CONTROLLER HIT -> cartId=" + cartId + ", productId=" + productId + ", quantity=" + quantity);
        return ResponseEntity.ok(cartService.addItemToCart(cartId, productId, quantity));
    }
}