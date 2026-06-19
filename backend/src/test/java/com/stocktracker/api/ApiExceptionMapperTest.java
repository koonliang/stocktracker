package com.stocktracker.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.dto.ApiErrorResponse;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Local unit test (no Quarkus context) asserting that an uncaught exception mapped to a 500 also
 * emits a single structured ERROR log line. The line is what CloudWatch captures in the deployed
 * Lambda, so it must survive the mapper handling the throwable itself (FR-027).
 */
class ApiExceptionMapperTest {

  private final ApiExceptionMapper mapper = new ApiExceptionMapper();
  private final List<LogRecord> records = new ArrayList<>();
  private Logger julLogger;
  private Handler handler;
  private boolean previousUseParentHandlers;

  @BeforeEach
  void attachHandler() {
    julLogger = Logger.getLogger(ApiExceptionMapper.class.getName());
    previousUseParentHandlers = julLogger.getUseParentHandlers();
    julLogger.setUseParentHandlers(false);
    handler =
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            records.add(record);
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        };
    handler.setLevel(Level.ALL);
    julLogger.addHandler(handler);
    julLogger.setLevel(Level.ALL);
  }

  @AfterEach
  void detachHandler() {
    julLogger.removeHandler(handler);
    julLogger.setUseParentHandlers(previousUseParentHandlers);
  }

  @Test
  void uncaughtExceptionReturns500AndLogsStructuredLine() {
    var response = mapper.toResponse(new IllegalStateException("boom"));

    assertEquals(500, response.getStatus());
    assertEquals("internal_error", ((ApiErrorResponse) response.getEntity()).code());

    var errorLine =
        records.stream()
            .filter(r -> r.getLevel().intValue() >= Level.SEVERE.intValue())
            .map(LogRecord::getMessage)
            .filter(m -> m.contains("code=internal_error"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no structured internal_error log line emitted"));

    assertTrue(
        errorLine.contains("exception=java.lang.IllegalStateException"),
        "log line should name the exception class, was: " + errorLine);
    assertTrue(errorLine.contains("message=boom"), "log line should carry the message");
  }

  @Test
  void mappedApiExceptionDoesNotLogInternalError() {
    var response =
        mapper.toResponse(
            new ApiException(Response.Status.NOT_FOUND, "not_found", "missing thing"));

    assertEquals(404, response.getStatus());
    assertTrue(
        records.stream().noneMatch(r -> r.getMessage().contains("code=internal_error")),
        "a domain ApiException must not be logged as an internal_error");
  }

  @Test
  void notFoundExceptionReturns404WithoutInternalErrorLog() {
    var response = mapper.toResponse(new NotFoundException("HTTP 404 Not Found"));

    assertEquals(404, response.getStatus());
    assertEquals("not_found", ((ApiErrorResponse) response.getEntity()).code());
    assertTrue(
        records.stream().noneMatch(r -> r.getMessage().contains("code=internal_error")),
        "a NotFoundException must not be logged as an internal_error");
  }
}
