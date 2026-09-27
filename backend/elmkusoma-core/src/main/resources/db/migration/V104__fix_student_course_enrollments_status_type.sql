-- Fix schema drift: entity expects VARCHAR(20) but column was created as smallint
ALTER TABLE student_course_enrollments
    DROP CONSTRAINT IF EXISTS student_course_enrollments_status_check;

ALTER TABLE student_course_enrollments
    ALTER COLUMN status TYPE varchar(20) USING status::text;

ALTER TABLE student_course_enrollments
    ALTER COLUMN status SET DEFAULT 'ENROLLED';
