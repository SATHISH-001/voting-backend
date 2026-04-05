-- =====================================================
-- Secure Voting System v4 Schema
-- =====================================================

CREATE TABLE IF NOT EXISTS users (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100)  NOT NULL,
    email               VARCHAR(150)  NOT NULL UNIQUE,
    password            VARCHAR(255)  NOT NULL,
    phone               VARCHAR(20),
    address             TEXT,
    voter_id            VARCHAR(50)   UNIQUE,
    voter_id_verified   BOOLEAN       DEFAULT FALSE,
    voter_id_rejected   BOOLEAN       DEFAULT FALSE,
    voter_id_reject_msg VARCHAR(500),
    face_image_path     VARCHAR(500),
    role                ENUM('VOTER','ADMIN') DEFAULT 'VOTER',
    verified            BOOLEAN       DEFAULT FALSE,
    otp_code            VARCHAR(10),
    otp_expiry          DATETIME,
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS polls (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      ENUM('DRAFT','ACTIVE','CLOSED') DEFAULT 'DRAFT',
    start_time  DATETIME     NOT NULL,
    end_time    DATETIME     NOT NULL,
    created_by  BIGINT       NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_poll_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS poll_options (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id       BIGINT       NOT NULL,
    label         VARCHAR(200) NOT NULL,
    display_order INT          DEFAULT 0,
    CONSTRAINT fk_option_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS votes (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id          BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL,
    encrypted_choice TEXT         NOT NULL,
    vote_hash        VARCHAR(64)  NOT NULL,
    face_verified    BOOLEAN      DEFAULT FALSE,
    face_snap_path   VARCHAR(500),
    cast_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_poll UNIQUE (poll_id, user_id),
    CONSTRAINT fk_vote_poll FOREIGN KEY (poll_id) REFERENCES polls(id),
    CONSTRAINT fk_vote_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    action     VARCHAR(100)  NOT NULL,
    user_id    BIGINT,
    details    VARCHAR(1000),
    ip_address VARCHAR(45),
    logged_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- Default admin (password: Admin@123)
INSERT INTO users (name, email, password, role, verified, voter_id, voter_id_verified)
VALUES ('Admin', 'admin@voting.com',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCMc7C5biN.rqxcGlBpuNQa',
        'ADMIN', TRUE, 'ADMIN-001', TRUE);
