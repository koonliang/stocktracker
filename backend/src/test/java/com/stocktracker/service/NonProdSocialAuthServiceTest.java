package com.stocktracker.service;

import com.stocktracker.client.FacebookAuthClient;
import com.stocktracker.client.GoogleAuthClient;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.dto.NonProdAuthDtos.SocialExchangeRequest;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class NonProdSocialAuthServiceTest extends IntegrationTestSupport {
  @Inject NonProdSocialAuthService service;

  @BeforeEach
  void resetSocialAccounts() throws Exception {
    QuarkusMock.installMockForType(
        new GoogleAuthClient() {
          @Override
          public ProviderProfile exchange(String code, String redirectUri) {
            return switch (code) {
              case "google-new-user" ->
                  new ProviderProfile("google-sub-new-user", "new.user@gmail.com", true);
              case "google-returning-user" ->
                  new ProviderProfile("google-sub-returning-user", "returning.user@gmail.com", true);
              default ->
                  throw new com.stocktracker.api.ApiException(
                      Status.UNAUTHORIZED, "AUTH_FAILED", "Unable to complete sign-in.");
            };
          }
        },
        GoogleAuthClient.class);
    QuarkusMock.installMockForType(
        new FacebookAuthClient() {
          @Override
          public GoogleAuthClient.ProviderProfile exchange(String code, String redirectUri) {
            if ("facebook-unverified-user".equals(code)) {
              return new GoogleAuthClient.ProviderProfile(
                  "facebook-sub-unverified-user", "returning.user@gmail.com", false);
            }
            throw new com.stocktracker.api.ApiException(
                Status.UNAUTHORIZED, "AUTH_FAILED", "Unable to complete sign-in.");
          }
        },
        FacebookAuthClient.class);

    inTransaction(
        () -> {
          SocialIdentity.deleteAll();
          AppUser.delete("email like ?1 or email like ?2", "%@gmail.com", "%@federated.local");
        });
  }

  @Test
  void createsAndActivatesNewGoogleAccountFromProviderProfile() {
    var response =
        service.exchange(
            "google",
            new SocialExchangeRequest("google-new-user", "http://localhost:5173/auth/callback"));

    Assertions.assertNotNull(response.token());
    Assertions.assertEquals("new.user@gmail.com", response.user().email());
    Assertions.assertEquals(1, AppUser.count("email", "new.user@gmail.com"));
    Assertions.assertEquals(
        1,
        SocialIdentity.count(
            "provider = ?1 and providerSubject = ?2",
            SocialIdentity.Provider.GOOGLE,
            "google-sub-new-user"));
  }

  @Test
  void reusesExistingVerifiedAccountWhenProviderEmailMatches() throws Exception {
    var existingId = new Long[1];
    inTransaction(
        () -> {
          var user = new AppUser();
          user.email = AppUser.normalizeEmail("returning.user@gmail.com");
          user.status = AppUser.Status.ACTIVE;
          user.emailVerified = true;
          user.persist();
          existingId[0] = user.id;
        });

    var response =
        service.exchange(
            "google",
            new SocialExchangeRequest(
                "google-returning-user", "http://localhost:5173/auth/callback"));

    Assertions.assertEquals("returning.user@gmail.com", response.user().email());
    Assertions.assertEquals(1, AppUser.count("email", "returning.user@gmail.com"));
    inTransaction(
        () -> {
          var identity =
              SocialIdentity.find(
                      "provider = ?1 and providerSubject = ?2",
                      SocialIdentity.Provider.GOOGLE,
                      "google-sub-returning-user")
                  .firstResult();
          Assertions.assertNotNull(identity);
          Assertions.assertEquals(existingId[0], ((SocialIdentity) identity).userId);
        });
  }

  @Test
  void doesNotLinkUnverifiedFacebookEmailToExistingAccount() throws Exception {
    var existingId = new Long[1];
    inTransaction(
        () -> {
          var user = new AppUser();
          user.email = AppUser.normalizeEmail("returning.user@gmail.com");
          user.status = AppUser.Status.ACTIVE;
          user.emailVerified = true;
          user.persist();
          existingId[0] = user.id;
        });

    var response =
        service.exchange(
            "facebook",
            new SocialExchangeRequest(
                "facebook-unverified-user", "http://localhost:5173/auth/callback"));

    Assertions.assertNotEquals("returning.user@gmail.com", response.user().email());
    Assertions.assertNotEquals(existingId[0], loadUserIdByEmail(response.user().email()));
    Assertions.assertEquals(1, AppUser.count("email", "returning.user@gmail.com"));
  }

  @Test
  void activatesPreviouslyUnverifiedAccountWhenSocialExchangeMatchesVerifiedEmail()
      throws Exception {
    inTransaction(
        () -> {
          var user = new AppUser();
          user.email = AppUser.normalizeEmail("returning.user@gmail.com");
          user.status = AppUser.Status.UNVERIFIED;
          user.emailVerified = false;
          user.persist();
        });

    var response =
        service.exchange(
            "google",
            new SocialExchangeRequest(
                "google-returning-user", "http://localhost:5173/auth/callback"));

    Assertions.assertEquals("returning.user@gmail.com", response.user().email());
    inTransaction(
        () -> {
          var user = (AppUser) AppUser.find("email", "returning.user@gmail.com").firstResult();
          Assertions.assertNotNull(user);
          Assertions.assertEquals(AppUser.Status.ACTIVE, user.status);
          Assertions.assertTrue(user.emailVerified);
        });
  }

  private Long loadUserIdByEmail(String email) throws Exception {
    var result = new Long[1];
    inTransaction(
        () -> {
          var user = (AppUser) AppUser.find("email", email).firstResult();
          result[0] = user == null ? null : user.id;
        });
    return result[0];
  }
}
