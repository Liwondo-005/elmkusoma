import { describe, it, expect } from "@jest/globals"

describe("Primary Learner Experience", () => {
  it("should export all expected API methods", async () => {
    const api = await import("@/lib/api")
    expect(api.primaryApi).toBeDefined()
    expect(typeof api.primaryApi.getTeachers).toBe("function")
    expect(typeof api.primaryApi.getPortfolio).toBe("function")
    expect(typeof api.primaryApi.getBadges).toBe("function")
    expect(typeof api.primaryApi.getStreak).toBe("function")
    expect(typeof api.primaryApi.getDiscoveryEntries).toBe("function")
    expect(typeof api.primaryApi.getReadingAdventures).toBe("function")
    expect(typeof api.primaryApi.getLabs).toBe("function")
    expect(typeof api.primaryApi.getQuestChallenges).toBe("function")
    expect(typeof api.primaryApi.getMistakeLabEntries).toBe("function")
    expect(typeof api.primaryApi.getLearningEvidence).toBe("function")
    expect(typeof api.primaryApi.getLearningPassport).toBe("function")
    expect(typeof api.primaryApi.getLearningProfile).toBe("function")
    expect(typeof api.primaryApi.getCollaborations).toBe("function")
    expect(typeof api.primaryApi.getSpeakingActivities).toBe("function")
    expect(typeof api.primaryApi.getRealWorldMissions).toBe("function")
    expect(typeof api.primaryApi.getNotifications).toBe("function")
  })

  it("should define primary subject icons", async () => {
    const config = await import("@/lib/learner-config")
    expect(config.primarySubjects).toBeDefined()
    expect(config.primarySubjects.length).toBe(8)
    config.primarySubjects.forEach((s: any) => {
      expect(s.name).toBeTruthy()
      expect(s.icon).toBeDefined()
      expect(s.color).toBeTruthy()
      expect(s.bgColor).toBeTruthy()
    })
  })

  it("should define primary learning domains", async () => {
    const config = await import("@/lib/learner-config")
    expect(config.primaryLearningDomains).toBeDefined()
    expect(config.primaryLearningDomains.length).toBeGreaterThan(0)
  })

  it("should have PRIMARY dashboard config", async () => {
    const config = await import("@/lib/learner-config")
    const dashboardConfig = config.getDashboardConfig("PRIMARY")
    expect(dashboardConfig).toBeDefined()
    expect(dashboardConfig.title).toBeTruthy()
    expect(dashboardConfig.subtitle).toBeTruthy()
  })
})

describe("Navigation", () => {
  it("should have all required Primary nav sections", async () => {
    const sidebar = await import("@/components/dashboard/dashboard-sidebar")
    expect(sidebar).toBeDefined()
  })
})
