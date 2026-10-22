CREATE TABLE city (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE,
    slug  TEXT GENERATED ALWAYS AS (public.slugify(name)) STORED UNIQUE CHECK (slug <> '')
);

CREATE TABLE barbershop (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    city_id      BIGINT NOT NULL REFERENCES city(id),
    name         VARCHAR(100)  NOT NULL,
    slug     TEXT GENERATED ALWAYS AS (public.slugify(name)) STORED CHECK (slug <> ''),
    CONSTRAINT uq_barbershop_city_slug UNIQUE (city_id, slug)
);

CREATE TABLE barber (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    barbershop_id BIGINT NOT NULL REFERENCES barbershop(id),
    name          VARCHAR(100) NOT NULL,
    surname       VARCHAR(100) NOT NULL,
    role          VARCHAR(50) NOT NULL,
    phone_number  VARCHAR(20),
    active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uq_barber_name_per_shop
    ON barber (barbershop_id, lower(name), lower(surname))
    WHERE active;

CREATE TABLE service (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    duration_minutes integer NOT NULL CHECK (duration_minutes > 0),
    price        numeric(10,2) NOT NULL CHECK (price > 0)
);


