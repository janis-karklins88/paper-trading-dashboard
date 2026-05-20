package com.jk.paper_trading_dashboard.watchlist.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.jk.paper_trading_dashboard.shared.security.UserPrincipal;
import com.jk.paper_trading_dashboard.watchlist.dto.AddWatchlistItemRequest;
import com.jk.paper_trading_dashboard.watchlist.dto.CreateWatchlistRequest;
import com.jk.paper_trading_dashboard.watchlist.dto.WatchlistDetailResponse;
import com.jk.paper_trading_dashboard.watchlist.dto.WatchlistItemResponse;
import com.jk.paper_trading_dashboard.watchlist.dto.WatchlistResponse;
import com.jk.paper_trading_dashboard.watchlist.service.WatchlistService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/watchlists")
@RequiredArgsConstructor
public class WatchlistController {

  private final WatchlistService watchlistService;

  @GetMapping
  public ResponseEntity<List<WatchlistResponse>> getWatchlists(@AuthenticationPrincipal UserPrincipal user) {
    return ResponseEntity.ok(watchlistService.getWatchlists(user.userId()));
  }

  @PostMapping
  public ResponseEntity<WatchlistResponse> createWatchlist(
      @AuthenticationPrincipal UserPrincipal user,
      @Valid @RequestBody CreateWatchlistRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(watchlistService.createWatchlist(user.userId(), request.name()));
  }

  @GetMapping("/{watchlistId}")
  public ResponseEntity<WatchlistDetailResponse> getWatchlist(
      @AuthenticationPrincipal UserPrincipal user,
      @PathVariable UUID watchlistId) {
    return ResponseEntity.ok(watchlistService.getWatchlist(user.userId(), watchlistId));
  }

  @PostMapping("/{watchlistId}/items")
  public ResponseEntity<WatchlistItemResponse> addItem(
      @AuthenticationPrincipal UserPrincipal user,
      @PathVariable UUID watchlistId,
      @Valid @RequestBody AddWatchlistItemRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(watchlistService.addItem(user.userId(), watchlistId, request.symbol()));
  }

  @DeleteMapping("/{watchlistId}/items/{itemId}")
  public ResponseEntity<Void> removeItem(
      @AuthenticationPrincipal UserPrincipal user,
      @PathVariable UUID watchlistId,
      @PathVariable UUID itemId) {
    watchlistService.removeItem(user.userId(), watchlistId, itemId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{watchlistId}")
  public ResponseEntity<Void> deleteWatchlist(
      @AuthenticationPrincipal UserPrincipal user,
      @PathVariable UUID watchlistId) {
    watchlistService.deleteWatchlist(user.userId(), watchlistId);
    return ResponseEntity.noContent().build();
  }
}
