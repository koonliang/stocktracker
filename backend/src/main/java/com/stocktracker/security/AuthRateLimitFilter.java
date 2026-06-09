package com.stocktracker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.dto.ApiErrorResponse;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Throttles {@code /api/auth/*} with a per-IP and per-email sliding window (FR-020/SC-006). Both
 * keys are checked so neither a single client nor repeated attempts at one account can brute-force.
 * In production Cognito additionally throttles its own endpoints.
 */
@Provider
public class AuthRateLimitFilter implements ContainerRequestFilter {
  @ConfigProperty(name = "stocktracker.auth.rate-limit.max-attempts", defaultValue = "10")
  int maxAttempts;

  @ConfigProperty(name = "stocktracker.auth.rate-limit.window-seconds", defaultValue = "60")
  long windowSeconds;

  @Inject ObjectMapper objectMapper;

  // Inject the request via CDI (a stable @RequestScoped bean) rather than @Context field injection
  // of the Vert.x HttpServerRequest, which is fragile under ArC bean-resolution in this provider.
  @Inject CurrentVertxRequest currentRequest;

  private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

  @Override
  public void filter(ContainerRequestContext context) throws IOException {
    var path = context.getUriInfo().getRequestUri().getPath();
    if (path == null || !path.startsWith("/api/auth/")) {
      return;
    }

    var now = System.currentTimeMillis();
    var ip = remoteIp();
    if (isOverLimit("ip:" + ip, now) | isOverLimit("email:" + extractEmail(context), now)) {
      context.abortWith(
          Response.status(429)
              .type(MediaType.APPLICATION_JSON)
              .entity(
                  new ApiErrorResponse(
                      "rate_limited", "Too many attempts, please try again later", null))
              .build());
    }
  }

  private String remoteIp() {
    var context = currentRequest.getCurrent();
    if (context == null || context.request() == null || context.request().remoteAddress() == null) {
      return "unknown";
    }
    return context.request().remoteAddress().hostAddress();
  }

  private boolean isOverLimit(String key, long now) {
    var windowMillis = windowSeconds * 1000;
    var timestamps = hits.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    synchronized (timestamps) {
      while (!timestamps.isEmpty() && timestamps.peekFirst() < now - windowMillis) {
        timestamps.pollFirst();
      }
      if (timestamps.size() >= maxAttempts) {
        return true;
      }
      timestamps.addLast(now);
      return false;
    }
  }

  private String extractEmail(ContainerRequestContext context) throws IOException {
    if (!context.hasEntity()) {
      return "none";
    }
    var body = context.getEntityStream().readAllBytes();
    // Reset the stream so the resource method can still read the body.
    context.setEntityStream(new ByteArrayInputStream(body));
    if (body.length == 0) {
      return "none";
    }
    try {
      var node = objectMapper.readTree(body);
      var email = node.get("email");
      return email != null && !email.isNull() ? email.asText().trim().toLowerCase() : "none";
    } catch (IOException ignored) {
      return "none";
    }
  }
}
