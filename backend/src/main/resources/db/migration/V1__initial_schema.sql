-- =====================================================
-- V1 INITIAL SCHEMA
--
-- Layout:
--   0. Extensions and functions (Amharic search support)
--   1. Lookup tables
--   2. Authentication (roles, users)
--   3. News (+ link tables, indexes)
--   4. Photo (+ link tables, indexes)
--   5. Digitized media / cassettes (+ cuts, indexes)
--   6. Production (+ indexes)
--
-- Index naming: idx_<table>_<column>[_desc | _trgm]
--   _desc : date column indexed newest-first for list pages
--   _trgm : trigram search index on amh_norm(title)
-- =====================================================


-- =====================================================
-- 0. EXTENSIONS AND FUNCTIONS
-- =====================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Maps look-alike Amharic letters to one canonical letter.
-- Each translate() pair must have FROM and TO of equal length.
-- Steps run from the inside out, and order matters:
--   ሓ ኃ -> ሃ (steps 1-2), then ሃ -> ሀ (step 3)
--   ዓ -> ኣ (step 6), then ኣ -> አ (step 7)
-- If this mapping ever changes, REINDEX every *_trgm index.
CREATE OR REPLACE FUNCTION amh_norm(t text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT AS $$
SELECT translate(translate(translate(translate(translate(translate(translate(translate(t,
                                                                                       'ሐሑሒሓሔሕሖ',   'ሀሁሂሃሄህሆ'),     -- ሐ-row -> ሀ-row
                                                                             'ኀኁኂኃኄኅኆ',   'ሀሁሂሃሄህሆ'),     -- ኀ-row -> ሀ-row
                                                                   'ሃ',           'ሀ'),              -- 4th order ሃ -> ሀ
                                                         'ሗኇ',          'ኋሇ'),             -- labialized forms
                                               'ሠሡሢሣሤሥሦሧ',  'ሰሱሲሳሴስሶሷ'),    -- ሠ-row -> ሰ-row
                                     'ዐዑዒዓዔዕዖ',   'አኡኢኣኤእኦ'),     -- ዐ-row -> አ-row
                           'ኣ',           'አ'),              -- 4th order ኣ -> አ
                 'ፀፁፂፃፄፅፆፇ',  'ጸጹጺጻጼጽጾጿ')     -- ፀ-row -> ጸ-row
$$;


-- =====================================================
-- 1. LOOKUP TABLES (no foreign keys)
-- =====================================================

CREATE TABLE staff_member (
                              member_id BIGINT GENERATED ALWAYS AS IDENTITY,
                              member_name VARCHAR(255) NOT NULL UNIQUE,
                              is_active BOOLEAN NOT NULL,

                              PRIMARY KEY (member_id)
);

CREATE TABLE cameraman (
                           cameraman_id BIGINT GENERATED ALWAYS AS IDENTITY,
                           cameraman_name VARCHAR(255) NOT NULL UNIQUE,
                           is_active BOOLEAN NOT NULL,

                           PRIMARY KEY (cameraman_id)
);

CREATE TABLE reporter (
                          reporter_id BIGINT GENERATED ALWAYS AS IDENTITY,
                          reporter_name VARCHAR(255) NOT NULL UNIQUE,
                          is_active BOOLEAN NOT NULL,

                          PRIMARY KEY (reporter_id)
);

CREATE TABLE location (
                          location_id BIGINT GENERATED ALWAYS AS IDENTITY,
                          location_name VARCHAR(255) NOT NULL UNIQUE,
                          is_abroad BOOLEAN NOT NULL,

                          PRIMARY KEY (location_id)
);

CREATE TABLE cassette_category (
                                   cassette_category_id BIGINT GENERATED ALWAYS AS IDENTITY,
                                   category_name VARCHAR(255) NOT NULL UNIQUE,

                                   PRIMARY KEY (cassette_category_id)
);


-- =====================================================
-- 2. AUTHENTICATION
-- =====================================================

CREATE TABLE app_role (
                          role_id BIGINT GENERATED ALWAYS AS IDENTITY,
                          role_name VARCHAR(255) NOT NULL UNIQUE,

                          PRIMARY KEY (role_id)
);

CREATE TABLE app_user (
                          user_id BIGINT GENERATED ALWAYS AS IDENTITY,
                          username VARCHAR(255) NOT NULL UNIQUE,
                          password VARCHAR(255) NOT NULL,
                          full_name VARCHAR(255) NOT NULL UNIQUE,
                          role_id BIGINT NOT NULL,
                          last_changed DATE NOT NULL,

                          PRIMARY KEY (user_id),

                          CONSTRAINT fk_user_role
                              FOREIGN KEY (role_id)
                                  REFERENCES app_role(role_id)
);

CREATE INDEX idx_app_user_role_id
    ON app_user(role_id);


-- =====================================================
-- 3. NEWS
-- =====================================================

CREATE TABLE news (
                      news_id BIGINT GENERATED ALWAYS AS IDENTITY,
                      title TEXT NOT NULL,
                      number_of_files INTEGER
                          CONSTRAINT chk_news_number_of_files_positive
                              CHECK (number_of_files > 0),
                      total_size_gb NUMERIC(12,2)
                          CONSTRAINT chk_news_total_size_positive
                              CHECK (total_size_gb > 0),
                      news_date DATE NOT NULL,
                      file_path TEXT NOT NULL,
                      private BOOLEAN NOT NULL DEFAULT FALSE,

                      imported_by BIGINT,
                      ingested_by BIGINT,
                      version BIGINT NOT NULL DEFAULT 0,

                      PRIMARY KEY (news_id),

                      CONSTRAINT fk_news_importer
                          FOREIGN KEY (imported_by)
                              REFERENCES staff_member(member_id),

                      CONSTRAINT fk_news_ingestor
                          FOREIGN KEY (ingested_by)
                              REFERENCES staff_member(member_id)
);

-- News: link tables

CREATE TABLE news_cameraman (
                                news_id BIGINT NOT NULL,
                                cameraman_id BIGINT NOT NULL,

                                PRIMARY KEY (news_id, cameraman_id),

                                CONSTRAINT fk_news_cameraman_news
                                    FOREIGN KEY (news_id)
                                        REFERENCES news(news_id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_news_cameraman_cameraman
                                    FOREIGN KEY (cameraman_id)
                                        REFERENCES cameraman(cameraman_id)
);

CREATE TABLE news_reporter (
                               news_id BIGINT NOT NULL,
                               reporter_id BIGINT NOT NULL,

                               PRIMARY KEY (news_id, reporter_id),

                               CONSTRAINT fk_news_reporter_news
                                   FOREIGN KEY (news_id)
                                       REFERENCES news(news_id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_news_reporter_reporter
                                   FOREIGN KEY (reporter_id)
                                       REFERENCES reporter(reporter_id)
);

CREATE TABLE news_location (
                               news_id BIGINT NOT NULL,
                               location_id BIGINT NOT NULL,

                               PRIMARY KEY (news_id, location_id),

                               CONSTRAINT fk_news_location_news
                                   FOREIGN KEY (news_id)
                                       REFERENCES news(news_id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_news_location_location
                                   FOREIGN KEY (location_id)
                                       REFERENCES location(location_id)
);

-- News: indexes

CREATE INDEX idx_news_date_desc
    ON news(news_date DESC);

CREATE INDEX idx_news_imported_by
    ON news(imported_by);

CREATE INDEX idx_news_ingested_by
    ON news(ingested_by);

CREATE INDEX idx_news_title_trgm
    ON news USING gin (amh_norm(title) gin_trgm_ops);

CREATE INDEX idx_news_cameraman_cameraman
    ON news_cameraman(cameraman_id);

CREATE INDEX idx_news_reporter_reporter
    ON news_reporter(reporter_id);

CREATE INDEX idx_news_location_location
    ON news_location(location_id);


-- =====================================================
-- 4. PHOTO
-- =====================================================

CREATE TABLE photo (
                       photo_id BIGINT GENERATED ALWAYS AS IDENTITY,
                       title TEXT NOT NULL,
                       file_path TEXT NOT NULL,
                       number_of_files INTEGER NOT NULL
                           CONSTRAINT chk_photo_number_of_files_positive
                               CHECK (number_of_files > 0),
                       file_size_mb NUMERIC(12,2) NOT NULL
                           CONSTRAINT chk_photo_file_size_positive
                               CHECK (file_size_mb > 0),
                       photo_date DATE NOT NULL,
                       private BOOLEAN NOT NULL DEFAULT FALSE,

                       imported_by BIGINT NOT NULL,
                       version BIGINT NOT NULL DEFAULT 0,

                       PRIMARY KEY (photo_id),

                       CONSTRAINT fk_photo_staff
                           FOREIGN KEY (imported_by)
                               REFERENCES staff_member(member_id)
);

-- Photo: link tables

CREATE TABLE photo_cameraman (
                                 photo_id BIGINT NOT NULL,
                                 cameraman_id BIGINT NOT NULL,

                                 PRIMARY KEY (photo_id, cameraman_id),

                                 CONSTRAINT fk_photo_cameraman_photo
                                     FOREIGN KEY (photo_id)
                                         REFERENCES photo(photo_id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_photo_cameraman_cameraman
                                     FOREIGN KEY (cameraman_id)
                                         REFERENCES cameraman(cameraman_id)
);

CREATE TABLE photo_location (
                                photo_id BIGINT NOT NULL,
                                location_id BIGINT NOT NULL,

                                PRIMARY KEY (photo_id, location_id),

                                CONSTRAINT fk_photo_location_photo
                                    FOREIGN KEY (photo_id)
                                        REFERENCES photo(photo_id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_photo_location_location
                                    FOREIGN KEY (location_id)
                                        REFERENCES location(location_id)
);

-- Photo: indexes

CREATE INDEX idx_photo_date_desc
    ON photo(photo_date DESC);

CREATE INDEX idx_photo_imported_by
    ON photo(imported_by);

CREATE INDEX idx_photo_title_trgm
    ON photo USING gin (amh_norm(title) gin_trgm_ops);

CREATE INDEX idx_photo_cameraman_cameraman
    ON photo_cameraman(cameraman_id);

CREATE INDEX idx_photo_location_location
    ON photo_location(location_id);


-- =====================================================
-- 5. DIGITIZED MEDIA / CASSETTES
--
-- One digitized_media record represents one physical cassette.
-- The cassette itself does not have a content title.
-- Its identifier identifies the cassette.
-- The individual content titles belong to cassette_cut.
--
-- One cassette can contain multiple cuts.
-- Each cut has:
--   - its own title
--   - an optional date
--   - exactly one category
--   - exactly one parent cassette
-- =====================================================

CREATE TABLE digitized_media (
                                 digitized_media_id BIGINT GENERATED ALWAYS AS IDENTITY,
                                 digitized_date DATE NOT NULL,

                                 identifier_category_id BIGINT NOT NULL,
                                 identifier_number INTEGER NOT NULL,

                                 file_path TEXT NOT NULL,
                                 private BOOLEAN NOT NULL DEFAULT FALSE,

                                 imported_by BIGINT NOT NULL,
                                 version BIGINT NOT NULL DEFAULT 0,

                                 PRIMARY KEY (digitized_media_id),

                                 CONSTRAINT uq_digitized_media_identifier
                                     UNIQUE (identifier_category_id, identifier_number),

                                 CONSTRAINT fk_digitized_media_identifier_category
                                     FOREIGN KEY (identifier_category_id)
                                         REFERENCES cassette_category(cassette_category_id),

                                 CONSTRAINT fk_digitized_media_staff
                                     FOREIGN KEY (imported_by)
                                         REFERENCES staff_member(member_id)
);

CREATE TABLE cassette_cut (
                              cassette_cut_id BIGINT GENERATED ALWAYS AS IDENTITY,

                              digitized_media_id BIGINT NOT NULL,
                              cassette_category_id BIGINT NOT NULL,

                              cut_title TEXT NOT NULL,
                              cut_date DATE,
                              version BIGINT NOT NULL DEFAULT 0,

                              PRIMARY KEY (cassette_cut_id),

                              CONSTRAINT fk_cassette_cut_digitized_media
                                  FOREIGN KEY (digitized_media_id)
                                      REFERENCES digitized_media(digitized_media_id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_cassette_cut_category
                                  FOREIGN KEY (cassette_category_id)
                                      REFERENCES cassette_category(cassette_category_id)
);

-- Digitized media: indexes

CREATE INDEX idx_digitized_media_date_desc
    ON digitized_media(digitized_date DESC);

CREATE INDEX idx_digitized_media_imported_by
    ON digitized_media(imported_by);

-- Cassette cut: indexes

CREATE INDEX idx_cassette_cut_digitized_media_id
    ON cassette_cut(digitized_media_id);

CREATE INDEX idx_cassette_cut_category_id
    ON cassette_cut(cassette_category_id);

CREATE INDEX idx_cassette_cut_date
    ON cassette_cut(cut_date);

CREATE INDEX idx_cassette_cut_title_trgm
    ON cassette_cut USING gin (amh_norm(cut_title) gin_trgm_ops);


-- =====================================================
-- 6. PRODUCTION
-- =====================================================

CREATE TABLE production (
                            production_id BIGINT GENERATED ALWAYS AS IDENTITY,
                            production_date DATE NOT NULL,
                            title TEXT NOT NULL,
                            file_path TEXT NOT NULL,
                            file_size_gb NUMERIC(12,2)
                                CONSTRAINT chk_production_file_size_positive
                                    CHECK (file_size_gb > 0),
                            production_duration_sec INTEGER NOT NULL,
                            version BIGINT NOT NULL DEFAULT 0,

                            PRIMARY KEY (production_id)
);

-- Production: indexes

CREATE INDEX idx_production_date_desc
    ON production(production_date DESC);

CREATE INDEX idx_production_title_trgm
    ON production USING gin (amh_norm(title) gin_trgm_ops);