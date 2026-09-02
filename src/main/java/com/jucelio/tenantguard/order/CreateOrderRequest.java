package com.jucelio.tenantguard.order;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String description
) {}
