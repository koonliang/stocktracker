package com.stocktracker.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** A linked external provider identity (Google/Facebook via Cognito federation). */
@Entity
@Table(name = "social_identity")
public class SocialIdentity extends PanacheEntityBase {
  public enum Provider {
    GOOGLE,
    FACEBOOK
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  public Provider provider;

  @Column(name = "provider_subject", nullable = false)
  public String providerSubject;

  @Column(name = "provider_email", length = 320)
  public String providerEmail;

  @Column(name = "email_verified", nullable = false)
  public boolean emailVerified;

  @Column(name = "linked_at", nullable = false)
  public LocalDateTime linkedAt;

  @PrePersist
  void prePersist() {
    if (linkedAt == null) {
      linkedAt = LocalDateTime.now();
    }
  }
}
