package com.example.Tecmed_App.Domain.Users.Exception;

public class UserValidationException extends RuntimeException {
    public UserValidationException(String message) {
        super(message);
    }
}