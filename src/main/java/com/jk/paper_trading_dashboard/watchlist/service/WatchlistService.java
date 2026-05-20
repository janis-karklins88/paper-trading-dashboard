package com.jk.paper_trading_dashboard.watchlist.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jk.paper_trading_dashboard.marketdata.domain.Symbols;
import com.jk.paper_trading_dashboard.marketdata.symbol.domain.Symbol;
import com.jk.paper_trading_dashboard.marketdata.symbol.repository.SymbolRepository;
import com.jk.paper_trading_dashboard.shared.exception.AlreadyExistsException;
import com.jk.paper_trading_dashboard.shared.exception.BadRequestException;
import com.jk.paper_trading_dashboard.shared.exception.NotFoundException;
import com.jk.paper_trading_dashboard.user.domain.User;
import com.jk.paper_trading_dashboard.user.repository.UserRepository;
import com.jk.paper_trading_dashboard.watchlist.config.DefaultWatchlistConfig;
import com.jk.paper_trading_dashboard.watchlist.domain.Watchlist;
import com.jk.paper_trading_dashboard.watchlist.domain.WatchlistItem;
import com.jk.paper_trading_dashboard.watchlist.dto.WatchlistDetailResponse;
import com.jk.paper_trading_dashboard.watchlist.dto.WatchlistItemResponse;
import com.jk.paper_trading_dashboard.watchlist.dto.WatchlistResponse;
import com.jk.paper_trading_dashboard.watchlist.repository.WatchlistItemRepository;
import com.jk.paper_trading_dashboard.watchlist.repository.WatchlistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WatchlistService {

  private final DefaultWatchlistLoader defaultWatchlistLoader;
  private final WatchlistRepository watchlistRepository;
  private final WatchlistItemRepository watchlistItemRepository;
  private final SymbolRepository symbolRepository;
  private final UserRepository userRepository;

  @Transactional
  public List<WatchlistResponse> createDefaultWatchlists(User user) {
    Objects.requireNonNull(user, "user is required");

    List<WatchlistResponse> watchlists = new ArrayList<>();

    for (DefaultWatchlistConfig config : defaultWatchlistLoader.load()) {
      Watchlist watchlist = watchlistRepository.findByUser_IdAndNameIgnoreCase(user.getId(), config.name())
          .orElseGet(() -> watchlistRepository.save(new Watchlist(user, config.name(), true)));

      createMissingDefaultItems(watchlist, config.symbols());
      watchlists.add(WatchlistResponse.from(watchlist));
    }

    return watchlists;
  }

  @Transactional
  public WatchlistResponse createWatchlist(UUID userId, String name) {
    User user = getUser(userId);
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }

    if (watchlistRepository.existsByUser_IdAndNameIgnoreCase(user.getId(), name.trim())) {
      throw new AlreadyExistsException("Watchlist with name already exists");
    }

    Watchlist watchlist = watchlistRepository.save(new Watchlist(user, name));
    return WatchlistResponse.from(watchlist);
  }

  @Transactional(readOnly = true)
  public List<WatchlistResponse> getWatchlists(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    List<Watchlist> watchlist = watchlistRepository.findAllByUser_IdOrderByCreatedAtAsc(userId);
    return watchlist.stream().map(WatchlistResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public WatchlistDetailResponse getWatchlist(UUID userId, UUID watchlistId) {
    Watchlist watchlist = getUserWatchlist(userId, watchlistId);
    List<WatchlistItemResponse> items = watchlistItemRepository
        .findAllByWatchlist_IdOrderBySortOrderAscCreatedAtAsc(watchlist.getId())
        .stream()
        .map(WatchlistItemResponse::from)
        .toList();

    return WatchlistDetailResponse.from(watchlist, items);
  }

  @Transactional
  public WatchlistItemResponse addItem(UUID userId, UUID watchlistId, String symbol) {
    Watchlist watchlist = getUserWatchlist(userId, watchlistId);
    String normalizedSymbol = Symbols.normalize(symbol);
    validateTradableSymbol(normalizedSymbol);

    if (watchlistItemRepository.existsByWatchlist_IdAndSymbol(watchlist.getId(), normalizedSymbol)) {
      throw new AlreadyExistsException("Symbol is already in watchlist");
    }

    int nextSortOrder = watchlistItemRepository.findMaxSortOrderByWatchlistId(watchlist.getId()) + 1;
    WatchlistItem item = watchlistItemRepository.save(new WatchlistItem(watchlist, normalizedSymbol, nextSortOrder));
    return WatchlistItemResponse.from(item);
  }

  @Transactional
  public void removeItem(UUID userId, UUID watchlistId, UUID itemId) {
    Watchlist watchlist = getUserWatchlist(userId, watchlistId);
    WatchlistItem item = watchlistItemRepository.findByIdAndWatchlist_Id(itemId, watchlist.getId())
        .orElseThrow(() -> new NotFoundException("Watchlist item not found"));

    watchlistItemRepository.delete(item);
  }

  @Transactional
  public void deleteWatchlist(UUID userId, UUID watchlistId) {
    Watchlist watchlist = getUserWatchlist(userId, watchlistId);

    if (watchlist.isDefaultWatchlist()) {
      throw new BadRequestException("Default watchlists cannot be deleted");
    }

    watchlistRepository.delete(watchlist);
  }

  private User getUser(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    return userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }

  private Watchlist getUserWatchlist(UUID userId, UUID watchlistId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    if (watchlistId == null) {
      throw new IllegalArgumentException("watchlistId is required");
    }

    return watchlistRepository.findByIdAndUser_Id(watchlistId, userId)
        .orElseThrow(() -> new NotFoundException("Watchlist not found"));
  }

  private void validateTradableSymbol(String symbol) {
    Symbol marketSymbol = symbolRepository.findBySymbol(symbol)
        .orElseThrow(() -> new NotFoundException("Symbol not found"));

    if (!marketSymbol.isActive() || !marketSymbol.isTradable()) {
      throw new BadRequestException("Symbol is not tradable");
    }
  }

  private void createMissingDefaultItems(Watchlist watchlist, List<String> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      return;
    }

    List<WatchlistItem> newItems = new ArrayList<>();
    Set<String> uniqueSymbols = new LinkedHashSet<>();

    for (String symbol : symbols) {
      uniqueSymbols.add(Symbols.normalize(symbol));
    }

    int sortOrder = 0;
    for (String symbol : uniqueSymbols) {
      if (!watchlistItemRepository.existsByWatchlist_IdAndSymbol(watchlist.getId(), symbol)) {
        newItems.add(new WatchlistItem(watchlist, symbol, sortOrder));
      }

      sortOrder++;
    }

    watchlistItemRepository.saveAll(newItems);
  }
}
