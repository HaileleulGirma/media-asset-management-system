CREATE TABLE checker (
    checker_id BIGINT PRIMARY KEY,
    checker_name VARCHAR(255) NOT NULL
);

CREATE TABLE news (
    news_id BIGINT PRIMARY KEY,
    title TEXT NOT NULL,
    number_of_files INTEGER NOT NULL DEFAULT 0,
    total_size BIGINT NOT NULL DEFAULT 0,
    news_date DATE NOT NULL,
    location VARCHAR(255) NOT NULL,
    checked_by BIGINT NOT NULL,

    CONSTRAINT fk_news_checker
        FOREIGN KEY (checked_by)
        REFERENCES checker(checker_id)
);

CREATE TABLE cameraman (
    cameraman_id BIGINT PRIMARY KEY,
    cameraman_name VARCHAR(255) NOT NULL
);

CREATE TABLE reporter (
    reporter_id BIGINT PRIMARY KEY,
    reporter_name VARCHAR(255) NOT NULL
);

CREATE TABLE news_cameraman (
    news_id BIGINT NOT NULL,
    cameraman_id BIGINT NOT NULL,

    PRIMARY KEY (news_id, cameraman_id),

    CONSTRAINT fk_nc_news
        FOREIGN KEY (news_id)
        REFERENCES news(news_id),

    CONSTRAINT fk_nc_cameraman
        FOREIGN KEY (cameraman_id)
        REFERENCES cameraman(cameraman_id)
);

CREATE TABLE news_reporter (
    news_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,

    PRIMARY KEY (news_id, reporter_id),

    CONSTRAINT fk_nr_news
        FOREIGN KEY (news_id)
        REFERENCES news(news_id),

    CONSTRAINT fk_nr_reporter
        FOREIGN KEY (reporter_id)
        REFERENCES reporter(reporter_id)
);

CREATE TABLE video_asset (
    hash_id VARCHAR(255) PRIMARY KEY,
    news_id BIGINT NOT NULL,

    CONSTRAINT fk_video_news
        FOREIGN KEY (news_id)
        REFERENCES news(news_id)
);
