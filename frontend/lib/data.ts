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

export type NurseryLevel = "Baby 1" | "Baby 2" | "Baby 3"
export type NurserySubject = "Kusoma" | "Kuandika" | "Kuhesabu" | "Kuchora"

export const nurseryLevels = [
  { id: "Baby 1" as NurseryLevel, name: "Baby 1", ages: "2-3 yrs", desc: "First steps in learning through play and discovery" },
  { id: "Baby 2" as NurseryLevel, name: "Baby 2", ages: "3-4 yrs", desc: "Building foundations with colors, sounds and words" },
  { id: "Baby 3" as NurseryLevel, name: "Baby 3", ages: "4-5 yrs", desc: "Preparing for primary school adventures" },
]

export const nurserySubjects = [
  { id: "Kusoma" as NurserySubject, name: "Kusoma", desc: "Reading & phonics", icon: "BookOpen", color: "text-blue-600 bg-blue-500/10" },
  { id: "Kuandika" as NurserySubject, name: "Kuandika", desc: "Writing & letters", icon: "PenTool", color: "text-green-600 bg-green-500/10" },
  { id: "Kuhesabu" as NurserySubject, name: "Kuhesabu", desc: "Counting & numbers", icon: "Calculator", color: "text-orange-600 bg-orange-500/10" },
  { id: "Kuchora" as NurserySubject, name: "Kuchora", desc: "Drawing & creativity", icon: "Palette", color: "text-purple-600 bg-purple-500/10" },
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
  { id: "tegeta-a", name: "Tegeta A Nursery School", location: "Temeke, DSM", students: 350, rating: 4.6, description: "A well-established nursery with dedicated Baby 1–3 classes and experienced teachers." },
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
  {
    id: "darasa-1",
    name: "Darasa I",
    label: "Standard 1",
    ages: "6-7 yrs",
    subjects: [
      "Kusoma na Kuandika",
      "Hisabati",
      "Mazingira na Maarifa ya Msingi",
      "Kiingereza",
      "Elimu ya Dini",
      "Sanaa na Michezo",
    ],
  },
  {
    id: "darasa-2",
    name: "Darasa II",
    label: "Standard 2",
    ages: "7-8 yrs",
    subjects: [
      "Kiswahili",
      "Hisabati",
      "Kiingereza",
      "Sayansi na Maarifa ya Mazingira",
      "Elimu ya Dini",
      "Sanaa na Michezo",
    ],
  },
  {
    id: "darasa-3",
    name: "Darasa III",
    label: "Standard 3",
    ages: "8-9 yrs",
    subjects: [
      "Kiswahili",
      "English",
      "Hisabati",
      "Sayansi",
      "Historia ya Tanzania na Maadili",
      "Jiografia na Mazingira",
      "Elimu ya Dini",
      "Sanaa na Michezo",
    ],
  },
  {
    id: "darasa-4",
    name: "Darasa IV",
    label: "Standard 4",
    ages: "9-10 yrs",
    subjects: [
      "Kiswahili",
      "English",
      "Hisabati",
      "Sayansi",
      "Historia ya Tanzania na Maadili",
      "Jiografia na Mazingira",
      "Elimu ya Dini",
      "Sanaa na Michezo",
    ],
  },
  {
    id: "darasa-5",
    name: "Darasa V",
    label: "Standard 5",
    ages: "10-11 yrs",
    subjects: [
      "Kiswahili",
      "English",
      "Hisabati",
      "Sayansi",
      "Historia ya Tanzania na Maadili",
      "Jiografia na Mazingira",
      "Elimu ya Dini",
      "Sanaa na Michezo",
    ],
  },
  {
    id: "darasa-6",
    name: "Darasa VI",
    label: "Standard 6",
    ages: "11-12 yrs",
    subjects: [
      "Kiswahili",
      "English",
      "Hisabati",
      "Sayansi",
      "Historia ya Tanzania na Maadili",
      "Jiografia na Mazingira",
      "Elimu ya Dini",
      "Sanaa na Michezo",
    ],
  },
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
  { id: "osterbay", name: "Osterbay Primary School", location: "Dar es Salaam", students: 380, rating: 4.8, description: "Balancing academics with extracurricular activities for well-rounded成长." },
  { id: "mlimani", name: "Mlimani Primary School", location: "Kinondoni, DSM", students: 280, rating: 4.7, description: "Continuing the learning journey from nursery to primary excellence." },
  { id: "tegeta-a", name: "Tegeta A Primary School", location: "Temeke, DSM", students: 350, rating: 4.6, description: "A well-established school with structured programs from Darasa I to VI." },
  { id: "mbezi-louis", name: "Mbezi Louis Primary School", location: "Mbezi, DSM", students: 310, rating: 4.5, description: "Growing community school focused on Kiswahili-medium instruction." },
]

export type SecondaryLevel = "O-Level" | "A-Level"

export type SecondaryForm = {
  id: string
  name: string
  label: string
  level: SecondaryLevel
  ages: string
  subjects: string[]
}

export const secondaryForms: SecondaryForm[] = [
  {
    id: "form-1",
    name: "Form I",
    label: "Form 1",
    level: "O-Level",
    ages: "13-14 yrs",
    subjects: [
      "Kiswahili",
      "English Language",
      "Mathematics",
      "Physics",
      "Chemistry",
      "Biology",
      "Geography",
      "History",
      "Civics",
      "Bookkeeping",
      "ICT",
      "Fine Arts",
      "Food and Nutrition",
      "Music",
      "Physical Education",
    ],
  },
  {
    id: "form-2",
    name: "Form II",
    label: "Form 2",
    level: "O-Level",
    ages: "14-15 yrs",
    subjects: [
      "Kiswahili",
      "English Language",
      "Mathematics",
      "Physics",
      "Chemistry",
      "Biology",
      "Geography",
      "History",
      "Civics",
      "Bookkeeping",
      "ICT",
      "Fine Arts",
      "Food and Nutrition",
      "Music",
      "Physical Education",
    ],
  },
  {
    id: "form-3",
    name: "Form III",
    label: "Form 3",
    level: "O-Level",
    ages: "15-16 yrs",
    subjects: [
      "Kiswahili",
      "English Language",
      "Mathematics",
      "Physics",
      "Chemistry",
      "Biology",
      "Geography",
      "History",
      "Civics",
      "Bookkeeping",
      "ICT",
      "Fine Arts",
      "Food and Nutrition",
      "Music",
      "Physical Education",
      "French",
      "Arabic",
    ],
  },
  {
    id: "form-4",
    name: "Form IV",
    label: "Form 4",
    level: "O-Level",
    ages: "16-17 yrs",
    subjects: [
      "Kiswahili",
      "English Language",
      "Mathematics",
      "Physics",
      "Chemistry",
      "Biology",
      "Geography",
      "History",
      "Civics",
      "Bookkeeping",
      "ICT",
      "Fine Arts",
      "Food and Nutrition",
      "Music",
      "Physical Education",
      "French",
      "Arabic",
    ],
  },
  {
    id: "form-5",
    name: "Form V",
    label: "Form 5",
    level: "A-Level",
    ages: "17-18 yrs",
    subjects: [],
  },
  {
    id: "form-6",
    name: "Form VI",
    label: "Form 6",
    level: "A-Level",
    ages: "18-19 yrs",
    subjects: [],
  },
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
  { id: "kemebos", name: "Kemebos Secondary School", location: "Moshi, Kilimanjaro", students: 850, rating: 4.8, levels: ["O-Level", "A-Level"], description: "A top-performing school with both O-Level and A-Level programs and excellent NECTA results." },
  { id: "josiah-girls", name: "Josiah's Girls Secondary School", location: "Dar es Salaam", students: 620, rating: 4.7, levels: ["O-Level", "A-Level"], description: "Empowering girls through quality education from Form I to Form VI." },
  { id: "ihungo", name: "Ihungo Secondary School", location: "Mwanza", students: 340, rating: 4.5, levels: ["A-Level"], description: "Specialized A-Level school offering PCM, PCB and HGL combinations." },
  { id: "kahororo", name: "Kahororo Secondary School", location: "Bukoba, Kagera", students: 280, rating: 4.4, levels: ["A-Level"], description: "An A-Level center with strong results in science combinations." },
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

export type College = {
  id: string
  name: string
  abbreviation: string
  location: string
  students: number
  rating: number
  departments: string[]
  description: string
}

export const colleges: College[] = [
  { id: "dit", name: "Dar es Salaam Institute of Technology", abbreviation: "DIT", location: "Dar es Salaam", students: 4500, rating: 4.7, departments: ["engineering", "ict", "business"], description: "A leading technical institute offering diploma and degree programs in engineering and technology." },
  { id: "cbe", name: "College of Business Education", abbreviation: "CBE", location: "Dar es Salaam", students: 3800, rating: 4.6, departments: ["business", "ict"], description: "Specializing in business education with programs in accountancy, marketing and management." },
  { id: "ifm", name: "Institute of Finance Management", abbreviation: "IFM", location: "Dar es Salaam", students: 3200, rating: 4.8, departments: ["business", "ict"], description: "Tanzania's premier institution for finance, banking and insurance education." },
  { id: "tit", name: "Tanzania Institute of Transport", abbreviation: "TIT", location: "Dar es Salaam", students: 2100, rating: 4.4, departments: ["engineering"], description: "Focused on transport logistics, maritime studies and automotive engineering." },
  { id: "must", name: "Mbeya University of Science and Technology", abbreviation: "MUST", location: "Mbeya", students: 5200, rating: 4.5, departments: ["engineering", "ict", "education", "agriculture"], description: "A full university offering science, technology and education programs at diploma and degree level." },
]

export type VetaTrade = {
  id: string
  name: string
  icon: string
  duration: string
  level: string
}

export type VetaCategory = {
  id: string
  name: string
  icon: string
  trades: VetaTrade[]
}

export const vetaCategories: VetaCategory[] = [
  {
    id: "construction",
    name: "Construction & Building",
    icon: "Hammer",
    trades: [
      { id: "carpentry", name: "Carpentry & Joinery", icon: "Hammer", duration: "2 years", level: "Trade Certificate" },
      { id: "masonry", name: "Masonry & Bricklaying", icon: "Hammer", duration: "2 years", level: "Trade Certificate" },
      { id: "plumbing", name: "Plumbing & Pipe Fitting", icon: "Hammer", duration: "2 years", level: "Trade Certificate" },
      { id: "painting", name: "Painting & Decorating", icon: "Hammer", duration: "1 year", level: "Trade Certificate" },
    ],
  },
  {
    id: "automotive",
    name: "Automotive & Mechanical",
    icon: "Car",
    trades: [
      { id: "auto-mechanics", name: "Automotive Mechanics", icon: "Car", duration: "2 years", level: "Trade Certificate" },
      { id: "welding", name: "Welding & Fabrication", icon: "Car", duration: "2 years", level: "Trade Certificate" },
      { id: "metal-fabrication", name: "Metal Fabrication", icon: "Car", duration: "2 years", level: "Trade Certificate" },
    ],
  },
  {
    id: "electrical",
    name: "Electrical & Electronics",
    icon: "Zap",
    trades: [
      { id: "electrical-installation", name: "Electrical Installation", icon: "Zap", duration: "2 years", level: "Trade Certificate" },
      { id: "electronics", name: "Electronics & Telecommunication", icon: "Zap", duration: "2 years", level: "Trade Certificate" },
    ],
  },
  {
    id: "ict",
    name: "ICT & Digital Skills",
    icon: "Monitor",
    trades: [
      { id: "ict-technician", name: "ICT Technician", icon: "Monitor", duration: "2 years", level: "Trade Certificate" },
      { id: "computer-networking", name: "Computer Networking", icon: "Monitor", duration: "1 year", level: "Trade Certificate" },
      { id: "graphic-design", name: "Graphic Design & Multimedia", icon: "Monitor", duration: "1 year", level: "Trade Certificate" },
    ],
  },
  {
    id: "hospitality",
    name: "Hospitality & Tourism",
    icon: "ChefHat",
    trades: [
      { id: "food-beverage", name: "Food & Beverage Production", icon: "ChefHat", duration: "2 years", level: "Trade Certificate" },
      { id: "hotel-management", name: "Hotel Management", icon: "ChefHat", duration: "2 years", level: "Trade Certificate" },
      { id: "tourism", name: "Tourism & Wildlife Management", icon: "ChefHat", duration: "2 years", level: "Trade Certificate" },
    ],
  },
  {
    id: "beauty",
    name: "Beauty & Personal Care",
    icon: "Scissors",
    trades: [
      { id: "hairdressing", name: "Hairdressing & Beauty Therapy", icon: "Scissors", duration: "1 year", level: "Trade Certificate" },
      { id: "tailoring", name: "Tailoring & Dressmaking", icon: "Scissors", duration: "2 years", level: "Trade Certificate" },
    ],
  },
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
  { id: "diploma", name: "Diploma", duration: "2–3 years", icon: "Award", description: "Specialized technical and professional training at undergraduate level" },
  { id: "bachelors", name: "Bachelor's Degree", duration: "3–4 years", icon: "GraduationCap", description: "Undergraduate degree programs across arts, science, engineering and more" },
  { id: "masters", name: "Master's Degree", duration: "1–2 years", icon: "BookOpen", description: "Postgraduate specialization and research-based advanced study" },
  { id: "phd", name: "PhD / Doctorate", duration: "3–4 years", icon: "Flame", description: "Original research contributing to new knowledge in your field" },
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
  {
    id: "udsm",
    name: "University of Dar es Salaam",
    abbreviation: "UDSM",
    location: "Dar es Salaam",
    students: 35000,
    rating: 4.8,
    degreeLevels: ["diploma", "bachelors", "masters", "phd"],
    programs: ["Arts & Social Sciences", "Science", "Engineering", "Law", "Medicine", "Business", "Education"],
    description: "Tanzania's oldest and largest university, offering a wide range of programs across multiple faculties.",
  },
  {
    id: "udom",
    name: "University of Dodoma",
    abbreviation: "UDOM",
    location: "Dodoma",
    students: 25000,
    rating: 4.6,
    degreeLevels: ["bachelors", "masters"],
    programs: ["Education", "Science & Technology", "Health Sciences", "Business", "Humanities", "Agriculture"],
    description: "Tanzania's fastest-growing public university with modern facilities and a wide range of programs.",
  },
  {
    id: "muhas",
    name: "Muhimbili University of Health and Allied Sciences",
    abbreviation: "MUHAS",
    location: "Dar es Salaam",
    students: 8000,
    rating: 4.9,
    degreeLevels: ["bachelors", "masters", "phd"],
    programs: ["Medicine", "Nursing", "Pharmacy", "Dentistry", "Public Health", "Laboratory Sciences"],
    description: "East Africa's leading health sciences university, training doctors, nurses and health professionals.",
  },
  {
    id: "aru",
    name: "Ardhi University",
    abbreviation: "ARU",
    location: "Dar es Salaam",
    students: 12000,
    rating: 4.7,
    degreeLevels: ["diploma", "bachelors", "masters"],
    programs: ["Surveying", "Urban Planning", "Real Estate", "Construction Management", "Environmental Science", "Geomatics"],
    description: "Specializing in land, built environment and geospatial sciences since 1956.",
  },
  {
    id: "nm-aist",
    name: "Nelson Mandela African Institution of Science and Technology",
    abbreviation: "NM-AIST",
    location: "Arusha",
    students: 3500,
    rating: 4.7,
    degreeLevels: ["masters", "phd"],
    programs: ["Computing & Informatics", "Life Sciences", "Physical Sciences", "Engineering", "Mathematics", "Infrastructure"],
    description: "A postgraduate-only institution focused on science, technology and innovation for Africa's development.",
  },
]
