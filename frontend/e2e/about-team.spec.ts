// Verification spec for the About "Meet the team" stationary centered cards.
import { test, expect, type Page } from "@playwright/test"

const MEMBERS = [
  { name: "Eng. Arthur C. A. Assenga", role: "Founder", img: "founder" },
  { name: "Asimwe A. Manyusi", role: "Full-Stack Engineer", img: "asimwe" },
  { name: "Suleji R. Issa", role: "IT", img: "suleji" },
  { name: "Johnson M. Wilson", role: "Computer Engineer", img: "johnson" },
  { name: "Idda S. Ngaiza", role: "Frontend Developer", img: "idda" },
  { name: "Twalhiya Amour Ally", role: "Computer Engineer", img: "twally" },
  { name: "Mwanaidi S. Shabani", role: "IT", img: "naah" },
]

const FORBIDDEN = [
  "Amina", "Ochieng", "Grace Nkomo", "Nick Roach", "Mitch Skolnik",
  "Kenny Sing", "Yury Portnykh", "Josh Ronk", "Falgout", "Fikri", "Fabio",
]

async function teamSection(page: Page) {
  const heading = page.getByRole("heading", { name: "Meet the team", exact: true })
  await heading.waitFor()
  return heading.locator("xpath=ancestor::section[1]")
}

const cards = (section: ReturnType<typeof teamSection>) => section.locator("h3")

test.describe("About > Meet the team (stationary)", () => {
  test("exactly 7 unique members with correct name, role and photo", async ({ page }) => {
    await page.goto("/about")
    const section = await teamSection(page)

    // Exactly one instance of each member — no duplicated marquee set.
    await expect(cards(section)).toHaveCount(7)
    expect(await cards(section).evaluateAll((e) => e.map((x) => x.textContent?.trim()))).toEqual(
      MEMBERS.map((m) => m.name),
    )
    // no aria-hidden duplicate markup left behind
    await expect(section.locator('[aria-hidden="true"] h3')).toHaveCount(0)

    const list = cards(section)
    for (const [i, m] of MEMBERS.entries()) {
      const card = list.nth(i).locator("xpath=ancestor::div[contains(@class,'max-w')][1]")
      await expect(card.getByRole("heading", { level: 3 })).toHaveText(m.name)
      await expect(card.locator("p")).toHaveText(m.role)
      await expect(card.locator("img")).toHaveAttribute("alt", m.name)
      await expect(card.locator("img")).toHaveAttribute("src", `/images/team/${m.img}.webp`)
      // photo + name + role only, no bio
      await expect(card.locator("p")).toHaveCount(1)
    }
  })

  test("no placeholder names, bios or '#' links", async ({ page }) => {
    await page.goto("/about")
    const section = await teamSection(page)
    const text = (await section.innerText()).replace(/\s+/g, " ")
    for (const bad of FORBIDDEN) expect(text).not.toContain(bad)
    await expect(section.locator('a[href="#"]')).toHaveCount(0)
    await expect(section.locator("a")).toHaveCount(0)
  })

  test("no animation or marquee markup remains", async ({ page }) => {
    await page.goto("/about")
    const section = await teamSection(page)

    await expect(section.locator(".team-marquee")).toHaveCount(0)
    await expect(section.locator(".team-marquee-viewport")).toHaveCount(0)

    const animated = await section.locator("*").evaluateAll((els) =>
      els
        .map((e) => getComputedStyle(e))
        .filter((cs) => {
          const dur = cs.animationDuration
          const name = cs.animationName
          return (name !== "none" && name !== "" && dur !== "0s") ||
                 cs.transitionProperty !== "all" || cs.transform !== "none"
        })
        .map((cs) => `${cs.animationName} ${cs.animationDuration} ${cs.transform}`),
    )
    // Only the hover lift transition is allowed; nothing may auto-animate.
    const autoAnimated = animated.filter(
      (d) => !/^none 0s none$/.test(d) && !/translateY\(0px\)/.test(d),
    )
    expect(autoAnimated).toEqual([])

    const names = await section
      .locator("*")
      .evaluateAll((els) => els.map((e) => getComputedStyle(e).animationName))
      .then((n) => n.filter((x) => x !== "none"))
    expect(names).toEqual([])
  })

  test("cards are stationary: positions unchanged over time", async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto("/about")
    const section = await teamSection(page)
    await section.scrollIntoViewIfNeeded()
    await page.waitForTimeout(800)

    const snap = () =>
      cards(section).evaluateAll((els) =>
        els.map((e) => {
          const b = e.getBoundingClientRect()
          return `${Math.round(b.left)},${Math.round(b.top)}`
        }),
      )

    const a = await snap()
    await page.waitForTimeout(1500)
    const b = await snap()
    expect(b).toEqual(a) // no horizontal movement whatsoever
  })

  test("wraps into centered rows, including the last row", async ({ page }) => {
    for (const width of [1440, 1024, 768, 390]) {
      await page.setViewportSize({ width, height: 900 })
      await page.goto("/about")
      const section = await teamSection(page)

      const rows = await cards(section).evaluateAll((els) => {
        const groups: { top: number; lefts: number[]; center: number }[] = []
        for (const e of els) {
          const b = e.getBoundingClientRect()
          const g = groups.find((x) => Math.abs(x.top - b.top) <= 4)
          if (g) {
            g.lefts.push(b.left + b.width / 2)
            g.center = g.lefts.reduce((s, v) => s + v, 0) / g.lefts.length
          } else {
            groups.push({ top: b.top, lefts: [b.left + b.width / 2], center: b.left + b.width / 2 })
          }
        }
        return groups.map((g) => ({ count: g.lefts.length, center: g.center }))
      })

      expect(rows.length).toBeGreaterThan(0)
      const total = rows.reduce((s, r) => s + r.count, 0)
      expect(total).toBe(7)

      // Every row — including a short final row — is centred on the
      // container's horizontal axis.
      const { groupCenter, containerCenter } = await section
        .locator("h2")
        .evaluate((h2) => {
          const container = h2.closest("section")!.querySelector("div.flex.flex-wrap")!
          const cb = container.getBoundingClientRect()
          const kids = [...container.children].map((c) => c.getBoundingClientRect())
          const left = Math.min(...kids.map((k) => k.left))
          const right = Math.max(...kids.map((k) => k.right))
          return {
            groupCenter: (left + right) / 2,
            containerCenter: cb.left + cb.width / 2,
          }
        })

      expect(
        Math.abs(groupCenter - containerCenter),
        `group not centred at ${width}px`,
      ).toBeLessThanOrEqual(2)

      for (const [i, r] of rows.entries()) {
        expect(
          Math.abs(r.center - containerCenter),
          `row ${i} (n=${r.count}) not centred at ${width}px`,
        ).toBeLessThanOrEqual(2)
      }
    }
  })

  test("team section causes no horizontal overflow at any width", async ({ page }) => {
    // NOTE: the page as a whole has a pre-existing ~4px overflow at 1024px
    // from the site header's Login/Register buttons (it reproduces on / and
    // /courses too). Out of scope here, so this asserts the *team section
    // itself* never overflows its container.
    for (const width of [390, 768, 1024, 1440]) {
      await page.setViewportSize({ width, height: 900 })
      await page.goto("/about")
      await page.waitForTimeout(500)

      const section = await teamSection(page)
      const { sectionOverflow, docOverflow } = await section.evaluate((el) => {
        const container = el.querySelector("div.flex.flex-wrap")!
        const cr = container.getBoundingClientRect()
        // Measure only the cards. The section's own max-w-7xl wrapper is
        // legitimately full-width (it carries the px-4/6/8 gutter), so
        // including it would report the gutter as a false overflow.
        const widest = Math.max(
          ...[...container.children].map((c) => c.getBoundingClientRect().right),
        )
        return {
          sectionOverflow: Math.ceil(widest - cr.right),
          docOverflow:
            document.documentElement.scrollWidth - document.documentElement.clientWidth,
        }
      })

      expect(
        sectionOverflow,
        `team section overflows its container at ${width}px`,
      ).toBeLessThanOrEqual(1)
      console.log(`page overflow @${width}px = ${docOverflow}px`)
    }
  })

  test("cards are not clipped and have consistent widths", async ({ page }) => {
    for (const width of [390, 768, 1440]) {
      await page.setViewportSize({ width, height: 900 })
      await page.goto("/about")
      const section = await teamSection(page)

      const box = await cards(section).evaluateAll((els) =>
        els.map((e) => {
          const outer = e.closest("div[class*='max-w']")!.getBoundingClientRect()
          return { left: outer.left, right: outer.right, width: outer.width }
        }),
      )
      expect(box).toHaveLength(7)

      const container = await section
        .locator("div.flex.flex-wrap")
        .evaluate((e) => {
          const b = e.getBoundingClientRect()
          return { left: b.left, right: b.right }
        })

      for (const c of box) {
        expect(c.width).toBeGreaterThan(150) // not collapsed
        expect(c.width).toBeLessThanOrEqual(261) // respects max-w
        expect(c.left).toBeGreaterThanOrEqual(container.left - 1) // not clipped
        expect(c.right).toBeLessThanOrEqual(container.right + 1)
      }
      // consistent sizing within a row
      for (const c of box) expect(c.width).toBeLessThanOrEqual(box[0].width + 1)
    }
  })

  test("images are visible, uncropped-by-overlay and undistorted", async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto("/about")
    const section = await teamSection(page)
    await section.scrollIntoViewIfNeeded()
    await page.waitForTimeout(2000)

    const imgs = section.locator("img")
    await expect(imgs).toHaveCount(7)

    const stats = await imgs.evaluateAll((list) =>
      list.map((i) => {
        const b = i.getBoundingClientRect()
        const parent = i.parentElement!
        // any sibling/pseudo overlay sitting on top would hide the photo
        const overlays = [...parent.children].filter(
          (c) => c !== i && getComputedStyle(c).display !== "none",
        )
        return {
          src: i.getAttribute("src"),
          naturalWidth: i.naturalWidth,
          objectFit: getComputedStyle(i).objectFit,
          renderedRatio: b.width / b.height,
          naturalRatio: i.naturalWidth / i.naturalHeight,
          overlays: overlays.length,
        }
      }),
    )

    for (const s of stats) {
      expect(s.naturalWidth, `${s.src} did not decode`).toBeGreaterThan(0)
      expect(s.objectFit).toBe("cover")
      expect(s.overlays, `overlay covers ${s.src}`).toBe(0)
      expect(Math.abs(s.renderedRatio / s.naturalRatio - 1)).toBeLessThan(0.01)
    }
  })

  test("no marquee CSS left in the compiled stylesheet", async ({ page }) => {
    await page.goto("/about")
    const css = await page.evaluate(async () => {
      const links = [...document.querySelectorAll('link[rel="stylesheet"]')].map((l) => l.href)
      let text = ""
      for (const href of links) {
        try {
          text += await (await fetch(href)).text()
        } catch {}
      }
      return text
    })
    expect(css).not.toContain("team-marquee")
  })
})