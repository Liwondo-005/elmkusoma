-- V07: Add category column to subjects (table created in V06)
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS category VARCHAR(100);
