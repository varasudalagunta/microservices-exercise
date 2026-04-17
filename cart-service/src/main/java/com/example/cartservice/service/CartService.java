package com.example.cartservice.service;

import com.example.cartservice.model.Cart;
import com.example.cartservice.model.CartItem;
import com.example.cartservice.repository.CartItemRepository;
import com.example.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.cartservice.exception.CartNotFoundException;
import com.example.cartservice.exception.InvalidQuantityException;
import com.example.cartservice.exception.ProductNotFoundException;



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

        return cartRepository.findById(cartId).orElse(null);
    }
}