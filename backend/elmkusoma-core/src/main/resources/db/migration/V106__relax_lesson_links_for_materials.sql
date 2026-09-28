-- Materials may be attached to any lesson.
--
-- Resources and video tutorials are linked to a lesson by id, but lesson ids come
-- from two tables: course_lessons (course builder) and lessons (/v1/learning/lessons).
-- The hard foreign keys only accepted course_lessons, so attaching a material to a
-- lesson created through the learning API failed with an FK violation even though
-- every read path (e.g. GET /v1/learner/courses/lessons/{id}) looks up both tables.
--
-- Drop the one-sided constraints and keep the soft link + indexes instead.

ALTER TABLE resources DROP CONSTRAINT IF EXISTS fk_resources_lesson;
ALTER TABLE video_tutorials DROP CONSTRAINT IF EXISTS video_tutorials_lesson_id_fkey;

CREATE INDEX IF NOT EXISTS idx_resources_lesson_id ON resources (lesson_id);
CREATE INDEX IF NOT EXISTS idx_video_tutorials_lesson_id ON video_tutorials (lesson_id);
