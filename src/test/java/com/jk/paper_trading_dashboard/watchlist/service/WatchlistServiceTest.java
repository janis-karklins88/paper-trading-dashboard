package com.jk.paper_trading_dashboard.watchlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jk.paper_trading_dashboard.marketdata.symbol.domain.AssetType;
import com.jk.paper_trading_dashboard.marketdata.symbol.domain.Symbol;
import com.jk.paper_trading_dashboard.marketdata.symbol.repository.SymbolRepository;
import com.jk.paper_trading_dashboard.shared.exception.AlreadyExistsException;
import com.jk.paper_trading_dashboard.shared.exception.BadRequestException;
import com.jk.paper_trading_dashboard.user.domain.User;
import com.jk.paper_trading_dashboard.user.repository.UserRepository;
import com.jk.paper_trading_dashboard.watchlist.config.DefaultWatchlistConfig;
import com.jk.paper_trading_dashboard.watchlist.domain.Watchlist;
import com.jk.paper_trading_dashboard.watchlist.domain.WatchlistItem;
import com.jk.paper_trading_dashboard.watchlist.repository.WatchlistItemRepository;
import com.jk.paper_trading_dashboard.watchlist.repository.WatchlistRepository;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

  @Mock
  private DefaultWatchlistLoader defaultWatchlistLoader;

  @Mock
  private WatchlistRepository watchlistRepository;

  @Mock
  private WatchlistItemRepository watchlistItemRepository;

  @Mock
  private SymbolRepository symbolRepository;

  @Mock
  private UserRepository userRepository;

  private WatchlistService watchlistService;

  @BeforeEach
  void setUp() {
    watchlistService = new WatchlistService(
        defaultWatchlistLoader,
        watchlistRepository,
        watchlistItemRepository,
        symbolRepository,
        userRepository);
  }

  @Test
  @SuppressWarnings("unchecked")
  void createDefaultWatchlistsCreatesMissingWatchlistAndItems() {
    User user = new User("test@example.com", "hash");
    user.setId(UUID.randomUUID());
    Watchlist savedWatchlist = new Watchlist(user, "Crypto", true);
    savedWatchlist.setId(UUID.randomUUID());
    when(defaultWatchlistLoader.load()).thenReturn(List.of(
        new DefaultWatchlistConfig("Crypto", List.of("btc/usd", "ETH/USD", "BTC/USD"))));
    when(watchlistRepository.findByUser_IdAndNameIgnoreCase(user.getId(), "Crypto")).thenReturn(Optional.empty());
    when(watchlistRepository.save(any(Watchlist.class))).thenReturn(savedWatchlist);
    when(watchlistItemRepository.existsByWatchlist_IdAndSymbol(savedWatchlist.getId(), "BTC/USD")).thenReturn(false);
    when(watchlistItemRepository.existsByWatchlist_IdAndSymbol(savedWatchlist.getId(), "ETH/USD")).thenReturn(false);

    var responses = watchlistService.createDefaultWatchlists(user);

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().name()).isEqualTo("Crypto");
    assertThat(responses.getFirst().defaultWatchlist()).isTrue();

    ArgumentCaptor<List<WatchlistItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
    verify(watchlistItemRepository).saveAll(itemsCaptor.capture());
    assertThat(itemsCaptor.getValue())
        .extracting(WatchlistItem::getSymbol)
        .containsExactly("BTC/USD", "ETH/USD");
    assertThat(itemsCaptor.getValue())
        .extracting(WatchlistItem::getSortOrder)
        .containsExactly(0, 1);
  }

  @Test
  void createWatchlistRejectsDuplicateName() {
    User user = new User("test@example.com", "hash");
    user.setId(UUID.randomUUID());
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(watchlistRepository.existsByUser_IdAndNameIgnoreCase(user.getId(), "Crypto")).thenReturn(true);

    assertThatThrownBy(() -> watchlistService.createWatchlist(user.getId(), " Crypto "))
        .isInstanceOf(AlreadyExistsException.class)
        .hasMessageContaining("Watchlist with name already exists");

    verify(watchlistRepository, never()).save(any(Watchlist.class));
  }

  @Test
  void getWatchlistsReturnsUserWatchlists() {
    User user = new User("test@example.com", "hash");
    user.setId(UUID.randomUUID());
    Watchlist watchlist = new Watchlist(user, "Crypto");
    watchlist.setId(UUID.randomUUID());
    when(watchlistRepository.findAllByUser_IdOrderByCreatedAtAsc(user.getId())).thenReturn(List.of(watchlist));

    var responses = watchlistService.getWatchlists(user.getId());

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().name()).isEqualTo("Crypto");
  }

  @Test
  void deleteWatchlistRejectsDefaultWatchlist() {
    User user = new User("test@example.com", "hash");
    user.setId(UUID.randomUUID());
    Watchlist watchlist = new Watchlist(user, "Crypto", true);
    watchlist.setId(UUID.randomUUID());
    when(watchlistRepository.findByIdAndUser_Id(watchlist.getId(), user.getId())).thenReturn(Optional.of(watchlist));

    assertThatThrownBy(() -> watchlistService.deleteWatchlist(user.getId(), watchlist.getId()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Default watchlists cannot be deleted");

    verify(watchlistRepository, never()).delete(any(Watchlist.class));
  }

  @Test
  void deleteWatchlistDeletesCustomWatchlist() {
    User user = new User("test@example.com", "hash");
    user.setId(UUID.randomUUID());
    Watchlist watchlist = new Watchlist(user, "Custom");
    watchlist.setId(UUID.randomUUID());
    when(watchlistRepository.findByIdAndUser_Id(watchlist.getId(), user.getId())).thenReturn(Optional.of(watchlist));

    watchlistService.deleteWatchlist(user.getId(), watchlist.getId());

    verify(watchlistRepository).delete(watchlist);
  }

  @Test
  void getWatchlistReturnsItems() {
    User user = new User("test@example.com", "hash");
    user.setId(UUID.randomUUID());
    Watchlist watchlist = new Watchlist(user, "Crypto");
    watchlist.setId(UUID.randomUUID());
    WatchlistItem item = new WatchlistItem(watchlist, "BTC/USD", 0);
    item.setId(UUID.randomUUID());
    when(watchlistRepository.findByIdAndUser_Id(watchlist.getId(), user.getId())).thenReturn(Optional.of(watchlist));
    when(watchlistItemRepository.findAllByWatchlist_IdOrderBySortOrderAscCreatedAtAsc(watchlist.getId()))
        .thenReturn(List.of(item));

    var response = watchlistService.getWatchlist(user.getId(), watchlist.getId());

    assertThat(response.name()).isEqualTo("Crypto");
    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().symbol()).isEqualTo("BTC/USD");
  }

  @Test
  void addItemValidatesSymbolAndAppendsNextSortOrder() {
    User user = new User("test@example.com", "hash");
    user.setId(UUID.randomUUID());
    Watchlist watchlist = new Watchlist(user, "Crypto");
    watchlist.setId(UUID.randomUUID());
    Symbol symbol = new Symbol("BTC/USD", "Bitcoin", AssetType.CRYPTO, "CRYPTO", true, true);
    when(watchlistRepository.findByIdAndUser_Id(watchlist.getId(), user.getId())).thenReturn(Optional.of(watchlist));
    when(symbolRepository.findBySymbol("BTC/USD")).thenReturn(Optional.of(symbol));
    when(watchlistItemRepository.existsByWatchlist_IdAndSymbol(watchlist.getId(), "BTC/USD")).thenReturn(false);
    when(watchlistItemRepository.findMaxSortOrderByWatchlistId(watchlist.getId())).thenReturn(2);
    when(watchlistItemRepository.save(any(WatchlistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = watchlistService.addItem(user.getId(), watchlist.getId(), "btc/usd");

    assertThat(response.symbol()).isEqualTo("BTC/USD");
    assertThat(response.sortOrder()).isEqualTo(3);
  }
}
