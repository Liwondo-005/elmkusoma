-- ============================================================
-- PRIMARY CURRICULUM SEED DATA
-- Inserts Primary-level subjects and curriculum topics
-- Apply via: psql -U postgres -d elmkusoma -f primary_curriculum_seed.sql
-- NOTE: Update institution_id to match your institution
-- ============================================================

-- Use the sample institution ID from V21 seed data
-- Replace this with your actual institution ID if different
DO $$
DECLARE
    inst_id UUID := 'fbd2e3e3-99df-48f3-b138-58d6f6f84103';
    math_id UUID;
    eng_id UUID;
    kis_id UUID;
    sci_id UUID;
    soc_id UUID;
    mor_id UUID;
    pe_id UUID;
    art_id UUID;
BEGIN

-- =============================================================================
-- SUBJECTS (Primary Level)
-- =============================================================================

INSERT INTO subjects (id, institution_id, education_level, name, code, description, is_active, created_at, is_deleted)
VALUES (gen_random_uuid(), inst_id, 'PRIMARY', 'Mathematics', 'MATH-P', 'Number sense, operations, geometry, measurement, and data handling for primary learners', true, NOW(), false)
RETURNING id INTO math_id;

INSERT INTO subjects (id, institution_id, education_level, name, code, description, is_active, created_at, is_deleted)
VALUES (gen_random_uuid(), inst_id, 'PRIMARY', 'English Language', 'ENG-P', 'Reading, writing, grammar, and communication skills in English', true, NOW(), false)
RETURNING id INTO eng_id;

INSERT INTO subjects (id, institution_id, education_level, name, code, description, is_active, created_at, is_deleted)
VALUES (gen_random_uuid(), inst_id, 'PRIMARY', 'Kiswahili Language', 'KIS-P', 'Usomaji, uandishi, na sarufi ya Kiswahili', true, NOW(), false)
RETURNING id INTO kis_id;

INSERT INTO subjects (id, institution_id, education_level, name, code, description, is_active, created_at, is_deleted)
VALUES (gen_random_uuid(), inst_id, 'PRIMARY', 'Science and Technology', 'SCI-P', 'Living things, materials, forces, energy, and earth sciences', true, NOW(), false)
RETURNING id INTO sci_id;

INSERT INTO subjects (id, institution_id, education_level, name, code, description, is_active, created_at, is_deleted)
VALUES (gen_random_uuid(), inst_id, 'PRIMARY', 'Social Studies', 'SOC-P', 'Family, community, Tanzania, Africa, and global awareness', true, NOW(), false)
RETURNING id INTO soc_id;

INSERT INTO subjects (id, institution_id, education_level, name, code, description, is_active, created_at, is_deleted)
VALUES (gen_random_uuid(), inst_id, 'PRIMARY', 'Moral and Religious Education', 'MOR-P', 'Values, ethics, cultural practices, and environmental stewardship', true, NOW(), false)
RETURNING id INTO mor_id;

INSERT INTO subjects (id, institution_id, education_level, name, code, description, is_active, created_at, is_deleted)
VALUES (gen_random_uuid(), inst_id, 'PRIMARY', 'Physical Education', 'PE-P', 'Movement, fitness, games, sports, and health education', true, NOW(), false)
RETURNING id INTO pe_id;

INSERT INTO subjects (id, institution_id, education_level, name, code, description, is_active, created_at, is_deleted)
VALUES (gen_random_uuid(), inst_id, 'PRIMARY', 'Creative Arts', 'ART-P', 'Drawing, painting, music, dance, drama, and crafts', true, NOW(), false)
RETURNING id INTO art_id;

-- =============================================================================
-- CURRICULUM TOPICS - Mathematics
-- =============================================================================

INSERT INTO curriculum_topics (id, institution_id, subject_id, topic_name, description, sort_order, total_lessons, education_level, created_at, is_deleted)
VALUES
    (gen_random_uuid(), inst_id, math_id, 'Numbers and Operations', 'Counting, place value, addition, subtraction, multiplication, and division', 1, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, math_id, 'Shapes and Space', '2D and 3D shapes, symmetry, position, and direction', 2, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, math_id, 'Measurement', 'Length, weight, capacity, time, and money', 3, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, math_id, 'Data Handling', 'Collecting, organizing, representing, and interpreting data', 4, 0, 'PRIMARY', NOW(), false);

-- =============================================================================
-- CURRICULUM TOPICS - English Language
-- =============================================================================

INSERT INTO curriculum_topics (id, institution_id, subject_id, topic_name, description, sort_order, total_lessons, education_level, created_at, is_deleted)
VALUES
    (gen_random_uuid(), inst_id, eng_id, 'Reading and Phonics', 'Letter sounds, blending, segmenting, and reading comprehension', 1, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, eng_id, 'Writing Skills', 'Letter formation, sentences, paragraphs, and creative writing', 2, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, eng_id, 'Grammar', 'Parts of speech, tenses, punctuation, and sentence structure', 3, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, eng_id, 'Speaking and Listening', 'Oral communication, storytelling, presentations, and active listening', 4, 0, 'PRIMARY', NOW(), false);

-- =============================================================================
-- CURRICULUM TOPICS - Kiswahili Language
-- =============================================================================

INSERT INTO curriculum_topics (id, institution_id, subject_id, topic_name, description, sort_order, total_lessons, education_level, created_at, is_deleted)
VALUES
    (gen_random_uuid(), inst_id, kis_id, 'Usomaji', 'Kusoma na kuelewa maandishi kwa Kiswahili', 1, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, kis_id, 'Uandishi', 'Kuandika herufi, maneno, na sentensi kwa Kiswahili', 2, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, kis_id, 'Sarufi', 'Viungo, aina za maneno, na muundo wa sentensi', 3, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, kis_id, 'Kuzungumza na Kusikiliza', 'Mawasiliano ya shauri, hadithi, na uwasilishaji', 4, 0, 'PRIMARY', NOW(), false);

-- =============================================================================
-- CURRICULUM TOPICS - Science and Technology
-- =============================================================================

INSERT INTO curriculum_topics (id, institution_id, subject_id, topic_name, description, sort_order, total_lessons, education_level, created_at, is_deleted)
VALUES
    (gen_random_uuid(), inst_id, sci_id, 'Living Things', 'Plants, animals, habitats, and life processes', 1, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, sci_id, 'Materials and Properties', 'Solids, liquids, gases, and material characteristics', 2, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, sci_id, 'Forces and Energy', 'Pushes, pulls, magnets, light, sound, and heat', 3, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, sci_id, 'Earth and Space', 'Rocks, weather, solar system, and environmental science', 4, 0, 'PRIMARY', NOW(), false);

-- =============================================================================
-- CURRICULUM TOPICS - Social Studies
-- =============================================================================

INSERT INTO curriculum_topics (id, institution_id, subject_id, topic_name, description, sort_order, total_lessons, education_level, created_at, is_deleted)
VALUES
    (gen_random_uuid(), inst_id, soc_id, 'My Family and I', 'Family structures, roles, relationships, and personal identity', 1, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, soc_id, 'My Community', 'Local community, leaders, services, and civic responsibility', 2, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, soc_id, 'Tanzania', 'History, geography, culture, governance, and national identity', 3, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, soc_id, 'Africa and the World', 'African continents, global connections, and cultural diversity', 4, 0, 'PRIMARY', NOW(), false);

-- =============================================================================
-- CURRICULUM TOPICS - Moral and Religious Education
-- =============================================================================

INSERT INTO curriculum_topics (id, institution_id, subject_id, topic_name, description, sort_order, total_lessons, education_level, created_at, is_deleted)
VALUES
    (gen_random_uuid(), inst_id, mor_id, 'Values and Ethics', 'Honesty, respect, responsibility, and moral decision-making', 1, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, mor_id, 'Cultural Practices', 'Traditions, customs, ceremonies, and cultural heritage', 2, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, mor_id, 'Environmental Care', 'Conservation, sustainability, and caring for nature', 3, 0, 'PRIMARY', NOW(), false);

-- =============================================================================
-- CURRICULUM TOPICS - Physical Education
-- =============================================================================

INSERT INTO curriculum_topics (id, institution_id, subject_id, topic_name, description, sort_order, total_lessons, education_level, created_at, is_deleted)
VALUES
    (gen_random_uuid(), inst_id, pe_id, 'Movement and Fitness', 'Locomotor skills, coordination, flexibility, and exercise', 1, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, pe_id, 'Games and Sports', 'Team games, individual sports, rules, and fair play', 2, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, pe_id, 'Health and Safety', 'Personal hygiene, nutrition, safety rules, and wellness', 3, 0, 'PRIMARY', NOW(), false);

-- =============================================================================
-- CURRICULUM TOPICS - Creative Arts
-- =============================================================================

INSERT INTO curriculum_topics (id, institution_id, subject_id, topic_name, description, sort_order, total_lessons, education_level, created_at, is_deleted)
VALUES
    (gen_random_uuid(), inst_id, art_id, 'Drawing and Painting', 'Lines, shapes, colours, shading, and art techniques', 1, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, art_id, 'Music', 'Rhythm, melody, singing, and instrument exploration', 2, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, art_id, 'Dance and Drama', 'Movement expression, role play, performance, and storytelling', 3, 0, 'PRIMARY', NOW(), false),
    (gen_random_uuid(), inst_id, art_id, 'Crafts', 'Paper, fabric, clay, and recycled material projects', 4, 0, 'PRIMARY', NOW(), false);

END $$;
