-- ============================================================
-- ELMKUSOMA LABS SEED DATA
-- Science labs for Primary students (ages 6-12)
-- Apply via: psql -U postgres -d elmkusoma -f primary_labs_seed.sql
-- NOTE: Update student_id and institution_id to match your data
-- ============================================================

DO $$
DECLARE
    inst_id UUID := 'fbd2e3e3-99df-48f3-b138-58d6f6f84103';
    student_id UUID := 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
BEGIN

-- Lab 1: Why do plants need sunlight? (NATURE)
INSERT INTO elmkusoma_labs (id, student_id, institution_id, lab_title, lab_type, hypothesis, materials_list, steps, expected_result, student_notes, is_attempted, score, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'Why do plants need sunlight?',
    'NATURE',
    'Plants need sunlight to make food and grow. Without sunlight, plants will not be able to produce energy through photosynthesis and will eventually wither and die.',
    '["2 small pots", "Soil", "2 bean seeds", "Water", "A windowsill with sunlight", "A dark cupboard", "Ruler"]',
    '["Fill both pots with the same amount of soil", "Plant one bean seed in each pot", "Place one pot on the sunny windowsill and the other in the dark cupboard", "Water both pots equally every day", "Measure and record the height of both plants every 3 days for 2 weeks", "Compare the growth of both plants"]',
    'The plant on the windowsill will grow tall, green, and healthy because it gets sunlight for photosynthesis. The plant in the dark cupboard will be pale, thin, and weak because it cannot make enough food without light.',
    '',
    false,
    NULL,
    NOW(),
    false
);

-- Lab 2: What makes ice melt faster? (MATERIALS)
INSERT INTO elmkusoma_labs (id, student_id, institution_id, lab_title, lab_type, hypothesis, materials_list, steps, expected_result, student_notes, is_attempted, score, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'What makes ice melt faster?',
    'MATERIALS',
    'Ice will melt faster when exposed to heat, salt, or warm water compared to being left in room temperature air. Salt lowers the freezing point of water, making ice melt quicker.',
    '["4 ice cubes of the same size", "A plate", "Table salt", "A cup of warm water", "A cup of cold water", "A timer or clock", "A notebook"]',
    '["Place one ice cube on a dry plate and record the time it takes to melt", "Place one ice cube on a plate and sprinkle salt on it, record the time", "Place one ice cube in a cup of cold water, record the time", "Place one ice cube in a cup of warm water, record the time", "Write down all your results in a table", "Compare which ice cube melted the fastest and slowest"]',
    'The ice cube in warm water will melt fastest. The ice cube with salt will melt second fastest. The ice cube in cold water will melt third. The ice cube on the dry plate will melt the slowest.',
    '',
    false,
    NULL,
    NOW(),
    false
);

-- Lab 3: How do magnets work? (FORCES)
INSERT INTO elmkusoma_labs (id, student_id, institution_id, lab_title, lab_type, hypothesis, materials_list, steps, expected_result, student_notes, is_attempted, score, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'How do magnets work?',
    'FORCES',
    'Magnets attract certain metals like iron and steel but not others like wood or plastic. The strength of a magnet depends on how close the object is and the size of the magnet.',
    '["2 bar magnets", "Iron nails (10)", "Paper clips (10)", "Wooden blocks (3)", "Plastic pieces (3)", "Aluminum foil", "A ruler", "A notebook"]',
    '["Test each material with the magnet and record whether it is attracted", "Arrange the attracted items by how strongly the magnet pulls them", "Measure how far from the magnet you can hold it and still pick up a paper clip", "Try using two magnets together to pick up nails and compare strength", "Record all results in your notebook", "Write a conclusion about which materials are magnetic"]',
    'The magnet will attract iron nails and steel paper clips strongly. It will not attract wood, plastic, or aluminum foil. Two magnets together will be stronger than one magnet alone.',
    '',
    false,
    NULL,
    NOW(),
    false
);

-- Lab 4: Where do birds sleep? (LIVING_THINGS)
INSERT INTO elmkusoma_labs (id, student_id, institution_id, lab_title, lab_type, hypothesis, materials_list, steps, expected_result, student_notes, is_attempted, score, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'Where do birds sleep?',
    'LIVING_THINGS',
    'Birds sleep in safe, sheltered places like tree branches, nests, or dense bushes to protect themselves from predators and bad weather. Different birds choose different sleeping spots.',
    '["Notebook", "Pencil", "Binoculars (if available)", "A bird identification book or phone app", "A camera or phone for photos", "A comfortable spot to sit quietly"]',
    '["Find a quiet spot in your garden or a nearby park early in the morning", "Watch and wait quietly for 20 minutes before sunrise", "Observe where birds are coming from as they wake up", "Look for nests, dense branches, and sheltered spots", "Sketch or photograph the sleeping locations you find", "Identify at least 3 different bird species and their sleeping spots", "Write a report about your findings"]',
    'You will find that birds sleep in different places depending on the species. Small birds like sparrows sleep in dense bushes, larger birds like pigeons sleep on tree branches, and some birds like weavers sleep in their woven nests. Birds choose spots that protect them from predators and rain.',
    '',
    false,
    NULL,
    NOW(),
    false
);

-- Lab 5: What happens when you mix colors? (MATERIALS)
INSERT INTO elmkusoma_labs (id, student_id, institution_id, lab_title, lab_type, hypothesis, materials_list, steps, expected_result, student_notes, is_attempted, score, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'What happens when you mix colors?',
    'MATERIALS',
    'Mixing two primary colors will create a new secondary color. For example, red and blue make purple, red and yellow make orange, and blue and yellow make green.',
    '["Red paint", "Blue paint", "Yellow paint", "White paper (6 sheets)", "Paintbrushes (3)", "Water cups (3)", "Paper towels", "A palette or mixing plate", "A notebook"]',
    '["Label each sheet of paper with a color combination", "Paint a red circle and a blue circle on the first sheet, then mix them on the palette", "Paint a red circle and a yellow circle on the second sheet, then mix them", "Paint a blue circle and a yellow circle on the third sheet, then mix them", "Record what new colors you created", "Try mixing three colors together and record the result", "Create a color mixing chart with all your discoveries"]',
    'Mixing red and blue will create purple. Mixing red and yellow will create orange. Mixing blue and yellow will create green. These are called secondary colors. Mixing all three primary colors together creates a brownish or dark color.',
    '',
    false,
    NULL,
    NOW(),
    false
);

-- Lab 6: How fast does sound travel? (FORCES)
INSERT INTO elmkusoma_labs (id, student_id, institution_id, lab_title, lab_type, hypothesis, materials_list, steps, expected_result, student_notes, is_attempted, score, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'How fast does sound travel?',
    'FORCES',
    'Sound travels faster through solids than through liquids, and faster through liquids than through gases. Sound needs a medium to travel and cannot travel in a vacuum.',
    '["A long metal pipe or ruler", "A wooden table", "A cup of water", "A mobile phone with a stopwatch", "A friend to help", "A notebook", "Earplugs (optional)"]',
    '["Have your friend tap the far end of the metal pipe while you listen at the other end, measure the time", "Have your friend tap the wooden table at one end while you listen at the other end, measure the time", "Have your friend tap the side of the water cup while you listen at the other side, measure the time", "Have your friend clap their hands 10 meters away while you listen, measure the time", "Record all times in a table", "Compare which material carried sound the fastest"]',
    'Sound will travel fastest through the metal pipe, then through the wooden table, then through water, and slowest through air. This is because sound waves move faster through denser materials where particles are closer together.',
    '',
    false,
    NULL,
    NOW(),
    false
);

END $$;
