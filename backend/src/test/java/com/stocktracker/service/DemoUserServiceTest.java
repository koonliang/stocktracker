package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.api.ApiException;
import com.stocktracker.bootstrap.DevDataBootstrap;
import com.stocktracker.config.NonProdAuthConfig;
import com.stocktracker.domain.AppUser;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserCreateRequest;
import com.stocktracker.persistence.AppUserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DemoUserServiceTest {
  private final AppUserRepository users = Mockito.mock(AppUserRepository.class);
  private final NonProdAuthConfig config = Mockito.mock(NonProdAuthConfig.class);
  private final DevDataBootstrap devDataBootstrap = Mockito.mock(DevDataBootstrap.class);
  private DemoUserService service;

  @BeforeEach
  void setUp() {
    service = new DemoUserService();
    service.users = users;
    service.config = config;
    service.devDataBootstrap = devDataBootstrap;
  }

  @Test
  void catalogSortsBySlotAndReportsCapacity() {
    when(config.demoUserMax()).thenReturn(3);
    when(users.listDemoUsers()).thenReturn(List.of(demoUser(2, "Second"), demoUser(1, "First")));

    var response = service.catalog();

    assertEquals(1, response.users().getFirst().slot());
    assertEquals(true, response.canCreate());
  }

  @Test
  void createRejectsWhenCapacityReached() {
    when(config.demoUsersEnabled()).thenReturn(true);
    when(config.demoUserMax()).thenReturn(1);
    when(users.listDemoUsers()).thenReturn(List.of(demoUser(1, "First")));

    var error = assertThrows(ApiException.class, () -> service.create(new DemoUserCreateRequest("New")));

    assertEquals("DEMO_USER_LIMIT_REACHED", error.code());
  }

  @Test
  void createUsesFirstFreeSlotAndSeedsPortfolio() throws Exception {
    when(config.demoUsersEnabled()).thenReturn(true);
    when(config.demoUserMax()).thenReturn(3);
    when(config.demoUserPrefix()).thenReturn("demo");
    when(users.listDemoUsers()).thenReturn(List.of(demoUser(1, "First")));
    when(users.findDemoUserBySlot(1)).thenReturn(Optional.of(demoUser(1, "First")));
    when(users.findDemoUserBySlot(2)).thenReturn(Optional.empty());
    doAnswer(
            invocation -> {
              var user = invocation.<AppUser>getArgument(0);
              user.id = 8L;
              return null;
            })
        .when(users)
        .persist(any(AppUser.class));

    var created = service.create(new DemoUserCreateRequest("  "));

    assertEquals(8L, created.id);
    assertEquals("demo2@stocktracker.local", created.email);
    assertEquals("Demo User 2", created.displayName);
    assertNotNull(created.demoLastActivatedAt);
    verify(devDataBootstrap).refreshDemoUserPortfolio(created);
  }

  @Test
  void createWrapsSeedingFailure() throws Exception {
    when(config.demoUsersEnabled()).thenReturn(true);
    when(config.demoUserMax()).thenReturn(2);
    when(config.demoUserPrefix()).thenReturn("demo");
    when(users.listDemoUsers()).thenReturn(List.of());
    when(users.findDemoUserBySlot(1)).thenReturn(Optional.empty());
    doThrow(new RuntimeException("boom")).when(devDataBootstrap).refreshDemoUserPortfolio(any(AppUser.class));

    assertThrows(IllegalStateException.class, () -> service.create(new DemoUserCreateRequest("A")));
  }

  @Test
  void loginUpdatesTimestampsForExistingDemoUser() {
    when(config.demoUsersEnabled()).thenReturn(true);
    var user = demoUser(2, "Second");
    when(users.findDemoUserBySlot(2)).thenReturn(Optional.of(user));

    var loggedIn = service.login(2);

    assertEquals(2, loggedIn.demoSlot.intValue());
    assertNotNull(loggedIn.lastLoginAt);
    assertNotNull(loggedIn.demoLastActivatedAt);
  }

  private AppUser demoUser(int slot, String label) {
    var user = new AppUser();
    user.id = (long) slot;
    user.demoSlot = (byte) slot;
    user.displayName = label;
    user.email = "demo" + slot + "@stocktracker.local";
    user.accountKind = AppUser.AccountKind.DEMO;
    return user;
  }
}
