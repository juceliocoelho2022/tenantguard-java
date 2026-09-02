package com.jucelio.tenantguard.order;

public record OrderResponse(
        Long id,
        String description
) {
    static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getDescription());
    }
}
