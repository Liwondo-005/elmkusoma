DO $$ BEGIN
    ALTER TABLE student_portfolio_items ALTER COLUMN institution_id DROP NOT NULL;
EXCEPTION WHEN others THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE student_portfolio_items ALTER COLUMN file_url DROP NOT NULL;
EXCEPTION WHEN others THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE student_portfolio_items ALTER COLUMN file_url TYPE TEXT;
EXCEPTION WHEN others THEN NULL;
END $$;

ALTER TABLE student_portfolio_items ADD COLUMN IF NOT EXISTS content TEXT;
