package com.stocktracker.api;

import jakarta.ws.rs.core.Response;

public final class ApiStatuses {
  public static final Response.StatusType UNPROCESSABLE_ENTITY =
      new SimpleStatusType(422, "Unprocessable Entity", Response.Status.Family.CLIENT_ERROR);

  private ApiStatuses() {}

  private static final class SimpleStatusType implements Response.StatusType {
    private final int statusCode;
    private final String reasonPhrase;
    private final Response.Status.Family family;

    private SimpleStatusType(int statusCode, String reasonPhrase, Response.Status.Family family) {
      this.statusCode = statusCode;
      this.reasonPhrase = reasonPhrase;
      this.family = family;
    }

    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public String getReasonPhrase() {
      return reasonPhrase;
    }

    @Override
    public Response.Status.Family getFamily() {
      return family;
    }
  }
}
