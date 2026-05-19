package com.jk.paper_trading_dashboard.order.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jk.paper_trading_dashboard.order.dto.OrderResponse;
import com.jk.paper_trading_dashboard.order.dto.PlaceOrderRequest;
import com.jk.paper_trading_dashboard.order.service.OrderService;
import com.jk.paper_trading_dashboard.shared.dto.PageResponse;
import com.jk.paper_trading_dashboard.shared.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  public ResponseEntity<OrderResponse> placeOrder(
      @AuthenticationPrincipal UserPrincipal user,
      @Valid @RequestBody PlaceOrderRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(user.userId(), request));
  }

  @GetMapping
  public ResponseEntity<PageResponse<OrderResponse>> getOrders(
      @AuthenticationPrincipal UserPrincipal user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return ResponseEntity.ok(orderService.getOrders(user.userId(), page, size));
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<OrderResponse> getOrder(
      @AuthenticationPrincipal UserPrincipal user,
      @PathVariable UUID orderId) {
    return ResponseEntity.ok(orderService.getOrder(user.userId(), orderId));
  }

  @PostMapping("/{orderId}/cancel")
  public ResponseEntity<OrderResponse> cancelOrder(
      @AuthenticationPrincipal UserPrincipal user,
      @PathVariable UUID orderId) {
    return ResponseEntity.ok(orderService.cancelOrder(user.userId(), orderId));
  }
}
