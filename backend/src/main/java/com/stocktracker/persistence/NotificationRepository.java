package com.stocktracker.persistence;

import com.stocktracker.domain.Notification;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
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

  public Optional<Notification> findByIdAndUser(Long id, Long userId) {
    return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
  }
}
