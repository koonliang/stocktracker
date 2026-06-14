package com.stocktracker.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public final class AlertDtos {
  private AlertDtos() {}

  public record AlertListResponse(List<AlertView> alerts) {}

  public record AlertRequest(String symbol, String conditionType, BigDecimal threshold) {}

  public record AlertView(
      String id,
      String symbol,
      String conditionType,
      double threshold,
      boolean armed,
      Instant lastTriggeredAt,
      LocalDateTime createdAt) {}

  public record NotificationListResponse(List<NotificationView> notifications) {}

  public record NotificationView(
      String id, String alertId, String message, boolean read, LocalDateTime createdAt) {}
}
