import { expect, test } from '@playwright/test'

test('incident dashboard loads, starts one workflow, and shows persisted report', async ({ page }) => {
  let posts = 0
  await page.route('**/api/v1/**', async route => {
    const { pathname } = new URL(route.request().url())
    const method = route.request().method()
    const respond = (body: unknown) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) })
    if (pathname === '/api/v1/incidents') return respond([{
      id: 'INC-DB-001', title: 'Database connection failures', description: 'DB pool timed out',
      serviceName: 'order-service', occurredAt: '2026-10-01T10:00:00Z',
    }])
    if (pathname.endsWith('/workflows/async') && method === 'POST') {
      posts++
      return route.fulfill({ status: 202, contentType: 'application/json', body: JSON.stringify({ workflowId: 'w-1', status: 'PENDING' }) })
    }
    if (pathname === '/api/v1/workflows/w-1') return respond({
      id: 'w-1', incidentId: 'INC-DB-001', status: 'INSUFFICIENT_EVIDENCE',
      stepsUsed: 1, followUpsUsed: 0, currentRole: null,
    })
    if (pathname === '/api/v1/workflows/w-1/steps') return respond([])
    if (pathname.endsWith('/outputs/ROOT_CAUSE')) return respond({
      json: JSON.stringify({ hypotheses: [{ description: 'Unverified pool exhaustion', confidence: 'LOW', supportingEvidenceIds: [], contradictingEvidenceIds: [] }], sufficientEvidence: false, openQuestions: [] }),
    })
    if (pathname.endsWith('/outputs/REVIEWER')) return respond({
      json: JSON.stringify({ decision: 'INSUFFICIENT_EVIDENCE', rationale: 'Not enough evidence', claimReviews: [], unresolvedQuestions: [] }),
    })
    return route.fulfill({ status: 404, body: '{}' })
  })

  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Database connection failures' })).toBeVisible()
  await page.getByRole('button', { name: /Start investigation/ }).click()
  await expect(page.getByText('Not enough evidence')).toBeVisible()
  await expect(page.getByText(/Evidence was insufficient/)).toBeVisible()
  expect(posts).toBe(1)
})
