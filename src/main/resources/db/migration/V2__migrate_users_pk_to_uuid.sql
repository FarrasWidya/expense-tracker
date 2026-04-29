-- Enable UUID generation function
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Step 1: Add new UUID column to users and backfill
ALTER TABLE users ADD COLUMN new_id UUID DEFAULT gen_random_uuid();
UPDATE users SET new_id = gen_random_uuid() WHERE new_id IS NULL;
ALTER TABLE users ALTER COLUMN new_id SET NOT NULL;

-- Step 2: Add UUID FK shadow columns on all child tables and backfill via join
DELETE FROM expense WHERE user_id IS NULL;
ALTER TABLE expense ADD COLUMN user_uuid UUID;
UPDATE expense e SET user_uuid = u.new_id FROM users u WHERE e.user_id = u.id;
ALTER TABLE expense ALTER COLUMN user_uuid SET NOT NULL;

ALTER TABLE rumah ADD COLUMN admin_uuid UUID;
UPDATE rumah r SET admin_uuid = u.new_id FROM users u WHERE r.admin_id = u.id;
ALTER TABLE rumah ALTER COLUMN admin_uuid SET NOT NULL;

ALTER TABLE rumah_member ADD COLUMN user_uuid UUID;
UPDATE rumah_member rm SET user_uuid = u.new_id FROM users u WHERE rm.user_id = u.id;
ALTER TABLE rumah_member ALTER COLUMN user_uuid SET NOT NULL;

ALTER TABLE shared_expense ADD COLUMN created_by_uuid UUID;
UPDATE shared_expense se SET created_by_uuid = u.new_id FROM users u WHERE se.created_by = u.id;
ALTER TABLE shared_expense ALTER COLUMN created_by_uuid SET NOT NULL;

-- Step 3: Drop all FK constraints on child tables (dynamic — handles any auto-generated constraint name)
DO $$ DECLARE r record; BEGIN
  FOR r IN SELECT conname FROM pg_constraint WHERE conrelid = 'expense'::regclass AND contype = 'f' LOOP
    EXECUTE 'ALTER TABLE expense DROP CONSTRAINT ' || quote_ident(r.conname);
  END LOOP;
  FOR r IN SELECT conname FROM pg_constraint WHERE conrelid = 'rumah'::regclass AND contype = 'f' LOOP
    EXECUTE 'ALTER TABLE rumah DROP CONSTRAINT ' || quote_ident(r.conname);
  END LOOP;
  FOR r IN SELECT conname FROM pg_constraint WHERE conrelid = 'rumah_member'::regclass AND contype = 'f' LOOP
    EXECUTE 'ALTER TABLE rumah_member DROP CONSTRAINT ' || quote_ident(r.conname);
  END LOOP;
  FOR r IN SELECT conname FROM pg_constraint WHERE conrelid = 'shared_expense'::regclass AND contype = 'f' LOOP
    EXECUTE 'ALTER TABLE shared_expense DROP CONSTRAINT ' || quote_ident(r.conname);
  END LOOP;
END $$;

-- Step 4: Drop integer PK on users and promote new_id to PK
ALTER TABLE users DROP CONSTRAINT users_pkey;
ALTER TABLE users DROP COLUMN id;
ALTER TABLE users RENAME COLUMN new_id TO id;
ALTER TABLE users ADD PRIMARY KEY (id);

-- Step 5: Swap FK columns and add new constraints

-- expense
ALTER TABLE expense DROP COLUMN user_id;
ALTER TABLE expense RENAME COLUMN user_uuid TO user_id;
ALTER TABLE expense ADD CONSTRAINT fk_expense_user FOREIGN KEY (user_id) REFERENCES users(id);

-- rumah
ALTER TABLE rumah DROP COLUMN admin_id;
ALTER TABLE rumah RENAME COLUMN admin_uuid TO admin_id;
ALTER TABLE rumah ADD CONSTRAINT fk_rumah_admin FOREIGN KEY (admin_id) REFERENCES users(id);

-- rumah_member (re-add both user and rumah FKs)
ALTER TABLE rumah_member DROP COLUMN user_id;
ALTER TABLE rumah_member RENAME COLUMN user_uuid TO user_id;
ALTER TABLE rumah_member ADD CONSTRAINT fk_rumah_member_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE rumah_member ADD CONSTRAINT fk_rumah_member_rumah FOREIGN KEY (rumah_id) REFERENCES rumah(id);

-- shared_expense (re-add both creator and rumah FKs)
ALTER TABLE shared_expense DROP COLUMN created_by;
ALTER TABLE shared_expense RENAME COLUMN created_by_uuid TO created_by;
ALTER TABLE shared_expense ADD CONSTRAINT fk_shared_expense_creator FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE shared_expense ADD CONSTRAINT fk_shared_expense_rumah FOREIGN KEY (rumah_id) REFERENCES rumah(id);
