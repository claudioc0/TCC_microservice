package com.tcc.order.security;

public record AuthenticatedPrincipal(Long id, String email, String role) {}
