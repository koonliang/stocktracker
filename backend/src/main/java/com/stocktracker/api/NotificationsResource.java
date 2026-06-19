package com.stocktracker.api;

import com.stocktracker.dto.NotificationDtos.NotificationListResponse;
import com.stocktracker.dto.NotificationDtos.ReadAllRequest;
import com.stocktracker.dto.NotificationDtos.ReadAllResponse;
import com.stocktracker.service.NotificationService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/notifications")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class NotificationsResource {
  @Inject NotificationService notificationService;

  @GET
  public NotificationListResponse list(
      @QueryParam("unread") boolean unread, @QueryParam("limit") int limit) {
    if (limit > 0) {
      return notificationService.list(limit);
    }
    return notificationService.list(unread);
  }

  @POST
  @Path("/{id}/read")
  public void read(@PathParam("id") Long id) {
    notificationService.markRead(id);
  }

  @POST
  @Path("/read-all")
  public ReadAllResponse readAll(ReadAllRequest request) {
    return notificationService.markAllRead(request);
  }

  @DELETE
  @Path("/{id}")
  public void delete(@PathParam("id") Long id) {
    notificationService.delete(id);
  }
}
