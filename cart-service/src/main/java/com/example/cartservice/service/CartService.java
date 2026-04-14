package com.example.cartservice.service;

import com.example.cartservice.model.Cart;
import com.example.cartservice.model.CartItem;
import com.example.cartservice.repository.CartItemRepository;
import com.example.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final WebClient webClient;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       WebClient webClient) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.webClient = webClient;
    }

    public Cart createCart(Cart cart) {
        return cartRepository.save(cart);
    }

    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    public Cart getCartById(Long id) {
        Optional<Cart> cart = cartRepository.findById(id);
        return cart.orElse(null);
    }

    public String deleteCart(Long id) {
        if (cartRepository.existsById(id)) {
            cartRepository.deleteById(id);
            return "Cart deleted successfully";
        }
        return "Cart not found";
    }

    public Cart addItemToCart(Long cartId, Long productId, Integer quantity) {

        String productUrl = "http://localhost:8081/products/" + productId;

        try {
            Object productResponse = webClient.get()
                    .uri(productUrl)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            if (productResponse == null) {
                throw new RuntimeException("Product not found");
            }

        } catch (Exception e) {
            throw new RuntimeException("Product not found");
        }

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setCart(cart);

        cartItemRepository.save(item);

        return cartRepository.findById(cartId).orElse(null);
    }
}