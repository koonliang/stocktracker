CREATE TABLE instrument (
  symbol VARCHAR(16) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  sector VARCHAR(120) NOT NULL,
  exchange VARCHAR(32) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE instrument_price_bar (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  instrument_symbol VARCHAR(16) NOT NULL,
  trade_date DATE NOT NULL,
  open_price DECIMAL(19, 4) NOT NULL,
  high_price DECIMAL(19, 4) NOT NULL,
  low_price DECIMAL(19, 4) NOT NULL,
  close_price DECIMAL(19, 4) NOT NULL,
  volume BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_price_bar_instrument FOREIGN KEY (instrument_symbol) REFERENCES instrument(symbol),
  CONSTRAINT uq_price_bar_symbol_date UNIQUE (instrument_symbol, trade_date)
);

CREATE TABLE instrument_stat (
  instrument_symbol VARCHAR(16) PRIMARY KEY,
  open_price DECIMAL(19, 4) NOT NULL,
  high_price DECIMAL(19, 4) NOT NULL,
  low_price DECIMAL(19, 4) NOT NULL,
  previous_close DECIMAL(19, 4) NOT NULL,
  volume BIGINT NOT NULL,
  week_52_high DECIMAL(19, 4) NOT NULL,
  week_52_low DECIMAL(19, 4) NOT NULL,
  market_cap BIGINT NOT NULL,
  pe_ratio DECIMAL(19, 4),
  as_of_date DATE NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_instrument_stat_instrument FOREIGN KEY (instrument_symbol) REFERENCES instrument(symbol)
);

CREATE TABLE portfolio_transaction (
  id BINARY(16) PRIMARY KEY,
  trade_date DATE NOT NULL,
  instrument_symbol VARCHAR(16) NOT NULL,
  transaction_type VARCHAR(16) NOT NULL,
  quantity DECIMAL(19, 6) NOT NULL,
  price DECIMAL(19, 4) NOT NULL,
  fees DECIMAL(19, 4) NOT NULL DEFAULT 0,
  source VARCHAR(16) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_portfolio_tx_instrument FOREIGN KEY (instrument_symbol) REFERENCES instrument(symbol)
);

CREATE TABLE watchlist (
  id BINARY(16) PRIMARY KEY,
  name VARCHAR(40) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_watchlist_name_lower ON watchlist ((LOWER(name)));

CREATE TABLE watchlist_item (
  id BINARY(16) PRIMARY KEY,
  watchlist_id BINARY(16) NOT NULL,
  instrument_symbol VARCHAR(16) NOT NULL,
  display_order INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_watchlist_item_watchlist FOREIGN KEY (watchlist_id) REFERENCES watchlist(id) ON DELETE CASCADE,
  CONSTRAINT fk_watchlist_item_instrument FOREIGN KEY (instrument_symbol) REFERENCES instrument(symbol),
  CONSTRAINT uq_watchlist_item_symbol UNIQUE (watchlist_id, instrument_symbol),
  CONSTRAINT uq_watchlist_item_order UNIQUE (watchlist_id, display_order)
);
