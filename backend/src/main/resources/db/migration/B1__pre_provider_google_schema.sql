CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    google_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    picture_url VARCHAR(255),
    login_method VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_users_google_id UNIQUE (google_id),
    CONSTRAINT uk_users_email UNIQUE (email)
);