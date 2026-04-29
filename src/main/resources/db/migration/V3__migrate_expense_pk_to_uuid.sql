ALTER TABLE expense ADD COLUMN new_id UUID DEFAULT gen_random_uuid();
UPDATE expense SET new_id = gen_random_uuid() WHERE new_id IS NULL;
ALTER TABLE expense ALTER COLUMN new_id SET NOT NULL;
ALTER TABLE expense DROP CONSTRAINT expense_pkey;
ALTER TABLE expense DROP COLUMN id;
ALTER TABLE expense RENAME COLUMN new_id TO id;
ALTER TABLE expense ADD PRIMARY KEY (id);
