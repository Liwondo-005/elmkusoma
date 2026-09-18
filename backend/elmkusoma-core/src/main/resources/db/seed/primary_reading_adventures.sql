-- ============================================================
-- PRIMARY READING ADVENTURES SEED DATA
-- Sample reading adventures for Primary students (ages 6-12)
-- Apply via: psql -U postgres -d elmkusoma -f primary_reading_adventures.sql
-- NOTE: Update student_id and institution_id to match your data
-- ============================================================

DO $$
DECLARE
    inst_id UUID := 'fbd2e3e3-99df-48f3-b138-58d6f6f84103';
    student_id UUID := 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
BEGIN

-- Story 1: The Magic of Mount Kilimanjaro
INSERT INTO reading_adventures (id, student_id, institution_id, title, content, subject_name, reading_level, word_count, read_time_minutes, times_read, is_favorite, cover_color, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'The Magic of Mount Kilimanjaro',
    'Amani had always dreamed of seeing the snow on top of Mount Kilimanjaro. One sunny morning, her grandmother told her the story of the mountain.

''Did you know,'' her grandmother said, ''that Kilimanjaro is the tallest mountain in Africa? It stands over 5,800 metres high, with three peaks: Kibo, Mawenzi, and Shira.'' Amani listened carefully.

''The mountain is special because it has snow at the top, even though it is near the equator,'' her grandmother continued. ''But the snow is getting smaller because of climate change.'' Amani felt sad.

That afternoon, Amani visited her school library and found a book about Kilimanjaro. She learned that the mountain has five climate zones: rainforest, heath, moorland, alpine desert, and the icy summit called the arctic zone.

''I want to help protect the mountain,'' Amani told her friends at school. Together, they started a tree-planting project. They planted ten mango seedlings in the school compound.

''Trees help the environment,'' Amani explained to her classmates. ''They absorb carbon dioxide and produce oxygen. If we plant enough trees, maybe we can help keep the snow on Kilimanjaro.''

Her teacher, Mr. Ochieng, was very proud. ''You are all young environmentalists,'' he said with a smile. ''Kilimanjaro is a treasure for Tanzania and the whole world.'' Amani looked at the mountain poster on the classroom wall and made a promise to herself: one day, she would climb to the top and see the snow with her own eyes.',
    'Science and Technology',
    'INTERMEDIATE',
    285,
    3,
    0,
    false,
    '#4CAF50',
    NOW(),
    false
);

-- Story 2: Amani and the Baobab Tree
INSERT INTO reading_adventures (id, student_id, institution_id, title, content, subject_name, reading_level, word_count, read_time_minutes, times_read, is_favorite, cover_color, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'Amani and the Baobab Tree',
    'Behind Amani''s house stood a giant baobab tree. Her grandmother said it was over 500 years old. The trunk was so wide that five children holding hands could not wrap around it.

One day, Amani noticed workers near the tree with saws. She ran to her grandmother. ''They are going to cut down the baobab!'' she cried.

Her grandmother walked to the workers calmly. ''This tree is important to our community,'' she said. ''It provides shade, fruit, and medicine. The baobab fruit is rich in vitamin C, and the bark can be used to make rope.''

The workers explained that the road needed to be wider. Amani had an idea. ''What if we move the road a little to the left?'' she suggested, pointing to the empty field nearby.

The workers thought about it and agreed. They would save the ancient tree. The whole village celebrated that evening. People danced and sang songs about the baobab.

Amani learned an important lesson that day. ''When communities work together, we can solve problems and protect nature,'' she told her friend Juma the next morning.

Juma nodded. ''My grandfather says the baobab is called the Tree of Life because so many creatures depend on it — birds nest in its branches, insects live in its bark, and elephants eat its fruit.'' Amani smiled. She loved learning about the wonders of nature right in her own village.',
    'Science and Technology',
    'BEGINNER',
    252,
    3,
    0,
    false,
    '#8D6E63',
    NOW(),
    false
);

-- Story 3: The Math Detective
INSERT INTO reading_adventures (id, student_id, institution_id, title, content, subject_name, reading_level, word_count, read_time_minutes, times_read, is_favorite, cover_color, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'The Math Detective',
    'Zara loved numbers. Her classmates called her the Math Detective because she could solve any puzzle with maths.

One Monday, her teacher brought a challenge. ''There are 24 students in our class,'' she said. ''If I divide you into groups of 6, how many groups will there be?'' Zara raised her hand immediately.

''Six times four equals 24, so there will be four groups,'' Zara answered proudly. Her teacher smiled.

On Tuesday, the challenge was harder. ''The school is buying 35 desks. Each desk costs 45,000 shillings. What is the total cost?'' The class went quiet.

Zara took out her pencil and wrote carefully: 35 times 45. She broke it down: 35 times 40 equals 1,400 and 35 times 5 equals 175. She added them: 1,575. Then she added the zeros back: 1,575,000 shillings.

''The answer is 1,575,000 shillings!'' Zara announced. Her classmates clapped.

On Wednesday, something strange happened. The school had exactly 1,575,000 shillings in the fund! The teacher looked at Zara with amazement. ''How did you know that?''

Zara grinned. ''I saw the accounting book on the desk yesterday. I was just curious about the numbers!'' Everyone laughed.

That evening, Zara told her mother about the week. ''Maths is everywhere, Mama. In the market, in the farm, everywhere!'' Her mother nodded. ''Keep using your maths brain, Zara. It will take you far.'' Zara dreamed of becoming an accountant one day.',
    'Mathematics',
    'INTERMEDIATE',
    278,
    3,
    0,
    false,
    '#2196F3',
    NOW(),
    false
);

-- Story 4: The Science of Rain
INSERT INTO reading_adventures (id, student_id, institution_id, title, content, subject_name, reading_level, word_count, read_time_minutes, times_read, is_favorite, cover_color, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'The Science of Rain',
    'When Kofi was five, he used to think that rain came from the clouds being full of water, like a sponge being squeezed. Now he was in Class Four, and he knew the real science.

It started during a science lesson about the water cycle. Ms. Njeri drew a big diagram on the board. ''Water evaporates from oceans, lakes, and rivers when the sun heats it,'' she explained. ''The water rises into the sky as invisible water vapour.''

She pointed to the top of her diagram. ''Up high, it gets very cold. The water vapour turns into tiny water droplets or ice crystals. These join together to form clouds.'' Kofi wrote everything in his notebook.

''When millions of tiny droplets join together, they become too heavy to stay in the air,'' Ms. Njeri continued. ''They fall back to earth as rain. This is called precipitation.'' Kofi raised his hand.

''Does all rain fall on the ground?'' he asked. Ms. Njeri shook her head. ''Some evaporates before it reaches the ground. Some is caught by trees and plants. Some flows into rivers and streams, and eventually back to the ocean. Then the cycle starts again!''

That afternoon, it rained. Kofi stood on the school veranda and watched the drops fall. He could see the whole water cycle happening in front of him. He smiled, knowing exactly where each raindrop had been and where it would go next.',
    'Science and Technology',
    'INTERMEDIATE',
    253,
    3,
    0,
    false,
    '#00BCD4',
    NOW(),
    false
);

-- Story 5: Jambo! Learning Kiswahili
INSERT INTO reading_adventures (id, student_id, institution_id, title, content, subject_name, reading_level, word_count, read_time_minutes, times_read, is_favorite, cover_color, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'Jambo! Learning Kiswahili',
    'When Amina moved from Nairobi to Dar es Salaam, she spoke English but not Kiswahili. Her new classmates wanted to be her friends, but language was a barrier.

Her teacher, Mr. Mwaipopo, noticed Amina struggling. He sat with her during break time. ''Let me teach you some Kiswahili phrases,'' he said kindly.

He started with greetings. ''You say Habari to ask how someone is. They answer Nzuri, meaning good. For hello, you say Jambo, and the reply is Jambo too!'' Amina repeated each word carefully.

The next day, Amina tried. ''Habari?'' she said to her deskmate, Fatuma. Fatuma''s face lit up. ''Nzuri!'' she replied, smiling broadly. The other students started teaching Amina more words during lunch.

By the end of the month, Amina could count to one hundred in Kiswahili, greet everyone, and even tell a short story. She learned that moja is one, mbili is two, tatu is three, and so on.

Her favourite Kiswahili word was furaha, which means happiness. ''Kiswahili is such a beautiful language,'' she told her mother over the phone. ''It connects me to so many people.''

Mr. Mwaipopo was proud of Amina. ''Language is a bridge,'' he told the class. ''When we learn each other''s languages, we understand each other better.'' Amina nodded. She had found more than a new language — she had found new friends and a second home.',
    'Kiswahili Language',
    'BEGINNER',
    271,
    3,
    0,
    false,
    '#FF9800',
    NOW(),
    false
);

-- Story 6: The Animal Kingdom Safari
INSERT INTO reading_adventures (id, student_id, institution_id, title, content, subject_name, reading_level, word_count, read_time_minutes, times_read, is_favorite, cover_color, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'The Animal Kingdom Safari',
    'Uncle Rashid was a wildlife guide in the Serengeti. When Baraka visited for the holidays, he took him on a real safari.

The sun was just rising when they drove into the park. Almost immediately, Baraka saw a group of elephants walking in a line. ''An elephant never walks alone,'' Uncle Rashid said. ''They live in family groups led by the oldest female, called the matriarch.''

Further along, they spotted a family of lions resting under an acacia tree. Baraka counted five lions. ''That is a pride,'' Uncle Rashid explained. ''The male lion with the mane protects the family, while the lionesses hunt for food.''

Near a waterhole, they saw something amazing. A huge crocodile lay perfectly still on the bank. ''Crocodiles are ancient animals,'' Uncle Rashid whispered. ''They have been on earth for over 200 million years, even before the dinosaurs disappeared.''

Baraka was amazed. ''Why do so many animals live in the Serengeti?'' he asked. Uncle Rashid smiled. ''Because the Serengeti is one of the most important ecosystems in the world. It has grasslands, rivers, and woodlands. The great migration, where over a million wildebeest move across the plains, is one of nature''s greatest spectacles.''

That night, lying in his tent, Baraka looked up at the stars. ''I want to be a wildlife scientist when I grow up,'' he said. Uncle Rashid nodded. ''Then you will help protect these amazing animals for future generations.''',
    'Science and Technology',
    'ADVANCED',
    284,
    4,
    0,
    false,
    '#4CAF50',
    NOW(),
    false
);

-- Story 7: Building a Bridge
INSERT INTO reading_adventures (id, student_id, institution_id, title, content, subject_name, reading_level, word_count, read_time_minutes, times_read, is_favorite, cover_color, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'Building a Bridge',
    'The rainy season had destroyed the wooden bridge over the Mtera River. Now the children of Mtera village had to walk five extra kilometres to reach school.

Abel, a Class Five student, was determined to solve this problem. He had been learning about simple machines and structures in science class. His teacher, Ms. Kimaro, had shown them that triangles are the strongest shape in engineering.

Abel drew a plan. He would use locally available materials: strong hardwood poles, metal wire, and nails. He asked his father and three neighbours to help. Over two weekends, they built a new bridge.

The design was clever. Abel used a triangular truss system, just like Ms. Kimaro had taught. The triangular supports distributed the weight evenly, making the bridge very strong. It was three metres long and wide enough for two people to walk side by side.

When the bridge was finished, the whole village came to see it. The village elder, Mama Nuru, said, ''This young boy has used his education to help the community. Education is truly powerful.''

Abel was proud, but he knew the bridge was not enough. He also wrote a letter to the district council, asking them to build a stronger concrete bridge for vehicles. The council wrote back, saying they would include it in next year''s budget.

Abel learned that solving problems requires both knowledge and action. ''You can have the best ideas in the world,'' he told his classmates, ''but you also need the courage to put them into practice.''',
    'Science and Technology',
    'ADVANCED',
    276,
    4,
    0,
    false,
    '#795548',
    NOW(),
    false
);

-- Story 8: The Stars Above Serengeti
INSERT INTO reading_adventures (id, student_id, institution_id, title, content, subject_name, reading_level, word_count, read_time_minutes, times_read, is_favorite, cover_color, created_at, is_deleted)
VALUES (
    gen_random_uuid(),
    student_id,
    inst_id,
    'The Stars Above Serengeti',
    'Nuru loved lying on the grass at night and looking at the stars. Her village in the Serengeti had very little light pollution, so the sky was always clear and bright.

One evening, her grandfather sat beside her. ''Do you know why we call that pattern the Big Dipper?'' he asked, pointing to seven bright stars. Nuru shook her head.

''The Big Dipper is part of a larger group called Ursa Major, the Great Bear,'' her grandfather explained. ''Ancient people used these stars to find north when they travelled at night. The two stars at the end of the Dipper always point to the North Star, Polaris.''

Nuru searched the sky and found the two bright stars. If she followed an invisible line from them, there was the North Star! ''That is amazing,'' she whispered.

Her grandfather continued. ''Our ancestors in Tanzania also used the stars for farming. When the Pleiades cluster appeared in the eastern sky in June, they knew the planting season was coming. The rising of Sirius told them the rains would begin soon.''

Nuru was amazed. ''Stars can help us know when to plant?'' Her grandfather smiled. ''The stars are like a calendar in the sky. Our forefathers were great astronomers, even without telescopes.''

The next day at school, Nuru told her teacher about what she had learned. Her teacher was so impressed that she organised a stargazing night for the whole class. They used simple cardboard star charts that Nuru helped make.

That night, under the vast African sky, thirty children lay on blankets and explored the universe. Nuru knew that one day, she would study astronomy and uncover even more secrets hidden in the stars.',
    'Science and Technology',
    'ADVANCED',
    298,
    4,
    0,
    false,
    '#1A237E',
    NOW(),
    false
);

END $$;
