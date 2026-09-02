package com.jucelio.tenantguard.order;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    protected Order() {}

    public Order(String description, String tenantId) {
        this.description = description;
        this.tenantId = tenantId;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getTenantId() {
        return tenantId;
    }
}
