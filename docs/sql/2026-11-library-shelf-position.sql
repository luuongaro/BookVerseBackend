-- Run before deploying the backend: Render uses ddl-auto=none.
ALTER TABLE reading_statuses ADD COLUMN IF NOT EXISTS shelf_position integer;
