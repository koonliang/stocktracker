package com.stocktracker.dto;

public record LoginResponse(String token, UserResponse user) {}
