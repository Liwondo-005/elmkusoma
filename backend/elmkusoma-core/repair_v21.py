from pathlib import Path
import re

path = Path("src/main/resources/db/migration/V21__sample_data.sql")

institution_id = "fbd2e3e3-99df-48f3-b138-58d6f6f84103"
admin_id = "22548351-f15a-4f60-b1ff-f807743ef4cc"
password_hash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

text = path.read_text(encoding="utf-8")

# ---------------------------------------------------------
# 1. Fix users INSERT headers
# ---------------------------------------------------------

old_header = (
    "INSERT INTO users "
    "(id, email, password_hash, first_name, middle_name, last_name, phone, "
    "role, is_active, is_email_verified, created_at, is_deleted)"
)

new_header = (
    "INSERT INTO users "
    "(id, institution_id, email, password_hash, first_name, middle_name, last_name, "
    "phone, role, is_active, is_email_verified, created_at, is_deleted)"
)

text = text.replace(old_header, new_header)

# ---------------------------------------------------------
# 2. Add institution_id to teacher/student/parent rows
# ---------------------------------------------------------

start_marker = "-- 2. CREATE USERS (Teachers)"
end_marker = "-- 5. CREATE INSTITUTION MEMBERSHIPS"

start = text.index(start_marker)
end = text.index(end_marker)

before = text[:start]
block = text[start:end]
after = text[end:]

pattern = re.compile(
    r"^(\s*\('[0-9a-f-]{36}'),\s*('[^']+@test\.com'),",
    re.MULTILINE
)

block = pattern.sub(
    rf"\1, '{institution_id}', \2,",
    block
)

# ---------------------------------------------------------
# 3. Add ADMIN user
# ---------------------------------------------------------

admin_sql = f"""-- 2.1 CREATE ADMIN USER
INSERT INTO users (
    id,
    institution_id,
    email,
    password_hash,
    first_name,
    middle_name,
    last_name,
    phone,
    role,
    is_active,
    is_email_verified,
    created_at,
    is_deleted
)
VALUES (
    '{admin_id}',
    '{institution_id}',
    'admin@test.com',
    '{password_hash}',
    'System',
    NULL,
    'Administrator',
    NULL,
    'ADMIN',
    true,
    true,
    '2026-01-10 08:00:00',
    false
);

"""

if "admin@test.com" not in block:
    block = block.replace(
        "-- 2. CREATE USERS (Teachers)\n",
        "-- 2. CREATE USERS (Teachers)\n" + admin_sql,
        1
    )

# ---------------------------------------------------------
# 4. Fix created_by references
# ---------------------------------------------------------

text = before + block + after

text = text.replace(
    "'admin@test.com'",
    f"'{admin_id}'"
)

# Restore the actual admin email in the admin user record.
text = text.replace(
    f"'{admin_id}',\n    '{institution_id}',\n    '{admin_id}',",
    f"'{admin_id}',\n    '{institution_id}',\n    'admin@test.com',"
)

path.write_text(text, encoding="utf-8")

print("V21 repair completed successfully.")
print(f"Institution ID: {institution_id}")
print(f"Admin ID:        {admin_id}")