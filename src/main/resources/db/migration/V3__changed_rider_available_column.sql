ALTER TABLE riders
    ADD is_available BOOLEAN;

ALTER TABLE riders
DROP
COLUMN want_to_delivery;