package com.example.productservice.controller;

import com.example.productservice.model.Product;
import com.example.productservice.service.ProductService;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }
    @GetMapping("/price/{price}")
    public List<Product> getProductsAbovePriceNative(@PathVariable Double price) {
        return productService.getProductsAbovePriceNative(price);
    }
    @GetMapping("/paginated")
    public Page<Product> getProductsPaginated(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return productService.getProductsPaginated(page, size, sortBy, direction);
    }

    @GetMapping("/filter/{price}")
    public List<Product> getProductsAbovePriceUsingStreams(@PathVariable Double price) {
        return productService.getProductsAbovePriceUsingStreams(price);
    }
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product updatedProduct) {
        return productService.updateProduct(id, updatedProduct);
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        return productService.deleteProduct(id);
    }
}