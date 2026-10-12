-- Fix regional.dar@test.com password to standard 'password' hash
UPDATE users SET password_hash = '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2' WHERE email = 'regional.dar@test.com';

-- Fix regional.aru@test.com password
UPDATE users SET password_hash = '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2' WHERE email = 'regional.aru@test.com';

-- Fix district.ila@test.com password
UPDATE users SET password_hash = '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2' WHERE email = 'district.ila@test.com';

-- Fix district.arc@test.com password
UPDATE users SET password_hash = '$2a$10$2xvXqwp2cvM36F9V5ZYW.uFijWhdJRrYrQfKMA19S78lawlUt4Nj2' WHERE email = 'district.arc@test.com';