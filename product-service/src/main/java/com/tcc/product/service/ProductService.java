package com.tcc.product.service;

import com.tcc.product.dto.ProductRequest;
import com.tcc.product.dto.ProductResponse;
import com.tcc.product.entity.Product;
import com.tcc.product.exception.ProductNotFoundException;
import com.tcc.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product(request.name(), request.description(), request.price(), request.stockQuantity());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProductOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream().map(ProductResponse::from).toList();
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProductOrThrow(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        return ProductResponse.from(productRepository.save(product));
    }

    public void delete(Long id) {
        getProductOrThrow(id);
        productRepository.deleteById(id);
    }

    public ProductResponse decreaseStock(Long id, int quantity) {
        Product product = getProductOrThrow(id);
        product.decreaseStock(quantity);
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse increaseStock(Long id, int quantity) {
        Product product = getProductOrThrow(id);
        product.increaseStock(quantity);
        return ProductResponse.from(productRepository.save(product));
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
