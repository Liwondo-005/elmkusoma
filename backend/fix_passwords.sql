-- Fix student1@darms.edu.tz password to standard 'password' hash
UPDATE users SET password_hash = '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2' WHERE email = 'student1@darms.edu.tz';

-- Fix admin@darms.edu.tz password to standard 'password' hash (if needed)
UPDATE users SET password_hash = '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2' WHERE email = 'admin@darms.edu.tz' AND password_hash != '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2';

-- Fix national@elmkusoma.go.tz email_verified to true
UPDATE users SET is_email_verified = true WHERE email = 'national@elmkusoma.go.tz';

-- Fix district@elmkusoma.go.tz email_verified to true
UPDATE users SET is_email_verified = true WHERE email = 'district@elmkusoma.go.tz';