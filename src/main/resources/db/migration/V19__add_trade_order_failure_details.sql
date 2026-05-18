ALTER TABLE trade_orders
    ADD COLUMN failure_reason_code VARCHAR(50) NULL AFTER canceled_at,
    ADD COLUMN failure_message VARCHAR(255) NULL AFTER failure_reason_code,
    ADD COLUMN failed_at DATETIME(6) NULL AFTER failure_message;
