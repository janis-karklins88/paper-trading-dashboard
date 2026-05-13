CREATE TABLE trading_accounts (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  starting_cash NUMERIC(19, 2) NOT NULL,
  cash_balance NUMERIC(19, 2) NOT NULL,
  reserved_margin NUMERIC(19, 2) NOT NULL,
  realized_pnl NUMERIC(19, 2) NOT NULL,
  unrealized_pnl NUMERIC(19, 2) NOT NULL,
  max_leverage NUMERIC(10, 2) NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_trading_accounts_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT ck_trading_accounts_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'LIQUIDATED'))
);

CREATE UNIQUE INDEX ux_trading_accounts_user_id ON trading_accounts (user_id);

CREATE TABLE orders (
  id UUID PRIMARY KEY,
  trading_account_id UUID NOT NULL,
  symbol VARCHAR(255) NOT NULL,
  side VARCHAR(10) NOT NULL,
  type VARCHAR(10) NOT NULL,
  quantity NUMERIC(19, 8),
  margin_amount NUMERIC(19, 8) NOT NULL,
  leverage NUMERIC(10, 2) NOT NULL,
  notional_value NUMERIC(19, 8) NOT NULL,
  filled_price NUMERIC(19, 8),
  limit_price NUMERIC(19, 8),
  take_profit_price NUMERIC(19, 8),
  stop_loss_price NUMERIC(19, 8),
  status VARCHAR(30) NOT NULL,
  reject_reason VARCHAR(255),
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  opened_at TIMESTAMP(6) WITH TIME ZONE,
  filled_at TIMESTAMP(6) WITH TIME ZONE,
  updated_at TIMESTAMP(6) WITH TIME ZONE,
  CONSTRAINT fk_orders_trading_account FOREIGN KEY (trading_account_id) REFERENCES trading_accounts (id),
  CONSTRAINT ck_orders_side CHECK (side IN ('BUY', 'SELL')),
  CONSTRAINT ck_orders_type CHECK (type IN ('MARKET', 'LIMIT')),
  CONSTRAINT ck_orders_status CHECK (status IN (
    'PENDING',
    'OPEN',
    'FILLED',
    'REJECTED',
    'CANCELED'
  ))
);

CREATE INDEX ix_orders_trading_account_id ON orders (trading_account_id);

CREATE TABLE positions (
  id UUID PRIMARY KEY,
  trading_account_id UUID NOT NULL,
  symbol VARCHAR(255) NOT NULL,
  side VARCHAR(10) NOT NULL,
  quantity NUMERIC(19, 8) NOT NULL,
  avg_entry_price NUMERIC(19, 8) NOT NULL,
  current_price NUMERIC(19, 8) NOT NULL,
  margin_used NUMERIC(19, 8) NOT NULL,
  leverage NUMERIC(10, 2) NOT NULL,
  unrealized_pnl NUMERIC(19, 8) NOT NULL,
  realized_pnl NUMERIC(19, 8) NOT NULL,
  status VARCHAR(30) NOT NULL,
  opened_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  closed_at TIMESTAMP(6) WITH TIME ZONE,
  updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_positions_trading_account FOREIGN KEY (trading_account_id) REFERENCES trading_accounts (id),
  CONSTRAINT ck_positions_side CHECK (side IN ('LONG', 'SHORT')),
  CONSTRAINT ck_positions_status CHECK (status IN ('OPEN', 'CLOSED', 'LIQUIDATED'))
);

CREATE INDEX ix_positions_trading_account_id ON positions (trading_account_id);
