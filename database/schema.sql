CREATE TABLE news (
    news_id BIGINT GENERATED ALWAYS AS IDENTITY,
    title TEXT NOT NULL,
    number_of_files INTEGER NOT NULL,
    total_size FLOAT(53) NOT NULL,
    news_date DATE NOT NULL,
    location VARCHAR(255) NOT NULL,

    PRIMARY KEY (news_id)
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

CREATE TABLE staff_member (
    member_id BIGINT GENERATED ALWAYS AS IDENTITY,
    member_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL,

    PRIMARY KEY (member_id)
);

CREATE TABLE app_user (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,

    PRIMARY KEY (user_id)
);

CREATE TABLE app_role (
    role_id BIGINT GENERATED ALWAYS AS IDENTITY,
    role_name VARCHAR(255) NOT NULL UNIQUE,

    PRIMARY KEY (role_id)
);

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

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id)
        REFERENCES app_role(role_id)
        ON DELETE CASCADE
);

CREATE TABLE news_importer (
    news_id BIGINT NOT NULL,
    imported_by BIGINT NOT NULL,

    PRIMARY KEY (news_id, imported_by),

    CONSTRAINT fk_news_importer_news
        FOREIGN KEY (news_id)
        REFERENCES news(news_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_news_importer_staff
        FOREIGN KEY (imported_by)
        REFERENCES staff_member(member_id)
);

CREATE TABLE news_ingestor (
    news_id BIGINT NOT NULL,
    ingested_by BIGINT NOT NULL,

    PRIMARY KEY (news_id, ingested_by),

    CONSTRAINT fk_news_ingestor_news
        FOREIGN KEY (news_id)
        REFERENCES news(news_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_news_ingestor_staff
        FOREIGN KEY (ingested_by)
        REFERENCES staff_member(member_id)
);