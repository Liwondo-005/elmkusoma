-- Fix national@elmkusoma.go.tz password to standard 'password' hash
UPDATE users SET password_hash = '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2' WHERE email = 'national@elmkusoma.go.tz';

-- Fix district@elmkusoma.go.tz password
UPDATE users SET password_hash = '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2' WHERE email = 'district@elmkusoma.go.tz';