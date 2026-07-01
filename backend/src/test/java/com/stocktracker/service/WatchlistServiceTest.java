package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.Watchlist;
import com.stocktracker.domain.WatchlistItem;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.WatchlistRepository;
import com.stocktracker.security.CurrentUser;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WatchlistServiceTest {
  private final WatchlistRepository watchlistRepository = Mockito.mock(WatchlistRepository.class);
  private final InstrumentRepository instrumentRepository =
      Mockito.mock(InstrumentRepository.class);
  private final EntityManager entityManager = Mockito.mock(EntityManager.class);
  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);

  private WatchlistService service;

  @BeforeEach
  void setUp() {
    service = new WatchlistService();
    service.watchlistRepository = watchlistRepository;
    service.instrumentRepository = instrumentRepository;
    service.entityManager = entityManager;
    service.currentUser = currentUser;
  }

  @Test
  void listWatchlistsMapsTickersAndInstrumentDetails() {
    when(currentUser.id()).thenReturn(1L);
    var watchlist = watchlist(9L, "Tech");
    when(watchlistRepository.listByUserUpdatedAt(1L)).thenReturn(List.of(watchlist));
    when(watchlistRepository.listItems(9L))
        .thenReturn(List.of(item(9L, "AAPL", 0), item(9L, "MSFT", 1)));
    when(instrumentRepository.findBySymbols(List.of("AAPL", "MSFT")))
        .thenReturn(
            Map.of(
                "AAPL", instrument("AAPL", "Apple", "NASDAQ", "USD"),
                "MSFT", instrument("MSFT", "Microsoft", "NASDAQ", "USD")));

    var response = service.listWatchlists();

    assertEquals(1, response.watchlists().size());
    assertEquals(List.of("AAPL", "MSFT"), response.watchlists().getFirst().tickers());
    assertEquals("Apple", response.watchlists().getFirst().instruments().getFirst().name());
  }

  @Test
  void createTrimsNameAndPersistsWatchlist() {
    when(currentUser.id()).thenReturn(3L);
    when(watchlistRepository.findByUserAndNameIgnoreCase(3L, "Growth"))
        .thenReturn(Optional.empty());
    doAnswer(
            invocation -> {
              var watchlist = invocation.<Watchlist>getArgument(0);
              watchlist.id = 41L;
              watchlist.createdAt = LocalDateTime.parse("2026-06-20T10:15:30");
              watchlist.updatedAt = LocalDateTime.parse("2026-06-20T10:15:30");
              return null;
            })
        .when(watchlistRepository)
        .persist(any(Watchlist.class));
    when(watchlistRepository.listItems(41L)).thenReturn(List.of());

    var created = service.create("  Growth ");

    verify(watchlistRepository).persist(any(Watchlist.class));
    assertEquals("Growth", created.name());
    assertEquals(List.of(), created.tickers());
  }

  @Test
  void renameRejectsDuplicateNameOwnedBySameUser() {
    when(currentUser.id()).thenReturn(5L);
    var existing = watchlist(11L, "Income");
    when(watchlistRepository.findByIdAndUser(11L, 5L)).thenReturn(Optional.of(existing));
    when(watchlistRepository.findByUserAndNameIgnoreCase(5L, "Growth"))
        .thenReturn(Optional.of(watchlist(12L, "Growth")));

    var error = assertThrows(ApiException.class, () -> service.rename(11L, "Growth"));

    assertEquals("duplicate_name", error.code());
  }

  @Test
  void createRejectsBlankAndTooLongNames() {
    when(currentUser.id()).thenReturn(3L);

    var blank = assertThrows(ApiException.class, () -> service.create("   "));
    assertEquals("validation_error", blank.code());

    var longName = assertThrows(ApiException.class, () -> service.create("x".repeat(41)));
    assertEquals("validation_error", longName.code());
  }

  @Test
  void renameUpdatesExistingWatchlist() {
    when(currentUser.id()).thenReturn(5L);
    var existing = watchlist(11L, "Income");
    when(watchlistRepository.findByIdAndUser(11L, 5L)).thenReturn(Optional.of(existing));
    when(watchlistRepository.findByUserAndNameIgnoreCase(5L, "Growth"))
        .thenReturn(Optional.empty());
    when(watchlistRepository.listItems(11L)).thenReturn(List.of());

    var response = service.rename(11L, " Growth ");

    assertEquals("Growth", response.name());
    verify(watchlistRepository).persist(existing);
  }

  @Test
  void addTickerRejectsUnknownTicker() {
    when(currentUser.id()).thenReturn(7L);
    var watchlist = watchlist(20L, "Core");
    when(watchlistRepository.findByIdAndUser(20L, 7L)).thenReturn(Optional.of(watchlist));
    when(instrumentRepository.existsSymbol("AAPL")).thenReturn(false);

    var error = assertThrows(ApiException.class, () -> service.addTicker(20L, " aapl "));

    assertEquals("validation_error", error.code());
  }

  @Test
  void renameAllowsSameNameForSameWatchlist() {
    when(currentUser.id()).thenReturn(5L);
    var existing = watchlist(11L, "Income");
    when(watchlistRepository.findByIdAndUser(11L, 5L)).thenReturn(Optional.of(existing));
    when(watchlistRepository.findByUserAndNameIgnoreCase(5L, "Income"))
        .thenReturn(Optional.of(existing));
    when(watchlistRepository.listItems(11L)).thenReturn(List.of());

    var response = service.rename(11L, "Income");

    assertEquals("Income", response.name());
  }

  @Test
  void reorderRejectsTickerListThatDoesNotMatchExistingItems() {
    when(currentUser.id()).thenReturn(8L);
    when(watchlistRepository.findByIdAndUser(30L, 8L))
        .thenReturn(Optional.of(watchlist(30L, "ETF")));
    when(watchlistRepository.listItems(30L))
        .thenReturn(List.of(item(30L, "SPY", 0), item(30L, "QQQ", 1)));

    var error = assertThrows(ApiException.class, () -> service.reorder(30L, List.of("SPY")));

    assertEquals("invalid_order", error.code());
  }

  @Test
  void reorderTemporarilyOffsetsExistingItemsThenAppliesNewOrder() {
    when(currentUser.id()).thenReturn(8L);
    var watchlist = watchlist(30L, "ETF");
    var spyItem1 = Mockito.spy(item(30L, "SPY", 0));
    var spyItem2 = Mockito.spy(item(30L, "QQQ", 1));
    when(watchlistRepository.findByIdAndUser(30L, 8L)).thenReturn(Optional.of(watchlist));
    when(watchlistRepository.listItems(30L))
        .thenReturn(List.of(spyItem1, spyItem2))
        .thenReturn(List.of(spyItem2, spyItem1));
    when(instrumentRepository.findBySymbols(List.of("QQQ", "SPY")))
        .thenReturn(
            Map.of(
                "QQQ", instrument("QQQ", "Invesco QQQ", "NASDAQ", "USD"),
                "SPY", instrument("SPY", "SPDR S&P 500", "NYSEARCA", "USD")));

    var response = service.reorder(30L, List.of("qqq", "spy"));

    verify(entityManager).flush();
    assertEquals(1, spyItem1.displayOrder);
    assertEquals(0, spyItem2.displayOrder);
    assertEquals(List.of("QQQ", "SPY"), response.tickers());
  }

  private Watchlist watchlist(Long id, String name) {
    var watchlist = new Watchlist();
    watchlist.id = id;
    watchlist.userId = 1L;
    watchlist.name = name;
    watchlist.createdAt = LocalDateTime.parse("2026-06-20T10:15:30");
    watchlist.updatedAt = LocalDateTime.parse("2026-06-20T10:15:30");
    return watchlist;
  }

  private WatchlistItem item(Long watchlistId, String symbol, int displayOrder) {
    var item = new WatchlistItem();
    item.watchlistId = watchlistId;
    item.instrumentSymbol = symbol;
    item.displayOrder = displayOrder;
    return item;
  }

  private Instrument instrument(String symbol, String name, String exchange, String currency) {
    var instrument = new Instrument();
    instrument.symbol = symbol;
    instrument.name = name;
    instrument.exchange = exchange;
    instrument.currency = currency;
    return instrument;
  }
}
