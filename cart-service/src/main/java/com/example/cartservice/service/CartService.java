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

        log.info("Add item request received. cartId={}, productId={}, quantity={}", cartId, productId, quantity);

        if (quantity == null || quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than 0");
        }

        String productUrl = "http://localhost:8081/products/" + productId;

        Object productResponse;
        try {
            productResponse = webClient.get()
                    .uri(productUrl)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (Exception e) {
            throw new ProductNotFoundException("Product not found with id: " + productId);
        }

        if (productResponse == null) {
            throw new ProductNotFoundException("Product not found with id: " + productId);
        }

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found with id: " + cartId));

        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setCart(cart);

        cartItemRepository.save(item);

       // CartEvent event = new CartEvent(cartId, productId, quantity);
       // cartEventProducer.sendCartEvent(event);

        log.info("Item added successfully to cartId={}", cartId);

        return cartRepository.findById(cartId).orElse(null);
    }
}