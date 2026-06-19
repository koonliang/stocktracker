package com.stocktracker.api;

import com.stocktracker.dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import org.jboss.logging.Logger;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {
  private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class);

  @Override
  public Response toResponse(Throwable exception) {
    if (exception instanceof ApiException apiException) {
      return Response.status(apiException.status())
          .type(MediaType.APPLICATION_JSON)
          .entity(
              new ApiErrorResponse(
                  apiException.code(), apiException.getMessage(), apiException.details()))
          .build();
    }

    if (exception instanceof ConstraintViolationException validationException) {
      var details = new LinkedHashMap<String, Object>();
      details.put(
          "violations",
          validationException.getConstraintViolations().stream()
              .map(v -> v.getPropertyPath() + ": " + v.getMessage())
              .toList());
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new ApiErrorResponse("validation_error", "Request validation failed", details))
          .build();
    }

    if (exception instanceof NotFoundException) {
      LOG.debugf(
          "code=not_found exception=%s message=%s",
          exception.getClass().getName(),
          exception.getMessage());
      return Response.status(Response.Status.NOT_FOUND)
          .type(MediaType.APPLICATION_JSON)
          .entity(new ApiErrorResponse("not_found", "Resource not found", null))
          .build();
    }

    // Uncaught exceptions are handled here, so Quarkus' default unhandled-exception
    // logging never fires. Emit one structured ERROR line (with the stack trace) so
    // CloudWatch captures the failure for every 500 the API returns.
    LOG.errorf(
        exception,
        "code=internal_error exception=%s message=%s",
        exception.getClass().getName(),
        exception.getMessage());

    return Response.serverError()
        .type(MediaType.APPLICATION_JSON)
        .entity(new ApiErrorResponse("internal_error", "An unexpected error occurred", null))
        .build();
  }
}
