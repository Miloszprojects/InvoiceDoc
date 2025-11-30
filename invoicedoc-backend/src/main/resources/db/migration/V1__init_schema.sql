-- ORGANIZATIONS
CREATE TABLE organizations (
                               id BIGSERIAL PRIMARY KEY,
                               name VARCHAR(255) NOT NULL,
                               created_at TIMESTAMP(6) NOT NULL,
                               CONSTRAINT uk_organizations_name UNIQUE (name)
);

-- SELLER_PROFILES
CREATE TABLE seller_profiles (
                                 id BIGSERIAL PRIMARY KEY,
                                 organization_id BIGINT NOT NULL,
                                 name VARCHAR(255) NOT NULL,
                                 nip_encrypted VARCHAR(255) NOT NULL,
                                 regon VARCHAR(255),
                                 krs VARCHAR(255),
                                 bank_name VARCHAR(255),
                                 bank_account VARCHAR(255),

    -- AddressEmbeddable
                                 street VARCHAR(255),
                                 building_number VARCHAR(255),
                                 apartment_number VARCHAR(255),
                                 postal_code VARCHAR(50),
                                 city VARCHAR(255),
                                 country VARCHAR(100),

                                 default_currency VARCHAR(3) NOT NULL,
                                 default_payment_term_days INTEGER NOT NULL,
                                 logo_path VARCHAR(255)
);

-- USERS
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(100) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(200) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       organization_id BIGINT,
                       approved_by_owner BOOLEAN NOT NULL,
                       CONSTRAINT uk_users_username UNIQUE (username),
                       CONSTRAINT uk_users_email UNIQUE (email)
);

-- CONTRACTORS
CREATE TABLE contractors (
                             id BIGSERIAL PRIMARY KEY,
                             organization_id BIGINT NOT NULL,
                             type VARCHAR(20) NOT NULL,
                             name VARCHAR(255) NOT NULL,
                             nip_encrypted VARCHAR(255),
                             pesel_encrypted VARCHAR(255),

    -- AddressEmbeddable
                             street VARCHAR(255),
                             building_number VARCHAR(255),
                             apartment_number VARCHAR(255),
                             postal_code VARCHAR(50),
                             city VARCHAR(255),
                             country VARCHAR(100),

                             email VARCHAR(255),
                             phone VARCHAR(50),
                             favorite BOOLEAN NOT NULL
);

-- INVOICES
CREATE TABLE invoices (
                          id BIGSERIAL PRIMARY KEY,
                          organization_id BIGINT NOT NULL,
                          seller_profile_id BIGINT,
                          contractor_id BIGINT,

                          seller_name VARCHAR(255) NOT NULL,
                          seller_nip_encrypted VARCHAR(255) NOT NULL,

    -- sellerAddress
                          seller_street VARCHAR(255),
                          seller_building_number VARCHAR(255),
                          seller_apartment_number VARCHAR(255),
                          seller_postal_code VARCHAR(50),
                          seller_city VARCHAR(255),
                          seller_country VARCHAR(100),

                          seller_bank_account VARCHAR(255),

                          buyer_name VARCHAR(255) NOT NULL,
                          buyer_nip_encrypted VARCHAR(255),
                          buyer_pesel_encrypted VARCHAR(255),

    -- buyerAddress
                          buyer_street VARCHAR(255),
                          buyer_building_number VARCHAR(255),
                          buyer_apartment_number VARCHAR(255),
                          buyer_postal_code VARCHAR(50),
                          buyer_city VARCHAR(255),
                          buyer_country VARCHAR(100),

                          number VARCHAR(255) NOT NULL,
                          issue_date DATE NOT NULL,
                          sale_date DATE NOT NULL,
                          due_date DATE NOT NULL,
                          payment_method VARCHAR(255) NOT NULL,
                          currency VARCHAR(3) NOT NULL,
                          status VARCHAR(255) NOT NULL,

                          total_net NUMERIC(15, 2) NOT NULL,
                          total_vat NUMERIC(15, 2) NOT NULL,
                          total_gross NUMERIC(15, 2) NOT NULL,

                          notes TEXT,
                          reverse_charge BOOLEAN NOT NULL,
                          split_payment BOOLEAN NOT NULL
);

-- INVOICE_ITEMS
CREATE TABLE invoice_items (
                               id BIGSERIAL PRIMARY KEY,
                               invoice_id BIGINT NOT NULL,
                               description VARCHAR(255) NOT NULL,
                               quantity NUMERIC(15, 4) NOT NULL,
                               unit VARCHAR(255),
                               net_unit_price NUMERIC(15, 2) NOT NULL,
                               vat_rate VARCHAR(10) NOT NULL,
                               net_total NUMERIC(15, 2) NOT NULL,
                               vat_amount NUMERIC(15, 2) NOT NULL,
                               gross_total NUMERIC(15, 2) NOT NULL
);

-- RELATIONS (FKs)

ALTER TABLE seller_profiles
    ADD CONSTRAINT fk_seller_profiles_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE contractors
    ADD CONSTRAINT fk_contractors_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE invoices
    ADD CONSTRAINT fk_invoices_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE invoices
    ADD CONSTRAINT fk_invoices_seller_profile
        FOREIGN KEY (seller_profile_id) REFERENCES seller_profiles(id);

ALTER TABLE invoices
    ADD CONSTRAINT fk_invoices_contractor
        FOREIGN KEY (contractor_id) REFERENCES contractors(id);

ALTER TABLE invoice_items
    ADD CONSTRAINT fk_invoice_items_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id);
