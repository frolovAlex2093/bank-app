CREATE TABLE IF NOT EXISTS accounts (
                                        id BIGSERIAL PRIMARY KEY,
                                        login VARCHAR(255) NOT NULL UNIQUE,
                                        first_name VARCHAR(255),
                                        last_name VARCHAR(255),
                                        birth_date DATE,
                                        balance DECIMAL(19, 2) DEFAULT 0.0
);

INSERT INTO accounts (login, first_name, last_name, birth_date, balance)
VALUES ('ivan_ivanov', 'Иван', 'Иванов', '1990-01-01', 1000.00);