package com.tcc.product.controller;

import com.tcc.product.dto.ProductRequest;
import com.tcc.product.dto.ProductResponse;
import com.tcc.product.dto.StockAdjustmentRequest;
import com.tcc.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ── Escrita (somente ADMIN) ───────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Leitura (público) ─────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(productService.searchByName(name));
    }

    // ── Estoque (chamado pelo order-service, autenticado com o token do usuário original) ──

    @PatchMapping("/{id}/stock/decrease")
    public ResponseEntity<ProductResponse> decreaseStock(@PathVariable Long id,
                                                           @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(productService.decreaseStock(id, request.quantity()));
    }

    @PatchMapping("/{id}/stock/increase")
    public ResponseEntity<ProductResponse> increaseStock(@PathVariable Long id,
                                                           @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(productService.increaseStock(id, request.quantity()));
    }
}
