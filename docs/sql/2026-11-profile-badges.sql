-- Apply before deploying: Render runs with ddl-auto=none.
CREATE TABLE IF NOT EXISTS profile_badges (
    user_id bigint PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    badge varchar(32) NOT NULL CHECK (badge IN (
        'FOUNDING_WRITER', 'FOUNDING_PARTNERS', 'FOUNDING_READER_1',
        'FOUNDING_READER_2', 'FOUNDING_READER_3'
    ))
);
