package com.stocktracker.service;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.domain.SocialIdentity.Provider;
import com.stocktracker.persistence.SocialIdentityRepository;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verified-email social account linking (FR-S03/S04, FR-T04) with mocked provider claims: a new
 * subject creates an account; a verified email matching an existing account links to it (no
 * duplicate); an unverified provider email is never auto-linked.
 */
@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class AccountLinkingTest extends IntegrationTestSupport {
  @Inject AccountLinkingService accountLinking;
  @Inject SocialIdentityRepository socialIdentities;

  @BeforeEach
  void cleanSocialAccounts() throws Exception {
    inTransaction(
        () -> {
          SocialIdentity.delete(
              "userId in (select u.id from AppUser u where u.email like ?1 or u.email like ?2)",
              "%@social.example",
              "%@federated.local");
          AppUser.delete("email like ?1 or email like ?2", "%@social.example", "%@federated.local");
        });
  }

  @Test
  void newVerifiedIdentityCreatesAccountAndLinksIdentity() {
    var user =
        accountLinking.resolveOrLink(
            Provider.GOOGLE, "google-sub-new", "fresh@social.example", true);

    Assertions.assertNotNull(user.id);
    Assertions.assertEquals(AppUser.Status.ACTIVE, user.status);
    Assertions.assertEquals("fresh@social.example", user.email);
    Assertions.assertTrue(
        socialIdentities.findByProviderSubject(Provider.GOOGLE, "google-sub-new").isPresent());
  }

  @Test
  void verifiedEmailMatchingExistingAccountLinksWithoutDuplicate() throws Exception {
    var existingId = new Long[1];
    inTransaction(
        () -> {
          var existing = new AppUser();
          existing.email = AppUser.normalizeEmail("shared@social.example");
          existing.status = AppUser.Status.ACTIVE;
          existing.emailVerified = true;
          existing.persist();
          existingId[0] = existing.id;
        });

    var user =
        accountLinking.resolveOrLink(
            Provider.GOOGLE, "google-sub-link", "shared@social.example", true);

    // Linked to the existing account — no second account for the same email (FR-S03).
    Assertions.assertEquals(existingId[0], user.id);
    Assertions.assertEquals(1, AppUser.count("email", "shared@social.example"));
    Assertions.assertTrue(
        socialIdentities.findByProviderSubject(Provider.GOOGLE, "google-sub-link").isPresent());
  }

  @Test
  void unverifiedProviderEmailIsNotLinkedToExistingAccount() throws Exception {
    var existingId = new Long[1];
    inTransaction(
        () -> {
          var existing = new AppUser();
          existing.email = AppUser.normalizeEmail("guarded@social.example");
          existing.status = AppUser.Status.ACTIVE;
          existing.emailVerified = true;
          existing.persist();
          existingId[0] = existing.id;
        });

    var user =
        accountLinking.resolveOrLink(
            Provider.FACEBOOK, "fb-sub-unverified", "guarded@social.example", false);

    // Refused link: a distinct account is created, leaving the pre-existing one untouched (FR-S04).
    Assertions.assertNotEquals(existingId[0], user.id);
    Assertions.assertNotEquals("guarded@social.example", user.email);
    Assertions.assertEquals(1, AppUser.count("email", "guarded@social.example"));
  }

  @Test
  void knownSubjectResolvesToTheSameAccount() {
    var first =
        accountLinking.resolveOrLink(
            Provider.GOOGLE, "google-sub-stable", "repeat@social.example", true);
    var second =
        accountLinking.resolveOrLink(
            Provider.GOOGLE, "google-sub-stable", "repeat@social.example", true);

    Assertions.assertEquals(first.id, second.id);
    Assertions.assertEquals(1, SocialIdentity.count("providerSubject", "google-sub-stable"));
  }
}
