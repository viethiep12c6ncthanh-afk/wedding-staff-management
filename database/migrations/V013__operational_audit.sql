CREATE TABLE operational_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_username VARCHAR(50) NOT NULL,
    actor_role VARCHAR(30) NULL,
    action VARCHAR(20) NOT NULL,
    resource_path VARCHAR(255) NOT NULL,
    http_status INT NOT NULL,
    success BIT(1) NOT NULL,
    client_ip VARCHAR(64) NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_occurred_at (occurred_at),
    INDEX idx_audit_actor_occurred (actor_username, occurred_at),
    INDEX idx_audit_action (action)
);
