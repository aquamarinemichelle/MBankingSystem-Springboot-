CREATE DATABASE mbankingdbspring;

USE mbankingdbspring;

CREATE TABLE accounts (
    account_number INT PRIMARY KEY,
    account_holder VARCHAR(100),
    balance DOUBLE,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE accounts ADD UNIQUE (email);

CREATE TABLE transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    account_number INT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    to_account VARCHAR(20),
    description VARCHAR(255),
    fee DECIMAL(10,2) DEFAULT 0.00,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
);

INSERT INTO accounts (account_number, account_holder, balance, email, password) VALUES
(100001, 'John Doe', 5000.00, 'john@example.com', 'password123'),
(100002, 'Jane Smith', 10000.00, 'jane@example.com', 'password123'),
(100003, 'Bob Johnson', 7500.00, 'bob@example.com', 'password123');

INSERT INTO transactions (account_number, transaction_type, amount, description, transaction_id) VALUES
(100001, 'DEPOSIT', 5000.00, 'Initial deposit', 'TXN001'),
(100002, 'DEPOSIT', 10000.00, 'Initial deposit', 'TXN002'),
(100003, 'DEPOSIT', 7500.00, 'Initial deposit', 'TXN003');

SELECT 'Accounts table:' as Table_Name, COUNT(*) as Row_Count FROM accounts
UNION
SELECT 'Transactions table:', COUNT(*) FROM transactions;