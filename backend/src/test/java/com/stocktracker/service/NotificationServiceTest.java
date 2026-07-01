package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.Notification;
import com.stocktracker.dto.NotificationDtos.ReadAllRequest;
import com.stocktracker.persistence.NotificationRepository;
import com.stocktracker.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NotificationServiceTest {
  private final NotificationRepository notifications = Mockito.mock(NotificationRepository.class);
  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);
  private NotificationService service;

  @BeforeEach
  void setUp() {
    service = new NotificationService();
    service.notifications = notifications;
    service.currentUser = currentUser;
  }

  @Test
  void listUnreadMapsNotificationsAndUnreadCount() {
    when(currentUser.id()).thenReturn(1L);
    when(notifications.listForUser(1L, true)).thenReturn(List.of(notification(9L, false)));
    when(notifications.unreadCount(1L)).thenReturn(3L);

    var response = service.list(true);

    assertEquals(3, response.unreadCount());
    assertEquals(1, response.notifications().size());
    assertEquals(0.0, response.notifications().getFirst().threshold());
  }

  @Test
  void listWithLimitSetsNextCursorWhenPageIsFull() {
    when(currentUser.id()).thenReturn(1L);
    when(notifications.listForUser(1L, 2))
        .thenReturn(List.of(notification(7L, false), notification(8L, true)));
    when(notifications.unreadCount(1L)).thenReturn(1L);

    var response = service.list(2);

    assertEquals("8", response.nextCursor());
  }

  @Test
  void markReadRejectsUnknownNotification() {
    when(currentUser.id()).thenReturn(1L);
    when(notifications.findByIdAndUser(4L, 1L)).thenReturn(Optional.empty());

    var error = assertThrows(ApiException.class, () -> service.markRead(4L));

    assertEquals("not_found", error.code());
  }

  @Test
  void markAllReadUsesSpecificIdsWhenProvided() {
    when(currentUser.id()).thenReturn(1L);
    when(notifications.markRead(1L, List.of(4L, 5L))).thenReturn(2L);
    when(notifications.unreadCount(1L)).thenReturn(1L);

    var response = service.markAllRead(new ReadAllRequest(List.of("4", "5")));

    assertEquals(2, response.updated());
    assertEquals(1, response.unreadCount());
  }

  @Test
  void markReadPersistsOwnedNotification() {
    when(currentUser.id()).thenReturn(1L);
    var notification = notification(4L, false);
    when(notifications.findByIdAndUser(4L, 1L)).thenReturn(Optional.of(notification));

    service.markRead(4L);

    assertEquals(true, notification.read);
    verify(notifications).persist(notification);
  }

  @Test
  void markAllReadWithoutIdsUsesBulkReadAll() {
    when(currentUser.id()).thenReturn(1L);
    when(notifications.markAllRead(1L)).thenReturn(3L);
    when(notifications.unreadCount(1L)).thenReturn(0L);

    var response = service.markAllRead(new ReadAllRequest(null));

    assertEquals(3, response.updated());
    assertEquals(0, response.unreadCount());
  }

  @Test
  void listWithLimitOmitsNextCursorWhenPageNotFull() {
    when(currentUser.id()).thenReturn(1L);
    when(notifications.listForUser(1L, 3)).thenReturn(List.of(notification(7L, false)));
    when(notifications.unreadCount(1L)).thenReturn(1L);

    var response = service.list(3);

    assertEquals(null, response.nextCursor());
  }

  @Test
  void deleteRemovesOwnedNotification() {
    when(currentUser.id()).thenReturn(1L);
    var notification = notification(3L, false);
    when(notifications.findByIdAndUser(3L, 1L)).thenReturn(Optional.of(notification));

    service.delete(3L);

    verify(notifications).delete(notification);
  }

  private Notification notification(Long id, boolean read) {
    var notification = new Notification();
    notification.id = id;
    notification.alertId = 2L;
    notification.instrumentSymbol = "AAPL";
    notification.conditionType = "price_above";
    notification.threshold = null;
    notification.observedValue = new BigDecimal("100");
    notification.observedCurrency = "USD";
    notification.triggeredAt = LocalDateTime.parse("2026-06-26T10:15:30");
    notification.read = read;
    notification.message = "Triggered";
    return notification;
  }
}
