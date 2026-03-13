UPDATE users
SET provider = 'GOOGLE'
WHERE provider IS NULL
  AND google_id IS NOT NULL;

UPDATE users
SET provider_user_id = google_id
WHERE provider_user_id IS NULL
  AND google_id IS NOT NULL;

UPDATE users
SET last_login_flow = login_method
WHERE last_login_flow IS NULL
  AND login_method IN ('SERVER_SIDE', 'CLIENT_SIDE');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        WHERE provider IS NULL OR provider_user_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Release 2 backfill failed: users still contain null provider identity';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM users
        GROUP BY provider, provider_user_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Release 2 backfill failed: duplicate provider identity detected';
    END IF;
END $$;