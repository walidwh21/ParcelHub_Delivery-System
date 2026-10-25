ALTER TABLE parcels
    ADD COLUMN pickup_division VARCHAR(255),
    ADD COLUMN pickup_district VARCHAR(255),
    ADD COLUMN pickup_upazila VARCHAR(255),
    ADD COLUMN pickup_village VARCHAR(255),
    ADD COLUMN receiver_division VARCHAR(255),
    ADD COLUMN receiver_district VARCHAR(255),
    ADD COLUMN receiver_upazila VARCHAR(255),
    ADD COLUMN receiver_village VARCHAR(255);
