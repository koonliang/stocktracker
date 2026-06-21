package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserCreateRequest;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class DemoUserServiceTest extends IntegrationTestSupport {
  @Inject DemoUserService service;

  @BeforeEach
  void resetDemoUsers() throws Exception {
    inTransaction(() -> AppUser.delete("accountKind", AppUser.AccountKind.DEMO));
  }

  @Test
  void createUsesLowestAvailableSlotAndNormalizesBlankLabels() throws Exception {
    var first = service.create(new DemoUserCreateRequest("   "));
    var second = service.create(new DemoUserCreateRequest("Growth Demo"));

    inTransaction(
        () -> {
          PortfolioTransaction.delete("userId", first.id);
          AppUser.deleteById(first.id);
        });

    var replacement = service.create(new DemoUserCreateRequest(null));

    Assertions.assertEquals(1, replacement.demoSlot.intValue());
    Assertions.assertEquals("Demo User 1", service.labelFor(replacement));
    Assertions.assertEquals("Growth Demo", service.labelFor(second));
    Assertions.assertEquals(AppUser.AccountKind.DEMO, replacement.accountKind);
    Assertions.assertTrue(replacement.emailVerified);
  }

  @Test
  void catalogIsSortedAndReportsWhenMoreDemoUsersCanBeCreated() {
    service.create(new DemoUserCreateRequest("Demo User 1"));
    service.create(new DemoUserCreateRequest("Demo User 2"));

    var catalog = service.catalog();

    Assertions.assertEquals(2, catalog.users().size());
    Assertions.assertEquals(1, catalog.users().get(0).slot());
    Assertions.assertEquals(2, catalog.users().get(1).slot());
    Assertions.assertEquals(3, catalog.maxUsers());
    Assertions.assertTrue(catalog.canCreate());
  }

  @Test
  void createRejectsFourthDemoUser() {
    service.create(new DemoUserCreateRequest("Demo User 1"));
    service.create(new DemoUserCreateRequest("Demo User 2"));
    service.create(new DemoUserCreateRequest("Demo User 3"));

    var error =
        Assertions.assertThrows(
            ApiException.class, () -> service.create(new DemoUserCreateRequest("Demo User 4")));

    Assertions.assertEquals(409, error.status().getStatusCode());
    Assertions.assertEquals("DEMO_USER_LIMIT_REACHED", error.code());
  }

  @Test
  void loginUpdatesDemoActivationAndLastLoginTimestamps() throws Exception {
    var created = service.create(new DemoUserCreateRequest("Demo User 1"));

    Thread.sleep(5L);
    var loggedIn = service.login(created.demoSlot.intValue());

    Assertions.assertNotNull(loggedIn.lastLoginAt);
    Assertions.assertNotNull(loggedIn.demoLastActivatedAt);

    var reloaded = new AppUser[1];
    inTransaction(() -> reloaded[0] = AppUser.findById(loggedIn.id));
    Assertions.assertNotNull(reloaded[0]);
    Assertions.assertNotNull(reloaded[0].lastLoginAt);
    Assertions.assertNotNull(reloaded[0].demoLastActivatedAt);
  }
}
