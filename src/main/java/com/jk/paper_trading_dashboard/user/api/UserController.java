package com.jk.paper_trading_dashboard.user.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jk.paper_trading_dashboard.shared.security.UserPrincipal;
import com.jk.paper_trading_dashboard.user.api.dto.AuthResponse;
import com.jk.paper_trading_dashboard.user.api.dto.UserLoginRequest;
import com.jk.paper_trading_dashboard.user.api.dto.UserRegistrationRequest;
import com.jk.paper_trading_dashboard.user.api.dto.UserResponse;
import com.jk.paper_trading_dashboard.user.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> registerUser(
      @Valid @RequestBody UserRegistrationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(userService.saveUser(request)));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> loginUser(
      @Valid @RequestBody UserLoginRequest request) {
    return ResponseEntity.ok(new AuthResponse(userService.loginUser(request)));
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal user) {
    return ResponseEntity.ok(userService.getCurrentUser(user));
  }
}
