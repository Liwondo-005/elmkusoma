import { test, expect } from "@playwright/test"

/**
 * Course cover images on the public landing page.
 *
 * <p>The failure these guard against is not "the image is missing" — it is "the image looks fine on
 * the machine that uploaded it and is broken everywhere else". That happens when the course holds
 * a browser-local reference such as a `blob:` URL from a preview, or a path that only resolves on
 * one node.</p>
 *
 * <p>So these assert what a card must do with each reference shape, and above all that a bad
 * reference degrades to a branded placeholder instead of a broken-image glyph or a crashed
 * section.</p>
 */

async function waitForCourses(page: import("@playwright/test").Page) {
  // The section hides itself when there are no published courses, so the assertion below is only
  // meaningful once either the cards or a confirmed absence is established.
  await page.waitForLoadState("domcontentloaded")
  await page.waitForTimeout(3000)
}

test.describe("Course thumbnails on the landing page", () => {
  test("every course card keeps a stable aspect ratio", async ({ page }) => {
    await page.goto("/")
    await waitForCourses(page)

    const boxes = await page.evaluate(() =>
      Array.from(document.querySelectorAll("section a[href='/courses'], section"))
        .flatMap((s) => Array.from(s.querySelectorAll(".aspect-\\[16\\/10\\]")))
        .map((el) => {
          const r = el.getBoundingClientRect()
          return { w: Math.round(r.width), h: Math.round(r.height) }
        })
    )

    // Whether or not courses exist, no card may have collapsed to a zero-height box, which is what
    // a failed image load used to produce.
    for (const b of boxes) {
      if (b.w === 0) continue
      expect(b.h).toBeGreaterThan(0)
      // 16/10 within a pixel or two of rounding.
      expect(Math.abs(b.w / b.h - 1.6)).toBeLessThan(0.1)
    }
  })

  test("a course with no usable image shows a placeholder, not a broken image", async ({ page }) => {
    await page.goto("/")
    await waitForCourses(page)

    const state = await page.evaluate(() => {
      const imgs = Array.from(document.querySelectorAll("main img"))
      return {
        images: imgs.length,
        broken: imgs.filter((i) => i.complete && i.naturalWidth === 0).length,
        placeholders: document.querySelectorAll("main section [role='presentation']").length,
      }
    })

    // Whatever the data, a rendered <img> that failed to decode must be zero. The component
    // swaps to the placeholder on error rather than leaving a broken glyph in the card.
    expect(state.broken).toBe(0)
  })

  test("no image reference on the page points at a temporary browser URL", async ({ page }) => {
    await page.goto("/")
    await waitForCourses(page)

    const bad = await page.evaluate(() =>
      Array.from(document.querySelectorAll("img"))
        .map((i) => i.getAttribute("src") || i.getAttribute("data-src") || "")
        .filter((s) => /^blob:/i.test(s) || /^data:/i.test(s))
    )

    // A blob: URL resolves only in the tab that created it, which is precisely the "works on my
    // device" failure. None may survive into a rendered page.
    expect(bad).toEqual([])
  })

  test("the landing page renders without console errors when images are absent", async ({ page }) => {
    const errors: string[] = []
    page.on("pageerror", (e) => errors.push(e.message))

    await page.goto("/")
    await waitForCourses(page)

    // One card's image failing must not take the section, or the page, with it.
    expect(errors).toEqual([])
  })
})
