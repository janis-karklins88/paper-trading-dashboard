CREATE TABLE symbols (
  id UUID PRIMARY KEY,
  symbol VARCHAR(255) NOT NULL,
  display_name VARCHAR(255) NOT NULL,
  asset_type VARCHAR(30) NOT NULL,
  active BOOLEAN NOT NULL,
  tradable BOOLEAN NOT NULL,
  exchange VARCHAR(255),
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  CONSTRAINT ck_symbols_asset_type CHECK (asset_type IN ('STOCK', 'CRYPTO'))
);

CREATE UNIQUE INDEX ux_symbols_symbol ON symbols (symbol);
CREATE INDEX ix_symbols_asset_type ON symbols (asset_type);
CREATE INDEX ix_symbols_tradable ON symbols (tradable);
