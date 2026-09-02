CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL
);

CREATE INDEX idx_orders_tenant_id ON orders (tenant_id);

INSERT INTO orders (description, tenant_id) VALUES
('Pedido A-001', 'TENANT_A'),
('Pedido A-002', 'TENANT_A'),
('Pedido B-001', 'TENANT_B'),
('Pedido C-001', 'TENANT_C'),
('Pedido C-002', 'TENANT_C');
