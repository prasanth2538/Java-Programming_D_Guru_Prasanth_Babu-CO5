CREATE DATABASE IF NOT EXISTS transport_db;
USE transport_db;

CREATE TABLE IF NOT EXISTS bus_pass (
    pass_id VARCHAR(30) PRIMARY KEY,
    reg_no VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    route_no INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS demo_users (
    user_id VARCHAR(60) PRIMARY KEY,
    pwd VARCHAR(100) NOT NULL
);
INSERT INTO demo_users(user_id, pwd)
VALUES ('admin', 'demo-password')
ON DUPLICATE KEY UPDATE pwd=VALUES(pwd);

CREATE TABLE IF NOT EXISTS wallets (
    wallet_id INT PRIMARY KEY,
    balance DECIMAL(12,2) NOT NULL
);
INSERT INTO wallets(wallet_id,balance) VALUES (1,5000),(2,1000)
ON DUPLICATE KEY UPDATE balance=VALUES(balance);
