package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.dto.AlertDtos.NotificationListResponse;
import com.stocktracker.dto.AlertDtos.NotificationView;
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
    return new NotificationListResponse(
        notifications.listForUser(currentUser.id(), unreadOnly).stream()
            .map(
                n ->
                    new NotificationView(
                        n.id.toString(),
                        n.alertId == null ? null : n.alertId.toString(),
                        n.message,
                        n.read,
                        n.createdAt))
            .toList());
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
}
