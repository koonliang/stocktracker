package com.stocktracker.api;

import com.stocktracker.dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.LinkedHashMap;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {
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

    return Response.serverError()
        .type(MediaType.APPLICATION_JSON)
        .entity(new ApiErrorResponse("internal_error", "An unexpected error occurred", null))
        .build();
  }
}
