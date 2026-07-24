package com.tcc.user.security;

/**
 * Principal reconstruído a partir das claims do JWT.
 * Nenhum serviço precisa consultar um banco de usuários para autenticar
 * requisições — o token já carrega tudo o que é necessário.
 */
public record AuthenticatedPrincipal(Long id, String email, String role) {}
