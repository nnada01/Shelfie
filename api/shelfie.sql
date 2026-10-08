-- Full schema for new installs. Upgrading an old DB: use schema_extensions.sql instead of re-running this file.
-- Location: XAMPP htdocs/shelfie_api/ (with your PHP API). Import via phpMyAdmin on shelfie_db.
CREATE DATABASE shelfie_db;
USE shelfie_db;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    is_admin TINYINT(1) NOT NULL DEFAULT 0,
    birthday DATE NULL,
    country VARCHAR(100) NULL,
    profile_image LONGBLOB NULL,
    reading_goal_type VARCHAR(20) NULL DEFAULT NULL,
    reading_goal_value INT NULL DEFAULT NULL,
    pages_read_week_total INT NOT NULL DEFAULT 0,
    pages_week_key VARCHAR(12) NULL DEFAULT NULL
);

CREATE TABLE books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    author VARCHAR(100) NOT NULL,
    category VARCHAR(80),
    total_pages INT DEFAULT 0,
    current_page INT DEFAULT 0,
    status VARCHAR(30) DEFAULT 'Reading',
    is_favorite TINYINT(1) DEFAULT 0,
    rating DECIMAL(2,1) NOT NULL DEFAULT 0.0,
    genres VARCHAR(255) NULL,
    is_recommended TINYINT(1) NOT NULL DEFAULT 0,
    completed_at DATE NULL DEFAULT NULL,
    CONSTRAINT fk_books_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE journals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    mood VARCHAR(30) DEFAULT 'Neutral',
    entry_date DATE NOT NULL,
    CONSTRAINT fk_journals_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE reading_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    log_date DATE NOT NULL,
    CONSTRAINT fk_reading_logs_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    UNIQUE KEY uq_user_log_date (user_id, log_date)
);

-- Per-book pages counted toward the weekly reading goal (see weekly_pages.inc.php)
CREATE TABLE book_weekly_pages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    week_key VARCHAR(10) NOT NULL,
    pages INT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE KEY uk_book_week (book_id, week_key),
    KEY idx_user_week (user_id, week_key),
    CONSTRAINT fk_bwp_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_bwp_book
        FOREIGN KEY (book_id) REFERENCES books(id)
        ON DELETE CASCADE
);
