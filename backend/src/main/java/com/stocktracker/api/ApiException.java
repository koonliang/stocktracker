package com.stocktracker.api;

import jakarta.ws.rs.core.Response.StatusType;
import java.util.Map;

public class ApiException extends RuntimeException {
  private final StatusType status;
  private final String code;
  private final Map<String, Object> details;

  public ApiException(StatusType status, String code, String message) {
    this(status, code, message, null);
  }

  public ApiException(StatusType status, String code, String message, Map<String, Object> details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }

  public StatusType status() {
    return status;
  }

  public String code() {
    return code;
  }

  public Map<String, Object> details() {
    return details;
  }
}
