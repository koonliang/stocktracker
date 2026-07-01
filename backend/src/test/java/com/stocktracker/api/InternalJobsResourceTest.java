package com.stocktracker.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.stocktracker.scheduler.FxRefreshJob;
import com.stocktracker.scheduler.PriceHistoryRefreshJob;
import com.stocktracker.scheduler.QuoteRefreshJob;
import com.stocktracker.scheduler.TokenCleanupJob;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InternalJobsResourceTest {
  private final QuoteRefreshJob quoteRefreshJob = Mockito.mock(QuoteRefreshJob.class);
  private final PriceHistoryRefreshJob priceHistoryRefreshJob =
      Mockito.mock(PriceHistoryRefreshJob.class);
  private final TokenCleanupJob tokenCleanupJob = Mockito.mock(TokenCleanupJob.class);
  private final FxRefreshJob fxRefreshJob = Mockito.mock(FxRefreshJob.class);

  private InternalJobsResource resource;

  @BeforeEach
  void setUp() {
    resource = new InternalJobsResource();
    resource.quoteRefreshJob = quoteRefreshJob;
    resource.priceHistoryRefreshJob = priceHistoryRefreshJob;
    resource.tokenCleanupJob = tokenCleanupJob;
    resource.fxRefreshJob = fxRefreshJob;
  }

  @Test
  void endpointsRunJobsWhenSchedulerTokenMatches() {
    resource.schedulerToken = Optional.of("secret");

    assertEquals(202, resource.quoteRefresh("secret").getStatus());
    assertEquals(202, resource.tokenCleanup("secret").getStatus());
    assertEquals(202, resource.priceHistoryRefresh("secret").getStatus());
    assertEquals(202, resource.fxRefresh("secret").getStatus());

    verify(quoteRefreshJob).refresh();
    verify(tokenCleanupJob).purge();
    verify(priceHistoryRefreshJob).refresh();
    verify(fxRefreshJob).refresh();
  }

  @Test
  void rejectsRequestWhenConfiguredTokenIsMissing() {
    resource.schedulerToken = Optional.empty();

    var error = assertThrows(ApiException.class, () -> resource.quoteRefresh("secret"));

    assertEquals("unauthorized", error.code());
  }

  @Test
  void rejectsRequestWhenConfiguredTokenIsBlank() {
    resource.schedulerToken = Optional.of("   ");

    var error = assertThrows(ApiException.class, () -> resource.tokenCleanup("secret"));

    assertEquals("unauthorized", error.code());
  }

  @Test
  void rejectsRequestWhenIncomingTokenIsNull() {
    resource.schedulerToken = Optional.of("secret");

    var error = assertThrows(ApiException.class, () -> resource.priceHistoryRefresh(null));

    assertEquals("unauthorized", error.code());
  }

  @Test
  void rejectsRequestWhenIncomingTokenDoesNotMatch() {
    resource.schedulerToken = Optional.of("secret");

    var error = assertThrows(ApiException.class, () -> resource.fxRefresh("wrong"));

    assertEquals("unauthorized", error.code());
  }
}
