package com.stocktracker.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class NotificationDtos {
  private NotificationDtos() {}

  public record NotificationListResponse(
      int unreadCount, List<NotificationView> notifications, String nextCursor) {}

  public record NotificationView(
      String id,
      String alertId,
      String symbol,
      String conditionType,
      double threshold,
      String thresholdCurrency,
      double observedValue,
      String observedCurrency,
      LocalDateTime triggeredAt,
      boolean read,
      String message) {}

  public record ReadAllRequest(List<String> ids) {}

  public record ReadAllResponse(int updated, int unreadCount) {}
}
