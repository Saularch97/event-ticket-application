ALTER TABLE tb_ticket_category
ALTER COLUMN ticket_category_id TYPE BIGINT;

ALTER TABLE tb_tickets
ALTER COLUMN ticket_category_id TYPE BIGINT;