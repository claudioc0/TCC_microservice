package com.tcc.report.security;

public record AuthenticatedPrincipal(Long id, String email, String role) {}
