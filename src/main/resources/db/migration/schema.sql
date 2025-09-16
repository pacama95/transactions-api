-- Transactions Database Schema
-- This script initializes the database schema for the transactions management system

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create enum types
CREATE TYPE currency_type AS ENUM ('USD', 'EUR', 'GBP');
CREATE TYPE transaction_type AS ENUM ('BUY', 'SELL', 'DIVIDEND', 'SPLIT');

-- Create transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ticker VARCHAR(20) NOT NULL,
    transaction_type transaction_type NOT NULL DEFAULT 'BUY',
    quantity DECIMAL(18, 6) NOT NULL,
    cost_per_share DECIMAL(18, 4) NOT NULL,
    currency currency_type NOT NULL,
    transaction_date DATE NOT NULL,
    commission DECIMAL(18, 4) DEFAULT 0.00,
    commission_currency currency_type,
    drip_confirmed BOOLEAN DEFAULT FALSE,
    is_fractional BOOLEAN DEFAULT FALSE,
    fractional_multiplier DECIMAL(10, 8) DEFAULT 1.0, -- TODO: use this when fractional transactions are detected to adjust the input market price
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_transactions_ticker ON transactions(ticker);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_ticker_date ON transactions(ticker, transaction_date);

-- Create a trigger to update the updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_transactions_updated_at BEFORE UPDATE ON transactions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();