import Config from '../consts'
import { json } from './request'

// AopApprovalService — client for the new isolated AOP approval workflow
// (backend base path: /aop-approval). Keyed on (plantId, year). Buttons are
// driven by the server-computed `viewer` block returned from getStatus.
export const AopApprovalService = {
  start,
  act,
  getStatus,
  getMyPending,
  getAuditTrail,
}

const base = () => `${Config.CaseEngineUrl}/aop-approval`

function authHeaders(keycloak) {
  return {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
}

// Start a new AOP approval workflow for a plant + year (409 if one already exists).
async function start(keycloak, plantId, year, remark = '', actorRole = '') {
  await keycloak.updateToken(30)
  let url = `${base()}/start?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}`
  if (remark) url += `&remark=${encodeURIComponent(remark)}`
  if (actorRole) url += `&actorRole=${encodeURIComponent(actorRole)}`
  const resp = await fetch(url, {
    method: 'POST',
    headers: authHeaders(keycloak),
    body: JSON.stringify({ remark, actorRole }),
  })
  return json(keycloak, resp)
}

// Apply a gate decision. decision = 'APPROVED' | 'REVERTED'.
async function act(
  keycloak,
  { taskId, plantId, year, gateName, decision, remark, actorRole },
) {
  await keycloak.updateToken(30)
  const resp = await fetch(`${base()}/act`, {
    method: 'POST',
    headers: authHeaders(keycloak),
    body: JSON.stringify({
      taskId,
      plantId,
      year,
      gateName,
      decision,
      remark,
      actorRole,
    }),
  })
  if (resp.status === 204) return true
  if (!resp.ok) {
    const text = await resp.text().catch(() => '')
    throw new Error(`Server error ${resp.status}: ${text}`.trim())
  }
  return true
}

// Status + server-computed button state (viewer) for a single (plant, year).
async function getStatus(keycloak, plantId, year) {
  const url = `${base()}/status?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}`
  const resp = await fetch(url, {
    method: 'GET',
    headers: authHeaders(keycloak),
  })
  return json(keycloak, resp)
}

// The caller's "My Approvals" inbox across all plants.
async function getMyPending(keycloak) {
  const resp = await fetch(`${base()}/my-pending`, {
    method: 'GET',
    headers: authHeaders(keycloak),
  })
  return json(keycloak, resp)
}

// Full audit trail for a plant + year.
async function getAuditTrail(keycloak, plantId, year) {
  const url = `${base()}/audit-trail?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}`
  const resp = await fetch(url, {
    method: 'GET',
    headers: authHeaders(keycloak),
  })
  return json(keycloak, resp)
}
