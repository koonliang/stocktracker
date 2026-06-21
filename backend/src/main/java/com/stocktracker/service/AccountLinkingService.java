package com.stocktracker.service;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.persistence.AppUserRepository;
import com.stocktracker.persistence.SocialIdentityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Locale;
import org.jboss.logging.Logger;

/**
 * Resolves the local {@link AppUser} behind a federated (Google/Facebook via Cognito) identity. A
 * known {@code (provider, subject)} returns its linked account. A new subject is auto-linked to an
 * existing account only when the provider asserts a verified email matching that account (FR-S03);
 * otherwise a fresh account is created. An unverified or absent provider email is never auto-linked
 * to a pre-existing account (FR-S04).
 */
@ApplicationScoped
public class AccountLinkingService {
  private static final Logger LOG = Logger.getLogger(AccountLinkingService.class);

  @Inject AppUserRepository users;
  @Inject SocialIdentityRepository socialIdentities;

  /**
   * @param provider the federated provider
   * @param subject the provider-side stable subject id
   * @param email the email asserted by the provider (may be null)
   * @param emailVerified whether the provider marked the email verified
   * @return the local account that owns this identity
   */
  @Transactional
  public AppUser resolveOrLink(
      SocialIdentity.Provider provider, String subject, String email, boolean emailVerified) {
    return resolveOrLink(provider, subject, email, emailVerified, false);
  }

  @Transactional
  public AppUser resolveOrLink(
      SocialIdentity.Provider provider,
      String subject,
      String email,
      boolean emailVerified,
      boolean activateImmediately) {
    var existingIdentity = socialIdentities.findByProviderSubject(provider, subject).orElse(null);
    if (existingIdentity != null) {
      return users.findById(existingIdentity.userId);
    }

    var normalizedEmail = email == null ? null : AppUser.normalizeEmail(email);
    AppUser user = null;
    // Auto-link to a pre-existing account only when the provider verified the email (FR-S03/S04).
    if (emailVerified && normalizedEmail != null && !normalizedEmail.isBlank()) {
      user = users.findByNormalizedEmail(normalizedEmail).orElse(null);
    }
    if (user == null) {
      user = createAccount(provider, subject, normalizedEmail, emailVerified, activateImmediately);
      LOG.infof("event=social_account_created provider=%s user_id=%d", provider, user.id);
    } else {
      if (activateImmediately) {
        user.status = AppUser.Status.ACTIVE;
        user.emailVerified = true;
      }
      LOG.infof("event=social_account_linked provider=%s user_id=%d", provider, user.id);
    }

    linkIdentity(user, provider, subject, normalizedEmail, emailVerified);
    return user;
  }

  /**
   * Provisions a non-federated (email+password) Cognito account on its first validated token. Runs
   * in its own transaction so the {@code persist} has an active context — it must be invoked
   * through the injected proxy, never by self-invocation.
   */
  @Transactional
  public AppUser provisionByEmail(String email) {
    var user = new AppUser();
    user.email = AppUser.normalizeEmail(email);
    user.status = AppUser.Status.ACTIVE;
    user.emailVerified = true;
    users.persist(user);
    return user;
  }

  private AppUser createAccount(
      SocialIdentity.Provider provider,
      String subject,
      String normalizedEmail,
      boolean emailVerified,
      boolean activateImmediately) {
    var user = new AppUser();
    // Only a provider-verified email is trusted as the account's identity. An unverified email is
    // never adopted and must not collide with an existing account (FR-S04), so fall back to a
    // unique provider-scoped synthetic address; the asserted email is still kept on the identity.
    var trustedEmail = emailVerified && normalizedEmail != null && !normalizedEmail.isBlank();
    user.email =
        trustedEmail
            ? normalizedEmail
            : provider.name().toLowerCase(Locale.ROOT) + "-" + subject + "@federated.local";
    user.status = AppUser.Status.ACTIVE;
    user.emailVerified = activateImmediately || emailVerified;
    users.persist(user);
    return user;
  }

  private void linkIdentity(
      AppUser user,
      SocialIdentity.Provider provider,
      String subject,
      String normalizedEmail,
      boolean emailVerified) {
    var identity = new SocialIdentity();
    identity.userId = user.id;
    identity.provider = provider;
    identity.providerSubject = subject;
    identity.providerEmail = normalizedEmail;
    identity.emailVerified = emailVerified;
    identity.persist();
  }
}
