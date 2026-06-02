CREATE TABLE account_equity_snapshots (
  id UUID PRIMARY KEY,
  account_id UUID NOT NULL,
  cash_balance NUMERIC(19, 2) NOT NULL,
  reserved_margin NUMERIC(19, 2) NOT NULL,
  realized_pnl NUMERIC(19, 2) NOT NULL,
  unrealized_pnl NUMERIC(19, 2) NOT NULL,
  total_equity NUMERIC(19, 2) NOT NULL,
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_account_equity_snapshots_account FOREIGN KEY (account_id) REFERENCES trading_accounts (id)
);

CREATE INDEX ix_account_equity_snapshots_account_created_at
  ON account_equity_snapshots (account_id, created_at DESC);
