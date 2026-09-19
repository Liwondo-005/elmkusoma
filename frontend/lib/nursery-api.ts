const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

async function nurseryFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  let institutionId = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_institution_id") : null
  if (!institutionId && typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("elmkusoma_current_user")
      if (raw) {
        const user = JSON.parse(raw)
        if (user?.institutionId) institutionId = user.institutionId
      }
    } catch {}
  }
  if (!institutionId) institutionId = "00000000-0000-0000-0000-000000000001"
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "X-Institution-Id": institutionId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || body.message || `Request failed: ${res.status}`)
  }
  const json = await res.json()
  return json.data ?? json
}

export interface NurseryActivity {
  id: string
  classGroupId: string
  institutionId: string
  activityName: string
  activityType: "GAME" | "SONG" | "STORY" | "CRAFT" | "PHYSICAL" | "EDUCATIONAL"
  description: string | null
  instructions: string | null
  durationMinutes: number | null
  maxParticipants: number | null
  materialsNeeded: string | null
  learningObjectives: string | null
  ageGroup: string | null
  activityDate: string
  status: "PLANNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"
  conductedBy: string | null
}

export interface NurseryMilestone {
  id: string
  studentId: string
  institutionId: string
  category: "PHYSICAL" | "COGNITIVE" | "SOCIAL" | "EMOTIONAL" | "LANGUAGE" | "MOTOR"
  milestoneName: string
  description: string | null
  expectedAgeMonths: number | null
  achievedDate: string | null
  status: "PENDING" | "ACHIEVED" | "IN_PROGRESS" | "NOT_OBSERVED"
  observedBy: string | null
  evidenceNotes: string | null
}

export const nurseryApi = {
  getActivities: (classId: string) =>
    nurseryFetch<NurseryActivity[]>(`/v1/nursery/activities?classId=${classId}`),

  getActivitiesByType: (type: string) =>
    nurseryFetch<NurseryActivity[]>(`/v1/nursery/activities/type/${type}`),

  getActivitiesByClassAndDate: (classId: string, date: string) =>
    nurseryFetch<NurseryActivity[]>(`/v1/nursery/activities/class/${classId}/date/${date}`),

  getActivity: (id: string) =>
    nurseryFetch<NurseryActivity>(`/v1/nursery/activities/${id}`),

  getMilestones: (studentId: string) =>
    nurseryFetch<NurseryMilestone[]>(`/v1/nursery/milestones/student/${studentId}`),

  getMilestonesByCategory: (studentId: string, category: string) =>
    nurseryFetch<NurseryMilestone[]>(`/v1/nursery/milestones/student/${studentId}/category/${category}`),

  getMilestone: (id: string) =>
    nurseryFetch<NurseryMilestone>(`/v1/nursery/milestones/${id}`),

  // Stories
  getStories: (classId: string) =>
    nurseryFetch<NurseryStory[]>(`/v1/nursery/extended/stories?classId=${classId}`),
  getStory: (id: string) =>
    nurseryFetch<NurseryStory>(`/v1/nursery/extended/stories/${id}`),
  createStory: (data: Partial<NurseryStory>) =>
    nurseryFetch<NurseryStory>(`/v1/nursery/extended/stories`, { method: "POST", body: JSON.stringify(data) }),

  // Daily Quests
  getDailyQuests: (classId: string) =>
    nurseryFetch<NurseryDailyQuest[]>(`/v1/nursery/extended/daily-quests?classId=${classId}`),
  getDailyQuestsByStudent: (studentId: string) =>
    nurseryFetch<NurseryDailyQuest[]>(`/v1/nursery/extended/daily-quests/student/${studentId}`),
  createDailyQuest: (data: Partial<NurseryDailyQuest>) =>
    nurseryFetch<NurseryDailyQuest>(`/v1/nursery/extended/daily-quests`, { method: "POST", body: JSON.stringify(data) }),
  updateDailyQuest: (id: string, data: Partial<NurseryDailyQuest>) =>
    nurseryFetch<NurseryDailyQuest>(`/v1/nursery/extended/daily-quests/${id}`, { method: "PUT", body: JSON.stringify(data) }),

  // Feelings
  getFeelingsByStudent: (studentId: string) =>
    nurseryFetch<NurseryFeelingsCheckin[]>(`/v1/nursery/extended/feelings/student/${studentId}`),
  createFeelingsCheckin: (data: Partial<NurseryFeelingsCheckin>) =>
    nurseryFetch<NurseryFeelingsCheckin>(`/v1/nursery/extended/feelings`, { method: "POST", body: JSON.stringify(data) }),

  // Missions
  getMissions: (classId: string) =>
    nurseryFetch<NurseryMission[]>(`/v1/nursery/extended/missions?classId=${classId}`),
  getMissionsByStudent: (studentId: string) =>
    nurseryFetch<NurseryMission[]>(`/v1/nursery/extended/missions/student/${studentId}`),
  createMission: (data: Partial<NurseryMission>) =>
    nurseryFetch<NurseryMission>(`/v1/nursery/extended/missions`, { method: "POST", body: JSON.stringify(data) }),
  updateMission: (id: string, data: Partial<NurseryMission>) =>
    nurseryFetch<NurseryMission>(`/v1/nursery/extended/missions/${id}`, { method: "PUT", body: JSON.stringify(data) }),

  // Tanzania Discovery
  getTanzaniaTopics: (classId: string) =>
    nurseryFetch<NurseryTanzaniaDiscovery[]>(`/v1/nursery/extended/tanzania?classId=${classId}`),
  getTanzaniaByCategory: (category: string) =>
    nurseryFetch<NurseryTanzaniaDiscovery[]>(`/v1/nursery/extended/tanzania/category/${category}`),

  // Parent Learning
  getParentLearningByStudent: (studentId: string) =>
    nurseryFetch<NurseryParentLearning[]>(`/v1/nursery/extended/parent-learning/student/${studentId}`),
  createParentLearning: (data: Partial<NurseryParentLearning>) =>
    nurseryFetch<NurseryParentLearning>(`/v1/nursery/extended/parent-learning`, { method: "POST", body: JSON.stringify(data) }),
  updateParentLearning: (id: string, data: Partial<NurseryParentLearning>) =>
    nurseryFetch<NurseryParentLearning>(`/v1/nursery/extended/parent-learning/${id}`, { method: "PUT", body: JSON.stringify(data) }),
}

export interface NurseryStory {
  id: string
  classGroupId: string
  title: string
  content: string
  storyType: "ANIMAL" | "FAIRY_TALE" | "TANZANIA" | "EDUCATIONAL" | "ADVENTURE" | "MORAL"
  illustrationUrl: string | null
  audioUrl: string | null
  durationMinutes: number | null
  readingLevel: string | null
  isPublished: boolean
  createdAt: string
}

export interface NurseryDailyQuest {
  id: string
  classGroupId: string
  studentId: string | null
  questTitle: string
  questDescription: string | null
  questType: "READING" | "MATH" | "SCIENCE" | "ART" | "PHYSICAL" | "LANGUAGE"
  rewardPoints: number
  status: "PENDING" | "IN_PROGRESS" | "COMPLETED" | "FAILED"
  dueDate: string | null
  completedDate: string | null
  createdAt: string
}

export interface NurseryFeelingsCheckin {
  id: string
  studentId: string
  classGroupId: string
  feeling: "HAPPY" | "SAD" | "EXCITED" | "CALM" | "WORRIED" | "ANGRY" | "PROUD" | "TIRED"
  emoji: string | null
  note: string | null
  checkinDate: string
  createdAt: string
}

export interface NurseryMission {
  id: string
  classGroupId: string
  studentId: string | null
  missionTitle: string
  missionDescription: string | null
  missionType: "HOME" | "COMMUNITY" | "NATURE" | "CREATIVITY"
  rewardPoints: number
  status: "PENDING" | "IN_PROGRESS" | "COMPLETED" | "FAILED"
  dueDate: string | null
  completedDate: string | null
  evidenceNotes: string | null
  evidenceImageUrl: string | null
  createdAt: string
}

export interface NurseryTanzaniaDiscovery {
  id: string
  classGroupId: string | null
  topicTitle: string
  topicDescription: string | null
  category: "ANIMALS" | "PLACES" | "CULTURE" | "FOOD" | "MUSIC" | "FLAGS" | "LANDMARKS" | "WEATHER"
  region: string | null
  funFacts: string | null
  imageUrl: string | null
  isPublished: boolean
  createdAt: string
}

export interface NurseryParentLearning {
  id: string
  studentId: string
  classGroupId: string
  activityTitle: string
  activityDescription: string | null
  activityType: "READING" | "COUNTING" | "COOKING" | "GARDENING" | "SINGING" | "CRAFTING"
  parentName: string | null
  completionStatus: "PENDING" | "IN_PROGRESS" | "COMPLETED"
  completedDate: string | null
  notes: string | null
  createdAt: string
}
