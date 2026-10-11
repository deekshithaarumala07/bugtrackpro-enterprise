CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    type VARCHAR(255),
    link VARCHAR(255),
    `read` BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_notifications_user_created (user_id, created_at),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
