package com.stocktracker.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class FacebookAuthClientTest {
  private final NonProdAuthConfig config = Mockito.mock(NonProdAuthConfig.class);

  @Test
  void exchangeReturnsProfileWhenFacebookEndpointsSucceed() throws Exception {
    try (var server =
        new LocalAuthServer(
            200,
            "{\"access_token\":\"fb-token\"}",
            200,
            "{\"id\":\"fb-sub\",\"email\":\"fb@example.com\"}")) {
      var client = client(server);

      var profile = client.exchange(" code ", "http://localhost/callback");

      assertEquals("fb-sub", profile.subject());
      assertEquals("fb@example.com", profile.email());
      assertEquals(true, profile.emailVerified());
    }
  }

  @Test
  void exchangeRejectsBlankCode() throws Exception {
    try (var server = new LocalAuthServer(200, "{\"access_token\":\"x\"}", 200, "{\"id\":\"y\"}")) {
      var client = client(server);

      var error =
          assertThrows(ApiException.class, () -> client.exchange("", "http://localhost/callback"));

      assertEquals("AUTH_FAILED", error.code());
    }
  }

  @Test
  void exchangeRejectsInvalidTokenResponse() throws Exception {
    try (var server =
        new LocalAuthServer(200, "{\"unexpected\":true}", 200, "{\"id\":\"unused\"}")) {
      var client = client(server);

      var error =
          assertThrows(
              ApiException.class, () -> client.exchange("code", "http://localhost/callback"));

      assertEquals("AUTH_FAILED", error.code());
    }
  }

  @Test
  void exchangeRejectsUserInfoFailures() throws Exception {
    try (var server =
        new LocalAuthServer(200, "{\"access_token\":\"fb-token\"}", 401, "{\"error\":\"bad\"}")) {
      var client = client(server);
      var error =
          assertThrows(
              ApiException.class, () -> client.exchange("code", "http://localhost/callback"));
      assertEquals("AUTH_FAILED", error.code());
    }

    try (var server =
        new LocalAuthServer(
            200,
            "{\"access_token\":\"fb-token\"}",
            200,
            "{\"email\":\"missing-id@example.com\"}")) {
      var client = client(server);
      var error =
          assertThrows(
              ApiException.class, () -> client.exchange("code", "http://localhost/callback"));
      assertEquals("AUTH_FAILED", error.code());
    }
  }

  private TestFacebookAuthClient client(LocalAuthServer server) {
    doNothing().when(config).validateProviderCredentials("facebook");
    when(config.requireRedirectUri("http://localhost/callback"))
        .thenReturn("http://localhost/callback");
    when(config.facebookClientId()).thenReturn("facebook-client");
    when(config.facebookClientSecret()).thenReturn("facebook-secret");

    var client = new TestFacebookAuthClient(server.tokenUri(), server.userInfoUri());
    client.config = config;
    client.objectMapper = new ObjectMapper();
    return client;
  }

  private static final class TestFacebookAuthClient extends FacebookAuthClient {
    private final URI tokenUri;
    private final URI userInfoUri;

    private TestFacebookAuthClient(URI tokenUri, URI userInfoUri) {
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
