BEGIN;

CREATE TABLE IF NOT EXISTS user_profile_appearance (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    cover_index INTEGER NOT NULL DEFAULT 0 CHECK (cover_index BETWEEN 0 AND 5),
    avatar_index INTEGER NOT NULL DEFAULT 0 CHECK (avatar_index BETWEEN 0 AND 5)
);

CREATE TABLE IF NOT EXISTS user_favorite_stories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    story_id BIGINT NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_favorite_story UNIQUE (user_id, story_id)
);

-- Access goes through the authenticated Java API. No direct anonymous Supabase access.
ALTER TABLE user_profile_appearance ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_favorite_stories ENABLE ROW LEVEL SECURITY;

COMMIT;
