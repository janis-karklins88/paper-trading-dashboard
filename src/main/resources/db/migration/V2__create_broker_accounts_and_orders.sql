CREATE TABLE broker_accounts (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  alpaca_account_id VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL,
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_broker_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX ux_broker_accounts_user_id ON broker_accounts (user_id);
CREATE UNIQUE INDEX ux_broker_accounts_alpaca_account_id ON broker_accounts (alpaca_account_id);

CREATE TABLE orders (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  symbol VARCHAR(255) NOT NULL,
  side VARCHAR(10) NOT NULL,
  type VARCHAR(10) NOT NULL,
  quantity NUMERIC(19, 8) NOT NULL,
  limit_price NUMERIC(19, 8),
  stop_price NUMERIC(19, 8),
  take_profit_price NUMERIC(19, 8),
  stop_loss_price NUMERIC(19, 8),
  status VARCHAR(30) NOT NULL,
  broker_order_id VARCHAR(255),
  broker_account_id UUID NOT NULL,
  broker_order_status VARCHAR(255),
  filled_quantity NUMERIC(19, 8),
  reject_reason VARCHAR(255),
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  submitted_at TIMESTAMP(6) WITH TIME ZONE,
  filled_at TIMESTAMP(6) WITH TIME ZONE,
  updated_at TIMESTAMP(6) WITH TIME ZONE,
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_orders_broker_account FOREIGN KEY (broker_account_id) REFERENCES broker_accounts (id),
  CONSTRAINT ck_orders_side CHECK (side IN ('BUY', 'SELL')),
  CONSTRAINT ck_orders_type CHECK (type IN ('MARKET', 'LIMIT')),
  CONSTRAINT ck_orders_status CHECK (status IN (
    'PENDING',
    'SUBMITTED',
    'ACCEPTED',
    'PARTIALLY_FILLED',
    'FILLED',
    'REJECTED',
    'CANCELED',
    'FAILED'
  ))
);

CREATE INDEX ix_orders_user_id ON orders (user_id);
CREATE INDEX ix_orders_broker_account_id ON orders (broker_account_id);
CREATE INDEX ix_orders_broker_order_id ON orders (broker_order_id);
