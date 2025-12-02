ALTER TABLE tb_ticket_category
ALTER COLUMN price TYPE DECIMAL(19, 2);

ALTER TABLE tb_orders
ALTER COLUMN order_price TYPE DECIMAL(19, 2);

ALTER TABLE tb_tickets
ALTER COLUMN ticket_price TYPE DECIMAL(19, 2);
