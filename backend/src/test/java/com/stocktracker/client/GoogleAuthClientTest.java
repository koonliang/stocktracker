package com.stocktracker.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.api.ApiException;
import com.stocktracker.config.NonProdAuthConfig;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GoogleAuthClientTest {
  private final NonProdAuthConfig config = Mockito.mock(NonProdAuthConfig.class);

  @Test
  void exchangeReturnsProfileFromLocalOauthServer() throws Exception {
    try (var server =
        new LocalAuthServer(
            200,
            "{\"access_token\":\"token-123\"}",
            200,
            "{\"sub\":\"google-sub\",\"email\":\" \",\"email_verified\":false}")) {
      var client = client(server);

      var profile = client.exchange(" code ", "http://localhost/callback");

      assertEquals("google-sub", profile.subject());
      assertNull(profile.email());
      assertEquals(false, profile.emailVerified());
    }
  }

  @Test
  void exchangeRejectsBlankCode() throws Exception {
    try (var server =
        new LocalAuthServer(200, "{\"access_token\":\"x\"}", 200, "{\"sub\":\"y\"}")) {
      var client = client(server);

      var error =
          assertThrows(
              ApiException.class, () -> client.exchange("   ", "http://localhost/callback"));

      assertEquals("AUTH_FAILED", error.code());
    }
  }

  @Test
  void exchangeRejectsTokenFailures() throws Exception {
    try (var server =
        new LocalAuthServer(401, "{\"error\":\"bad\"}", 200, "{\"sub\":\"unused\"}")) {
      var client = client(server);

      var error =
          assertThrows(
              ApiException.class, () -> client.exchange("code", "http://localhost/callback"));

      assertEquals("AUTH_FAILED", error.code());
    }
  }

  @Test
  void exchangeRejectsMissingTokenAndInvalidUserInfo() throws Exception {
    try (var tokenMissingServer =
        new LocalAuthServer(200, "{\"token_type\":\"Bearer\"}", 200, "{\"sub\":\"unused\"}")) {
      var client = client(tokenMissingServer);
      var error =
          assertThrows(
              ApiException.class, () -> client.exchange("code", "http://localhost/callback"));
      assertEquals("AUTH_FAILED", error.code());
    }

    try (var invalidUserServer =
        new LocalAuthServer(
            200, "{\"access_token\":\"token-123\"}", 200, "{\"email\":\"user@example.com\"}")) {
      var client = client(invalidUserServer);
      var error =
          assertThrows(
              ApiException.class, () -> client.exchange("code", "http://localhost/callback"));
      assertEquals("AUTH_FAILED", error.code());
    }
  }

  private TestGoogleAuthClient client(LocalAuthServer server) {
    doNothing().when(config).validateProviderCredentials("google");
    when(config.requireRedirectUri("http://localhost/callback"))
        .thenReturn("http://localhost/callback");
    when(config.googleClientId()).thenReturn("google-client");
    when(config.googleClientSecret()).thenReturn("google-secret");

    var client = new TestGoogleAuthClient(server.tokenUri(), server.userInfoUri());
    client.config = config;
    client.objectMapper = new ObjectMapper();
    return client;
  }

  private static final class TestGoogleAuthClient extends GoogleAuthClient {
    private final URI tokenUri;
    private final URI userInfoUri;

    private TestGoogleAuthClient(URI tokenUri, URI userInfoUri) {
      this.tokenUri = tokenUri;
      this.userInfoUri = userInfoUri;
    }

    @Override
    URI tokenUri() {
      return tokenUri;
    }

    @Override
    URI userInfoUri() {
      return userInfoUri;
    }
  }

  private static final class LocalAuthServer implements AutoCloseable {
    private final HttpServer server;

    private LocalAuthServer(int tokenStatus, String tokenBody, int userStatus, String userBody)
        throws IOException {
      server = HttpServer.create(new InetSocketAddress(0), 0);
      server.createContext("/token", exchange -> respond(exchange, tokenStatus, tokenBody));
      server.createContext("/userinfo", exchange -> respond(exchange, userStatus, userBody));
      server.start();
    }

    private URI tokenUri() {
      return URI.create("http://localhost:" + server.getAddress().getPort() + "/token");
    }

    private URI userInfoUri() {
      return URI.create("http://localhost:" + server.getAddress().getPort() + "/userinfo");
    }

    @Override
    public void close() {
      server.stop(0);
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
        throws IOException {
      var bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, bytes.length);
      try (OutputStream output = exchange.getResponseBody()) {
        output.write(bytes);
      }
    }
  }
}
