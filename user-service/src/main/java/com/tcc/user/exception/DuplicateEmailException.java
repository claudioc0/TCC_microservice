package com.tcc.user.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}
