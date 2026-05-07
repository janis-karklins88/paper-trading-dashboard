package com.jk.paper_trading_dashboard.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

    @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters long") String password) {

}
