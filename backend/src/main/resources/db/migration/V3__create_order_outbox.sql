CREATE TABLE order_outbox (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID           NOT NULL,
    attempts      INTEGER        NOT NULL DEFAULT 0,
    last_error    VARCHAR(1000),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ,

    CONSTRAINT fk_order_outbox_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_outbox_attempts_non_negative
        CHECK (attempts >= 0)
);

CREATE INDEX ix_order_outbox_pending
    ON order_outbox (created_at)
    WHERE published_at IS NULL;
