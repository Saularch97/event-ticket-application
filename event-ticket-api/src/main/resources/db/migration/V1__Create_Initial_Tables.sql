-- V1__Create_Initial_Tables.sql

CREATE TABLE tb_users (
    user_id UUID PRIMARY KEY,
    user_name VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE tb_roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL
);

CREATE TABLE tb_user_roles (
    user_id UUID NOT NULL REFERENCES tb_users(user_id),
    role_id INTEGER NOT NULL REFERENCES tb_roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE tb_events (
    event_id UUID PRIMARY KEY,
    event_name VARCHAR(255) NOT NULL,
    event_location VARCHAR(255),
    event_date TIMESTAMP,
    event_ticket_price DOUBLE PRECISION,
    available_tickets INTEGER,
    original_amount_of_tickets INTEGER,
    is_trending BOOLEAN DEFAULT FALSE,
    tickets_emitted_in_trending_period BIGINT DEFAULT 0,
    user_id UUID REFERENCES tb_users(user_id)
);

CREATE TABLE tb_ticket_category (
    ticket_category_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION,
    available_category_tickets INTEGER,
    event_id UUID NOT NULL REFERENCES tb_events(event_id)
);

CREATE TABLE tb_orders (
    order_id UUID PRIMARY KEY,
    order_price DOUBLE PRECISION,
    user_id UUID REFERENCES tb_users(user_id)
);

CREATE TABLE tb_tickets (
    ticket_id UUID PRIMARY KEY,
    emitted_at TIMESTAMP,
    ticket_event_location VARCHAR(255),
    ticket_event_date VARCHAR(255),
    ticket_category_name VARCHAR(255),
    ticket_price DOUBLE PRECISION,
    event_id UUID REFERENCES tb_events(event_id),
    user_id UUID REFERENCES tb_users(user_id),
    order_id UUID REFERENCES tb_orders(order_id),
    ticket_category_id INTEGER REFERENCES tb_ticket_category(ticket_category_id)
);

CREATE TABLE tb_refreshtoken (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    user_id UUID UNIQUE REFERENCES tb_users(user_id)
);

CREATE TABLE payment (
    payment_id UUID PRIMARY KEY,
    user_email VARCHAR(255),
    amount DOUBLE PRECISION
);