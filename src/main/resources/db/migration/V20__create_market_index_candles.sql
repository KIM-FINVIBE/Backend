CREATE TABLE IF NOT EXISTS market_index_candles (
    index_candle_id BIGINT NOT NULL AUTO_INCREMENT,
    index_code VARCHAR(20) NOT NULL,
    index_name VARCHAR(50) NOT NULL,
    timeframe VARCHAR(20) NOT NULL,
    candle_at DATETIME(6) NOT NULL,
    open_price DECIMAL(20,4) NOT NULL,
    high_price DECIMAL(20,4) NOT NULL,
    low_price DECIMAL(20,4) NOT NULL,
    close_price DECIMAL(20,4) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,
    trading_value_krw BIGINT NOT NULL DEFAULT 0,
    source VARCHAR(30) NOT NULL DEFAULT 'KIS',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (index_candle_id),
    CONSTRAINT uk_market_index_candles_code_timeframe_at UNIQUE (index_code, timeframe, candle_at),
    INDEX idx_market_index_candles_code_timeframe (index_code, timeframe),
    INDEX idx_market_index_candles_candle_at (candle_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
