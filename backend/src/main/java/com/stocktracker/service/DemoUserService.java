package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.bootstrap.DevDataBootstrap;
import com.stocktracker.config.NonProdAuthConfig;
import com.stocktracker.domain.AppUser;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserCatalogResponse;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserCreateRequest;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserListItem;
import com.stocktracker.persistence.AppUserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;
import java.time.LocalDateTime;
import java.util.Comparator;

@ApplicationScoped
public class DemoUserService {
  @Inject AppUserRepository users;
  @Inject NonProdAuthConfig config;
  @Inject DevDataBootstrap devDataBootstrap;

  @Transactional
  public DemoUserCatalogResponse catalog() {
    var entries =
        users.listDemoUsers().stream()
            .sorted(Comparator.comparingInt(user -> user.demoSlot.intValue()))
            .map(user -> new DemoUserListItem(user.demoSlot.intValue(), labelFor(user), user.email))
            .toList();
    return new DemoUserCatalogResponse(
        entries, config.demoUserMax(), entries.size() < config.demoUserMax());
  }

  @Transactional
  public AppUser create(DemoUserCreateRequest request) {
    ensureEnabled();
    var existing = users.listDemoUsers();
    if (existing.size() >= config.demoUserMax()) {
      throw new ApiException(
          Status.CONFLICT,
          "DEMO_USER_LIMIT_REACHED",
          "The maximum of %d demo users already exists.".formatted(config.demoUserMax()));
    }

    int slot = 1;
    while (users.findDemoUserBySlot(slot).isPresent()) {
      slot += 1;
    }

    var user = new AppUser();
    user.email = "%s%d@stocktracker.local".formatted(config.demoUserPrefix(), slot);
    user.status = AppUser.Status.ACTIVE;
    user.emailVerified = true;
    user.accountKind = AppUser.AccountKind.DEMO;
    user.demoSlot = (byte) slot;
    user.displayName = normalizeLabel(request == null ? null : request.label(), slot);
    user.demoSeedProfile = "seed";
    user.demoLastActivatedAt = LocalDateTime.now();
    users.persist(user);
    try {
      devDataBootstrap.refreshDemoUserPortfolio(user);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to seed demo user portfolio data.", exception);
    }
    return user;
  }

  @Transactional
  public AppUser login(int slot) {
    ensureEnabled();
    var user =
        users
            .findDemoUserBySlot(slot)
            .orElseThrow(
                () ->
                    new ApiException(
                        Status.NOT_FOUND, "DEMO_USER_NOT_FOUND", "Unknown demo user."));
    user.lastLoginAt = LocalDateTime.now();
    user.demoLastActivatedAt = LocalDateTime.now();
    return user;
  }

  public void ensureEnabled() {
    if (!config.demoUsersEnabled()) {
      throw new ApiException(Status.NOT_FOUND, "not_found", "Not found");
    }
  }

  public String labelFor(AppUser user) {
    return normalizeLabel(user.displayName, user.demoSlot == null ? 1 : user.demoSlot.intValue());
  }

  private String normalizeLabel(String label, int slot) {
    var trimmed = label == null ? "" : label.trim();
    return trimmed.isBlank() ? "Demo User %d".formatted(slot) : trimmed;
  }
}
