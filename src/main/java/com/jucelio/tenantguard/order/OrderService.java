package com.jucelio.tenantguard.order;

import com.jucelio.tenantguard.audit.AuditService;
import com.jucelio.tenantguard.tenant.RlsTenantGuard;
import com.jucelio.tenantguard.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final RlsTenantGuard rlsTenantGuard;
    private final AuditService auditService;

    public OrderService(
            OrderRepository repository,
            RlsTenantGuard rlsTenantGuard,
            AuditService auditService) {
        this.repository = repository;
        this.rlsTenantGuard = rlsTenantGuard;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyCurrentTenant();

        List<OrderResponse> orders = repository.findByTenantIdOrderById(tenantId)
                .stream()
                .map(OrderResponse::from)
                .toList();

        auditService.record("ORDER_LIST", "ORDER", null, "SUCCESS");
        return orders;
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyCurrentTenant();

        OrderResponse order = repository.findByIdAndTenantId(id, tenantId)
                .map(OrderResponse::from)
                .orElseThrow(OrderNotFoundException::new);

        auditService.record("ORDER_READ", "ORDER", id.toString(), "SUCCESS");
        return order;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyCurrentTenant();

        Order order = new Order(request.description(), tenantId);
        OrderResponse created = OrderResponse.from(repository.save(order));
        auditService.record("ORDER_CREATE", "ORDER", created.id().toString(), "SUCCESS");
        return created;
    }
}
