ALTER TABLE positions
  ADD COLUMN take_profit_price NUMERIC(19, 8),
  ADD COLUMN stop_loss_price NUMERIC(19, 8);
