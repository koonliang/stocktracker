package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.dto.NotificationDtos.NotificationListResponse;
import com.stocktracker.dto.NotificationDtos.NotificationView;
import com.stocktracker.dto.NotificationDtos.ReadAllRequest;
import com.stocktracker.dto.NotificationDtos.ReadAllResponse;
import com.stocktracker.persistence.NotificationRepository;
import com.stocktracker.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;

@ApplicationScoped
public class NotificationService {
  @Inject NotificationRepository notifications;
  @Inject CurrentUser currentUser;

  public NotificationListResponse list(boolean unreadOnly) {
    var all = notifications.listForUser(currentUser.id(), unreadOnly);
    var unreadCount = notifications.unreadCount(currentUser.id());
    return new NotificationListResponse(
        (int) unreadCount,
        all.stream()
            .map(
                n ->
                    new NotificationView(
                        n.id.toString(),
                        n.alertId == null ? null : n.alertId.toString(),
                        n.instrumentSymbol,
                        n.conditionType,
                        n.threshold == null ? 0 : n.threshold.doubleValue(),
                        n.observedCurrency,
                        n.observedValue == null ? 0 : n.observedValue.doubleValue(),
                        n.observedCurrency,
                        n.triggeredAt,
                        n.read,
                        n.message))
            .toList(),
        null);
  }

  public NotificationListResponse list(int limit) {
    var all = notifications.listForUser(currentUser.id(), limit);
    var unreadCount = notifications.unreadCount(currentUser.id());
    return new NotificationListResponse(
        (int) unreadCount,
        all.stream()
            .map(
                n ->
                    new NotificationView(
                        n.id.toString(),
                        n.alertId == null ? null : n.alertId.toString(),
                        n.instrumentSymbol,
                        n.conditionType,
                        n.threshold == null ? 0 : n.threshold.doubleValue(),
                        n.observedCurrency,
                        n.observedValue == null ? 0 : n.observedValue.doubleValue(),
                        n.observedCurrency,
                        n.triggeredAt,
                        n.read,
                        n.message))
            .toList(),
        all.size() >= limit ? all.get(all.size() - 1).id.toString() : null);
  }

  @Transactional
  public void markRead(Long id) {
    var notification =
        notifications
            .findByIdAndUser(id, currentUser.id())
            .orElseThrow(
                () -> new ApiException(Status.NOT_FOUND, "not_found", "Notification not found"));
    notification.read = true;
    notifications.persist(notification);
  }

  @Transactional
  public ReadAllResponse markAllRead(ReadAllRequest request) {
    long updated;
    if (request.ids() != null && !request.ids().isEmpty()) {
      var ids = request.ids().stream().map(Long::valueOf).toList();
      updated = notifications.markRead(currentUser.id(), ids);
    } else {
      updated = notifications.markAllRead(currentUser.id());
    }
    var unreadCount = notifications.unreadCount(currentUser.id());
    return new ReadAllResponse((int) updated, (int) unreadCount);
  }

  @Transactional
  public void delete(Long id) {
    var notification =
        notifications
            .findByIdAndUser(id, currentUser.id())
            .orElseThrow(
                () -> new ApiException(Status.NOT_FOUND, "not_found", "Notification not found"));
    notifications.delete(notification);
  }
}
