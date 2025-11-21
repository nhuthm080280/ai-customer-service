-- V1__create_products_schema.sql
-- Create tables for products, variants, images, and options
-- Designed for PostgreSQL. Keeps the original upstream IDs (bigint)
-- and also stores raw JSON payloads for easy future changes.

-- Enable needed extensions (if not already enabled)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- PRODUCTS
CREATE TABLE products (
    id                BIGINT PRIMARY KEY,    -- upstream product id (Shopify)
    title             TEXT NOT NULL,
    handle            TEXT,
    body_html         TEXT,
    published_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,
    vendor            TEXT,
    product_type      TEXT,
    tags              TEXT[],                 -- tags array (may be empty)
    options           JSONB,                  -- raw "options" structure (name, position, values)
    raw_json          JSONB,                  -- store full original JSON for this product
    created_on        TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_products_title ON products (lower(title));
CREATE INDEX idx_products_handle ON products (handle);

-- VARIANTS
CREATE TABLE variants (
    id                BIGINT PRIMARY KEY,    -- upstream variant id
    product_id        BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    title             TEXT,
    option1           TEXT,
    option2           TEXT,
    option3           TEXT,
    sku               TEXT,
    requires_shipping BOOLEAN,
    taxable           BOOLEAN,
    featured_image_id BIGINT,                 -- references images.id if desired (nullable)
    available         BOOLEAN,
    price             NUMERIC(12,2),
    grams             INTEGER,
    compare_at_price  NUMERIC(12,2),
    position          INTEGER,
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,
    raw_json          JSONB,
    created_on        TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_variants_product_id ON variants (product_id);
CREATE INDEX idx_variants_sku ON variants (sku);

-- IMAGES
CREATE TABLE images (
    id           BIGINT PRIMARY KEY,        -- upstream image id
    product_id   BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ,
    position     INTEGER,
    src          TEXT,
    alt          TEXT,
    width        INTEGER,
    height       INTEGER,
    variant_ids  BIGINT[],                  -- array of variant ids that reference this image
    raw_json     JSONB,
    created_on   TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_images_product_id ON images (product_id);

-- OPTIONAL: A small table for option values if you prefer normalization (not required)
CREATE TABLE product_option_values (
    id          UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    option_name TEXT NOT NULL,
    value       TEXT NOT NULL,
    position    INTEGER
);

CREATE INDEX idx_option_values_product ON product_option_values (product_id);

-- Helpful view: product with primary fields and variant count
CREATE VIEW vw_product_overview AS
SELECT
    p.id,
    p.title,
    p.handle,
    p.vendor,
    p.product_type,
    p.tags,
    p.published_at,
    p.created_at,
    p.updated_at,
    COALESCE(v.variant_count, 0) AS variant_count
FROM products p
         LEFT JOIN (
    SELECT product_id, count(*) AS variant_count
    FROM variants
    GROUP BY product_id
) v ON v.product_id = p.id;

-- Example constraint/guard (optional)
ALTER TABLE products
    ADD CONSTRAINT chk_products_title_not_empty CHECK (coalesce(btrim(title), '') <> '');

