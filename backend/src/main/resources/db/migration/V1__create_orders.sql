CREATE TABLE orders (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_name   VARCHAR(150)    NOT NULL,
    customer_email  VARCHAR(254)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'RECEIVED',
    total_amount    NUMERIC(14, 2)  NOT NULL,
    failure_reason  VARCHAR(500),
    attempts        INTEGER         NOT NULL DEFAULT 0,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    processed_at    TIMESTAMPTZ,

    CONSTRAINT ck_orders_status
        CHECK (status IN ('RECEIVED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_orders_total_non_negative
        CHECK (total_amount >= 0),
    CONSTRAINT ck_orders_attempts_non_negative
        CHECK (attempts >= 0)
);

CREATE TABLE order_items (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id     UUID            NOT NULL,
    description  VARCHAR(255)    NOT NULL,
    quantity     INTEGER         NOT NULL,
    unit_price   NUMERIC(12, 2)  NOT NULL,
    line_total   NUMERIC(14, 2)  NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_items_quantity_positive
        CHECK (quantity > 0),
    CONSTRAINT ck_order_items_unit_price_non_negative
        CHECK (unit_price >= 0),
    CONSTRAINT ck_order_items_line_total
        CHECK (line_total = quantity * unit_price),
    CONSTRAINT ck_order_items_description_not_blank
        CHECK (length(trim(description)) > 0)
);

CREATE INDEX ix_order_items_order_id ON order_items (order_id);
CREATE INDEX ix_orders_created_at ON orders (created_at DESC);
CREATE INDEX ix_orders_status_created_at ON orders (status, created_at DESC);
