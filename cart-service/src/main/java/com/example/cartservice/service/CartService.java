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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import java.util.List;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final WebClient webClient;
    private final CartEventProducer cartEventProducer;
    private final Executor taskExecutor;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       WebClient webClient,
                       CartEventProducer cartEventProducer, Executor taskExecutor) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.webClient = webClient;
        this.cartEventProducer = cartEventProducer;
        this.taskExecutor = taskExecutor;
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

        CompletableFuture<Object> productFuture = CompletableFuture.supplyAsync(() -> {
            log.info("ASYNC STEP A - Calling product-service: {}", productUrl);
            try {
                return webClient.get()
                        .uri(productUrl)
                        .retrieve()
                        .bodyToMono(Object.class)
                        .block();
            } catch (Exception e) {
                log.error("ASYNC STEP A FAILED - Product lookup failed", e);
                throw new ProductNotFoundException("Product not found with id: " + productId);
            }
        }, taskExecutor);

        CompletableFuture<Cart> cartFuture = CompletableFuture.supplyAsync(() -> {
            log.info("ASYNC STEP B - Fetching cart with id={}", cartId);
            return cartRepository.findById(cartId)
                    .orElseThrow(() -> new CartNotFoundException("Cart not found with id: " + cartId));
        }, taskExecutor);

        Object productResponse;
        Cart cart;

        try {
            productResponse = productFuture.get();
            cart = cartFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Async processing interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Async processing failed", e.getCause());
        }

        if (productResponse == null) {
            throw new ProductNotFoundException("Product not found with id: " + productId);
        }

        log.info("STEP 2 - Product and cart fetched successfully");

        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setCart(cart);

        log.info("STEP 3 - Saving cart item");
        cartItemRepository.save(item);

        CartEvent event = new CartEvent(cartId, productId, quantity);
        try {
            log.info("STEP 4 - Sending Kafka event");
            cartEventProducer.sendCartEvent(event);
            log.info("STEP 5 - Kafka event sent successfully");
        } catch (Exception e) {
            log.error("STEP 5 FAILED - Kafka send failed", e);
            throw new RuntimeException("Failed to send Kafka event", e);
        }

        log.info("STEP 6 - Fetching updated cart");
        Cart updatedCart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found after update with id: " + cartId));

        log.info("STEP 7 - Add item completed successfully for cartId={}, productId={}, quantity={}",
                cartId, productId, quantity);

        return updatedCart;
    }
}