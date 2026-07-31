CREATE TABLE IF NOT EXISTS appointments (
    id           SERIAL PRIMARY KEY,
    barber_name  VARCHAR(100)  NOT NULL,
    client_name  VARCHAR(100)  NOT NULL,
    phone_number VARCHAR(20)   NOT NULL,
    comment      VARCHAR(500),
    date_time    TIMESTAMP     NOT NULL,
    status       SMALLINT      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_barber_slot ON appointments (barber_name, date_time) WHERE status = 0;