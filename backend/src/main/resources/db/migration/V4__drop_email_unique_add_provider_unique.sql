DO $$
DECLARE
    email_attnum SMALLINT;
    constraint_name TEXT;
BEGIN
    SELECT attnum
    INTO email_attnum
    FROM pg_attribute
    WHERE attrelid = 'users'::regclass
      AND attname = 'email'
      AND NOT attisdropped;

    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        WHERE con.conrelid = 'users'::regclass
          AND con.contype = 'u'
          AND array_length(con.conkey, 1) = 1
          AND con.conkey[1] = email_attnum
    LOOP
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE users
    ALTER COLUMN provider SET NOT NULL,
    ALTER COLUMN provider_user_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_provider_identity UNIQUE (provider, provider_user_id);