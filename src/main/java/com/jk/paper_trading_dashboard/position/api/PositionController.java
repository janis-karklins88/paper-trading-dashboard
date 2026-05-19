package com.jk.paper_trading_dashboard.position.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jk.paper_trading_dashboard.position.domain.PositionStatus;
import com.jk.paper_trading_dashboard.position.dto.PositionResponse;
import com.jk.paper_trading_dashboard.position.service.PositionService;
import com.jk.paper_trading_dashboard.shared.dto.PageResponse;
import com.jk.paper_trading_dashboard.shared.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/positions")
@RequiredArgsConstructor
public class PositionController {

  private final PositionService positionService;

  @GetMapping
  public ResponseEntity<PageResponse<PositionResponse>> getPositions(
      @AuthenticationPrincipal UserPrincipal user,
      @RequestParam(required = false) PositionStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return ResponseEntity.ok(positionService.getPositions(user.userId(), status, page, size));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PositionResponse> getPosition(
      @AuthenticationPrincipal UserPrincipal user,
      @PathVariable UUID id) {
    return ResponseEntity.ok(positionService.getPosition(user.userId(), id));
  }

  @PostMapping("/{id}/close")
  public ResponseEntity<PositionResponse> closePosition(
      @AuthenticationPrincipal UserPrincipal user,
      @PathVariable UUID id) {
    return ResponseEntity.ok(positionService.closePosition(user.userId(), id));
  }
}
