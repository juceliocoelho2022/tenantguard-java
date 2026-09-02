package com.jucelio.tenantguard.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByTenantIdOrderById(String tenantId);
    Optional<Order> findByIdAndTenantId(Long id, String tenantId);
}
