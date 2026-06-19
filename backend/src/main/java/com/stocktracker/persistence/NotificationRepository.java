package com.stocktracker.persistence;

import com.stocktracker.domain.Notification;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class NotificationRepository implements PanacheRepositoryBase<Notification, Long> {
  public List<Notification> listForUser(Long userId, boolean unreadOnly) {
    if (unreadOnly) {
      return list("userId = ?1 and read = false order by createdAt desc", userId);
    }
    return list("userId = ?1 order by createdAt desc", userId);
  }

  public List<Notification> listForUser(Long userId, int limit) {
    return find("userId = ?1 order by triggeredAt desc, id desc", userId).page(0, limit).list();
  }

  public Optional<Notification> findByIdAndUser(Long id, Long userId) {
    return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
  }

  public long unreadCount(Long userId) {
    return count("userId = ?1 and read = false", userId);
  }

  @Transactional
  public long markAllRead(Long userId) {
    return update("read = true where userId = ?1 and read = false", userId);
  }

  @Transactional
  public long markRead(Long userId, List<Long> ids) {
    return update("read = true where userId = ?1 and id in ?2", userId, ids);
  }

  @Transactional
  public long deleteByAlertId(Long alertId) {
    return delete("alertId", alertId);
  }
}
