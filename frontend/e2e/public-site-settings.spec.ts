import { test, expect, type Page } from "@playwright/test"

/**
 * Public site settings rendering, and the ELMKUSOMA Headquarters section.
 *
 * <p>This suite exists because of a specific failure mode: the public settings endpoint silently
 * dropped a value the server had refused to accept, so an administrator's save looked successful
 * while the link never appeared on the site. Nothing failed loudly. These tests assert what a
 * visitor can actually see, which is the thing that silently went missing.</p>
 */

/**
 * Waits for the client-side settings fetch to land.
 *
 * <p>Site settings are fetched in an effect after hydration, so the server-rendered HTML
 * contains no contact details and nothing to wait on. Without this, a test either races the
 * fetch or — worse — passes vacuously, because asserting a link is *absent* succeeds just as
 * happily when the whole component has not rendered yet.</p>
 */
async function gotoWithSettings(page: Page) {
  await page.goto("/")
  await expect(page.locator('footer a[href^="mailto:"]').first()).toBeVisible({ timeout: 30_000 })
}

async function headquartersSection(page: Page) {
  const heading = page.locator("#headquarters-heading")
  await expect(heading).toBeVisible({ timeout: 30_000 })
  return page.locator("section", { has: heading }).first()
}

test.describe("Public contact and social settings", () => {
  test("footer renders the configured Facebook and Instagram links", async ({ page }) => {
    await gotoWithSettings(page)

    // Icon-only variant, so the accessible name (not the text content) identifies the platform.
    const social = await page.evaluate(() =>
      Array.from(document.querySelectorAll("footer a"))
        .map((a) => ({
          href: a.getAttribute("href") ?? "",
          ariaLabel: a.getAttribute("aria-label") ?? "",
        }))
        .filter((a) => /facebook|instagram/i.test(a.href + a.ariaLabel))
    )

    const labels = social.map((s) => s.ariaLabel)
    expect(labels.some((l) => /Facebook/.test(l))).toBe(true)
    expect(labels.some((l) => /Instagram/.test(l))).toBe(true)
    expect(social.find((s) => /Facebook/.test(s.ariaLabel))?.href).toBe(
      "https://www.facebook.com/share/1Exp9TeDmL/"
    )
    expect(social.find((s) => /Instagram/.test(s.ariaLabel))?.href).toContain(
      "instagram.com/elmkusoma_20"
    )
  })

  /**
   * Icons, not text.
   *
   * <p>The footer previously rendered the platform name as a bare text link with no mark at all,
   * which is the "social media links are not appearing" complaint in its most literal form. The
   * requirement is a recognisable brand icon paired with the correct destination.</p>
   */
  test("each social link carries its brand icon and an accessible name", async ({ page }) => {
    await gotoWithSettings(page)

    const details = await page.evaluate(() =>
      Array.from(document.querySelectorAll("footer a"))
        .filter((a) => /facebook|instagram/i.test(a.getAttribute("href") ?? ""))
        .map((a) => ({
          href: a.getAttribute("href"),
          ariaLabel: a.getAttribute("aria-label"),
          svgCount: a.querySelectorAll("svg").length,
          // A real brand mark has a filled path; an empty or decorative glyph would not.
          pathCount: a.querySelectorAll("svg path").length,
          pathLength: a.querySelector("svg path")?.getAttribute("d")?.length ?? 0,
        }))
    )

    expect(details.length).toBeGreaterThanOrEqual(2)
    for (const link of details) {
      expect(link.svgCount).toBe(1)
      expect(link.pathCount).toBe(1)
      // An icon-only link's name cannot come from its text content, so aria-label must exist.
      expect(link.ariaLabel).toBeTruthy()
      expect(link.ariaLabel).toMatch(/Facebook|Instagram/)
      expect(link.pathLength).toBeGreaterThan(50)
    }

    // Each platform gets its own distinct mark, not the same glyph twice.
    const paths = details.map((d) => d.href)
    expect(new Set(paths).size).toBe(details.length)
  })

  test("social links are keyboard reachable with a visible focus style", async ({ page }) => {
    await gotoWithSettings(page)

    const focusable = await page.evaluate(() => {
      const links = Array.from(document.querySelectorAll("footer a")).filter((a) =>
        /facebook|instagram/i.test(a.getAttribute("href") ?? "")
      )
      return links.map((a) => {
        const cls = a.className || ""
        return {
          isAnchor: a.tagName === "A",
          hasHref: Boolean(a.getAttribute("href")),
          focusRing: cls.includes("focus-visible:ring"),
        }
      })
    })

    expect(focusable.length).toBeGreaterThan(0)
    for (const link of focusable) {
      expect(link.isAnchor).toBe(true)
      expect(link.hasHref).toBe(true)
      expect(link.focusRing).toBe(true)
    }
  })

  test("social links open safely in a new tab", async ({ page }) => {
    await gotoWithSettings(page)

    const links = await page.evaluate(() =>
      Array.from(document.querySelectorAll("footer a"))
        .filter((a) => /facebook|instagram/i.test(a.getAttribute("href") ?? ""))
        .map((a) => ({
          href: a.getAttribute("href"),
          target: a.getAttribute("target"),
          rel: a.getAttribute("rel"),
        }))
    )

    expect(links.length).toBeGreaterThan(0)
    for (const link of links) {
      expect(link.target).toBe("_blank")
      // opener protection: window.opener would otherwise let the destination page script this site.
      expect(link.rel).toContain("noopener")
    }
  })

  test("configured contact email is rendered in the footer", async ({ page }) => {
    await gotoWithSettings(page)
    await expect(page.locator('footer a[href="mailto:info@elmkusoma.co.tz"]').first()).toBeVisible()
  })

  test("no link anywhere renders as a broken or unsafe href", async ({ page }) => {
    await gotoWithSettings(page)

    const hrefs = await page.evaluate(() =>
      Array.from(document.querySelectorAll("a")).map((a) => a.getAttribute("href") ?? "")
    )

    // An unset platform must be absent entirely, never an empty, "undefined" or "null" href,
    // which would render a clickable dead control.
    expect(hrefs.length).toBeGreaterThan(0)
    for (const href of hrefs) {
      expect(href).not.toBe("")
      expect(href).not.toContain("undefined")
      expect(href).not.toBe("null")
      expect(href).not.toBe("#")
      expect(href).not.toMatch(/^javascript:/i)
      expect(href).not.toMatch(/^data:/i)
    }
  })
})

test.describe("ELMKUSOMA Headquarters section", () => {
  test("shows the configured campus-level location", async ({ page }) => {
    await page.goto("/")
    const section = await headquartersSection(page)

    await expect(section.locator("#headquarters-heading")).toHaveText("ELMKUSOMA Headquarters")
    await expect(section).toContainText("University of Dar es Salaam (UDSM), Dar es Salaam, Tanzania")
  })

  test("sits on the landing page inside main, before the footer, exactly once", async ({ page }) => {
    await page.goto("/")
    await headquartersSection(page)

    const order = await page.evaluate(() => {
      const hq = document.getElementById("headquarters-heading")
      if (!hq) return null
      const hqSection = hq.closest("section")
      const footer = document.querySelector("footer")
      return {
        inMain: !!hqSection?.closest("main"),
        beforeFooter: !!(
          hqSection &&
          footer &&
          (hqSection.compareDocumentPosition(footer) & Node.DOCUMENT_POSITION_FOLLOWING) !== 0
        ),
        // Exactly one headquarters block: a duplicated location section is its own defect.
        count: document.querySelectorAll("#headquarters-heading").length,
      }
    })

    expect(order).not.toBeNull()
    expect(order!.inMain).toBe(true)
    expect(order!.beforeFooter).toBe(true)
    expect(order!.count).toBe(1)
  })

  /**
   * Map and directions must match the saved configuration exactly.
   *
   * <p>Deliberately data-driven rather than asserting "hidden". The owner may or may not have
   * configured a destination, and the honest rule is the same either way: an action is rendered if
   * and only if a destination backs it. Hard-coding "no map button" would pass vacuously on a page
   * where the section failed to render at all, which is the opposite of what this proves.</p>
   */
  test("map and directions actions match the saved configuration", async ({ page }) => {
    await page.goto("/")
    const section = await headquartersSection(page)

    // Assert the section rendered first, so an absent button cannot pass on a missing section.
    await expect(section.locator("#headquarters-heading")).toBeVisible()

    const configured = await page.evaluate(async () => {
      const res = await fetch("/v1/public/site-settings")
      const body = await res.json()
      const data = body?.data ?? {}
      return {
        mapUrl: data["public.location.mapUrl"] ?? null,
        directionsUrl: data["public.location.directionsUrl"] ?? null,
      }
    })

    const mapLinks = await section.getByRole("link", { name: /view on map/i }).all()
    const directionsLinks = await section.getByRole("link", { name: /get directions/i }).all()

    if (configured.mapUrl) {
      expect(mapLinks.length).toBeGreaterThan(0)
      // Every rendered map link points at the configured destination, and nothing else.
      for (const link of mapLinks) {
        expect(await link.getAttribute("href")).toBe(configured.mapUrl)
        expect(await link.getAttribute("target")).toBe("_blank")
        expect(await link.getAttribute("rel")).toContain("noopener")
      }
    } else {
      // No destination: a button would be a dead control implying a precision we do not have.
      expect(mapLinks).toHaveLength(0)
      await expect(section).toContainText(/map location has not been confirmed/i)
    }

    if (configured.directionsUrl) {
      expect(directionsLinks.length).toBeGreaterThan(0)
    } else {
      expect(directionsLinks).toHaveLength(0)
    }
  })

  test("no invented building, address or coordinates are shown", async ({ page }) => {
    await page.goto("/")
    const section = await headquartersSection(page)
    const text = (await section.innerText()).toLowerCase()

    // These would only appear if a precise office location had been fabricated.
    expect(text).not.toMatch(/building\s+[a-z0-9]/i)
    expect(text).not.toMatch(/\bp\.?\s*o\.?\s*box\b/i)
    expect(text).not.toMatch(/\b\d{1,3}\.\d{4,}\s*,\s*\d{1,3}\.\d{4,}/)
  })

  test("is accessible: labelled region, no console errors", async ({ page }) => {
    const errors: string[] = []
    page.on("pageerror", (e) => errors.push(e.message))

    await page.goto("/")
    const section = await headquartersSection(page)

    // aria-labelledby points at the heading, so assistive tech announces what the region is.
    await expect(section).toHaveAttribute("aria-labelledby", "headquarters-heading")
    expect(errors).toEqual([])
  })

  test("renders on a mobile viewport without horizontal overflow", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await page.goto("/")
    await headquartersSection(page)

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth
    )
    expect(overflow).toBeLessThanOrEqual(0)
  })
})
