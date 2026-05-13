CREATE TABLE checker (
    checker_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    checker_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE cameraman (
    cameraman_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cameraman_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE reporter (
    reporter_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reporter_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE news (
    news_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title TEXT NOT NULL,
    number_of_files INTEGER NOT NULL DEFAULT 0,
    total_size BIGINT NOT NULL DEFAULT 0,
    news_date DATE NOT NULL,
    location VARCHAR(255) NOT NULL
);

CREATE TABLE news_checker (
    news_id BIGINT NOT NULL,
    checker_id BIGINT NOT NULL,

    PRIMARY KEY (news_id, checker_id),

    CONSTRAINT fk_news_checker_news
        FOREIGN KEY (news_id)
        REFERENCES news(news_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_news_checker_checker
        FOREIGN KEY (checker_id)
        REFERENCES checker(checker_id)
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

CREATE TABLE app_user (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE app_role (
    role_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL UNIQUE
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
);