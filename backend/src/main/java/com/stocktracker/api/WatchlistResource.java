package com.stocktracker.api;

import com.stocktracker.dto.WatchlistMutationRequest;
import com.stocktracker.dto.WatchlistResponse;
import com.stocktracker.service.WatchlistService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/watchlists")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WatchlistResource {
  @Inject WatchlistService watchlistService;

  @GET
  public WatchlistResponse getWatchlists() {
    return watchlistService.listWatchlists();
  }

  @POST
  public WatchlistResponse.WatchlistItemView createWatchlist(WatchlistMutationRequest request) {
    return watchlistService.create(request.name());
  }

  @PATCH
  @Path("/{watchlistId}")
  public WatchlistResponse.WatchlistItemView renameWatchlist(
      @PathParam("watchlistId") UUID watchlistId, WatchlistMutationRequest request) {
    return watchlistService.rename(watchlistId, request.name());
  }

  @DELETE
  @Path("/{watchlistId}")
  public void deleteWatchlist(@PathParam("watchlistId") UUID watchlistId) {
    watchlistService.delete(watchlistId);
  }

  @POST
  @Path("/{watchlistId}/tickers")
  public WatchlistResponse.WatchlistItemView addTicker(
      @PathParam("watchlistId") UUID watchlistId, WatchlistMutationRequest request) {
    return watchlistService.addTicker(watchlistId, request.ticker());
  }

  @DELETE
  @Path("/{watchlistId}/tickers/{ticker}")
  public WatchlistResponse.WatchlistItemView removeTicker(
      @PathParam("watchlistId") UUID watchlistId, @PathParam("ticker") String ticker) {
    return watchlistService.removeTicker(watchlistId, ticker);
  }

  @PUT
  @Path("/{watchlistId}/ticker-order")
  public WatchlistResponse.WatchlistItemView reorderTickers(
      @PathParam("watchlistId") UUID watchlistId, WatchlistMutationRequest request) {
    return watchlistService.reorder(watchlistId, request.tickers());
  }
}
