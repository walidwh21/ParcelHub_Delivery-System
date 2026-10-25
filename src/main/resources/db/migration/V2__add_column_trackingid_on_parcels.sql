ALTER TABLE parcels
    ADD tracking_id VARCHAR(6);

ALTER TABLE parcels
    ADD CONSTRAINT uc_parcels_trackingid UNIQUE (tracking_id);

ALTER TABLE parcels
    ALTER COLUMN price TYPE DECIMAL USING (price::DECIMAL);