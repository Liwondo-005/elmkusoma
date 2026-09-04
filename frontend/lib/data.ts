export type LiveStatus = "live" | "soon" | "tomorrow" | "scheduled"

export type LiveClass = {
  id: string
  title: string
  subtitle: string
  instructor: string
  image: string
  status: LiveStatus
  badge: string
  watching?: number
  going?: number
  time: string
  level: "Beginner" | "Intermediate" | "Advanced"
}

export const liveClasses: LiveClass[] = [
  {
    id: "advanced-mathematics-calculus-ii",
    title: "Advanced Mathematics",
    subtitle: "Calculus II",
    instructor: "Dr. John Mwangi",
    image: "/images/class-math.png",
    status: "live",
    badge: "LIVE NOW",
    watching: 128,
    time: "In progress",
    level: "Advanced",
  },
  {
    id: "data-science-with-python",
    title: "Data Science with Python",
    subtitle: "for Beginners",
    instructor: "Grace N.",
    image: "/images/class-datascience.png",
    status: "soon",
    badge: "STARTS IN 30 MIN",
    going: 86,
    time: "Today, 4:00 PM",
    level: "Beginner",
  },
  {
    id: "web-development-full-stack",
    title: "Web Development",
    subtitle: "Full Stack Basics",
    instructor: "David O.",
    image: "/images/class-webdev.png",
    status: "tomorrow",
    badge: "STARTS TOMORROW",
    going: 120,
    time: "Tomorrow, 10:00 AM",
    level: "Intermediate",
  },
  {
    id: "digital-marketing-strategy",
    title: "Digital Marketing",
    subtitle: "Strategy Masterclass",
    instructor: "Sarah K.",
    image: "/images/class-marketing.png",
    status: "scheduled",
    badge: "STARTS FRIDAY",
    going: 75,
    time: "Fri, 12:00 PM",
    level: "Intermediate",
  },
  {
    id: "physics-mechanics",
    title: "Physics Mechanics",
    subtitle: "Problem Solving",
    instructor: "Prof. A. Hamdan",
    image: "/images/class-physics.png",
    status: "scheduled",
    badge: "UPCOMING",
    going: 54,
    time: "Tomorrow, 10:00 AM",
    level: "Intermediate",
  },
]

export type Course = {
  id: string
  title: string
  instructor: string
  image: string
  rating: number
  reviews: string
  tag: string
}

export const featuredCourses: Course[] = [
  {
    id: "web-development-bootcamp",
    title: "Web Development Bootcamp",
    instructor: "David O.",
    image: "/images/class-webdev.png",
    rating: 4.8,
    reviews: "12.4k",
    tag: "Popular Course",
  },
  {
    id: "data-science-python",
    title: "Data Science with Python",
    instructor: "Grace N.",
    image: "/images/class-datascience.png",
    rating: 4.9,
    reviews: "8.1k",
    tag: "Trending",
  },
  {
    id: "digital-marketing",
    title: "Digital Marketing Strategy",
    instructor: "Sarah K.",
    image: "/images/class-marketing.png",
    rating: 4.7,
    reviews: "5.6k",
    tag: "New",
  },
]

export const levels = [
  { name: "Nursery", desc: "Baby, Middle & Top levels", href: "/courses/nursery" },
  { name: "Primary", desc: "Standard 1 — 6 curriculum", href: "/courses/primary" },
  { name: "Lower Secondary", desc: "Form I — IV, O-Level", href: "/courses/lower-secondary" },
  { name: "Advanced Secondary", desc: "Form V — VI, A-Level", href: "/courses/advanced-secondary" },
  { name: "Vocational (VETA)", desc: "Trade skills & modules", href: "/courses/veta" },
  { name: "Colleges & Universities", desc: "Degree & diploma programs", href: "/courses/colleges-universities" },
]

export const stats = [
  { value: "8,500+", label: "Active Learners" },
  { value: "500+", label: "Expert Instructors" },
  { value: "1,200+", label: "Courses & Classes" },
  { value: "50+", label: "Partner Institutions" },
]

export type EducationLevel = "Nursery" | "Primary" | "Lower Secondary" | "Advanced Secondary"

export type SubjectCategory =
  | "Core"
  | "Social Science"
  | "Language and Literature"
  | "Natural Science"
  | "Mathematics"
  | "Technology"
  | "Business and Economics"
  | "Culture, Arts and Sports"

export type CurriculumSubject = {
  id: string
  name: string
  level: EducationLevel
  className: string
  category: SubjectCategory
  description: string
  enrolled: number
}

export type CourseLevel = {
  name: string
  slug: string
  description: string
  icon: string
  subjectCount: number
  comingSoon?: boolean
}

export const courseLevels: CourseLevel[] = [
  { name: "Nursery", slug: "nursery", description: "Early literacy and numeracy through play — Reading, Writing, and Counting for ages 2-5.", icon: "baby", subjectCount: 9 },
  { name: "Primary", slug: "primary", description: "Standard I to VI — core subjects including Kiswahili, English, Mathematics, and Science.", icon: "school", subjectCount: 44 },
  { name: "Lower Secondary", slug: "lower-secondary", description: "Form I to IV — comprehensive O-Level curriculum with 16+ subjects per form.", icon: "book-open", subjectCount: 64 },
  { name: "Advanced Secondary", slug: "advanced-secondary", description: "Form V to VI — specialized A-Level studies across 7 curriculum areas.", icon: "graduation-cap", subjectCount: 58 },
  { name: "VETA", slug: "veta", description: "Vocational and technical skills training — explore trades, programmes and training modules.", icon: "wrench", subjectCount: 8 },
  { name: "Colleges & Universities", slug: "colleges-universities", description: "Degree programs, diplomas, and professional courses from partner institutions.", icon: "university", subjectCount: 3 },
]

export function getCourseLevelBySlug(slug: string): CourseLevel | undefined {
  return courseLevels.find((l) => l.slug === slug)
}

export function getSubjectsByLevel(level: EducationLevel): CurriculumSubject[] {
  return curriculumSubjects.filter((s) => s.level === level)
}

export function getSubjectsByLevelAndClass(level: EducationLevel, className: string): CurriculumSubject[] {
  return curriculumSubjects.filter((s) => s.level === level && s.className === className)
}

export function levelSlugToEducationLevel(slug: string): EducationLevel | undefined {
  const map: Record<string, EducationLevel> = {
    nursery: "Nursery",
    primary: "Primary",
    "lower-secondary": "Lower Secondary",
    "advanced-secondary": "Advanced Secondary",
  }
  return map[slug]
}

export function educationLevelToSlug(level: EducationLevel): string {
  const map: Record<EducationLevel, string> = {
    Nursery: "nursery",
    Primary: "primary",
    "Lower Secondary": "lower-secondary",
    "Advanced Secondary": "advanced-secondary",
  }
  return map[level]
}

export function classNameToSlug(className: string): string {
  return className.toLowerCase().replace(/\s+/g, "-")
}

export function slugToClassName(slug: string, level: EducationLevel): string | undefined {
  const classes = classesByLevel[level]
  return classes?.find((c) => classNameToSlug(c) === slug)
}

export const educationLevels: EducationLevel[] = [
  "Nursery",
  "Primary",
  "Lower Secondary",
  "Advanced Secondary",
]

export const classesByLevel: Record<EducationLevel, string[]> = {
  Nursery: ["Baby", "Middle", "Top"],
  Primary: ["Standard 1", "Standard 2", "Standard 3", "Standard 4", "Standard 5", "Standard 6"],
  "Lower Secondary": ["Form I", "Form II", "Form III", "Form IV"],
  "Advanced Secondary": ["Form V", "Form VI"],
}

export const categoriesByLevel: Record<EducationLevel, SubjectCategory[]> = {
  Nursery: ["Core"],
  Primary: ["Core"],
  "Lower Secondary": ["Core"],
  "Advanced Secondary": [
    "Social Science",
    "Language and Literature",
    "Natural Science",
    "Mathematics",
    "Technology",
    "Business and Economics",
    "Culture, Arts and Sports",
  ],
}

export const curriculumSubjects: CurriculumSubject[] = [
  { id: "nursery-baby1-reading", name: "Reading", level: "Nursery", className: "Baby", category: "Core", description: "Introduction to reading skills through phonics, letter recognition, and early literacy activities.", enrolled: 320 },
  { id: "nursery-baby1-writing", name: "Writing", level: "Nursery", className: "Baby", category: "Core", description: "Early writing practice including letter tracing, stroke patterns, and basic pen control.", enrolled: 310 },
  { id: "nursery-baby1-counting", name: "Counting", level: "Nursery", className: "Baby", category: "Core", description: "Introduction to numbers, counting, and basic arithmetic through playful activities.", enrolled: 315 },
  { id: "nursery-baby2-reading", name: "Reading", level: "Nursery", className: "Middle", category: "Core", description: "Building reading fluency with simple words, sentences, and early comprehension.", enrolled: 295 },
  { id: "nursery-baby2-writing", name: "Writing", level: "Nursery", className: "Middle", category: "Core", description: "Writing simple words and short sentences with increasing independence.", enrolled: 290 },
  { id: "nursery-baby2-counting", name: "Counting", level: "Nursery", className: "Middle", category: "Core", description: "Counting to 20, number recognition, and simple addition and subtraction.", enrolled: 288 },
  { id: "nursery-baby3-reading", name: "Reading", level: "Nursery", className: "Top", category: "Core", description: "Reading short stories, comprehension exercises, and vocabulary building.", enrolled: 280 },
  { id: "nursery-baby3-writing", name: "Writing", level: "Nursery", className: "Top", category: "Core", description: "Writing paragraphs, creative writing, and proper sentence construction.", enrolled: 275 },
  { id: "nursery-baby3-counting", name: "Counting", level: "Nursery", className: "Top", category: "Core", description: "Number operations, patterns, and introduction to shapes and measurements.", enrolled: 278 },

  { id: "primary-s1-reading", name: "Reading & Writing", level: "Primary", className: "Standard 1", category: "Core", description: "Reading and writing fundamentals including phonics and early composition.", enrolled: 450 },
  { id: "primary-s1-math", name: "Mathematics", level: "Primary", className: "Standard 1", category: "Core", description: "Basic mathematics: numbers, addition, subtraction, and simple shapes.", enrolled: 445 },
  { id: "primary-s1-environment", name: "Environmental Knowledge", level: "Primary", className: "Standard 1", category: "Core", description: "Exploring the environment, basic science concepts, and social awareness.", enrolled: 440 },
  { id: "primary-s1-english", name: "English", level: "Primary", className: "Standard 1", category: "Core", description: "Introduction to English: alphabet, basic vocabulary, and simple greetings.", enrolled: 435 },
  { id: "primary-s1-religion", name: "Religious Education", level: "Primary", className: "Standard 1", category: "Core", description: "Religious education covering basic moral values and spiritual awareness.", enrolled: 430 },
  { id: "primary-s1-arts", name: "Arts & Sports", level: "Primary", className: "Standard 1", category: "Core", description: "Arts, crafts, music, and physical activities for creative development.", enrolled: 425 },

  { id: "primary-s2-kiswahili", name: "Kiswahili", level: "Primary", className: "Standard 2", category: "Core", description: "Kiswahili language skills: reading, writing, and oral communication.", enrolled: 440 },
  { id: "primary-s2-math", name: "Mathematics", level: "Primary", className: "Standard 2", category: "Core", description: "Mathematics: multiplication basics, division concepts, and word problems.", enrolled: 435 },
  { id: "primary-s2-english", name: "English", level: "Primary", className: "Standard 2", category: "Core", description: "English language basics: sentences, simple reading, and conversation.", enrolled: 430 },
  { id: "primary-s2-science", name: "Science & Environmental Knowledge", level: "Primary", className: "Standard 2", category: "Core", description: "Science and environmental studies: living things, water, and weather.", enrolled: 425 },
  { id: "primary-s2-religion", name: "Religious Education", level: "Primary", className: "Standard 2", category: "Core", description: "Religious education with emphasis on community values and ethics.", enrolled: 420 },
  { id: "primary-s2-arts", name: "Arts & Sports", level: "Primary", className: "Standard 2", category: "Core", description: "Creative arts, sports, and physical education activities.", enrolled: 418 },

  { id: "primary-s3-kiswahili", name: "Kiswahili", level: "Primary", className: "Standard 3", category: "Core", description: "Intermediate Kiswahili: composition, grammar, and literature appreciation.", enrolled: 430 },
  { id: "primary-s3-english", name: "English", level: "Primary", className: "Standard 3", category: "Core", description: "English reading comprehension, writing skills, and grammar basics.", enrolled: 425 },
  { id: "primary-s3-math", name: "Mathematics", level: "Primary", className: "Standard 3", category: "Core", description: "Mathematics: fractions, decimals, measurement, and geometry introduction.", enrolled: 420 },
  { id: "primary-s3-science", name: "Science", level: "Primary", className: "Standard 3", category: "Core", description: "Science: human body, plants, animals, and basic experiments.", enrolled: 415 },
  { id: "primary-s3-history", name: "History & Ethics", level: "Primary", className: "Standard 3", category: "Core", description: "Tanzanian history, cultural heritage, and moral values.", enrolled: 410 },
  { id: "primary-s3-geography", name: "Geography & Environment", level: "Primary", className: "Standard 3", category: "Core", description: "Geography: maps, climate, natural resources, and environmental conservation.", enrolled: 408 },
  { id: "primary-s3-religion", name: "Religious Education", level: "Primary", className: "Standard 3", category: "Core", description: "Religious studies exploring faith, practice, and community responsibility.", enrolled: 405 },
  { id: "primary-s3-arts", name: "Arts & Sports", level: "Primary", className: "Standard 3", category: "Core", description: "Visual arts, performing arts, and sports activities.", enrolled: 403 },

  { id: "primary-s4-kiswahili", name: "Kiswahili", level: "Primary", className: "Standard 4", category: "Core", description: "Advanced Kiswahili: essay writing, poetry, and oral literature.", enrolled: 425 },
  { id: "primary-s4-english", name: "English", level: "Primary", className: "Standard 4", category: "Core", description: "English language: reading strategies, paragraph writing, and tenses.", enrolled: 420 },
  { id: "primary-s4-math", name: "Mathematics", level: "Primary", className: "Standard 4", category: "Core", description: "Mathematics: percentages, ratios, data handling, and problem solving.", enrolled: 415 },
  { id: "primary-s4-science", name: "Science", level: "Primary", className: "Standard 4", category: "Core", description: "Science: matter, energy, forces, and the solar system.", enrolled: 410 },
  { id: "primary-s4-history", name: "History & Ethics", level: "Primary", className: "Standard 4", category: "Core", description: "Tanzanian history: pre-colonial, colonial, and independence movements.", enrolled: 405 },
  { id: "primary-s4-geography", name: "Geography & Environment", level: "Primary", className: "Standard 4", category: "Core", description: "Geography: rivers, lakes, mountains, and economic activities.", enrolled: 403 },
  { id: "primary-s4-religion", name: "Religious Education", level: "Primary", className: "Standard 4", category: "Core", description: "Religious education: deeper understanding of faith and moral practice.", enrolled: 400 },
  { id: "primary-s4-arts", name: "Arts & Sports", level: "Primary", className: "Standard 4", category: "Core", description: "Creative expression through art, music, dance, and athletics.", enrolled: 398 },

  { id: "primary-s5-kiswahili", name: "Kiswahili", level: "Primary", className: "Standard 5", category: "Core", description: "Kiswahili proficiency: literature analysis, advanced grammar, and composition.", enrolled: 420 },
  { id: "primary-s5-english", name: "English", level: "Primary", className: "Standard 5", category: "Core", description: "English: comprehension skills, narrative writing, and language use.", enrolled: 415 },
  { id: "primary-s5-math", name: "Mathematics", level: "Primary", className: "Standard 5", category: "Core", description: "Mathematics: algebra basics, geometry, measurement, and statistics.", enrolled: 410 },
  { id: "primary-s5-science", name: "Science", level: "Primary", className: "Standard 5", category: "Core", description: "Science: ecosystems, simple machines, health, and nutrition.", enrolled: 405 },
  { id: "primary-s5-history", name: "History & Ethics", level: "Primary", className: "Standard 5", category: "Core", description: "Tanzanian history: governance, democracy, and national development.", enrolled: 400 },
  { id: "primary-s5-geography", name: "Geography & Environment", level: "Primary", className: "Standard 5", category: "Core", description: "Geography: population, urbanization, trade, and environmental issues.", enrolled: 398 },
  { id: "primary-s5-religion", name: "Religious Education", level: "Primary", className: "Standard 5", category: "Core", description: "Religious studies: ethics, social responsibility, and spiritual growth.", enrolled: 395 },
  { id: "primary-s5-arts", name: "Arts & Sports", level: "Primary", className: "Standard 5", category: "Core", description: "Advanced arts, sports training, and creative projects.", enrolled: 393 },

  { id: "primary-s6-kiswahili", name: "Kiswahili", level: "Primary", className: "Standard 6", category: "Core", description: "Kiswahili mastery: literature, advanced composition, and exam preparation.", enrolled: 415 },
  { id: "primary-s6-english", name: "English", level: "Primary", className: "Standard 6", category: "Core", description: "English: advanced reading, essay writing, and exam preparation.", enrolled: 410 },
  { id: "primary-s6-math", name: "Mathematics", level: "Primary", className: "Standard 6", category: "Core", description: "Mathematics: advanced problem solving, geometry, and exam techniques.", enrolled: 405 },
  { id: "primary-s6-science", name: "Science", level: "Primary", className: "Standard 6", category: "Core", description: "Science: revision of key concepts, practical applications, and exam prep.", enrolled: 400 },
  { id: "primary-s6-history", name: "History & Ethics", level: "Primary", className: "Standard 6", category: "Core", description: "Tanzanian history: comprehensive review and national identity.", enrolled: 395 },
  { id: "primary-s6-geography", name: "Geography & Environment", level: "Primary", className: "Standard 6", category: "Core", description: "Geography: map skills, resource management, and global awareness.", enrolled: 393 },
  { id: "primary-s6-religion", name: "Religious Education", level: "Primary", className: "Standard 6", category: "Core", description: "Religious education: values review and spiritual preparation.", enrolled: 390 },
  { id: "primary-s6-arts", name: "Arts & Sports", level: "Primary", className: "Standard 6", category: "Core", description: "Creative arts and sports: showcase projects and competitions.", enrolled: 388 },

  { id: "lower-f1-math", name: "Mathematics", level: "Lower Secondary", className: "Form I", category: "Core", description: "Algebra, geometry, statistics, and problem-solving foundations.", enrolled: 380 },
  { id: "lower-f1-kiswahili", name: "Kiswahili", level: "Lower Secondary", className: "Form I", category: "Core", description: "Kiswahili language: grammar, composition, and literary studies.", enrolled: 375 },
  { id: "lower-f1-english", name: "English Language", level: "Lower Secondary", className: "Form I", category: "Core", description: "English: reading comprehension, writing, and language structure.", enrolled: 370 },
  { id: "lower-f1-biology", name: "Biology", level: "Lower Secondary", className: "Form I", category: "Core", description: "Introduction to life sciences: cells, organisms, and ecosystems.", enrolled: 365 },
  { id: "lower-f1-chemistry", name: "Chemistry", level: "Lower Secondary", className: "Form I", category: "Core", description: "Basic chemistry: elements, compounds, and chemical reactions.", enrolled: 360 },
  { id: "lower-f1-physics", name: "Physics", level: "Lower Secondary", className: "Form I", category: "Core", description: "Fundamentals of physics: forces, energy, and simple machines.", enrolled: 355 },
  { id: "lower-f1-geography", name: "Geography", level: "Lower Secondary", className: "Form I", category: "Core", description: "Physical and human geography: maps, climate, and populations.", enrolled: 350 },
  { id: "lower-f1-history", name: "History", level: "Lower Secondary", className: "Form I", category: "Core", description: "World and Tanzanian history: civilizations and independence.", enrolled: 348 },
  { id: "lower-f1-maadili", name: "History & Ethics", level: "Lower Secondary", className: "Form I", category: "Core", description: "Tanzanian history and moral values: citizenship and ethics.", enrolled: 345 },
  { id: "lower-f1-civics", name: "Civic Education", level: "Lower Secondary", className: "Form I", category: "Core", description: "Rights, responsibilities, governance, and democratic principles.", enrolled: 343 },
  { id: "lower-f1-computer", name: "Computer Science", level: "Lower Secondary", className: "Form I", category: "Core", description: "Introduction to computers, digital literacy, and basic programming.", enrolled: 340 },
  { id: "lower-f1-agriculture", name: "Agriculture", level: "Lower Secondary", className: "Form I", category: "Core", description: "Farming practices, crop science, and agricultural economics.", enrolled: 338 },
  { id: "lower-f1-business", name: "Business Studies", level: "Lower Secondary", className: "Form I", category: "Core", description: "Introduction to business: trade, entrepreneurship, and basic accounting.", enrolled: 335 },
  { id: "lower-f1-bookkeeping", name: "Bookkeeping", level: "Lower Secondary", className: "Form I", category: "Core", description: "Basic bookkeeping: ledgers, balance sheets, and financial records.", enrolled: 333 },
  { id: "lower-f1-nutrition", name: "Food and Nutrition", level: "Lower Secondary", className: "Form I", category: "Core", description: "Nutrition basics: food groups, meal planning, and health.", enrolled: 330 },
  { id: "lower-f1-textile", name: "Textile and Garment Construction", level: "Lower Secondary", className: "Form I", category: "Core", description: "Fabric types, sewing techniques, and garment design basics.", enrolled: 328 },

  { id: "lower-f2-math", name: "Mathematics", level: "Lower Secondary", className: "Form II", category: "Core", description: "Intermediate algebra, trigonometry basics, and data analysis.", enrolled: 375 },
  { id: "lower-f2-kiswahili", name: "Kiswahili", level: "Lower Secondary", className: "Form II", category: "Core", description: "Kiswahili: advanced grammar, essay writing, and oral literature.", enrolled: 370 },
  { id: "lower-f2-english", name: "English Language", level: "Lower Secondary", className: "Form II", category: "Core", description: "English: grammar mastery, creative writing, and comprehension.", enrolled: 365 },
  { id: "lower-f2-biology", name: "Biology", level: "Lower Secondary", className: "Form II", category: "Core", description: "Human biology, plant science, and ecological relationships.", enrolled: 360 },
  { id: "lower-f2-chemistry", name: "Chemistry", level: "Lower Secondary", className: "Form II", category: "Core", description: "Chemical bonding, reactions, acids, bases, and salts.", enrolled: 355 },
  { id: "lower-f2-physics", name: "Physics", level: "Lower Secondary", className: "Form II", category: "Core", description: "Motion, electricity, magnetism, and wave phenomena.", enrolled: 350 },
  { id: "lower-f2-geography", name: "Geography", level: "Lower Secondary", className: "Form II", category: "Core", description: "Weather, resources, industries, and environmental management.", enrolled: 345 },
  { id: "lower-f2-history", name: "History", level: "Lower Secondary", className: "Form II", category: "Core", description: "African history, colonialism, and the struggle for independence.", enrolled: 343 },
  { id: "lower-f2-maadili", name: "History & Ethics", level: "Lower Secondary", className: "Form II", category: "Core", description: "Tanzanian governance, cultural heritage, and moral development.", enrolled: 340 },
  { id: "lower-f2-civics", name: "Civic Education", level: "Lower Secondary", className: "Form II", category: "Core", description: "Constitutional rights, community participation, and leadership.", enrolled: 338 },
  { id: "lower-f2-computer", name: "Computer Science", level: "Lower Secondary", className: "Form II", category: "Core", description: "Programming concepts, software applications, and digital skills.", enrolled: 335 },
  { id: "lower-f2-agriculture", name: "Agriculture", level: "Lower Secondary", className: "Form II", category: "Core", description: "Livestock, soil science, irrigation, and agribusiness.", enrolled: 333 },
  { id: "lower-f2-business", name: "Business Studies", level: "Lower Secondary", className: "Form II", category: "Core", description: "Marketing, management, and business communication.", enrolled: 330 },
  { id: "lower-f2-bookkeeping", name: "Bookkeeping", level: "Lower Secondary", className: "Form II", category: "Core", description: "Advanced bookkeeping: trial balance, profit and loss, and cash flow.", enrolled: 328 },
  { id: "lower-f2-nutrition", name: "Food and Nutrition", level: "Lower Secondary", className: "Form II", category: "Core", description: "Food preparation, preservation, and dietary planning.", enrolled: 325 },
  { id: "lower-f2-textile", name: "Textile and Garment Construction", level: "Lower Secondary", className: "Form II", category: "Core", description: "Pattern making, stitching methods, and textile technology.", enrolled: 323 },

  { id: "lower-f3-math", name: "Mathematics", level: "Lower Secondary", className: "Form III", category: "Core", description: "Advanced algebra, geometry proofs, and mathematical reasoning.", enrolled: 370 },
  { id: "lower-f3-kiswahili", name: "Kiswahili", level: "Lower Secondary", className: "Form III", category: "Core", description: "Kiswahili literature analysis, drama, and advanced composition.", enrolled: 365 },
  { id: "lower-f3-english", name: "English Language", level: "Lower Secondary", className: "Form III", category: "Core", description: "English literature, advanced grammar, and academic writing.", enrolled: 360 },
  { id: "lower-f3-biology", name: "Biology", level: "Lower Secondary", className: "Form III", category: "Core", description: "Genetics, evolution, human physiology, and biotechnology.", enrolled: 355 },
  { id: "lower-f3-chemistry", name: "Chemistry", level: "Lower Secondary", className: "Form III", category: "Core", description: "Organic chemistry basics, periodic table, and chemical analysis.", enrolled: 350 },
  { id: "lower-f3-physics", name: "Physics", level: "Lower Secondary", className: "Form III", category: "Core", description: "Optics, thermodynamics, advanced electricity, and nuclear physics intro.", enrolled: 345 },
  { id: "lower-f3-geography", name: "Geography", level: "Lower Secondary", className: "Form III", category: "Core", description: "Regional geography, development issues, and GIS introduction.", enrolled: 343 },
  { id: "lower-f3-history", name: "History", level: "Lower Secondary", className: "Form III", category: "Core", description: "Modern world history, cold war, and African politics.", enrolled: 340 },
  { id: "lower-f3-maadili", name: "History & Ethics", level: "Lower Secondary", className: "Form III", category: "Core", description: "Political history of Tanzania, national values, and civic duty.", enrolled: 338 },
  { id: "lower-f3-civics", name: "Civic Education", level: "Lower Secondary", className: "Form III", category: "Core", description: "Human rights, international organizations, and global citizenship.", enrolled: 335 },
  { id: "lower-f3-computer", name: "Computer Science", level: "Lower Secondary", className: "Form III", category: "Core", description: "Web development, databases, networking, and cybersecurity basics.", enrolled: 333 },
  { id: "lower-f3-agriculture", name: "Agriculture", level: "Lower Secondary", className: "Form III", category: "Core", description: "Advanced farming techniques, pest control, and agricultural policy.", enrolled: 330 },
  { id: "lower-f3-business", name: "Business Studies", level: "Lower Secondary", className: "Form III", category: "Core", description: "Finance, trade, and economic principles.", enrolled: 328 },
  { id: "lower-f3-bookkeeping", name: "Bookkeeping", level: "Lower Secondary", className: "Form III", category: "Core", description: "Financial statements, budgeting, and accounting systems.", enrolled: 325 },
  { id: "lower-f3-nutrition", name: "Food and Nutrition", level: "Lower Secondary", className: "Form III", category: "Core", description: "Nutrition science, food safety, and community health.", enrolled: 323 },
  { id: "lower-f3-textile", name: "Textile and Garment Construction", level: "Lower Secondary", className: "Form III", category: "Core", description: "Advanced garment construction, fashion design, and textile care.", enrolled: 320 },

  { id: "lower-f4-math", name: "Mathematics", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Comprehensive mathematics for national examinations.", enrolled: 365 },
  { id: "lower-f4-kiswahili", name: "Kiswahili", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Kiswahili: national exam preparation, literature, and composition.", enrolled: 360 },
  { id: "lower-f4-english", name: "English Language", level: "Lower Secondary", className: "Form IV", category: "Core", description: "English: exam preparation, comprehension, and essay mastery.", enrolled: 355 },
  { id: "lower-f4-biology", name: "Biology", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Biology: comprehensive review and national exam preparation.", enrolled: 350 },
  { id: "lower-f4-chemistry", name: "Chemistry", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Chemistry: advanced topics and exam techniques.", enrolled: 345 },
  { id: "lower-f4-physics", name: "Physics", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Physics: problem solving, practical applications, and exam prep.", enrolled: 340 },
  { id: "lower-f4-geography", name: "Geography", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Geography: spatial analysis, development, and exam preparation.", enrolled: 338 },
  { id: "lower-f4-history", name: "History", level: "Lower Secondary", className: "Form IV", category: "Core", description: "History: thematic review and national exam preparation.", enrolled: 335 },
  { id: "lower-f4-maadili", name: "History & Ethics", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Tanzanian studies: comprehensive review and exam techniques.", enrolled: 333 },
  { id: "lower-f4-civics", name: "Civic Education", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Civic knowledge: governance systems and exam preparation.", enrolled: 330 },
  { id: "lower-f4-computer", name: "Computer Science", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Computer science: programming, systems, and exam preparation.", enrolled: 328 },
  { id: "lower-f4-agriculture", name: "Agriculture", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Agriculture: sustainable farming and exam preparation.", enrolled: 325 },
  { id: "lower-f4-business", name: "Business Studies", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Business: entrepreneurship and national exam preparation.", enrolled: 323 },
  { id: "lower-f4-bookkeeping", name: "Bookkeeping", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Bookkeeping: comprehensive financial management and exam prep.", enrolled: 320 },
  { id: "lower-f4-nutrition", name: "Food and Nutrition", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Nutrition: food science, health, and exam preparation.", enrolled: 318 },
  { id: "lower-f4-textile", name: "Textile and Garment Construction", level: "Lower Secondary", className: "Form IV", category: "Core", description: "Textile studies: design, construction, and exam preparation.", enrolled: 315 },

  { id: "adv-fv-history", name: "History", level: "Advanced Secondary", className: "Form V", category: "Social Science", description: "Advanced historical analysis: historiography, sources, and critical thinking.", enrolled: 180 },
  { id: "adv-fv-geography", name: "Geography", level: "Advanced Secondary", className: "Form V", category: "Social Science", description: "Advanced geography: spatial theories, development planning, and GIS.", enrolled: 175 },
  { id: "adv-fv-biblical", name: "Bible Knowledge", level: "Advanced Secondary", className: "Form V", category: "Social Science", description: "Biblical studies: theology, ethics, and scriptural interpretation.", enrolled: 120 },
  { id: "adv-fv-islamic", name: "Islamic Knowledge", level: "Advanced Secondary", className: "Form V", category: "Social Science", description: "Islamic studies: theology, jurisprudence, and moral philosophy.", enrolled: 115 },
  { id: "adv-fv-maadili", name: "History & Ethics", level: "Advanced Secondary", className: "Form V", category: "Social Science", description: "Advanced Tanzanian history and moral philosophy.", enrolled: 160 },

  { id: "adv-fv-kiswahili", name: "Kiswahili", level: "Advanced Secondary", className: "Form V", category: "Language and Literature", description: "Advanced Kiswahili: linguistics, literature criticism, and creative writing.", enrolled: 200 },
  { id: "adv-fv-english", name: "English", level: "Advanced Secondary", className: "Form V", category: "Language and Literature", description: "Advanced English: literary analysis, academic writing, and rhetoric.", enrolled: 195 },
  { id: "adv-fv-arabic", name: "Arabic", level: "Advanced Secondary", className: "Form V", category: "Language and Literature", description: "Arabic language: grammar, reading, and conversation.", enrolled: 90 },
  { id: "adv-fv-french", name: "French", level: "Advanced Secondary", className: "Form V", category: "Language and Literature", description: "French language: grammar, vocabulary, and communication skills.", enrolled: 85 },
  { id: "adv-fv-chinese", name: "Chinese", level: "Advanced Secondary", className: "Form V", category: "Language and Literature", description: "Chinese language: characters, pronunciation, and basic conversation.", enrolled: 70 },
  { id: "adv-fv-academic-comm", name: "Academic Communication", level: "Advanced Secondary", className: "Form V", category: "Language and Literature", description: "Academic writing, presentation skills, and research communication.", enrolled: 150 },
  { id: "adv-fv-kiswahili-lit", name: "Kiswahili Literature", level: "Advanced Secondary", className: "Form V", category: "Language and Literature", description: "Kiswahili literary works: novels, poetry, and drama analysis.", enrolled: 140 },
  { id: "adv-fv-english-lit", name: "Literature in English", level: "Advanced Secondary", className: "Form V", category: "Language and Literature", description: "English literature: novels, plays, poetry, and literary criticism.", enrolled: 135 },

  { id: "adv-fv-physics", name: "Physics", level: "Advanced Secondary", className: "Form V", category: "Natural Science", description: "Advanced physics: mechanics, electromagnetism, and modern physics.", enrolled: 160 },
  { id: "adv-fv-chemistry", name: "Chemistry", level: "Advanced Secondary", className: "Form V", category: "Natural Science", description: "Advanced chemistry: organic, inorganic, and physical chemistry.", enrolled: 155 },
  { id: "adv-fv-biology", name: "Biology", level: "Advanced Secondary", className: "Form V", category: "Natural Science", description: "Advanced biology: genetics, ecology, and biotechnology.", enrolled: 150 },
  { id: "adv-fv-agriculture", name: "Agriculture", level: "Advanced Secondary", className: "Form V", category: "Natural Science", description: "Advanced agriculture: crop science, animal husbandry, and agribusiness.", enrolled: 100 },
  { id: "adv-fv-nutrition", name: "Food and Nutrition", level: "Advanced Secondary", className: "Form V", category: "Natural Science", description: "Advanced nutrition: dietetics, food technology, and public health.", enrolled: 95 },

  { id: "adv-fv-math", name: "Mathematics", level: "Advanced Secondary", className: "Form V", category: "Mathematics", description: "Advanced mathematics: calculus, statistics, and mathematical modelling.", enrolled: 170 },
  { id: "adv-fv-applied-math", name: "Basic Applied Mathematics", level: "Advanced Secondary", className: "Form V", category: "Mathematics", description: "Applied mathematics: engineering math, financial math, and problem solving.", enrolled: 120 },

  { id: "adv-fv-computer", name: "Computer Science", level: "Advanced Secondary", className: "Form V", category: "Technology", description: "Advanced computing: programming, algorithms, and systems design.", enrolled: 140 },

  { id: "adv-fv-economics", name: "Economics", level: "Advanced Secondary", className: "Form V", category: "Business and Economics", description: "Microeconomics, macroeconomics, and economic policy analysis.", enrolled: 165 },
  { id: "adv-fv-business", name: "Business Studies", level: "Advanced Secondary", className: "Form V", category: "Business and Economics", description: "Advanced business: management, marketing, and strategic planning.", enrolled: 130 },
  { id: "adv-fv-accountancy", name: "Accountancy", level: "Advanced Secondary", className: "Form V", category: "Business and Economics", description: "Advanced accounting: financial statements, auditing, and taxation.", enrolled: 110 },

  { id: "adv-fv-fine-arts", name: "Fine Arts", level: "Advanced Secondary", className: "Form V", category: "Culture, Arts and Sports", description: "Visual arts: painting, sculpture, and art history.", enrolled: 80 },
  { id: "adv-fv-music", name: "Music", level: "Advanced Secondary", className: "Form V", category: "Culture, Arts and Sports", description: "Music theory, performance, and composition.", enrolled: 75 },
  { id: "adv-fv-theatre", name: "Theatre Arts", level: "Advanced Secondary", className: "Form V", category: "Culture, Arts and Sports", description: "Drama, performance studies, and theatrical production.", enrolled: 70 },
  { id: "adv-fv-textile", name: "Textile Garment Construction", level: "Advanced Secondary", className: "Form V", category: "Culture, Arts and Sports", description: "Advanced textile design, fashion technology, and garment production.", enrolled: 65 },
  { id: "adv-fv-sports", name: "Sports", level: "Advanced Secondary", className: "Form V", category: "Culture, Arts and Sports", description: "Sports science, coaching, and physical education theory.", enrolled: 90 },

  { id: "adv-fvi-history", name: "History", level: "Advanced Secondary", className: "Form VI", category: "Social Science", description: "Comprehensive history for advanced examinations.", enrolled: 170 },
  { id: "adv-fvi-geography", name: "Geography", level: "Advanced Secondary", className: "Form VI", category: "Social Science", description: "Advanced geographical studies and exam preparation.", enrolled: 165 },
  { id: "adv-fvi-biblical", name: "Bible Knowledge", level: "Advanced Secondary", className: "Form VI", category: "Social Science", description: "Advanced biblical theology and exam preparation.", enrolled: 110 },
  { id: "adv-fvi-islamic", name: "Islamic Knowledge", level: "Advanced Secondary", className: "Form VI", category: "Social Science", description: "Advanced Islamic studies and exam preparation.", enrolled: 105 },
  { id: "adv-fvi-maadili", name: "History & Ethics", level: "Advanced Secondary", className: "Form VI", category: "Social Science", description: "Advanced Tanzanian studies and national exam preparation.", enrolled: 150 },

  { id: "adv-fvi-kiswahili", name: "Kiswahili", level: "Advanced Secondary", className: "Form VI", category: "Language and Literature", description: "Advanced Kiswahili for national examinations.", enrolled: 190 },
  { id: "adv-fvi-english", name: "English", level: "Advanced Secondary", className: "Form VI", category: "Language and Literature", description: "Advanced English for national examinations.", enrolled: 185 },
  { id: "adv-fvi-arabic", name: "Arabic", level: "Advanced Secondary", className: "Form VI", category: "Language and Literature", description: "Advanced Arabic language studies.", enrolled: 85 },
  { id: "adv-fvi-french", name: "French", level: "Advanced Secondary", className: "Form VI", category: "Language and Literature", description: "Advanced French language studies.", enrolled: 80 },
  { id: "adv-fvi-chinese", name: "Chinese", level: "Advanced Secondary", className: "Form VI", category: "Language and Literature", description: "Advanced Chinese language studies.", enrolled: 65 },
  { id: "adv-fvi-academic-comm", name: "Academic Communication", level: "Advanced Secondary", className: "Form VI", category: "Language and Literature", description: "Advanced academic communication and research skills.", enrolled: 140 },
  { id: "adv-fvi-kiswahili-lit", name: "Kiswahili Literature", level: "Advanced Secondary", className: "Form VI", category: "Language and Literature", description: "Advanced Kiswahili literary analysis and exam preparation.", enrolled: 130 },
  { id: "adv-fvi-english-lit", name: "Literature in English", level: "Advanced Secondary", className: "Form VI", category: "Language and Literature", description: "Advanced English literature and exam preparation.", enrolled: 125 },

  { id: "adv-fvi-physics", name: "Physics", level: "Advanced Secondary", className: "Form VI", category: "Natural Science", description: "Advanced physics for national examinations.", enrolled: 150 },
  { id: "adv-fvi-chemistry", name: "Chemistry", level: "Advanced Secondary", className: "Form VI", category: "Natural Science", description: "Advanced chemistry for national examinations.", enrolled: 145 },
  { id: "adv-fvi-biology", name: "Biology", level: "Advanced Secondary", className: "Form VI", category: "Natural Science", description: "Advanced biology for national examinations.", enrolled: 140 },
  { id: "adv-fvi-agriculture", name: "Agriculture", level: "Advanced Secondary", className: "Form VI", category: "Natural Science", description: "Advanced agricultural science and exam preparation.", enrolled: 95 },
  { id: "adv-fvi-nutrition", name: "Food and Nutrition", level: "Advanced Secondary", className: "Form VI", category: "Natural Science", description: "Advanced nutrition science and exam preparation.", enrolled: 90 },

  { id: "adv-fvi-math", name: "Mathematics", level: "Advanced Secondary", className: "Form VI", category: "Mathematics", description: "Advanced mathematics for national examinations.", enrolled: 160 },
  { id: "adv-fvi-applied-math", name: "Basic Applied Mathematics", level: "Advanced Secondary", className: "Form VI", category: "Mathematics", description: "Applied mathematics for national examinations.", enrolled: 110 },

  { id: "adv-fvi-computer", name: "Computer Science", level: "Advanced Secondary", className: "Form VI", category: "Technology", description: "Advanced computer science and exam preparation.", enrolled: 130 },

  { id: "adv-fvi-economics", name: "Economics", level: "Advanced Secondary", className: "Form VI", category: "Business and Economics", description: "Advanced economics for national examinations.", enrolled: 155 },
  { id: "adv-fvi-business", name: "Business Studies", level: "Advanced Secondary", className: "Form VI", category: "Business and Economics", description: "Advanced business studies and exam preparation.", enrolled: 120 },
  { id: "adv-fvi-accountancy", name: "Accountancy", level: "Advanced Secondary", className: "Form VI", category: "Business and Economics", description: "Advanced accountancy for national examinations.", enrolled: 100 },

  { id: "adv-fvi-fine-arts", name: "Fine Arts", level: "Advanced Secondary", className: "Form VI", category: "Culture, Arts and Sports", description: "Advanced fine arts and exam preparation.", enrolled: 75 },
  { id: "adv-fvi-music", name: "Music", level: "Advanced Secondary", className: "Form VI", category: "Culture, Arts and Sports", description: "Advanced music studies and exam preparation.", enrolled: 70 },
  { id: "adv-fvi-theatre", name: "Theatre Arts", level: "Advanced Secondary", className: "Form VI", category: "Culture, Arts and Sports", description: "Advanced theatre arts and exam preparation.", enrolled: 65 },
  { id: "adv-fvi-textile", name: "Textile Garment Construction", level: "Advanced Secondary", className: "Form VI", category: "Culture, Arts and Sports", description: "Advanced textile and garment studies.", enrolled: 60 },
  { id: "adv-fvi-sports", name: "Sports", level: "Advanced Secondary", className: "Form VI", category: "Culture, Arts and Sports", description: "Advanced sports science and exam preparation.", enrolled: 85 },
]

export type VetaLesson = {
  id: string
  title: string
  description: string
  duration: string
}

export type VetaModule = {
  id: string
  name: string
  description: string
  lessons: VetaLesson[]
}

export type VetaLevel = {
  id: string
  name: string
  description: string
  modules: VetaModule[]
}

export type VetaTrade = {
  id: string
  name: string
  description: string
  icon: string
  levels: VetaLevel[]
}

export const vetaTrades: VetaTrade[] = [
  {
    id: "electrical-installation",
    name: "Electrical Installation",
    description: "Learn practical electrical installation, maintenance, and safety skills for domestic and industrial settings.",
    icon: "zap",
    levels: [
      {
        id: "level-1",
        name: "Level I",
        description: "Foundation skills in electrical safety, tools, and basic installation practices.",
        modules: [
          {
            id: "electrical-safety",
            name: "Electrical Safety",
            description: "Understanding electrical hazards, safety protocols, and protective equipment.",
            lessons: [
              { id: "lesson-1", title: "Introduction to Electrical Safety", description: "Overview of electrical safety principles and why they matter.", duration: "45 min" },
              { id: "lesson-2", title: "Electrical Hazards", description: "Identifying common electrical hazards and their risks.", duration: "40 min" },
              { id: "lesson-3", title: "Personal Protective Equipment", description: "Selecting and using appropriate PPE for electrical work.", duration: "35 min" },
              { id: "lesson-4", title: "Safe Working Practices", description: "Procedures for safe electrical work including lockout/tagout.", duration: "50 min" },
            ],
          },
          {
            id: "workshop-practice",
            name: "Workshop Practice",
            description: "Hands-on workshop skills, tool handling, and workspace organisation.",
            lessons: [
              { id: "lesson-1", title: "Workshop Tools and Equipment", description: "Identifying and using basic electrical workshop tools.", duration: "40 min" },
              { id: "lesson-2", title: "Workplace Organisation", description: "Maintaining a clean and safe workshop environment.", duration: "30 min" },
              { id: "lesson-3", title: "Basic Hand Skills", description: "Cutting, stripping, and bending wires and conduits.", duration: "45 min" },
            ],
          },
          {
            id: "basic-electrical-installation",
            name: "Basic Electrical Installation",
            description: "Fundamentals of electrical wiring, circuits, and component installation.",
            lessons: [
              { id: "lesson-1", title: "Electrical Circuits Fundamentals", description: "Understanding series and parallel circuits.", duration: "50 min" },
              { id: "lesson-2", title: "Wire Types and Sizing", description: "Selecting appropriate cables for different applications.", duration: "40 min" },
              { id: "lesson-3", title: "Basic Wiring Diagrams", description: "Reading and interpreting electrical schematics.", duration: "45 min" },
              { id: "lesson-4", title: "Installing Sockets and Switches", description: "Step-by-step installation of outlets and switches.", duration: "55 min" },
            ],
          },
        ],
      },
      {
        id: "level-2",
        name: "Level II",
        description: "Intermediate skills in domestic and commercial electrical installations.",
        modules: [
          {
            id: "domestic-installation",
            name: "Domestic Installation",
            description: "Complete domestic wiring systems from installation to testing.",
            lessons: [
              { id: "lesson-1", title: "Domestic Wiring Systems", description: "Overview of residential electrical systems.", duration: "50 min" },
              { id: "lesson-2", title: "Consumer Unit Installation", description: "Installing and wiring consumer units and distribution boards.", duration: "55 min" },
              { id: "lesson-3", title: "Lighting Circuits", description: "Designing and installing residential lighting.", duration: "45 min" },
              { id: "lesson-4", title: "Ring and Radial Circuits", description: "Installing power circuits for domestic applications.", duration: "50 min" },
            ],
          },
          {
            id: "electrical-maintenance",
            name: "Electrical Maintenance",
            description: "Diagnostic techniques and preventive maintenance for electrical systems.",
            lessons: [
              { id: "lesson-1", title: "Fault Finding Techniques", description: "Systematic approach to diagnosing electrical faults.", duration: "55 min" },
              { id: "lesson-2", title: "Testing and Inspection", description: "Using testing equipment to verify installations.", duration: "50 min" },
              { id: "lesson-3", title: "Preventive Maintenance", description: "Scheduled maintenance procedures for electrical systems.", duration: "40 min" },
            ],
          },
        ],
      },
      {
        id: "level-3",
        name: "Level III",
        description: "Advanced skills in industrial electrical systems and project management.",
        modules: [
          {
            id: "advanced-electrical-installation",
            name: "Advanced Electrical Installation",
            description: "Complex industrial installations, three-phase systems, and motor controls.",
            lessons: [
              { id: "lesson-1", title: "Three-Phase Systems", description: "Understanding and wiring three-phase power systems.", duration: "60 min" },
              { id: "lesson-2", title: "Motor Control Circuits", description: "Installing and troubleshooting motor starters and controllers.", duration: "55 min" },
              { id: "lesson-3", title: "Industrial Panel Wiring", description: "Wiring control panels and distribution systems.", duration: "50 min" },
              { id: "lesson-4", title: "Project Supervision", description: "Managing electrical installation projects.", duration: "45 min" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "motor-vehicle-mechanics",
    name: "Motor Vehicle Mechanics",
    description: "Comprehensive training in vehicle maintenance, diagnostics, and repair systems.",
    icon: "car",
    levels: [
      {
        id: "level-1",
        name: "Level I",
        description: "Foundation skills in vehicle systems and basic maintenance.",
        modules: [
          {
            id: "vehicle-systems-introduction",
            name: "Vehicle Systems Introduction",
            description: "Overview of major vehicle systems and their functions.",
            lessons: [
              { id: "lesson-1", title: "Engine Basics", description: "Understanding engine components and operating principles.", duration: "50 min" },
              { id: "lesson-2", title: "Chassis and Suspension", description: "Vehicle frame, suspension systems, and steering.", duration: "45 min" },
              { id: "lesson-3", title: "Braking Systems", description: "Disc, drum, and ABS braking systems.", duration: "40 min" },
            ],
          },
          {
            id: "basic-tools-equipment",
            name: "Basic Tools and Equipment",
            description: "Using automotive hand tools and workshop equipment safely.",
            lessons: [
              { id: "lesson-1", title: "Hand Tools for Mechanics", description: "Selection and use of common automotive tools.", duration: "40 min" },
              { id: "lesson-2", title: "Workshop Equipment", description: "Using lifts, jacks, and diagnostic equipment.", duration: "45 min" },
              { id: "lesson-3", title: "Tool Maintenance", description: "Caring for and maintaining automotive tools.", duration: "30 min" },
            ],
          },
        ],
      },
      {
        id: "level-2",
        name: "Level II",
        description: "Intermediate diagnostics and repair of vehicle systems.",
        modules: [
          {
            id: "engine-diagnostics",
            name: "Engine Diagnostics",
            description: "Modern engine diagnostics and troubleshooting procedures.",
            lessons: [
              { id: "lesson-1", title: "OBD-II Diagnostics", description: "Using onboard diagnostic systems for fault detection.", duration: "55 min" },
              { id: "lesson-2", title: "Fuel System Diagnosis", description: "Testing and repairing fuel injection systems.", duration: "50 min" },
              { id: "lesson-3", title: "Ignition System Troubleshooting", description: "Diagnosing ignition and misfire issues.", duration: "45 min" },
            ],
          },
          {
            id: "electrical-systems",
            name: "Electrical Systems",
            description: "Vehicle electrical systems, wiring, and component repair.",
            lessons: [
              { id: "lesson-1", title: "Automotive Wiring", description: "Reading wiring diagrams and tracing circuits.", duration: "50 min" },
              { id: "lesson-2", title: "Battery and Charging", description: "Testing batteries, alternators, and charging systems.", duration: "45 min" },
              { id: "lesson-3", title: "Lighting and Accessories", description: "Installing and repairing vehicle lighting systems.", duration: "40 min" },
            ],
          },
        ],
      },
      {
        id: "level-3",
        name: "Level III",
        description: "Advanced engine overhaul and specialised vehicle systems.",
        modules: [
          {
            id: "engine-overhaul",
            name: "Engine Overhaul",
            description: "Complete engine disassembly, inspection, and rebuild procedures.",
            lessons: [
              { id: "lesson-1", title: "Engine Disassembly", description: "Systematic teardown and parts cataloguing.", duration: "60 min" },
              { id: "lesson-2", title: "Component Inspection", description: "Measuring and inspecting engine components for wear.", duration: "55 min" },
              { id: "lesson-3", title: "Engine Reassembly", description: "Proper reassembly procedures and torque specifications.", duration: "60 min" },
              { id: "lesson-4", title: "Engine Testing", description: "Post-rebuild testing and performance verification.", duration: "50 min" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "plumbing",
    name: "Plumbing",
    description: "Learn water supply, drainage, and sanitary installation skills for residential and commercial buildings.",
    icon: "droplets",
    levels: [
      {
        id: "level-1",
        name: "Level I",
        description: "Foundation plumbing skills and basic pipe work.",
        modules: [
          {
            id: "plumbing-fundamentals",
            name: "Plumbing Fundamentals",
            description: "Basic principles of plumbing, tools, and materials.",
            lessons: [
              { id: "lesson-1", title: "Introduction to Plumbing", description: "Overview of plumbing systems and trade basics.", duration: "40 min" },
              { id: "lesson-2", title: "Plumbing Tools", description: "Essential tools and their proper use.", duration: "35 min" },
              { id: "lesson-3", title: "Pipe Types and Fittings", description: "Identifying different pipes, fittings, and their uses.", duration: "45 min" },
            ],
          },
          {
            id: "basic-pipe-work",
            name: "Basic Pipe Work",
            description: "Cutting, joining, and installing basic piping systems.",
            lessons: [
              { id: "lesson-1", title: "Measuring and Cutting Pipes", description: "Accurate measurement and cutting techniques.", duration: "40 min" },
              { id: "lesson-2", title: "Jointing Techniques", description: "Solvent welding, compression, and push-fit joints.", duration: "50 min" },
              { id: "lesson-3", title: "Supporting Pipework", description: "Correctly supporting and securing pipe runs.", duration: "35 min" },
            ],
          },
        ],
      },
      {
        id: "level-2",
        name: "Level II",
        description: "Intermediate water supply and drainage systems.",
        modules: [
          {
            id: "water-supply-systems",
            name: "Water Supply Systems",
            description: "Installing hot and cold water supply systems.",
            lessons: [
              { id: "lesson-1", title: "Cold Water Systems", description: "Mains supply, storage tanks, and distribution.", duration: "50 min" },
              { id: "lesson-2", title: "Hot Water Systems", description: "Boilers, cylinders, and unvented systems.", duration: "55 min" },
              { id: "lesson-3", title: "Pump Installation", description: "Selecting and installing water pumps.", duration: "45 min" },
            ],
          },
          {
            id: "drainage-systems",
            name: "Drainage Systems",
            description: "Below-ground and above-ground drainage installation.",
            lessons: [
              { id: "lesson-1", title: "Foul Water Drainage", description: "Sewer connections and waste pipe systems.", duration: "50 min" },
              { id: "lesson-2", title: "Surface Water Drainage", description: "Rainwater harvesting and drainage systems.", duration: "45 min" },
              { id: "lesson-3", title: "Drainage Testing", description: "Commissioning and testing drainage systems.", duration: "40 min" },
            ],
          },
        ],
      },
      {
        id: "level-3",
        name: "Level III",
        description: "Advanced plumbing systems and project supervision.",
        modules: [
          {
            id: "advanced-systems",
            name: "Advanced Plumbing Systems",
            description: "Complex plumbing installations and system design.",
            lessons: [
              { id: "lesson-1", title: "Commercial Plumbing", description: "Large-scale plumbing for commercial buildings.", duration: "55 min" },
              { id: "lesson-2", title: "Gas Installation Basics", description: "Introduction to gas pipework and safety.", duration: "50 min" },
              { id: "lesson-3", title: "System Design", description: "Planning and designing plumbing layouts.", duration: "45 min" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "carpentry-joinery",
    name: "Carpentry & Joinery",
    description: "Develop skills in wood construction, furniture making, and building carpentry.",
    icon: "hammer",
    levels: [
      {
        id: "level-1",
        name: "Level I",
        description: "Foundation woodworking skills and basic joint construction.",
        modules: [
          {
            id: "woodworking-fundamentals",
            name: "Woodworking Fundamentals",
            description: "Understanding timber, tools, and basic workshop practices.",
            lessons: [
              { id: "lesson-1", title: "Timber Types and Properties", description: "Identifying different woods and their characteristics.", duration: "40 min" },
              { id: "lesson-2", title: "Workshop Safety", description: "Safe use of woodworking machinery and hand tools.", duration: "35 min" },
              { id: "lesson-3", title: "Hand Tool Techniques", description: "Using planes, chisels, saws, and marking tools.", duration: "50 min" },
            ],
          },
          {
            id: "basic-joints",
            name: "Basic Joints",
            description: "Constructing fundamental woodworking joints.",
            lessons: [
              { id: "lesson-1", title: "Butt and Lap Joints", description: "Simple joint construction techniques.", duration: "45 min" },
              { id: "lesson-2", title: "Mortise and Tenon", description: "Traditional mortise and tenon joint construction.", duration: "55 min" },
              { id: "lesson-3", title: "Dovetail Joints", description: "Cutting and fitting dovetail joints.", duration: "50 min" },
            ],
          },
        ],
      },
      {
        id: "level-2",
        name: "Level II",
        description: "Intermediate furniture making and site carpentry.",
        modules: [
          {
            id: "furniture-construction",
            name: "Furniture Construction",
            description: "Building functional furniture pieces from plans.",
            lessons: [
              { id: "lesson-1", title: "Reading Technical Drawings", description: "Interpreting plans and specifications.", duration: "45 min" },
              { id: "lesson-2", title: "Carcass Construction", description: "Building cabinet carcasses and frames.", duration: "55 min" },
              { id: "lesson-3", title: "Door and Drawer Making", description: "Fitting doors, drawers, and hardware.", duration: "50 min" },
            ],
          },
          {
            id: "site-carpentry",
            name: "Site Carpentry",
            description: "First fix and second fix carpentry on building sites.",
            lessons: [
              { id: "lesson-1", title: "First Fix Carpentry", description: "Roofing, flooring, and structural timber work.", duration: "55 min" },
              { id: "lesson-2", title: "Second Fix Carpentry", description: "Skirting, architraves, and finishing work.", duration: "45 min" },
              { id: "lesson-3", title: "Window and Door Installation", description: "Fitting windows, doors, and ironmongery.", duration: "50 min" },
            ],
          },
        ],
      },
      {
        id: "level-3",
        name: "Level III",
        description: "Advanced furniture design and workshop supervision.",
        modules: [
          {
            id: "advanced-furniture-design",
            name: "Advanced Furniture Design",
            description: "Designing and constructing complex furniture pieces.",
            lessons: [
              { id: "lesson-1", title: "Design Principles", description: "Ergonomics, aesthetics, and functional design.", duration: "50 min" },
              { id: "lesson-2", title: "Advanced Joinery", description: "Complex joints, inlays, and decorative techniques.", duration: "55 min" },
              { id: "lesson-3", title: "Finishing Techniques", description: "Staining, varnishing, and surface treatments.", duration: "45 min" },
              { id: "lesson-4", title: "Workshop Management", description: "Managing a carpentry workshop and production.", duration: "40 min" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "ict-computer",
    name: "ICT / Computer Studies",
    description: "Develop computer literacy, software skills, and basic IT support capabilities.",
    icon: "monitor",
    levels: [
      {
        id: "level-1",
        name: "Level I",
        description: "Computer fundamentals and basic office applications.",
        modules: [
          {
            id: "computer-fundamentals",
            name: "Computer Fundamentals",
            description: "Understanding computer hardware, software, and operating systems.",
            lessons: [
              { id: "lesson-1", title: "Introduction to Computers", description: "Computer components and how they work together.", duration: "40 min" },
              { id: "lesson-2", title: "Operating Systems", description: "Using Windows and basic file management.", duration: "45 min" },
              { id: "lesson-3", title: "Input and Output Devices", description: "Keyboards, mice, printers, and displays.", duration: "35 min" },
            ],
          },
          {
            id: "office-applications",
            name: "Office Applications",
            description: "Using word processors, spreadsheets, and presentation software.",
            lessons: [
              { id: "lesson-1", title: "Word Processing", description: "Creating and formatting documents.", duration: "50 min" },
              { id: "lesson-2", title: "Spreadsheets", description: "Creating formulas, charts, and basic data analysis.", duration: "55 min" },
              { id: "lesson-3", title: "Presentations", description: "Building and delivering effective presentations.", duration: "40 min" },
            ],
          },
        ],
      },
      {
        id: "level-2",
        name: "Level II",
        description: "Intermediate networking, databases, and web basics.",
        modules: [
          {
            id: "networking-basics",
            name: "Networking Basics",
            description: "Understanding computer networks and internet connectivity.",
            lessons: [
              { id: "lesson-1", title: "Network Types", description: "LAN, WAN, and internet infrastructure.", duration: "45 min" },
              { id: "lesson-2", title: "IP Addressing", description: "Understanding IP addresses and subnetting.", duration: "50 min" },
              { id: "lesson-3", title: "Network Security Basics", description: "Firewalls, passwords, and safe browsing.", duration: "40 min" },
            ],
          },
          {
            id: "database-introduction",
            name: "Database Introduction",
            description: "Creating and managing databases using practical examples.",
            lessons: [
              { id: "lesson-1", title: "Database Concepts", description: "Tables, records, fields, and relationships.", duration: "45 min" },
              { id: "lesson-2", title: "Creating Databases", description: "Building tables and forms in Access/MySQL.", duration: "55 min" },
              { id: "lesson-3", title: "Queries and Reports", description: "Extracting data and generating reports.", duration: "50 min" },
            ],
          },
        ],
      },
      {
        id: "level-3",
        name: "Level III",
        description: "Advanced IT support, basic programming, and system administration.",
        modules: [
          {
            id: "it-support",
            name: "IT Support",
            description: "Hardware troubleshooting, software installation, and helpdesk skills.",
            lessons: [
              { id: "lesson-1", title: "Hardware Troubleshooting", description: "Diagnosing and fixing common PC issues.", duration: "55 min" },
              { id: "lesson-2", title: "Software Installation", description: "Installing, updating, and removing software.", duration: "40 min" },
              { id: "lesson-3", title: "Helpdesk Skills", description: "Customer service and technical support techniques.", duration: "45 min" },
            ],
          },
          {
            id: "introduction-to-programming",
            name: "Introduction to Programming",
            description: "Basic programming concepts using Python or similar language.",
            lessons: [
              { id: "lesson-1", title: "Programming Concepts", description: "Variables, loops, and functions.", duration: "50 min" },
              { id: "lesson-2", title: "Writing Simple Programs", description: "Building basic programs step by step.", duration: "55 min" },
              { id: "lesson-3", title: "Debugging and Testing", description: "Finding and fixing errors in code.", duration: "45 min" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "tailoring-fashion",
    name: "Tailoring & Fashion Design",
    description: "Learn garment construction, pattern making, and fashion design principles.",
    icon: "scissors",
    levels: [
      {
        id: "level-1",
        name: "Level I",
        description: "Basic sewing skills, fabric handling, and simple garment construction.",
        modules: [
          {
            id: "sewing-basics",
            name: "Sewing Basics",
            description: "Hand and machine sewing fundamentals.",
            lessons: [
              { id: "lesson-1", title: "Hand Sewing Techniques", description: "Stitches, seams, and finishing by hand.", duration: "40 min" },
              { id: "lesson-2", title: "Using a Sewing Machine", description: "Setting up and operating a sewing machine.", duration: "50 min" },
              { id: "lesson-3", title: "Fabric Types", description: "Identifying and working with different fabrics.", duration: "35 min" },
            ],
          },
          {
            id: "simple-garments",
            name: "Simple Garments",
            description: "Making basic clothing items from patterns.",
            lessons: [
              { id: "lesson-1", title: "Reading Patterns", description: "Understanding pattern symbols and instructions.", duration: "45 min" },
              { id: "lesson-2", title: "Skirt Construction", description: "Making a simple skirt from start to finish.", duration: "55 min" },
              { id: "lesson-3", title: "Trouser Basics", description: "Constructing basic trousers with a fly zip.", duration: "50 min" },
            ],
          },
        ],
      },
      {
        id: "level-2",
        name: "Level II",
        description: "Pattern drafting, advanced construction, and design principles.",
        modules: [
          {
            id: "pattern-drafting",
            name: "Pattern Drafting",
            description: "Creating custom patterns from body measurements.",
            lessons: [
              { id: "lesson-1", title: "Body Measurements", description: "Taking accurate measurements for pattern making.", duration: "40 min" },
              { id: "lesson-2", title: "Basic Block Drafting", description: "Drafting bodice, skirt, and trouser blocks.", duration: "55 min" },
              { id: "lesson-3", title: "Pattern Alterations", description: "Modifying patterns for different styles.", duration: "50 min" },
            ],
          },
          {
            id: "advanced-construction",
            name: "Advanced Construction",
            description: "Complex garments with linings, collars, and fastenings.",
            lessons: [
              { id: "lesson-1", title: "Collar and Cuff Construction", description: "Making shirt collars, cuffs, and plackets.", duration: "55 min" },
              { id: "lesson-2", title: "Lining and Interfacing", description: "Adding structure and finish to garments.", duration: "45 min" },
              { id: "lesson-3", title: "Zipper Installation", description: "Invisible, exposed, and buttonhole zippers.", duration: "50 min" },
            ],
          },
        ],
      },
      {
        id: "level-3",
        name: "Level III",
        description: "Fashion design, collection development, and business skills.",
        modules: [
          {
            id: "fashion-design",
            name: "Fashion Design",
            description: "Creating original designs and developing collections.",
            lessons: [
              { id: "lesson-1", title: "Design Inspiration", description: "Research, mood boards, and trend analysis.", duration: "45 min" },
              { id: "lesson-2", title: "Fashion Illustration", description: "Sketching garment designs and technical flats.", duration: "50 min" },
              { id: "lesson-3", title: "Collection Development", description: "Building a cohesive fashion collection.", duration: "55 min" },
            ],
          },
          {
            id: "tailoring-business",
            name: "Tailoring Business",
            description: "Setting up and running a tailoring business.",
            lessons: [
              { id: "lesson-1", title: "Business Planning", description: "Creating a business plan for a tailoring shop.", duration: "40 min" },
              { id: "lesson-2", title: "Pricing and Costing", description: "Calculating costs and setting prices.", duration: "45 min" },
              { id: "lesson-3", title: "Customer Service", description: "Building client relationships and managing orders.", duration: "35 min" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "welding-metalwork",
    name: "Welding & Metal Fabrication",
    description: "Master welding techniques, metal cutting, and fabrication of metal structures.",
    icon: "flame",
    levels: [
      {
        id: "level-1",
        name: "Level I",
        description: "Introduction to welding processes, safety, and basic metal work.",
        modules: [
          {
            id: "welding-safety",
            name: "Welding Safety",
            description: "Personal protection and safe welding practices.",
            lessons: [
              { id: "lesson-1", title: "Welding Hazards", description: "Identifying risks in welding operations.", duration: "40 min" },
              { id: "lesson-2", title: "Protective Equipment", description: "Selecting and using welding PPE.", duration: "35 min" },
              { id: "lesson-3", title: "Safe Work Procedures", description: "Establishing safe welding work areas.", duration: "45 min" },
            ],
          },
          {
            id: "basic-welding",
            name: "Basic Welding Techniques",
            description: "Introduction to MMA, MIG, and gas welding processes.",
            lessons: [
              { id: "lesson-1", title: "MMA (Stick) Welding", description: "Setting up and performing basic arc welding.", duration: "55 min" },
              { id: "lesson-2", title: "MIG Welding Basics", description: "Gas metal arc welding fundamentals.", duration: "50 min" },
              { id: "lesson-3", title: "Gas Welding and Cutting", description: "Oxy-fuel welding and cutting techniques.", duration: "45 min" },
            ],
          },
        ],
      },
      {
        id: "level-2",
        name: "Level II",
        description: "Intermediate welding positions and metal fabrication techniques.",
        modules: [
          {
            id: "welding-positions",
            name: "Welding Positions",
            description: "Performing welds in all positions and joints.",
            lessons: [
              { id: "lesson-1", title: "Horizontal and Vertical Welds", description: "Welding in non-flat positions.", duration: "55 min" },
              { id: "lesson-2", title: "Overhead Welding", description: "Performing safe overhead welds.", duration: "50 min" },
              { id: "lesson-3", title: "Joint Preparation", description: "Butt, fillet, and edge joint preparation.", duration: "45 min" },
            ],
          },
          {
            id: "metal-fabrication",
            name: "Metal Fabrication",
            description: "Cutting, shaping, and assembling metal components.",
            lessons: [
              { id: "lesson-1", title: "Metal Cutting", description: "Plasma, flame, and mechanical cutting.", duration: "50 min" },
              { id: "lesson-2", title: "Bending and Forming", description: "Bending metal sheets and sections.", duration: "45 min" },
              { id: "lesson-3", title: "Assembly Techniques", description: "Clamping, tacking, and final assembly.", duration: "50 min" },
            ],
          },
        ],
      },
      {
        id: "level-3",
        name: "Level III",
        description: "Advanced welding certifications and structural fabrication.",
        modules: [
          {
            id: "advanced-welding",
            name: "Advanced Welding",
            description: "Specialised welding processes and quality control.",
            lessons: [
              { id: "lesson-1", title: "TIG Welding", description: "Tungsten inert gas welding for precision work.", duration: "55 min" },
              { id: "lesson-2", title: "Weld Inspection", description: "Visual inspection and non-destructive testing.", duration: "50 min" },
              { id: "lesson-3", title: "Welding Codes", description: "Understanding welding standards and certifications.", duration: "45 min" },
            ],
          },
          {
            id: "structural-fabrication",
            name: "Structural Fabrication",
            description: "Fabricating steel structures and components.",
            lessons: [
              { id: "lesson-1", title: "Reading Fabrication Drawings", description: "Interpreting structural steel drawings.", duration: "50 min" },
              { id: "lesson-2", title: "Steel Fabrication", description: "Building frames, trusses, and structures.", duration: "55 min" },
              { id: "lesson-3", title: "Quality Assurance", description: "Ensuring fabrication quality and compliance.", duration: "45 min" },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "beauty-therapy",
    name: "Beauty Therapy",
    description: "Professional beauty treatments including skincare, hairdressing, and cosmetic application.",
    icon: "sparkles",
    levels: [
      {
        id: "level-1",
        name: "Level I",
        description: "Introduction to beauty treatments and client care.",
        modules: [
          {
            id: "beauty-fundamentals",
            name: "Beauty Fundamentals",
            description: "Basic beauty principles, hygiene, and client consultation.",
            lessons: [
              { id: "lesson-1", title: "Introduction to Beauty Therapy", description: "Overview of beauty industry and career opportunities.", duration: "40 min" },
              { id: "lesson-2", title: "Hygiene and Sterilisation", description: "Maintaining cleanliness and preventing infection.", duration: "35 min" },
              { id: "lesson-3", title: "Client Consultation", description: "Assessing client needs and recommending treatments.", duration: "45 min" },
            ],
          },
          {
            id: "basic-skincare",
            name: "Basic Skincare",
            description: "Facial treatments and skin analysis basics.",
            lessons: [
              { id: "lesson-1", title: "Skin Types and Conditions", description: "Identifying different skin types and concerns.", duration: "40 min" },
              { id: "lesson-2", title: "Facial Cleansing", description: "Performing basic facial cleansing routines.", duration: "45 min" },
              { id: "lesson-3", title: "Mask Application", description: "Selecting and applying facial masks.", duration: "35 min" },
            ],
          },
        ],
      },
      {
        id: "level-2",
        name: "Level II",
        description: "Intermediate treatments including hairdressing and nail care.",
        modules: [
          {
            id: "hairdressing-basics",
            name: "Hairdressing Basics",
            description: "Washing, cutting, and styling hair.",
            lessons: [
              { id: "lesson-1", title: "Hair Washing and Conditioning", description: "Proper shampooing and conditioning techniques.", duration: "40 min" },
              { id: "lesson-2", title: "Basic Haircutting", description: "Fundamental cutting techniques.", duration: "55 min" },
              { id: "lesson-3", title: "Blow Drying and Styling", description: "Creating basic hairstyles and finishes.", duration: "50 min" },
            ],
          },
          {
            id: "nail-care",
            name: "Nail Care",
            description: "Manicure, pedicure, and nail enhancement techniques.",
            lessons: [
              { id: "lesson-1", title: "Manicure Techniques", description: "Performing a professional manicure.", duration: "45 min" },
              { id: "lesson-2", title: "Pedicure Techniques", description: "Professional pedicure procedures.", duration: "45 min" },
              { id: "lesson-3", title: "Nail Enhancements", description: "Applying gel and acrylic nails.", duration: "50 min" },
            ],
          },
        ],
      },
      {
        id: "level-3",
        name: "Level III",
        description: "Advanced beauty treatments and salon management.",
        modules: [
          {
            id: "advanced-treatments",
            name: "Advanced Treatments",
            description: "Specialised beauty treatments and techniques.",
            lessons: [
              { id: "lesson-1", title: "Advanced Facial Treatments", description: "Chemical peels, microdermabrasion, and masks.", duration: "55 min" },
              { id: "lesson-2", title: "Makeup Artistry", description: "Professional makeup application for occasions.", duration: "50 min" },
              { id: "lesson-3", title: "Body Treatments", description: "Massage, wrapping, and body care routines.", duration: "45 min" },
            ],
          },
          {
            id: "salon-management",
            name: "Salon Management",
            description: "Running a beauty salon and managing client relationships.",
            lessons: [
              { id: "lesson-1", title: "Salon Setup", description: "Planning and equipping a beauty salon.", duration: "40 min" },
              { id: "lesson-2", title: "Client Management", description: "Booking systems, records, and retention.", duration: "45 min" },
              { id: "lesson-3", title: "Marketing Your Salon", description: "Advertising and building a client base.", duration: "40 min" },
            ],
          },
        ],
      },
    ],
  },
]

export function getVetaTradeBySlug(slug: string): VetaTrade | undefined {
  return vetaTrades.find((t) => t.id === slug)
}

export function getVetaLevelBySlug(trade: VetaTrade, levelSlug: string): VetaLevel | undefined {
  return trade.levels.find((l) => l.id === levelSlug)
}

export function getVetaModuleBySlug(level: VetaLevel, moduleSlug: string): VetaModule | undefined {
  return level.modules.find((m) => m.id === moduleSlug)
}

export function getVetaLessonBySlug(module: VetaModule, lessonSlug: string): VetaLesson | undefined {
  return module.lessons.find((l) => l.id === lessonSlug)
}

export function vetaLessonIndex(module: VetaModule, lessonId: string): number {
  return module.lessons.findIndex((l) => l.id === lessonId)
}

export type CollegeCourse = {
  id: string
  name: string
  code: string
  description: string
  credits: number
}

export type CollegeProgramme = {
  id: string
  name: string
  degreeType: string
  description: string
  duration: string
  courses: CollegeCourse[]
}

export type CollegeFaculty = {
  id: string
  name: string
  description: string
  programmes: CollegeProgramme[]
}

export type College = {
  id: string
  name: string
  shortName: string
  description: string
  location: string
  faculties: CollegeFaculty[]
}

export const colleges: College[] = [
  {
    id: "university-of-dar-es-salaam",
    name: "University of Dar es Salaam",
    shortName: "UDSM",
    description: "Tanzania's oldest and largest public university, offering a wide range of undergraduate and postgraduate programmes.",
    location: "Dar es Salaam",
    faculties: [
      {
        id: "college-of-humanities-and-social-sciences",
        name: "College of Humanities and Social Sciences",
        description: "Arts, social sciences, and humanities programmes.",
        programmes: [
          {
            id: "bachelor-of-arts-in-english",
            name: "Bachelor of Arts in English",
            degreeType: "Degree",
            description: "Comprehensive study of English language, literature, and linguistics.",
            duration: "3 years",
            courses: [
              { id: "eng-101", name: "Introduction to English Studies", code: "ENG 101", description: "Foundation course in English language and literary analysis.", credits: 3 },
              { id: "eng-201", name: "English Literature I", code: "ENG 201", description: "Survey of major English literary works from Renaissance to Modern period.", credits: 3 },
              { id: "eng-301", name: "Linguistics and Phonology", code: "ENG 301", description: "Introduction to linguistic theory and phonological systems.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-arts-in-history",
            name: "Bachelor of Arts in History",
            degreeType: "Degree",
            description: "Study of African and world history with emphasis on East African heritage.",
            duration: "3 years",
            courses: [
              { id: "his-101", name: "Introduction to African History", code: "HIS 101", description: "Survey of African civilizations from ancient to colonial period.", credits: 3 },
              { id: "his-201", name: "East African History", code: "HIS 201", description: "Pre-colonial and colonial history of East Africa.", credits: 3 },
              { id: "his-301", name: "Research Methods in History", code: "HIS 301", description: "Historical research methodology and source analysis.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-social-sciences",
            name: "Bachelor of Social Sciences",
            degreeType: "Degree",
            description: "Interdisciplinary social science programme covering sociology, political science, and economics.",
            duration: "3 years",
            courses: [
              { id: "soc-101", name: "Introduction to Sociology", code: "SOC 101", description: "Fundamental concepts of society, culture, and social structures.", credits: 3 },
              { id: "pol-101", name: "Introduction to Political Science", code: "POL 101", description: "Political systems, governance, and public policy.", credits: 3 },
              { id: "eco-101", name: "Principles of Economics", code: "ECO 101", description: "Microeconomics and macroeconomics fundamentals.", credits: 3 },
            ],
          },
        ],
      },
      {
        id: "college-of-natural-and-applied-sciences",
        name: "College of Natural and Applied Sciences",
        description: "Sciences, mathematics, and technology programmes.",
        programmes: [
          {
            id: "bachelor-of-science-in-computer-science",
            name: "Bachelor of Science in Computer Science",
            degreeType: "Degree",
            description: "Computing theory, software development, and systems design.",
            duration: "3 years",
            courses: [
              { id: "cs-101", name: "Introduction to Programming", code: "CS 101", description: "Fundamentals of programming using Python and Java.", credits: 3 },
              { id: "cs-201", name: "Data Structures and Algorithms", code: "CS 201", description: "Fundamental data structures and algorithm design.", credits: 3 },
              { id: "cs-301", name: "Database Systems", code: "CS 301", description: "Relational databases, SQL, and database design.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-science-in-mathematics",
            name: "Bachelor of Science in Mathematics",
            degreeType: "Degree",
            description: "Pure and applied mathematics with computational focus.",
            duration: "3 years",
            courses: [
              { id: "mat-101", name: "Calculus I", code: "MAT 101", description: "Limits, derivatives, and integrals.", credits: 3 },
              { id: "mat-201", name: "Linear Algebra", code: "MAT 201", description: "Vector spaces, matrices, and linear transformations.", credits: 3 },
              { id: "mat-301", name: "Differential Equations", code: "MAT 301", description: "Ordinary and partial differential equations.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-science-in-physics",
            name: "Bachelor of Science in Physics",
            degreeType: "Degree",
            description: "Theoretical and experimental physics with laboratory training.",
            duration: "3 years",
            courses: [
              { id: "phy-101", name: "Mechanics and Thermodynamics", code: "PHY 101", description: "Classical mechanics and thermal physics.", credits: 3 },
              { id: "phy-201", name: "Electromagnetism", code: "PHY 201", description: "Electric and magnetic fields, circuits, and electromagnetic waves.", credits: 3 },
              { id: "phy-301", name: "Quantum Mechanics", code: "PHY 301", description: "Introduction to quantum theory and applications.", credits: 3 },
            ],
          },
        ],
      },
      {
        id: "college-of-engineering-and-technology",
        name: "College of Engineering and Technology",
        description: "Engineering, technology, and built environment programmes.",
        programmes: [
          {
            id: "bachelor-of-civil-engineering",
            name: "Bachelor of Civil Engineering",
            degreeType: "Degree",
            description: "Structural, geotechnical, and transportation engineering.",
            duration: "4 years",
            courses: [
              { id: "cen-101", name: "Engineering Drawing", code: "CEN 101", description: "Technical drawing and CAD fundamentals.", credits: 3 },
              { id: "cen-201", name: "Strength of Materials", code: "CEN 201", description: "Stress, strain, and structural analysis.", credits: 3 },
              { id: "cen-301", name: "Structural Design", code: "CEN 301", description: "Design of concrete and steel structures.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-electrical-engineering",
            name: "Bachelor of Electrical Engineering",
            degreeType: "Degree",
            description: "Power systems, electronics, and control engineering.",
            duration: "4 years",
            courses: [
              { id: "een-101", name: "Circuit Theory", code: "EEN 101", description: "Analysis of electrical circuits and network theorems.", credits: 3 },
              { id: "een-201", name: "Electronics I", code: "EEN 201", description: "Semiconductor devices and analog circuits.", credits: 3 },
              { id: "een-301", name: "Power Systems", code: "EEN 301", description: "Generation, transmission, and distribution of electrical power.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-computer-engineering",
            name: "Bachelor of Computer Engineering",
            degreeType: "Degree",
            description: "Hardware design, embedded systems, and computer architecture.",
            duration: "4 years",
            courses: [
              { id: "cen-101", name: "Digital Logic Design", code: "CPE 101", description: "Boolean algebra, logic gates, and digital circuits.", credits: 3 },
              { id: "cen-201", name: "Microprocessor Systems", code: "CPE 201", description: "Microprocessor architecture and assembly programming.", credits: 3 },
              { id: "cen-301", name: "Embedded Systems", code: "CPE 301", description: "Design and programming of embedded controllers.", credits: 3 },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "sokoine-university-of-agriculture",
    name: "Sokoine University of Agriculture",
    shortName: "SUA",
    description: "Tanzania's leading agricultural university, offering programmes in agriculture, forestry, and veterinary sciences.",
    location: "Morogoro",
    faculties: [
      {
        id: "faculty-of-agriculture",
        name: "Faculty of Agriculture",
        description: "Crop science, soil science, and agricultural economics.",
        programmes: [
          {
            id: "bachelor-of-science-in-agriculture",
            name: "Bachelor of Science in Agriculture",
            degreeType: "Degree",
            description: "Comprehensive agricultural science covering crops, soils, and farm management.",
            duration: "4 years",
            courses: [
              { id: "agr-101", name: "Introduction to Agriculture", code: "AGR 101", description: "Overview of agricultural systems and practices.", credits: 3 },
              { id: "agr-201", name: "Crop Science", code: "AGR 201", description: "Plant biology, crop production, and protection.", credits: 3 },
              { id: "agr-301", name: "Soil Science", code: "AGR 301", description: "Soil properties, fertility management, and conservation.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-agricultural-economics",
            name: "Bachelor of Agricultural Economics",
            degreeType: "Degree",
            description: "Economic analysis of agricultural systems and food security.",
            duration: "4 years",
            courses: [
              { id: "age-101", name: "Principles of Agricultural Economics", code: "AGE 101", description: "Economic principles applied to agriculture.", credits: 3 },
              { id: "age-201", name: "Farm Management", code: "AGE 201", description: "Decision making and resource allocation on farms.", credits: 3 },
              { id: "age-301", name: "Agricultural Policy", code: "AGE 301", description: "Policy analysis and food security frameworks.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-science-in-agronomy",
            name: "Bachelor of Science in Agronomy",
            degreeType: "Degree",
            description: "Advanced crop production systems and research methods.",
            duration: "4 years",
            courses: [
              { id: "agr-101", name: "Plant Physiology", code: "AGR 101", description: "Plant growth, development, and metabolic processes.", credits: 3 },
              { id: "agr-201", name: "Seed Science and Technology", code: "AGR 201", description: "Seed production, processing, and quality control.", credits: 3 },
              { id: "agr-301", name: "Agricultural Research Methods", code: "AGR 301", description: "Experimental design and statistical analysis in agriculture.", credits: 3 },
            ],
          },
        ],
      },
      {
        id: "faculty-of-forestry-and-wildlife",
        name: "Faculty of Forestry and Wildlife",
        description: "Forest management, wildlife conservation, and natural resource management.",
        programmes: [
          {
            id: "bachelor-of-science-in-forestry",
            name: "Bachelor of Science in Forestry",
            degreeType: "Degree",
            description: "Forest ecology, management, and sustainable forestry practices.",
            duration: "4 years",
            courses: [
              { id: "for-101", name: "Introduction to Forestry", code: "FOR 101", description: "Forest ecosystems and forestry principles.", credits: 3 },
              { id: "for-201", name: "Forest Ecology", code: "FOR 201", description: "Ecological processes in forest environments.", credits: 3 },
              { id: "for-301", name: "Forest Management", code: "FOR 301", description: "Planning and managing forest resources.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-science-in-wildlife-management",
            name: "Bachelor of Science in Wildlife Management",
            degreeType: "Degree",
            description: "Wildlife ecology, conservation, and protected area management.",
            duration: "4 years",
            courses: [
              { id: "wlm-101", name: "Introduction to Wildlife Biology", code: "WLM 101", description: "Wildlife populations, habitats, and behaviour.", credits: 3 },
              { id: "wlm-201", name: "Conservation Biology", code: "WLM 201", description: "Biodiversity conservation and threat assessment.", credits: 3 },
              { id: "wlm-301", name: "Protected Area Management", code: "WLM 301", description: "Management of national parks and game reserves.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-science-in-natural-resources-management",
            name: "Bachelor of Science in Natural Resources Management",
            degreeType: "Degree",
            description: "Integrated management of land, water, and biological resources.",
            duration: "4 years",
            courses: [
              { id: "nrm-101", name: "Introduction to Natural Resources", code: "NRM 101", description: "Overview of renewable and non-renewable resources.", credits: 3 },
              { id: "nrm-201", name: "Land Use Planning", code: "NRM 201", description: "Spatial analysis and land allocation methods.", credits: 3 },
              { id: "nrm-301", name: "Community-Based Resource Management", code: "NRM 301", description: "Participatory approaches to resource management.", credits: 3 },
            ],
          },
        ],
      },
      {
        id: "faculty-of-veterinary-medicine",
        name: "Faculty of Veterinary Medicine",
        description: "Animal health, veterinary science, and livestock management.",
        programmes: [
          {
            id: "doctor-of-veterinary-medicine",
            name: "Doctor of Veterinary Medicine",
            degreeType: "Degree",
            description: "Comprehensive veterinary training covering animal health and production.",
            duration: "5 years",
            courses: [
              { id: "vet-101", name: "Introduction to Veterinary Science", code: "VET 101", description: "Overview of veterinary medicine and animal health.", credits: 3 },
              { id: "vet-201", name: "Animal Anatomy", code: "VET 201", description: "Comparative anatomy of domestic animals.", credits: 3 },
              { id: "vet-301", name: "Veterinary Pathology", code: "VET 301", description: "Disease processes and diagnostic techniques.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-science-in-animal-science",
            name: "Bachelor of Science in Animal Science",
            degreeType: "Degree",
            description: "Livestock production, nutrition, and breeding.",
            duration: "4 years",
            courses: [
              { id: "ans-101", name: "Introduction to Animal Science", code: "ANS 101", description: "Livestock species, breeds, and production systems.", credits: 3 },
              { id: "ans-201", name: "Animal Nutrition", code: "ANS 201", description: "Feed evaluation and nutritional requirements.", credits: 3 },
              { id: "ans-301", name: "Animal Breeding", code: "ANS 301", description: "Genetic improvement and breeding programmes.", credits: 3 },
            ],
          },
          {
            id: "bachelor-of-science-in-veterinary-public-health",
            name: "Bachelor of Science in Veterinary Public Health",
            degreeType: "Degree",
            description: "Food safety, zoonotic diseases, and public health.",
            duration: "4 years",
            courses: [
              { id: "vph-101", name: "Introduction to Public Health", code: "VPH 101", description: "Public health principles and epidemiology.", credits: 3 },
              { id: "vph-201", name: "Food Safety and Inspection", code: "VPH 201", description: "Meat inspection and food safety standards.", credits: 3 },
              { id: "vph-301", name: "Zoonotic Diseases", code: "VPH 301", description: "Diseases transmissible between animals and humans.", credits: 3 },
            ],
          },
        ],
      },
    ],
  },
  {
    id: "arusha-technical-college",
    name: "Arusha Technical College",
    shortName: "ATC",
    description: "A leading technical college offering diploma and certificate programmes in engineering, computing, and applied sciences.",
    location: "Arusha",
    faculties: [
      {
        id: "department-of-engineering",
        name: "Department of Engineering",
        description: "Mechanical, electrical, and civil engineering programmes.",
        programmes: [
          {
            id: "diploma-in-mechanical-engineering",
            name: "Diploma in Mechanical Engineering",
            degreeType: "Diploma",
            description: "Practical mechanical engineering skills for industry.",
            duration: "3 years",
            courses: [
              { id: "me-101", name: "Engineering Mechanics", code: "ME 101", description: "Statics, dynamics, and strength of materials.", credits: 3 },
              { id: "me-201", name: "Thermodynamics", code: "ME 201", description: "Heat transfer, engines, and energy systems.", credits: 3 },
              { id: "me-301", name: "Manufacturing Processes", code: "ME 301", description: "Materials processing, machining, and fabrication.", credits: 3 },
            ],
          },
          {
            id: "diploma-in-electrical-engineering",
            name: "Diploma in Electrical Engineering",
            degreeType: "Diploma",
            description: "Electrical systems, power, and instrumentation.",
            duration: "3 years",
            courses: [
              { id: "ee-101", name: "Basic Electrical Engineering", code: "EE 101", description: "Circuit analysis and electrical fundamentals.", credits: 3 },
              { id: "ee-201", name: "Electrical Machines", code: "EE 201", description: "Motors, generators, and transformers.", credits: 3 },
              { id: "ee-301", name: "Industrial Instrumentation", code: "EE 301", description: "Measurement systems and process control.", credits: 3 },
            ],
          },
          {
            id: "diploma-in-civil-engineering",
            name: "Diploma in Civil Engineering",
            degreeType: "Diploma",
            description: "Construction, surveying, and infrastructure development.",
            duration: "3 years",
            courses: [
              { id: "ce-101", name: "Building Construction", code: "CE 101", description: "Construction materials and methods.", credits: 3 },
              { id: "ce-201", name: "Surveying", code: "CE 201", description: "Land measurement and mapping.", credits: 3 },
              { id: "ce-301", name: "Structural Analysis", code: "CE 301", description: "Analysis of beams, trusses, and frames.", credits: 3 },
            ],
          },
        ],
      },
      {
        id: "department-of-computing-and-information-technology",
        name: "Department of Computing and Information Technology",
        description: "Computing, networking, and information systems programmes.",
        programmes: [
          {
            id: "diploma-in-information-technology",
            name: "Diploma in Information Technology",
            degreeType: "Diploma",
            description: "IT skills covering programming, networking, and systems administration.",
            duration: "3 years",
            courses: [
              { id: "it-101", name: "Introduction to Computing", code: "IT 101", description: "Computer fundamentals and operating systems.", credits: 3 },
              { id: "it-201", name: "Programming Fundamentals", code: "IT 201", description: "Introduction to programming using Python.", credits: 3 },
              { id: "it-301", name: "Networking Basics", code: "IT 301", description: "Network design, configuration, and troubleshooting.", credits: 3 },
            ],
          },
          {
            id: "diploma-in-computer-science",
            name: "Diploma in Computer Science",
            degreeType: "Diploma",
            description: "Computer science theory and practical software development.",
            duration: "3 years",
            courses: [
              { id: "cs-101", name: "Programming I", code: "CS 101", description: "Fundamentals of programming and problem solving.", credits: 3 },
              { id: "cs-201", name: "Data Structures", code: "CS 201", description: "Arrays, linked lists, trees, and graphs.", credits: 3 },
              { id: "cs-301", name: "Web Development", code: "CS 301", description: "HTML, CSS, JavaScript, and web frameworks.", credits: 3 },
            ],
          },
          {
            id: "diploma-in-library-and-information-science",
            name: "Diploma in Library and Information Science",
            degreeType: "Diploma",
            description: "Information management, cataloguing, and digital libraries.",
            duration: "3 years",
            courses: [
              { id: "lis-101", name: "Introduction to Library Science", code: "LIS 101", description: "Library operations and information services.", credits: 3 },
              { id: "lis-201", name: "Cataloguing and Classification", code: "LIS 201", description: "Organizing and retrieving information.", credits: 3 },
              { id: "lis-301", name: "Digital Libraries", code: "LIS 301", description: "Digital content management and preservation.", credits: 3 },
            ],
          },
        ],
      },
      {
        id: "department-of-applied-and-health-sciences",
        name: "Department of Applied and Health Sciences",
        description: "Applied sciences, laboratory technology, and health programmes.",
        programmes: [
          {
            id: "diploma-in-laboratory-technology",
            name: "Diploma in Laboratory Technology",
            degreeType: "Diploma",
            description: "Medical and analytical laboratory techniques.",
            duration: "3 years",
            courses: [
              { id: "lab-101", name: "Introduction to Laboratory Science", code: "LAB 101", description: "Laboratory safety and basic techniques.", credits: 3 },
              { id: "lab-201", name: "Clinical Chemistry", code: "LAB 201", description: "Biochemical analysis and diagnostics.", credits: 3 },
              { id: "lab-301", name: "Microbiology", code: "LAB 301", description: "Microbial identification and culture techniques.", credits: 3 },
            ],
          },
          {
            id: "diploma-in-food-science-and-technology",
            name: "Diploma in Food Science and Technology",
            degreeType: "Diploma",
            description: "Food processing, quality control, and safety.",
            duration: "3 years",
            courses: [
              { id: "fst-101", name: "Introduction to Food Science", code: "FST 101", description: "Food composition, nutrition, and safety.", credits: 3 },
              { id: "fst-201", name: "Food Processing", code: "FST 201", description: "Preservation, packaging, and processing methods.", credits: 3 },
              { id: "fst-301", name: "Quality Assurance", code: "FST 301", description: "Quality control systems and standards.", credits: 3 },
            ],
          },
          {
            id: "certificate-in-community-health",
            name: "Certificate in Community Health",
            degreeType: "Certificate",
            description: "Community health worker training and primary healthcare.",
            duration: "2 years",
            courses: [
              { id: "ch-101", name: "Community Health Foundations", code: "CH 101", description: "Primary healthcare and community health principles.", credits: 3 },
              { id: "ch-201", name: "Health Promotion", code: "CH 201", description: "Health education and behaviour change communication.", credits: 3 },
              { id: "ch-301", name: "Disease Prevention", code: "CH 301", description: "Prevention and control of communicable diseases.", credits: 3 },
            ],
          },
        ],
      },
    ],
  },
]

export function getCollegeBySlug(slug: string): College | undefined {
  return colleges.find((c) => c.id === slug)
}

export function getFacultyBySlug(college: College, facultySlug: string): CollegeFaculty | undefined {
  return college.faculties.find((f) => f.id === facultySlug)
}

export function getProgrammeBySlug(faculty: CollegeFaculty, programmeSlug: string): CollegeProgramme | undefined {
  return faculty.programmes.find((p) => p.id === programmeSlug)
}

// --- Schools page data (used by components/schools/) ---

export type NurseryLevel = "Baby" | "Middle" | "Top"
export type NurserySubject = "Reading" | "Writing" | "Counting" | "Drawing"

export const nurseryLevels = [
  { id: "Baby" as NurseryLevel, name: "Baby", ages: "2-3 yrs", desc: "First steps in learning through play and discovery" },
  { id: "Middle" as NurseryLevel, name: "Middle", ages: "3-4 yrs", desc: "Building foundations with colors, sounds and words" },
  { id: "Top" as NurseryLevel, name: "Top", ages: "4-5 yrs", desc: "Preparing for primary school adventures" },
]

export const nurserySubjects = [
  { id: "Reading" as NurserySubject, name: "Reading", desc: "Reading & phonics", icon: "BookOpen", color: "text-blue-600 bg-blue-500/10" },
  { id: "Writing" as NurserySubject, name: "Writing", desc: "Writing & letters", icon: "PenTool", color: "text-green-600 bg-green-500/10" },
  { id: "Counting" as NurserySubject, name: "Counting", desc: "Counting & numbers", icon: "Calculator", color: "text-orange-600 bg-orange-500/10" },
  { id: "Drawing" as NurserySubject, name: "Drawing", desc: "Drawing & creativity", icon: "Palette", color: "text-purple-600 bg-purple-500/10" },
]

export type NurserySchool = {
  id: string
  name: string
  location: string
  students: number
  rating: number
  description: string
}

export const nurserySchools: NurserySchool[] = [
  { id: "fortune", name: "Fortune Nursery School", location: "Dar es Salaam", students: 120, rating: 4.8, description: "Nurturing young minds through interactive play and structured learning." },
  { id: "maxmillian", name: "Maxmillian Nursery School", location: "Dodoma", students: 95, rating: 4.7, description: "Building strong foundations in literacy and numeracy for every child." },
  { id: "abc-capital", name: "ABC Capital Nursery School", location: "Arusha", students: 140, rating: 4.9, description: "Where alphabets come alive and every child discovers the joy of learning." },
  { id: "tegeta-a", name: "Tegeta A Nursery School", location: "Temeke, DSM", students: 350, rating: 4.6, description: "A well-established nursery with dedicated Baby-Middle-Top classes and experienced teachers." },
  { id: "mlimani", name: "Mlimani Nursery School", location: "Kinondoni, DSM", students: 280, rating: 4.7, description: "Creative nursery programs blending Kiswahili literacy with hands-on activities." },
]

export type PrimaryClass = {
  id: string
  name: string
  label: string
  ages: string
  subjects: string[]
}

export const primaryClasses: PrimaryClass[] = [
  { id: "darasa-1", name: "Standard 1", label: "Standard 1", ages: "6-7 yrs", subjects: ["Reading & Writing", "Mathematics", "Environmental Knowledge", "English", "Religious Education", "Arts & Sports"] },
  { id: "darasa-2", name: "Standard 2", label: "Standard 2", ages: "7-8 yrs", subjects: ["Kiswahili", "Mathematics", "English", "Science & Environmental Knowledge", "Religious Education", "Arts & Sports"] },
  { id: "darasa-3", name: "Standard 3", label: "Standard 3", ages: "8-9 yrs", subjects: ["Kiswahili", "English", "Mathematics", "Science", "History & Ethics", "Geography & Environment", "Religious Education", "Arts & Sports"] },
  { id: "darasa-4", name: "Standard 4", label: "Standard 4", ages: "9-10 yrs", subjects: ["Kiswahili", "English", "Mathematics", "Science", "History & Ethics", "Geography & Environment", "Religious Education", "Arts & Sports"] },
  { id: "darasa-5", name: "Standard 5", label: "Standard 5", ages: "10-11 yrs", subjects: ["Kiswahili", "English", "Mathematics", "Science", "History & Ethics", "Geography & Environment", "Religious Education", "Arts & Sports"] },
  { id: "darasa-6", name: "Standard 6", label: "Standard 6", ages: "11-12 yrs", subjects: ["Kiswahili", "English", "Mathematics", "Science", "History & Ethics", "Geography & Environment", "Religious Education", "Arts & Sports"] },
]

export type PrimarySchool = {
  id: string
  name: string
  location: string
  students: number
  rating: number
  description: string
}

export const primarySchools: PrimarySchool[] = [
  { id: "bunge", name: "Bunge Primary School", location: "Dodoma", students: 420, rating: 4.7, description: "A government school with strong academic performance and dedicated teachers." },
  { id: "osterbay", name: "Osterbay Primary School", location: "Dar es Salaam", students: 380, rating: 4.8, description: "Balancing academics with extracurricular activities for well-rounded growth." },
  { id: "mlimani", name: "Mlimani Primary School", location: "Kinondoni, DSM", students: 280, rating: 4.7, description: "Continuing the learning journey from nursery to primary excellence." },
  { id: "tegeta-a", name: "Tegeta A Primary School", location: "Temeke, DSM", students: 350, rating: 4.6, description: "A well-established school with structured programs from Standard 1 to 6." },
  { id: "mbezi-louis", name: "Mbezi Louis Primary School", location: "Mbezi, DSM", students: 310, rating: 4.5, description: "Growing community school focused on Kiswahili-medium instruction." },
]

export type SecondaryLevel = "O-Level" | "A-Level" | "CO-School"

export type SecondaryForm = {
  id: string
  name: string
  label: string
  level: SecondaryLevel
  ages: string
  subjects: string[]
}

export const secondaryForms: SecondaryForm[] = [
  { id: "form-1", name: "Form I", label: "Form 1", level: "O-Level", ages: "13-14 yrs", subjects: ["Kiswahili", "English Language", "Mathematics", "Physics", "Chemistry", "Biology", "Geography", "History", "Civics", "Bookkeeping", "ICT", "Fine Arts", "Food and Nutrition", "Music", "Physical Education"] },
  { id: "form-2", name: "Form II", label: "Form 2", level: "O-Level", ages: "14-15 yrs", subjects: ["Kiswahili", "English Language", "Mathematics", "Physics", "Chemistry", "Biology", "Geography", "History", "Civics", "Bookkeeping", "ICT", "Fine Arts", "Food and Nutrition", "Music", "Physical Education"] },
  { id: "form-3", name: "Form III", label: "Form 3", level: "O-Level", ages: "15-16 yrs", subjects: ["Kiswahili", "English Language", "Mathematics", "Physics", "Chemistry", "Biology", "Geography", "History", "Civics", "Bookkeeping", "ICT", "Fine Arts", "Food and Nutrition", "Music", "Physical Education", "French", "Arabic"] },
  { id: "form-4", name: "Form IV", label: "Form 4", level: "O-Level", ages: "16-17 yrs", subjects: ["Kiswahili", "English Language", "Mathematics", "Physics", "Chemistry", "Biology", "Geography", "History", "Civics", "Bookkeeping", "ICT", "Fine Arts", "Food and Nutrition", "Music", "Physical Education", "French", "Arabic"] },
  { id: "form-5", name: "Form V", label: "Form 5", level: "A-Level", ages: "17-18 yrs", subjects: [] },
  { id: "form-6", name: "Form VI", label: "Form 6", level: "A-Level", ages: "18-19 yrs", subjects: [] },
]

export type ALevelCombination = {
  id: string
  name: string
  abbreviation: string
  subjects: string[]
  description: string
}

export const aLevelCombinations: ALevelCombination[] = [
  { id: "pcm", name: "Physical Sciences", abbreviation: "PCM", subjects: ["Physics", "Chemistry", "Mathematics"], description: "For engineering, physical sciences and technology careers" },
  { id: "pcb", name: "Biological Sciences", abbreviation: "PCB", subjects: ["Physics", "Chemistry", "Biology"], description: "For medicine, pharmacy, biotechnology and life sciences" },
  { id: "hgl", name: "Arts & Humanities", abbreviation: "HGL", subjects: ["History", "Geography", "Literature in English"], description: "For law, social sciences, education and public administration" },
  { id: "eca", name: "Commerce", abbreviation: "ECA", subjects: ["Economics", "Commerce", "Accountancy"], description: "For business, finance, accounting and commerce careers" },
  { id: "pgk", name: "Agriculture", abbreviation: "PGK", subjects: ["Physics", "Geography", "Kiswahili"], description: "For agriculture, environmental science and rural development" },
  { id: "hkl", name: "Arts & Languages", abbreviation: "HKL", subjects: ["History", "Kiswahili", "Literature in English"], description: "For language studies, media, journalism and cultural studies" },
]

export type SecondarySchool = {
  id: string
  name: string
  location: string
  students: number
  rating: number
  levels: SecondaryLevel[]
  description: string
}

export const secondarySchools: SecondarySchool[] = [
  { id: "kemebos", name: "Kemebos Secondary School", location: "Moshi, Kilimanjaro", students: 850, rating: 4.8, levels: ["CO-School"], description: "A top-performing school with both O-Level and A-Level programs and excellent NECTA results." },
  { id: "josiah-girls", name: "Josiah's Girls Secondary School", location: "Dar es Salaam", students: 620, rating: 4.7, levels: ["CO-School"], description: "Empowering girls through quality education from Form I to Form VI." },
  { id: "ihungo", name: "Ihungo Secondary School", location: "Mwanza", students: 340, rating: 4.5, levels: ["A-Level"], description: "Specialized A-Level school offering PCM, PCB and HGL combinations." },
  { id: "kahororo", name: "Kahororo Secondary School", location: "Bukoba, Kagera", students: 280, rating: 4.4, levels: ["O-Level"], description: "An O-Level center with strong results in core subjects." },
  { id: "nyakato", name: "Nyakato High School", location: "Mwanza", students: 310, rating: 4.6, levels: ["A-Level"], description: "Known for excellent A-Level performance in PCM and PCB combinations." },
]

export type CollegeDepartment = {
  id: string
  name: string
  icon: string
  programs: string[]
}

export const collegeDepartments: CollegeDepartment[] = [
  { id: "engineering", name: "Engineering & Technology", icon: "Wrench", programs: ["Civil Engineering", "Electrical Engineering", "Mechanical Engineering", "Automotive Engineering", "Surveying"] },
  { id: "business", name: "Business & Management", icon: "Briefcase", programs: ["Business Administration", "Accountancy", "Procurement & Supplies", "Marketing", "Human Resource Management"] },
  { id: "ict", name: "Information & Communication Technology", icon: "Monitor", programs: ["Computer Science", "Information Systems", "Network & Cyber Security", "Software Engineering", "Data Science"] },
  { id: "health", name: "Health Sciences", icon: "Heart", programs: ["Nursing", "Pharmacy", "Laboratory Technology", "Clinical Medicine", "Public Health"] },
  { id: "education", name: "Education & Training", icon: "GraduationCap", programs: ["Science Education", "Arts Education", "Primary Education", "Educational Management", "Special Education"] },
  { id: "agriculture", name: "Agriculture & Environmental Sciences", icon: "Leaf", programs: ["Agriculture", "Environmental Science", "Food Science & Technology", "Livestock Management", "Agricultural Economics"] },
]

export type CollegeInfo = {
  id: string
  name: string
  abbreviation: string
  location: string
  students: number
  rating: number
  departments: string[]
  description: string
}

export const collegeList: CollegeInfo[] = [
  { id: "dit", name: "Dar es Salaam Institute of Technology", abbreviation: "DIT", location: "Dar es Salaam", students: 4500, rating: 4.7, departments: ["engineering", "ict", "business"], description: "A leading technical institute offering diploma and degree programs in engineering and technology." },
  { id: "cbe", name: "College of Business Education", abbreviation: "CBE", location: "Dar es Salaam", students: 3800, rating: 4.6, departments: ["business", "ict"], description: "Specializing in business education with programs in accountancy, marketing and management." },
  { id: "ifm", name: "Institute of Finance Management", abbreviation: "IFM", location: "Dar es Salaam", students: 3200, rating: 4.8, departments: ["business", "ict"], description: "Tanzania's premier institution for finance, banking and insurance education." },
  { id: "tit", name: "Tanzania Institute of Transport", abbreviation: "TIT", location: "Dar es Salaam", students: 2100, rating: 4.4, departments: ["engineering"], description: "Focused on transport logistics, maritime studies and automotive engineering." },
  { id: "must", name: "Mbeya University of Science and Technology", abbreviation: "MUST", location: "Mbeya", students: 5200, rating: 4.5, departments: ["engineering", "ict", "education", "agriculture"], description: "A full university offering science, technology and education programs at diploma and degree level." },
]

export type VetaCategory = {
  id: string
  name: string
  icon: string
  trades: { id: string; name: string; icon: string; duration: string; level: string }[]
}

export const vetaCategories: VetaCategory[] = [
  { id: "construction", name: "Construction & Building", icon: "Hammer", trades: [
    { id: "carpentry", name: "Carpentry & Joinery", icon: "Hammer", duration: "2 years", level: "Trade Certificate" },
    { id: "masonry", name: "Masonry & Bricklaying", icon: "Hammer", duration: "2 years", level: "Trade Certificate" },
    { id: "plumbing", name: "Plumbing & Pipe Fitting", icon: "Hammer", duration: "2 years", level: "Trade Certificate" },
    { id: "painting", name: "Painting & Decorating", icon: "Hammer", duration: "1 year", level: "Trade Certificate" },
  ]},
  { id: "automotive", name: "Automotive & Mechanical", icon: "Car", trades: [
    { id: "auto-mechanics", name: "Automotive Mechanics", icon: "Car", duration: "2 years", level: "Trade Certificate" },
    { id: "welding", name: "Welding & Fabrication", icon: "Car", duration: "2 years", level: "Trade Certificate" },
    { id: "metal-fabrication", name: "Metal Fabrication", icon: "Car", duration: "2 years", level: "Trade Certificate" },
  ]},
  { id: "electrical", name: "Electrical & Electronics", icon: "Zap", trades: [
    { id: "electrical-installation", name: "Electrical Installation", icon: "Zap", duration: "2 years", level: "Trade Certificate" },
    { id: "electronics", name: "Electronics & Telecommunication", icon: "Zap", duration: "2 years", level: "Trade Certificate" },
  ]},
  { id: "ict", name: "ICT & Digital Skills", icon: "Monitor", trades: [
    { id: "ict-technician", name: "ICT Technician", icon: "Monitor", duration: "2 years", level: "Trade Certificate" },
    { id: "computer-networking", name: "Computer Networking", icon: "Monitor", duration: "1 year", level: "Trade Certificate" },
    { id: "graphic-design", name: "Graphic Design & Multimedia", icon: "Monitor", duration: "1 year", level: "Trade Certificate" },
  ]},
  { id: "hospitality", name: "Hospitality & Tourism", icon: "ChefHat", trades: [
    { id: "food-beverage", name: "Food & Beverage Production", icon: "ChefHat", duration: "2 years", level: "Trade Certificate" },
    { id: "hotel-management", name: "Hotel Management", icon: "ChefHat", duration: "2 years", level: "Trade Certificate" },
    { id: "tourism", name: "Tourism & Wildlife Management", icon: "ChefHat", duration: "2 years", level: "Trade Certificate" },
  ]},
  { id: "beauty", name: "Beauty & Personal Care", icon: "Scissors", trades: [
    { id: "hairdressing", name: "Hairdressing & Beauty Therapy", icon: "Scissors", duration: "1 year", level: "Trade Certificate" },
    { id: "tailoring", name: "Tailoring & Dressmaking", icon: "Scissors", duration: "2 years", level: "Trade Certificate" },
  ]},
]

export type VetaCenter = {
  id: string
  name: string
  abbreviation: string
  location: string
  students: number
  rating: number
  categories: string[]
  description: string
}

export const vetaCenters: VetaCenter[] = [
  { id: "dar-rvtsc", name: "Dar es Salaam Regional Vocational Training and Service Centre", abbreviation: "Dar RVTSC", location: "Dar es Salaam", students: 1800, rating: 4.7, categories: ["construction", "automotive", "electrical"], description: "One of Tanzania's largest VETA centers offering hands-on training in construction, automotive and electrical trades." },
  { id: "kipawa-ict", name: "Kipawa ICT Centre", abbreviation: "Kipawa ICT", location: "Dar es Salaam", students: 650, rating: 4.6, categories: ["ict"], description: "Specialized ICT training center focusing on computer hardware, networking and digital skills." },
  { id: "arusha-vtc", name: "Arusha Vocational Training Centre", abbreviation: "Arusha VTC", location: "Arusha", students: 920, rating: 4.5, categories: ["hospitality", "construction", "beauty"], description: "Serving the northern tourism corridor with hospitality, construction and beauty trade programs." },
  { id: "tanga-rvtsc", name: "Tanga Regional Vocational Training and Service Centre", abbreviation: "Tanga RVTSC", location: "Tanga", students: 740, rating: 4.4, categories: ["automotive", "construction", "electrical"], description: "Regional center offering trade certificates in automotive, construction and electrical installation." },
  { id: "moshi-rvtsc", name: "Moshi Regional Vocational Training and Service Centre", abbreviation: "Moshi RVTSC", location: "Moshi, Kilimanjaro", students: 810, rating: 4.5, categories: ["hospitality", "agriculture", "construction"], description: "Training center near Kilimanjaro with hospitality and agriculture trade programs." },
]

export type DegreeLevel = {
  id: string
  name: string
  duration: string
  icon: string
  description: string
}

export const degreeLevels: DegreeLevel[] = [
  { id: "diploma", name: "Diploma", duration: "2-3 years", icon: "Award", description: "Specialized technical and professional training at undergraduate level" },
  { id: "bachelors", name: "Bachelor's Degree", duration: "3-4 years", icon: "GraduationCap", description: "Undergraduate degree programs across arts, science, engineering and more" },
  { id: "masters", name: "Master's Degree", duration: "1-2 years", icon: "BookOpen", description: "Postgraduate specialization and research-based advanced study" },
  { id: "phd", name: "PhD / Doctorate", duration: "3-4 years", icon: "Flame", description: "Original research contributing to new knowledge in your field" },
]

export type University = {
  id: string
  name: string
  abbreviation: string
  location: string
  students: number
  rating: number
  degreeLevels: string[]
  programs: string[]
  description: string
}

export const universities: University[] = [
  { id: "udsm", name: "University of Dar es Salaam", abbreviation: "UDSM", location: "Dar es Salaam", students: 35000, rating: 4.8, degreeLevels: ["diploma", "bachelors", "masters", "phd"], programs: ["Arts & Social Sciences", "Science", "Engineering", "Law", "Medicine", "Business", "Education"], description: "Tanzania's oldest and largest university, offering a wide range of programs across multiple faculties." },
  { id: "udom", name: "University of Dodoma", abbreviation: "UDOM", location: "Dodoma", students: 25000, rating: 4.6, degreeLevels: ["bachelors", "masters"], programs: ["Education", "Science & Technology", "Health Sciences", "Business", "Humanities", "Agriculture"], description: "Tanzania's fastest-growing public university with modern facilities and a wide range of programs." },
  { id: "muhas", name: "Muhimbili University of Health and Allied Sciences", abbreviation: "MUHAS", location: "Dar es Salaam", students: 8000, rating: 4.9, degreeLevels: ["bachelors", "masters", "phd"], programs: ["Medicine", "Nursing", "Pharmacy", "Dentistry", "Public Health", "Laboratory Sciences"], description: "East Africa's leading health sciences university, training doctors, nurses and health professionals." },
  { id: "aru", name: "Ardhi University", abbreviation: "ARU", location: "Dar es Salaam", students: 12000, rating: 4.7, degreeLevels: ["diploma", "bachelors", "masters"], programs: ["Surveying", "Urban Planning", "Real Estate", "Construction Management", "Environmental Science", "Geomatics"], description: "Specializing in land, built environment and geospatial sciences since 1956." },
  { id: "nm-aist", name: "Nelson Mandela African Institution of Science and Technology", abbreviation: "NM-AIST", location: "Arusha", students: 3500, rating: 4.7, degreeLevels: ["masters", "phd"], programs: ["Computing & Informatics", "Life Sciences", "Physical Sciences", "Engineering", "Mathematics", "Infrastructure"], description: "A postgraduate-only institution focused on science, technology and innovation for Africa's development." },
]

// --- Unified institution data model ---

export type Institution = {
  id: string
  name: string
  educationLevel: "Nursery" | "Primary" | "Secondary" | "Colleges" | "Vocational" | "Universities"
  description: string
  location: string
  images: string[]
  ownership: "Government" | "Private"
  verified: boolean
  students: number
  teachers?: number
  rating: number
  phone?: string
  email?: string
  website?: string
  facilities: string[]
  programs?: string[]
  admissionUrl?: string
  secondaryLevel?: "O-Level" | "A-Level" | "CO-School"
}

export const institutions: Institution[] = [
  // --- Nursery (5) ---
  {
    id: "fortune",
    name: "Fortune Nursery School",
    educationLevel: "Nursery",
    description: "Nurturing young minds through interactive play and structured learning.",
    location: "Dar es Salaam",
    images: ["/images/school-1.jpg", "/images/school-2.jpg", "/images/school-3.jpg"],
    ownership: "Private",
    verified: true,
    students: 120,
    teachers: 18,
    rating: 4.8,
    phone: "+255 22 212 3456",
    email: "info@fortunenursery.tz",
    facilities: ["Playground", "Outdoor Learning Area", "Art Room", "Story Corner"],
  },
  {
    id: "maxmillian",
    name: "Maxmillian Nursery School",
    educationLevel: "Nursery",
    description: "Building strong foundations in literacy and numeracy for every child.",
    location: "Dodoma",
    images: ["/images/school-4.jpg", "/images/school-5.jpg", "/images/school-6.jpg"],
    ownership: "Private",
    verified: true,
    students: 95,
    teachers: 14,
    rating: 4.7,
    phone: "+255 26 233 7890",
    email: "info@maxmillian.tz",
    facilities: ["Colorful Classrooms", "Music Room", "Small Library", "Safe Outdoor Play Zone"],
  },
  {
    id: "abc-capital",
    name: "ABC Capital Nursery School",
    educationLevel: "Nursery",
    description: "Where alphabets come alive and every child discovers the joy of learning.",
    location: "Arusha",
    images: ["/images/school-7.jpg", "/images/school-8.jpg", "/images/school-9.jpg"],
    ownership: "Private",
    verified: true,
    students: 140,
    teachers: 20,
    rating: 4.9,
    phone: "+255 27 254 1234",
    email: "info@abccapital.tz",
    facilities: ["Interactive Smart Board", "Sensory Play Area", "Garden Classroom", "Parent Lounge"],
  },
  {
    id: "tegeta-a",
    name: "Tegeta A Nursery School",
    educationLevel: "Nursery",
    description: "A well-established nursery with dedicated Baby-Middle-Top classes and experienced teachers.",
    location: "Temeke, DSM",
    images: ["/images/school-10.jpg", "/images/school-11.jpg", "/images/school-12.jpg", "/images/school-13.jpg"],
    ownership: "Private",
    verified: true,
    students: 350,
    teachers: 32,
    rating: 4.6,
    phone: "+255 22 285 5678",
    email: "info@tegeta-nursery.tz",
    facilities: ["Spacious Classrooms", "Computer Lab", "Dance Studio", "Feeding Area"],
  },
  {
    id: "mlimani",
    name: "Mlimani Nursery School",
    educationLevel: "Nursery",
    description: "Creative nursery programs blending Kiswahili literacy with hands-on activities.",
    location: "Kinondoni, DSM",
    images: ["/images/school-14.jpg", "/images/school-15.jpg", "/images/school-16.jpg"],
    ownership: "Private",
    verified: true,
    students: 280,
    teachers: 26,
    rating: 4.7,
    phone: "+255 22 277 3456",
    email: "info@mlimani-nursery.tz",
    facilities: ["Storytelling Hall", "Art & Craft Room", "Nature Playground", "Computer Corner"],
  },

  // --- Primary (5) ---
  {
    id: "bunge",
    name: "Bunge Primary School",
    educationLevel: "Primary",
    description: "A government school with strong academic performance and dedicated teachers.",
    location: "Dodoma",
    images: ["/images/school-17.jpg", "/images/school-18.jpg", "/images/school-19.jpg"],
    ownership: "Government",
    verified: true,
    students: 420,
    teachers: 28,
    rating: 4.7,
    phone: "+255 26 233 2000",
    email: "info@bungeprimary.tz",
    facilities: ["Science Laboratory", "Computer Room", "Library", "Sports Field"],
  },
  {
    id: "osterbay",
    name: "Osterbay Primary School",
    educationLevel: "Primary",
    description: "Balancing academics with extracurricular activities for well-rounded growth.",
    location: "Dar es Salaam",
    images: ["/images/school-20.jpg", "/images/school-21.jpg", "/images/school-22.jpg"],
    ownership: "Private",
    verified: true,
    students: 380,
    teachers: 30,
    rating: 4.8,
    phone: "+255 22 215 7890",
    email: "info@osterbay.tz",
    facilities: ["Fully Equipped Labs", "Music Hall", "Swimming Pool", "Tuck Shop"],
  },
  {
    id: "mlimani",
    name: "Mlimani Primary School",
    educationLevel: "Primary",
    description: "Continuing the learning journey from nursery to primary excellence.",
    location: "Kinondoni, DSM",
    images: ["/images/school-23.jpg", "/images/school-24.jpg", "/images/school-25.jpg"],
    ownership: "Private",
    verified: true,
    students: 280,
    teachers: 22,
    rating: 4.7,
    phone: "+255 22 277 4000",
    email: "info@mlimani-primary.tz",
    facilities: ["Digital Library", "Playground", "Cafeteria", "Assembly Hall"],
  },
  {
    id: "tegeta-a",
    name: "Tegeta A Primary School",
    educationLevel: "Primary",
    description: "A well-established school with structured programs from Standard 1 to 6.",
    location: "Temeke, DSM",
    images: ["/images/school-26.jpg", "/images/school-27.jpg", "/images/school-28.jpg", "/images/school-29.jpg"],
    ownership: "Private",
    verified: true,
    students: 350,
    teachers: 26,
    rating: 4.6,
    phone: "+255 22 285 6000",
    email: "info@tegeta-primary.tz",
    facilities: ["Laboratories", "ICT Room", "School Farm", "Sports Courts"],
  },
  {
    id: "mbezi-louis",
    name: "Mbezi Louis Primary School",
    educationLevel: "Primary",
    description: "Growing community school focused on Kiswahili-medium instruction.",
    location: "Mbezi, DSM",
    images: ["/images/school-30.jpg", "/images/school-31.jpg", "/images/school-32.jpg"],
    ownership: "Government",
    verified: true,
    students: 310,
    teachers: 20,
    rating: 4.5,
    phone: "+255 22 271 8901",
    email: "info@mbezilouis.tz",
    facilities: ["Open-air Classroom", "Playground", "Feeding Program Kitchen", "Community Hall"],
  },

  // --- Secondary (5) ---
  {
    id: "kemebos",
    name: "Kemebos Secondary School",
    educationLevel: "Secondary",
    description: "A top-performing school with both O-Level and A-Level programs and excellent NECTA results.",
    location: "Moshi, Kilimanjaro",
    images: ["/images/school-33.jpg", "/images/school-34.jpg", "/images/school-35.jpg", "/images/school-36.jpg"],
    ownership: "Government",
    verified: true,
    students: 850,
    teachers: 52,
    rating: 4.8,
    phone: "+255 27 275 2000",
    email: "info@kemebos.tz",
    facilities: ["Science Labs", "Computer Lab", "Library", "Sports Complex", "Dormitories"],
    secondaryLevel: "CO-School",
  },
  {
    id: "josiah-girls",
    name: "Josiah's Girls Secondary School",
    educationLevel: "Secondary",
    description: "Empowering girls through quality education from Form I to Form VI.",
    location: "Dar es Salaam",
    images: ["/images/school-37.jpg", "/images/school-38.jpg", "/images/school-39.jpg"],
    ownership: "Private",
    verified: true,
    students: 620,
    teachers: 45,
    rating: 4.7,
    phone: "+255 22 216 5678",
    email: "info@josiahgirls.tz",
    facilities: ["Modern Classrooms", "ICT Center", "Library", "Boarding Facilities", "Health Clinic"],
    secondaryLevel: "CO-School",
  },
  {
    id: "ihungo",
    name: "Ihungo Secondary School",
    educationLevel: "Secondary",
    description: "Specialized A-Level school offering PCM, PCB and HGL combinations.",
    location: "Mwanza",
    images: ["/images/school-40.jpg", "/images/school-41.jpg", "/images/school-42.jpg"],
    ownership: "Government",
    verified: true,
    students: 340,
    teachers: 28,
    rating: 4.5,
    phone: "+255 28 280 3000",
    email: "info@ihungo.tz",
    facilities: ["Physics Lab", "Chemistry Lab", "Biology Lab", "Study Rooms", "Sports Field"],
    secondaryLevel: "A-Level",
  },
  {
    id: "kahororo",
    name: "Kahororo Secondary School",
    educationLevel: "Secondary",
    description: "An O-Level center with strong results in core subjects.",
    location: "Bukoba, Kagera",
    images: ["/images/school-43.jpg", "/images/school-44.jpg", "/images/school-45.jpg"],
    ownership: "Government",
    verified: true,
    students: 280,
    teachers: 24,
    rating: 4.4,
    phone: "+255 28 222 4000",
    email: "info@kahororo.tz",
    facilities: ["Science Laboratories", "Library", "Computer Room", "Assembly Hall"],
    secondaryLevel: "O-Level",
  },
  {
    id: "nyakato",
    name: "Nyakato High School",
    educationLevel: "Secondary",
    description: "Known for excellent A-Level performance in PCM and PCB combinations.",
    location: "Mwanza",
    images: ["/images/school-46.jpg", "/images/school-47.jpg", "/images/school-48.jpg"],
    ownership: "Government",
    verified: true,
    students: 310,
    teachers: 26,
    rating: 4.6,
    phone: "+255 28 280 5000",
    email: "info@nyakato.tz",
    facilities: ["Well-Equipped Labs", "Library", "Sports Ground", "Dormitories"],
    secondaryLevel: "A-Level",
  },

  // --- Colleges (5) ---
  {
    id: "dit",
    name: "Dar es Salaam Institute of Technology",
    educationLevel: "Colleges",
    description: "A leading technical institute offering diploma and degree programs in engineering and technology.",
    location: "Dar es Salaam",
    images: ["/images/school-49.jpg", "/images/school-50.jpg", "/images/school-51.jpg"],
    ownership: "Government",
    verified: true,
    students: 4500,
    teachers: 180,
    rating: 4.7,
    phone: "+255 22 215 1000",
    email: "info@dit.ac.tz",
    website: "https://www.dit.ac.tz",
    facilities: ["Engineering Labs", "Workshop", "Library", "ICT Center", "Cafeteria"],
    programs: ["Civil Engineering", "Electrical Engineering", "Mechanical Engineering", "Computer Science", "Business Administration"],
  },
  {
    id: "cbe",
    name: "College of Business Education",
    educationLevel: "Colleges",
    description: "Specializing in business education with programs in accountancy, marketing and management.",
    location: "Dar es Salaam",
    images: ["/images/school-52.jpg", "/images/school-53.jpg", "/images/school-54.jpg"],
    ownership: "Government",
    verified: true,
    students: 3800,
    teachers: 150,
    rating: 4.6,
    phone: "+255 22 212 8000",
    email: "info@cbe.ac.tz",
    website: "https://www.cbe.ac.tz",
    facilities: ["Lecture Halls", "Computer Lab", "Library", "Business Incubator"],
    programs: ["Accountancy", "Marketing", "Human Resource Management", "Procurement & Supplies", "Banking & Finance"],
  },
  {
    id: "ifm",
    name: "Institute of Finance Management",
    educationLevel: "Colleges",
    description: "Tanzania's premier institution for finance, banking and insurance education.",
    location: "Dar es Salaam",
    images: ["/images/school-55.jpg", "/images/school-56.jpg", "/images/school-57.jpg", "/images/school-58.jpg"],
    ownership: "Government",
    verified: true,
    students: 3200,
    teachers: 140,
    rating: 4.8,
    phone: "+255 22 211 3000",
    email: "info@ifm.ac.tz",
    website: "https://www.ifm.ac.tz",
    facilities: ["Finance Trading Room", "Library", "ICT Center", "Conference Hall", "Hostels"],
    programs: ["Accountancy", "Banking & Insurance", "Finance & Management", "Taxation", "Information Technology"],
  },
  {
    id: "tit",
    name: "Tanzania Institute of Transport",
    educationLevel: "Colleges",
    description: "Focused on transport logistics, maritime studies and automotive engineering.",
    location: "Dar es Salaam",
    images: ["/images/school-59.jpg", "/images/school-60.jpg", "/images/school-61.jpg"],
    ownership: "Government",
    verified: true,
    students: 2100,
    teachers: 95,
    rating: 4.4,
    phone: "+255 22 286 2000",
    email: "info@tit.ac.tz",
    facilities: ["Automotive Workshop", "Maritime Simulator", "Library", "Logistics Lab"],
    programs: ["Automotive Engineering", "Logistics & Transport Management", "Maritime Studies", "Supply Chain Management"],
  },
  {
    id: "must",
    name: "Mbeya University of Science and Technology",
    educationLevel: "Colleges",
    description: "A full university offering science, technology and education programs at diploma and degree level.",
    location: "Mbeya",
    images: ["/images/school-62.jpg", "/images/school-63.jpg", "/images/school-64.jpg"],
    ownership: "Government",
    verified: true,
    students: 5200,
    teachers: 220,
    rating: 4.5,
    phone: "+255 25 250 2000",
    email: "info@must.ac.tz",
    website: "https://www.must.ac.tz",
    facilities: ["Science Labs", "Engineering Workshop", "Library", "ICT Center", "Sports Complex"],
    programs: ["Civil Engineering", "Information Technology", "Education", "Agriculture", "Applied Sciences"],
  },

  // --- Vocational (5) ---
  {
    id: "dar-rvtsc",
    name: "Dar es Salaam Regional Vocational Training and Service Centre",
    educationLevel: "Vocational",
    description: "One of Tanzania's largest VETA centers offering hands-on training in construction, automotive and electrical trades.",
    location: "Dar es Salaam",
    images: ["/images/school-65.jpg", "/images/school-66.jpg", "/images/school-67.jpg"],
    ownership: "Government",
    verified: true,
    students: 1800,
    teachers: 85,
    rating: 4.7,
    phone: "+255 22 284 3000",
    email: "info@dar-rvtsc.tz",
    facilities: ["Automotive Workshop", "Electrical Lab", "Construction Yard", "Hostels", "Cafeteria"],
    programs: ["Carpentry & Joinery", "Masonry", "Plumbing", "Automotive Mechanics", "Welding", "Electrical Installation"],
  },
  {
    id: "kipawa-ict",
    name: "Kipawa ICT Centre",
    educationLevel: "Vocational",
    description: "Specialized ICT training center focusing on computer hardware, networking and digital skills.",
    location: "Dar es Salaam",
    images: ["/images/school-68.jpg", "/images/school-69.jpg", "/images/school-70.jpg"],
    ownership: "Government",
    verified: true,
    students: 650,
    teachers: 35,
    rating: 4.6,
    phone: "+255 22 232 7000",
    email: "info@kipawa-ict.tz",
    facilities: ["Computer Lab", "Networking Lab", "Digital Library", "Training Rooms"],
    programs: ["ICT Technician", "Computer Networking", "Graphic Design & Multimedia", "Software Development"],
  },
  {
    id: "arusha-vtc",
    name: "Arusha Vocational Training Centre",
    educationLevel: "Vocational",
    description: "Serving the northern tourism corridor with hospitality, construction and beauty trade programs.",
    location: "Arusha",
    images: ["/images/school-71.jpg", "/images/school-72.jpg", "/images/school-73.jpg"],
    ownership: "Government",
    verified: true,
    students: 920,
    teachers: 48,
    rating: 4.5,
    phone: "+255 27 254 6000",
    email: "info@arushavtc.tz",
    facilities: ["Training Kitchen", "Construction Workshop", "Beauty Salon", "Hostels"],
    programs: ["Food & Beverage Production", "Hotel Management", "Carpentry", "Masonry", "Hairdressing"],
  },
  {
    id: "tanga-rvtsc",
    name: "Tanga Regional Vocational Training and Service Centre",
    educationLevel: "Vocational",
    description: "Regional center offering trade certificates in automotive, construction and electrical installation.",
    location: "Tanga",
    images: ["/images/school-74.jpg", "/images/school-75.jpg", "/images/school-76.jpg"],
    ownership: "Government",
    verified: true,
    students: 740,
    teachers: 40,
    rating: 4.4,
    phone: "+255 27 264 4000",
    email: "info@tanga-rvtsc.tz",
    facilities: ["Automotive Workshop", "Electrical Lab", "Construction Yard", "Admin Block"],
    programs: ["Automotive Mechanics", "Welding & Fabrication", "Electrical Installation", "Carpentry & Joinery"],
  },
  {
    id: "moshi-rvtsc",
    name: "Moshi Regional Vocational Training and Service Centre",
    educationLevel: "Vocational",
    description: "Training center near Kilimanjaro with hospitality and agriculture trade programs.",
    location: "Moshi, Kilimanjaro",
    images: ["/images/school-77.jpg", "/images/school-78.jpg", "/images/school-79.jpg"],
    ownership: "Government",
    verified: true,
    students: 810,
    teachers: 42,
    rating: 4.5,
    phone: "+255 27 275 8000",
    email: "info@moshi-rvtsc.tz",
    facilities: ["Training Kitchen", "Farm Workshop", "Construction Lab", "Hostels", "Cafeteria"],
    programs: ["Hotel Management", "Tourism & Wildlife Management", "Agriculture", "Carpentry", "Plumbing"],
  },

  // --- Universities (5) ---
  {
    id: "udsm",
    name: "University of Dar es Salaam",
    educationLevel: "Universities",
    description: "Tanzania's oldest and largest university, offering a wide range of programs across multiple faculties.",
    location: "Dar es Salaam",
    images: ["/images/school-80.jpg", "/images/school-81.jpg", "/images/school-82.jpg", "/images/school-83.jpg"],
    ownership: "Government",
    verified: true,
    students: 35000,
    teachers: 1200,
    rating: 4.8,
    phone: "+255 22 241 0000",
    email: "info@udsm.ac.tz",
    website: "https://www.udsm.ac.tz",
    facilities: ["Central Library", "Research Labs", "ICT Center", "Sports Complex", "Medical Center", "Hostels"],
    programs: ["Arts & Social Sciences", "Science", "Engineering", "Law", "Medicine", "Business", "Education"],
  },
  {
    id: "udom",
    name: "University of Dodoma",
    educationLevel: "Universities",
    description: "Tanzania's fastest-growing public university with modern facilities and a wide range of programs.",
    location: "Dodoma",
    images: ["/images/school-84.jpg", "/images/school-85.jpg", "/images/school-86.jpg"],
    ownership: "Government",
    verified: true,
    students: 25000,
    teachers: 900,
    rating: 4.6,
    phone: "+255 26 231 0000",
    email: "info@udom.ac.tz",
    website: "https://www.udom.ac.tz",
    facilities: ["Modern Library", "Science Laboratories", "ICT Center", "Sports Stadium", "Hostels"],
    programs: ["Education", "Science & Technology", "Health Sciences", "Business", "Humanities", "Agriculture"],
  },
  {
    id: "muhas",
    name: "Muhimbili University of Health and Allied Sciences",
    educationLevel: "Universities",
    description: "East Africa's leading health sciences university, training doctors, nurses and health professionals.",
    location: "Dar es Salaam",
    images: ["/images/school-87.jpg", "/images/school-88.jpg", "/images/school-89.jpg"],
    ownership: "Government",
    verified: true,
    students: 8000,
    teachers: 600,
    rating: 4.9,
    phone: "+255 22 215 0000",
    email: "info@muhas.ac.tz",
    website: "https://www.muhas.ac.tz",
    facilities: ["Teaching Hospital", "Medical Labs", "Pharmacy Lab", "Nursing Simulation Center", "Library"],
    programs: ["Medicine", "Nursing", "Pharmacy", "Dentistry", "Public Health", "Laboratory Sciences"],
  },
  {
    id: "aru",
    name: "Ardhi University",
    educationLevel: "Universities",
    description: "Specializing in land, built environment and geospatial sciences since 1956.",
    location: "Dar es Salaam",
    images: ["/images/school-90.jpg", "/images/school-91.jpg", "/images/school-92.jpg"],
    ownership: "Government",
    verified: true,
    students: 12000,
    teachers: 450,
    rating: 4.7,
    phone: "+255 22 277 5000",
    email: "info@aru.ac.tz",
    website: "https://www.aru.ac.tz",
    facilities: ["Geomatics Lab", "Surveying Equipment", "GIS Center", "Library", "Construction Workshop"],
    programs: ["Surveying", "Urban Planning", "Real Estate", "Construction Management", "Environmental Science", "Geomatics"],
  },
  {
    id: "nm-aist",
    name: "Nelson Mandela African Institution of Science and Technology",
    educationLevel: "Universities",
    description: "A postgraduate-only institution focused on science, technology and innovation for Africa's development.",
    location: "Arusha",
    images: ["/images/school-93.jpg", "/images/school-94.jpg", "/images/school-95.jpg"],
    ownership: "Government",
    verified: true,
    students: 3500,
    teachers: 200,
    rating: 4.7,
    phone: "+255 27 254 9000",
    email: "info@nmaist.ac.tz",
    website: "https://www.nmaist.ac.tz",
    facilities: ["Research Labs", "Supercomputing Center", "Innovation Hub", "Conference Center", "Hostels"],
    programs: ["Computing & Informatics", "Life Sciences", "Physical Sciences", "Engineering", "Mathematics", "Infrastructure"],
  },
]

export function getInstitutionsByLevel(level: Institution["educationLevel"]): Institution[] {
  return institutions.filter((i) => i.educationLevel === level)
}

export function getInstitutionById(id: string): Institution | undefined {
  return institutions.find((i) => i.id === id)
}

// --- Study Materials (used by notes library) ---

export type MaterialType = "notes" | "slides" | "formula-sheet" | "practice"
export type MaterialAccess = "view" | "download"

export type StudyMaterial = {
  id: string
  title: string
  subject: string
  level: string
  type: MaterialType
  access: MaterialAccess
  instructor: string
  pages?: number
}

export const studyMaterials: StudyMaterial[] = [
  { id: "mat-000", title: "Alphabet & Number Tracing Workbook", subject: "Early Learning", level: "Nursery", type: "practice", access: "download", instructor: "Ms. Amina H.", pages: 16 },
  { id: "mat-001", title: "Calculus II — Derivatives & Integrals", subject: "Mathematics", level: "College", type: "notes", access: "view", instructor: "Dr. John Mwangi", pages: 24 },
  { id: "mat-002", title: "Physics Mechanics Formula Sheet", subject: "Physics", level: "Secondary", type: "formula-sheet", access: "download", instructor: "Prof. A. Hamdan", pages: 4 },
  { id: "mat-003", title: "Python Data Analysis Guide", subject: "Computer Science", level: "College", type: "notes", access: "view", instructor: "Grace N.", pages: 32 },
  { id: "mat-004", title: "HTML & CSS Cheat Sheet", subject: "Computer Science", level: "Secondary", type: "notes", access: "download", instructor: "David O.", pages: 6 },
  { id: "mat-005", title: "Digital Marketing Fundamentals", subject: "Business Studies", level: "College", type: "slides", access: "view", instructor: "Sarah K.", pages: 48 },
  { id: "mat-006", title: "Statistics Practice Problems", subject: "Mathematics", level: "College", type: "practice", access: "download", instructor: "Dr. John Mwangi", pages: 12 },
  { id: "mat-007", title: "Chemistry Lab Report Template", subject: "Science", level: "Secondary", type: "practice", access: "download", instructor: "Prof. A. Hamdan", pages: 3 },
  { id: "mat-008", title: "Business Strategy & Planning Notes", subject: "Business Studies", level: "University", type: "notes", access: "view", instructor: "Sarah K.", pages: 18 },
  { id: "mat-009", title: "English Grammar & Composition", subject: "English", level: "Primary", type: "notes", access: "download", instructor: "Mr. James O.", pages: 14 },
  { id: "mat-010", title: "Kiswahili Literature & Poetry", subject: "Kiswahili", level: "Secondary", type: "notes", access: "view", instructor: "Mwalimu Rehema", pages: 20 },
  { id: "mat-011", title: "Chemistry Organic Compounds Guide", subject: "Chemistry", level: "Secondary", type: "notes", access: "view", instructor: "Dr. Peter M.", pages: 22 },
  { id: "mat-012", title: "Biology Cell Structure & Function", subject: "Biology", level: "Secondary", type: "slides", access: "download", instructor: "Dr. Grace L.", pages: 36 },
  { id: "mat-013", title: "East African Geography Atlas Notes", subject: "Geography", level: "Secondary", type: "notes", access: "view", instructor: "Mr. Julius K.", pages: 28 },
  { id: "mat-014", title: "World History — Colonialism & Independence", subject: "History", level: "Secondary", type: "notes", access: "download", instructor: "Mr. Julius K.", pages: 16 },
  { id: "mat-015", title: "Civic Education & Governance", subject: "Civic Education", level: "Secondary", type: "slides", access: "download", instructor: "Ms. Fatima A.", pages: 30 },
  { id: "mat-016", title: "Christian Religious Studies — Old Testament", subject: "Religious Studies", level: "Primary", type: "notes", access: "download", instructor: "Mr. Daniel W.", pages: 10 },
  { id: "mat-017", title: "Islamic Education — Five Pillars", subject: "Religious Studies", level: "Secondary", type: "notes", access: "view", instructor: "Sheikh Omar M.", pages: 8 },
]
