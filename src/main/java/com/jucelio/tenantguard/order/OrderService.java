package com.jucelio.tenantguard.order;

import com.jucelio.tenantguard.tenant.RlsTenantGuard;
import com.jucelio.tenantguard.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final RlsTenantGuard rlsTenantGuard;

    public OrderService(OrderRepository repository, RlsTenantGuard rlsTenantGuard) {
        this.repository = repository;
        this.rlsTenantGuard = rlsTenantGuard;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyCurrentTenant();

        return repository.findByTenantIdOrderById(tenantId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyCurrentTenant();

        return repository.findByIdAndTenantId(id, tenantId)
                .map(OrderResponse::from)
                .orElseThrow(OrderNotFoundException::new);
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyCurrentTenant();

        Order order = new Order(request.description(), tenantId);
        return OrderResponse.from(repository.save(order));
    }
}
