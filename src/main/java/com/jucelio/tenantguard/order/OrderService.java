package com.jucelio.tenantguard.order;

import com.jucelio.tenantguard.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        String tenantId = TenantContext.getTenant();

        return repository.findByTenantIdOrderById(tenantId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        String tenantId = TenantContext.getTenant();

        return repository.findByIdAndTenantId(id, tenantId)
                .map(OrderResponse::from)
                .orElseThrow(OrderNotFoundException::new);
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        String tenantId = TenantContext.getTenant();

        Order order = new Order(request.description(), tenantId);
        return OrderResponse.from(repository.save(order));
    }
}
