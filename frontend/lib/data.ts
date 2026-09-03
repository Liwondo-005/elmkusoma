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
  { name: "Nursery", desc: "Games & interactive learning", href: "/schools/nursery" },
  { name: "Primary", desc: "Classes, subjects & quizzes", href: "/schools/primary" },
  { name: "Secondary", desc: "O-Level & A-Level programs", href: "/schools/secondary" },
  { name: "College", desc: "Departments & programs", href: "/schools/colleges" },
  { name: "Vocational", desc: "VETA training & modules", href: "/schools/vocational" },
  { name: "University", desc: "Faculties & degree programs", href: "/schools/universities" },
]

export const stats = [
  { value: "8,500+", label: "Active Learners" },
  { value: "500+", label: "Expert Instructors" },
  { value: "1,200+", label: "Courses & Classes" },
  { value: "50+", label: "Partner Institutions" },
]

export type Certificate = {
  id: string
  studentName: string
  courseTitle: string
  instructor: string
  completionDate: string
  grade: string
  skills: string[]
}

export const certificates: Certificate[] = [
  {
    id: "ELM-CERT-2026-001",
    studentName: "Asha M.",
    courseTitle: "HTML & CSS Basics",
    instructor: "David O.",
    completionDate: "August 28, 2026",
    grade: "Distinction",
    skills: ["HTML5", "CSS3", "Responsive Design", "Flexbox", "Grid"],
  },
  {
    id: "ELM-CERT-2026-002",
    studentName: "Asha M.",
    courseTitle: "Data Science with Python",
    instructor: "Grace N.",
    completionDate: "August 15, 2026",
    grade: "Merit",
    skills: ["Python", "Pandas", "NumPy", "Data Visualization", "Statistics"],
  },
  {
    id: "ELM-CERT-2026-003",
    studentName: "John K.",
    courseTitle: "Digital Marketing Strategy",
    instructor: "Sarah K.",
    completionDate: "July 30, 2026",
    grade: "Distinction",
    skills: ["SEO", "Social Media Marketing", "Content Strategy", "Analytics"],
  },
]

export function verifyCertificate(id: string): Certificate | undefined {
  return certificates.find((cert) => cert.id.toLowerCase() === id.trim().toLowerCase())
}

export type MaterialType = "notes" | "slides" | "formula-sheet" | "practice"
export type MaterialAccess = "view" | "download"
export type EducationLevel = "Nursery" | "Primary" | "Secondary" | "College" | "University"

export type StudyMaterial = {
  id: string
  title: string
  subject: string
  level: EducationLevel
  type: MaterialType
  access: MaterialAccess
  instructor: string
  pages?: number
}

export const studyMaterials: StudyMaterial[] = [
  {
    id: "mat-000",
    title: "Alphabet & Number Tracing Workbook",
    subject: "Early Learning",
    level: "Nursery",
    type: "practice",
    access: "download",
    instructor: "Ms. Amina H.",
    pages: 16,
  },
  {
    id: "mat-001",
    title: "Calculus II — Derivatives & Integrals",
    subject: "Mathematics",
    level: "College",
    type: "notes",
    access: "view",
    instructor: "Dr. John Mwangi",
    pages: 24,
  },
  {
    id: "mat-002",
    title: "Physics Mechanics Formula Sheet",
    subject: "Physics",
    level: "Secondary",
    type: "formula-sheet",
    access: "download",
    instructor: "Prof. A. Hamdan",
    pages: 4,
  },
  {
    id: "mat-003",
    title: "Python Data Analysis Guide",
    subject: "Computer Science",
    level: "College",
    type: "notes",
    access: "view",
    instructor: "Grace N.",
    pages: 32,
  },
  {
    id: "mat-004",
    title: "HTML & CSS Cheat Sheet",
    subject: "Computer Science",
    level: "Secondary",
    type: "notes",
    access: "download",
    instructor: "David O.",
    pages: 6,
  },
  {
    id: "mat-005",
    title: "Digital Marketing Fundamentals",
    subject: "Business Studies",
    level: "College",
    type: "slides",
    access: "view",
    instructor: "Sarah K.",
    pages: 48,
  },
  {
    id: "mat-006",
    title: "Statistics Practice Problems",
    subject: "Mathematics",
    level: "College",
    type: "practice",
    access: "download",
    instructor: "Dr. John Mwangi",
    pages: 12,
  },
  {
    id: "mat-007",
    title: "Chemistry Lab Report Template",
    subject: "Science",
    level: "Secondary",
    type: "practice",
    access: "download",
    instructor: "Prof. A. Hamdan",
    pages: 3,
  },
  {
    id: "mat-008",
    title: "Business Strategy & Planning Notes",
    subject: "Business Studies",
    level: "University",
    type: "notes",
    access: "view",
    instructor: "Sarah K.",
    pages: 18,
  },
  {
    id: "mat-009",
    title: "English Grammar & Composition",
    subject: "English",
    level: "Primary",
    type: "notes",
    access: "download",
    instructor: "Mr. James O.",
    pages: 14,
  },
  {
    id: "mat-010",
    title: "Kiswahili Fasihi — Utunzi & Ushairi",
    subject: "Kiswahili",
    level: "Secondary",
    type: "notes",
    access: "view",
    instructor: "Mwalimu Rehema",
    pages: 20,
  },
  {
    id: "mat-011",
    title: "Chemistry Organic Compounds Guide",
    subject: "Chemistry",
    level: "Secondary",
    type: "notes",
    access: "view",
    instructor: "Dr. Peter M.",
    pages: 22,
  },
  {
    id: "mat-012",
    title: "Biology Cell Structure & Function",
    subject: "Biology",
    level: "Secondary",
    type: "slides",
    access: "download",
    instructor: "Dr. Grace L.",
    pages: 36,
  },
  {
    id: "mat-013",
    title: "East African Geography Atlas Notes",
    subject: "Geography",
    level: "Secondary",
    type: "notes",
    access: "view",
    instructor: "Mr. Julius K.",
    pages: 28,
  },
  {
    id: "mat-014",
    title: "World History — Colonialism & Independence",
    subject: "History",
    level: "Secondary",
    type: "notes",
    access: "download",
    instructor: "Mr. Julius K.",
    pages: 16,
  },
  {
    id: "mat-015",
    title: "Civic Education & Governance",
    subject: "Civic Education",
    level: "Secondary",
    type: "slides",
    access: "download",
    instructor: "Ms. Fatima A.",
    pages: 30,
  },
  {
    id: "mat-016",
    title: "Christian Religious Studies — Old Testament",
    subject: "Religious Studies",
    level: "Primary",
    type: "notes",
    access: "download",
    instructor: "Mr. Daniel W.",
    pages: 10,
  },
  {
    id: "mat-017",
    title: "Islamic Education — Five Pillars",
    subject: "Religious Studies",
    level: "Secondary",
    type: "notes",
    access: "view",
    instructor: "Sheikh Omar M.",
    pages: 8,
  },
]
