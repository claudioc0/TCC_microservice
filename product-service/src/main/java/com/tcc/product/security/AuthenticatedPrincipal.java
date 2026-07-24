package com.tcc.product.security;

public record AuthenticatedPrincipal(Long id, String email, String role) {}
