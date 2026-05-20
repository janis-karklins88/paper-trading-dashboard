CREATE TABLE watchlists (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  name VARCHAR(255) NOT NULL,
  default_watchlist BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_watchlists_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX ix_watchlists_user_id ON watchlists (user_id);
CREATE UNIQUE INDEX ux_watchlists_user_id_name ON watchlists (user_id, LOWER(name));

CREATE TABLE watchlist_items (
  id UUID PRIMARY KEY,
  watchlist_id UUID NOT NULL,
  symbol VARCHAR(255) NOT NULL,
  sort_order INTEGER NOT NULL,
  created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_watchlist_items_watchlist FOREIGN KEY (watchlist_id) REFERENCES watchlists (id) ON DELETE CASCADE
);

CREATE INDEX ix_watchlist_items_watchlist_id ON watchlist_items (watchlist_id);
CREATE UNIQUE INDEX ux_watchlist_items_watchlist_id_symbol ON watchlist_items (watchlist_id, symbol);
