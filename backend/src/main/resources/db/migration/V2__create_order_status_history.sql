CREATE TABLE order_status_history (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id     UUID          NOT NULL,
    from_status  VARCHAR(20),
    to_status    VARCHAR(20)   NOT NULL,
    reason       VARCHAR(500),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_status_history_from
        CHECK (from_status IS NULL OR from_status IN ('RECEIVED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_order_status_history_to
        CHECK (to_status IN ('RECEIVED', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX ix_order_status_history_order_created
    ON order_status_history (order_id, created_at);
