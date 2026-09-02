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
