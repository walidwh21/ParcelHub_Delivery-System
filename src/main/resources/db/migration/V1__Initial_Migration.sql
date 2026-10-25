CREATE TABLE parcels
(
    id               UUID             NOT NULL,
    category_id      UUID,
    description      TEXT,
    weight           DOUBLE PRECISION NOT NULL,
    size             DOUBLE PRECISION NOT NULL,
    receiver_name    VARCHAR(255)     NOT NULL,
    receiver_phone   VARCHAR(255)     NOT NULL,
    receiver_email   VARCHAR(255),
    receiver_address VARCHAR(255)     NOT NULL,
    receiver_otp     VARCHAR(255),
    sender_id        UUID             NOT NULL,
    rider_id         UUID,
    rider_otp        VARCHAR(255),
    assigned_at      TIMESTAMP WITHOUT TIME ZONE,
    status           VARCHAR(255),
    pickup_time      TIMESTAMP WITHOUT TIME ZONE,
    delivery_time    TIMESTAMP WITHOUT TIME ZONE,
    price            DECIMAL          NOT NULL,
    distance         DOUBLE PRECISION NOT NULL,
    delivered_at     TIMESTAMP WITHOUT TIME ZONE,
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    updated_at       TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_parcels PRIMARY KEY (id)
);

CREATE TABLE payment
(
    id                  UUID NOT NULL,
    transaction_id      VARCHAR(255),
    amount              DECIMAL,
    currency            VARCHAR(255),
    payment_method      VARCHAR(255),
    bank_transaction_id VARCHAR(255),
    user_id             UUID,
    parcel_id           UUID,
    payment_status      SMALLINT,
    CONSTRAINT pk_payment PRIMARY KEY (id)
);

CREATE TABLE productcategory
(
    id         UUID         NOT NULL,
    category   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_productcategory PRIMARY KEY (id)
);

CREATE TABLE riders
(
    id                                 UUID NOT NULL,
    date_of_birth                      date,
    gender                             VARCHAR(255),
    address                            VARCHAR(255),
    upazila                            VARCHAR(255),
    district                           VARCHAR(255),
    postal_code                        VARCHAR(255),
    vehicle_type                       VARCHAR(255),
    vehicle_model                      VARCHAR(255),
    vehicle_registration_number        VARCHAR(255),
    vehicle_insurance_provider         VARCHAR(255),
    insurance_expiry_date              date,
    vehicle_registration_document_path VARCHAR(255),
    insurance_document_path            VARCHAR(255),
    national_id_number                 VARCHAR(255),
    national_id_front_path             VARCHAR(255),
    national_id_back_path              VARCHAR(255),
    drivers_license_number             VARCHAR(255),
    license_expiry_date                date,
    drivers_license_front_path         VARCHAR(255),
    drivers_license_back_path          VARCHAR(255),
    verification_status                VARCHAR(255),
    want_to_delivery                   BOOLEAN,
    rider_avg_rating                   DOUBLE PRECISION,
    total_rating                       INTEGER,
    rider_balance                      DOUBLE PRECISION,
    CONSTRAINT pk_riders PRIMARY KEY (id)
);

CREATE TABLE user_roles
(
    user_id UUID NOT NULL,
    role    VARCHAR(255)
);

CREATE TABLE users
(
    id              UUID         NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(255),
    email_verified  VARCHAR(255),
    profile_picture VARCHAR(255),
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE productcategory
    ADD CONSTRAINT uc_productcategory_category UNIQUE (category);

ALTER TABLE riders
    ADD CONSTRAINT uc_riders_nationalidnumber UNIQUE (national_id_number);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE parcels
    ADD CONSTRAINT FK_PARCELS_ON_CATEGORY FOREIGN KEY (category_id) REFERENCES productcategory (id);

ALTER TABLE parcels
    ADD CONSTRAINT FK_PARCELS_ON_RIDER FOREIGN KEY (rider_id) REFERENCES users (id);

ALTER TABLE parcels
    ADD CONSTRAINT FK_PARCELS_ON_SENDER FOREIGN KEY (sender_id) REFERENCES users (id);

ALTER TABLE payment
    ADD CONSTRAINT FK_PAYMENT_ON_PARCEL FOREIGN KEY (parcel_id) REFERENCES parcels (id);

ALTER TABLE payment
    ADD CONSTRAINT FK_PAYMENT_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE riders
    ADD CONSTRAINT FK_RIDERS_ON_ID FOREIGN KEY (id) REFERENCES users (id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_on_user FOREIGN KEY (user_id) REFERENCES users (id);