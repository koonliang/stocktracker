package com.stocktracker.dto;

public record ResetPasswordRequest(String token, String newPassword) {}
