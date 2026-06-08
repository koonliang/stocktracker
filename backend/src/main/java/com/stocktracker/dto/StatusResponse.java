package com.stocktracker.dto;

/**
 * Generic non-enumerating status acknowledgement (e.g. {@code verification_sent}, {@code
 * verified}).
 */
public record StatusResponse(String status) {}
