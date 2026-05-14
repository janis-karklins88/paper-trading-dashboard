package com.jk.paper_trading_dashboard.user.service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jk.paper_trading_dashboard.account.service.TradingAccountService;
import com.jk.paper_trading_dashboard.shared.exception.AlreadyExistsException;
import com.jk.paper_trading_dashboard.shared.exception.UnauthorizedException;
import com.jk.paper_trading_dashboard.shared.security.JwtService;
import com.jk.paper_trading_dashboard.shared.security.UserPrincipal;
import com.jk.paper_trading_dashboard.user.api.dto.UserLoginRequest;
import com.jk.paper_trading_dashboard.user.api.dto.UserRegistrationRequest;
import com.jk.paper_trading_dashboard.user.api.dto.UserResponse;
import com.jk.paper_trading_dashboard.user.domain.User;
import com.jk.paper_trading_dashboard.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepo;
  private final TradingAccountService tradingAccountService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @Transactional
  public String saveUser(UserRegistrationRequest request) {
    if (userRepo.existsByEmail(request.email())) {
      throw new AlreadyExistsException("User with email already exists");
    }

    User user = userRepo.save(new User(request.email(), passwordEncoder.encode(request.password())));
    tradingAccountService.createForUser(user);
    return createToken(user);
  }

  public String loginUser(UserLoginRequest request) {
    User user = userRepo.findByEmail(request.email())
        .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new UnauthorizedException("Invalid email or password");
    }

    return createToken(user);
  }

  public UserResponse getCurrentUser(UserPrincipal user) {
    return new UserResponse(user.userId(), user.email());
  }

  private String createToken(User user) {
    return jwtService.createToken(user);
  }
}
