SET CONSTRAINTS ALL DEFERRED;

TRUNCATE TABLE
    tb_user_roles,
    tb_refreshtoken,
    tb_tickets,
    tb_ticket_category,
    tb_orders,
    tb_users,
    tb_events,
    payment
RESTART IDENTITY CASCADE;

SET CONSTRAINTS ALL IMMEDIATE;
