-- =====================================================
-- LOOKUP TABLES (no foreign keys)
-- =====================================================

CREATE TABLE staff_member (
                              member_id BIGINT GENERATED ALWAYS AS IDENTITY,
                              member_name VARCHAR(255) NOT NULL,
                              is_active BOOLEAN NOT NULL,

                              PRIMARY KEY (member_id)
);

CREATE TABLE cameraman (
                           cameraman_id BIGINT GENERATED ALWAYS AS IDENTITY,
                           cameraman_name VARCHAR(255) NOT NULL,
                           is_active BOOLEAN NOT NULL,

                           PRIMARY KEY (cameraman_id)
);

CREATE TABLE reporter (
                          reporter_id BIGINT GENERATED ALWAYS AS IDENTITY,
                          reporter_name VARCHAR(255) NOT NULL,
                          is_active BOOLEAN NOT NULL,

                          PRIMARY KEY (reporter_id)
);

CREATE TABLE location (
                          location_id BIGINT GENERATED ALWAYS AS IDENTITY,
                          location_name VARCHAR(255) NOT NULL,
                          is_abroad BOOLEAN NOT NULL,

                          PRIMARY KEY (location_id)
);

CREATE TABLE app_role (
                          role_id BIGINT GENERATED ALWAYS AS IDENTITY,
                          role_name VARCHAR(255) NOT NULL UNIQUE,

                          PRIMARY KEY (role_id)
);

-- =====================================================
-- TABLES THAT DEPEND ON LOOKUP TABLES
-- =====================================================

CREATE TABLE news (
                      news_id BIGINT GENERATED ALWAYS AS IDENTITY,
                      title TEXT NOT NULL,
                      number_of_files INTEGER NOT NULL,
                      total_size FLOAT(53) NOT NULL,
                      news_date DATE NOT NULL,
                      file_path VARCHAR(255) NOT NULL,

                      imported_by BIGINT NOT NULL,
                      ingested_by BIGINT NOT NULL,

                      PRIMARY KEY (news_id),

                      CONSTRAINT fk_news_importer
                          FOREIGN KEY (imported_by)
                              REFERENCES staff_member(member_id),

                      CONSTRAINT fk_news_ingestor
                          FOREIGN KEY (ingested_by)
                              REFERENCES staff_member(member_id)
);

CREATE TABLE app_user (
                          user_id BIGINT GENERATED ALWAYS AS IDENTITY,
                          username VARCHAR(255) NOT NULL UNIQUE,
                          password VARCHAR(255) NOT NULL,
                          full_name VARCHAR(255) NOT NULL UNIQUE,
                          role_id BIGINT NOT NULL,

                          PRIMARY KEY (user_id),

                          CONSTRAINT fk_user_role
                              FOREIGN KEY (role_id)
                                  REFERENCES app_role(role_id)
);

-- =====================================================
-- MANY-TO-MANY TABLES
-- =====================================================

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


-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_news_reporter_reporter
    ON news_reporter(reporter_id);

CREATE INDEX idx_news_cameraman_cameraman
    ON news_cameraman(cameraman_id);

CREATE INDEX idx_news_location_location
    ON news_location(location_id);

CREATE INDEX idx_news_date_desc
    ON news(news_date DESC);

CREATE INDEX idx_news_imported_by
    ON news(imported_by);

CREATE INDEX idx_news_ingested_by
    ON news(ingested_by);

CREATE INDEX idx_app_user_role_id
    ON app_user(role_id);

CREATE INDEX news_title_fts_idx
    ON news
        USING gin (
                   to_tsvector('simple', title)
            );