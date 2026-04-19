package com.example.cartservice.service;

import com.example.cartservice.dto.CartEvent;
import com.example.cartservice.exception.CartNotFoundException;
import com.example.cartservice.exception.InvalidQuantityException;
import com.example.cartservice.exception.ProductNotFoundException;
import com.example.cartservice.model.Cart;
import com.example.cartservice.model.CartItem;
import com.example.cartservice.producer.CartEventProducer;
import com.example.cartservice.repository.CartItemRepository;
import com.example.cartservice.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final WebClient webClient;
    private final CartEventProducer cartEventProducer;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       WebClient webClient,
                       CartEventProducer cartEventProducer) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.webClient = webClient;
        this.cartEventProducer = cartEventProducer;
    }

    public Cart createCart(Cart cart) {
        return cartRepository.save(cart);
    }

    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    public Cart getCartById(Long id) {
        return cartRepository.findById(id)
                .orElseThrow(() -> new CartNotFoundException("Cart not found with id: " + id));
    }

    public String deleteCart(Long id) {
        if (cartRepository.existsById(id)) {
            cartRepository.deleteById(id);
            return "Cart deleted successfully";
        }
        throw new CartNotFoundException("Cart not found with id: " + id);
    }

    public Cart addItemToCart(Long cartId, Long productId, Integer quantity) {

        log.info("STEP 1 - Add item request received. cartId={}, productId={}, quantity={}", cartId, productId, quantity);

        if (quantity == null || quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than 0");
        }

        String productUrl = "http://localhost:8081/products/" + productId;

        Object productResponse;
        try {
            log.info("STEP 2 - Calling product-service: {}", productUrl);
            productResponse = webClient.get()
                    .uri(productUrl)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            log.info("STEP 3 - Product-service response received: {}", productResponse);
        } catch (Exception e) {
            log.error("STEP 3 FAILED - Product lookup failed", e);
            throw new ProductNotFoundException("Product not found with id: " + productId);
        }

        if (productResponse == null) {
            throw new ProductNotFoundException("Product not found with id: " + productId);
        }

        log.info("STEP 4 - Fetching cart with id={}", cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found with id: " + cartId));

        log.info("STEP 5 - Creating cart item");
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setCart(cart);

        log.info("STEP 6 - Saving cart item");
        cartItemRepository.save(item);

        CartEvent event = new CartEvent(cartId, productId, quantity);
        try {
            log.info("STEP 7 - Sending Kafka event");
            cartEventProducer.sendCartEvent(event);
            log.info("STEP 8 - Kafka event sent successfully");
        } catch (Exception e) {
            log.error("STEP 8 FAILED - Kafka send failed", e);
            throw new RuntimeException("Failed to send Kafka event", e);
        }

        log.info("STEP 9 - Fetching updated cart");
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found after update with id: " + cartId));
    }
}