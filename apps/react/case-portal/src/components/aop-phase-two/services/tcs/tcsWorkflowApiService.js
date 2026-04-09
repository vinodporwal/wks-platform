import Config from 'consts/index'
import { json } from '../../../../services/request'
import { getRoleLabel } from 'components/aop-phase-two/tcs/utils/roleUtils'

export const TcsWorkflowApiService = {
  // ============ Common/Shared APIs ============
  getWorkflowVariables,
  checkWorkflowStatus,
  triggerWorkflow,
  getPlantwiseHistory,
  getPlantDataForApproveReject,
  getAuditTrail,

  // ============ Plant Manager APIs ============
  saveRemark,
  getPlantManagerSubmissionHistory,

  // ============ EPS Engineer APIs ============
  epsEngineerSingleApproveReject,
  epsEngineerMultipleApproveReject,
  epsEngineerSubmission,
  getEpsEngineerSubmissionHistory,

  // ============ CTS/EPS Head APIs ============
  ctsHeadApproveReject,
  ctsHeadSubmission,
  getCtsHeadSubmissionHistory,
  getCtsHeadApproveRejectAuditTrail,

  // ============ Cluster Head APIs ============
  clusterHeadApproveReject,
  clusterHeadSubmission,
  getClusterHeadSubmissionHistory,
  getClusterHeadApproveRejectAuditTrail,

  // ============ Reset Workflow API ============
  resetWorkflow,
}

// ========================================================================
// ============ COMMON/SHARED APIs ============
// ========================================================================

async function getWorkflowVariables(keycloak, verticalId, siteId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/variables/${verticalId}/${siteId}/${aopYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function checkWorkflowStatus(keycloak, verticalId, siteId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/process-exists/${verticalId}/${siteId}/${aopYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function triggerWorkflow(keycloak, verticalId, siteId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/start/${verticalId}/${siteId}/${aopYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    // Backend returns plain text, not JSON
    const result = await resp.text()
    return { success: true, message: result }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getPlantwiseHistory(
  keycloak,
  plantId,
  siteId,
  verticalId,
  financialYear,
) {
  const url = `${Config.CaseEngineUrl}/task/plant-submission-audit-trail-by-tab/${plantId}/${siteId}/${verticalId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const data = await json(keycloak, resp)

    return data
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getPlantDataForApproveReject(
  keycloak,
  siteId,
  verticalId,
  year,
) {
  const url = `${Config.CaseEngineUrl}/task/ebs-approve-reject-audit-trail/${siteId}/${verticalId}/${year}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const data = await json(keycloak, resp)

    return data
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ========================================================================
// ============ PLANT MANAGER APIs ============
// ========================================================================

async function saveRemark(
  keycloak,
  plantId,
  plantName,
  siteId,
  verticalId,
  userRole,
  userName,
  remark,
  aopYear,
) {
  const url = `${Config.CaseEngineUrl}/task/complete-plant-submission-task/${plantName}/${siteId}/${aopYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify({
    plantId,
    plantName,
    siteId,
    verticalId,
    submittedBy: getRoleLabel(userRole),
    userName,
    submissionRemark: remark,
  })
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    // Backend returns plain text, not JSON
    const result = await resp.text()
    return { success: true, message: result }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getPlantManagerSubmissionHistory(
  keycloak,
  plantId,
  siteId,
  verticalId,
  financialYear,
) {
  const url = `${Config.CaseEngineUrl}/task/plant-submission-audit-trail/${plantId}/${siteId}/${verticalId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const data = await json(keycloak, resp)
    return data
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ========================================================================
// ============ EPS ENGINEER APIs ============
// ========================================================================

async function epsEngineerSingleApproveReject(
  keycloak,
  plantId,
  siteId,
  verticalId,
  approvalStatus,
  remark,
  year,
  userRole,
  userName,
  plantName,
) {
  const url = `${Config.CaseEngineUrl}/task/ebs-approve-reject/${plantName}/${siteId}/${approvalStatus}/${year}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify({
    plantId,
    plantName,
    siteId,
    verticalId,
    submissionRemark: remark,
    submittedBy: getRoleLabel(userRole),
    userName,
  })
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    // Backend returns plain text, not JSON
    const result = await resp.text()
    return { success: true, message: result }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function epsEngineerMultipleApproveReject(
  keycloak,
  siteId,
  approvalStatus,
  year,
  plantSubmissionList,
) {
  const url = `${Config.CaseEngineUrl}/task/bulk-ebs-approve-reject/${siteId}/${approvalStatus}/${year}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(plantSubmissionList)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    // Backend returns plain text, not JSON
    const result = await resp.text()
    return { success: true, message: result }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function epsEngineerSubmission(
  keycloak,
  plantName,
  siteId,
  verticalId,
  financialYear,
  remark,
  userRole,
  userName,
) {
  const url = `${Config.CaseEngineUrl}/task/ebs-submission/${siteId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify({
    siteId: siteId,
    verticalId: verticalId,
    submissionRemark: remark,
    submittedBy: getRoleLabel(userRole),
    userName,
  })
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    // Backend returns plain text, not JSON
    const result = await resp.text()
    return { success: true, message: result }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getEpsEngineerSubmissionHistory(
  keycloak,
  siteId,
  verticalId,
  financialYear,
) {
  const url = `${Config.CaseEngineUrl}/task/ebs-submission-audit-trail/${siteId}/${verticalId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const data = await json(keycloak, resp)

    return data
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ========================================================================
// ============ CTS/EPS HEAD APIs ============
// ========================================================================

async function ctsHeadApproveReject(payload, approvalStatus) {
  const {
    keycloak,
    SITE_ID,
    AOP_YEAR,
    remark,
    userRole,
    userName,
    VERTICAL_ID,
  } = payload
  const url = `${Config.CaseEngineUrl}/task/cts-approve-reject/${SITE_ID}/${approvalStatus}/${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify({
    siteId: SITE_ID,
    verticalId: VERTICAL_ID,
    submissionRemark: remark,
    submittedBy: getRoleLabel(userRole),
    userName,
  })
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    // Backend returns plain text, not JSON
    const result = await resp.text()
    return { success: true, message: result }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function ctsHeadSubmission(payload) {
  const {
    keycloak,
    SITE_ID,
    AOP_YEAR,
    remark,
    userRole,
    userName,
    VERTICAL_ID,
  } = payload
  const url = `${Config.CaseEngineUrl}/task/cts-submission/${SITE_ID}/${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify({
    siteId: SITE_ID,
    verticalId: VERTICAL_ID,
    submissionRemark: remark,
    submittedBy: getRoleLabel(userRole),
    userName,
  })
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    // Backend returns plain text, not JSON
    const result = await resp.text()
    return { success: true, message: result }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getCtsHeadSubmissionHistory(
  keycloak,
  siteId,
  verticalId,
  financialYear,
) {
  const url = `${Config.CaseEngineUrl}/task/cts-submission-audit-trail/${siteId}/${verticalId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const data = await json(keycloak, resp)

    return data
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getCtsHeadApproveRejectAuditTrail(
  keycloak,
  siteId,
  verticalId,
  financialYear,
) {
  const url = `${Config.CaseEngineUrl}/task/cts-approve-reject-audit-trail/${siteId}/${verticalId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const data = await json(keycloak, resp)

    return data
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ========================================================================
// ============ CLUSTER HEAD APIs ============
// ========================================================================

async function clusterHeadApproveReject(payload, approvalStatus) {
  const {
    keycloak,
    SITE_ID,
    AOP_YEAR,
    remark,
    userRole,
    userName,
    VERTICAL_ID,
  } = payload
  const url = `${Config.CaseEngineUrl}/task/cluster-head-approve-reject/${SITE_ID}/${approvalStatus}/${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify({
    siteId: SITE_ID,
    verticalId: VERTICAL_ID,
    submissionRemark: remark,
    submittedBy: getRoleLabel(userRole),
    userName,
  })
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    // Backend returns plain text, not JSON
    const result = await resp.text()
    return { success: true, message: result }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function clusterHeadSubmission(payload) {
  const {
    keycloak,
    SITE_ID,
    AOP_YEAR,
    remark,
    userRole,
    userName,
    VERTICAL_ID,
  } = payload
  const url = `${Config.CaseEngineUrl}/task/cluster-head-submission/${SITE_ID}/${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify({
    siteId: SITE_ID,
    verticalId: VERTICAL_ID,
    submissionRemark: remark,
    submittedBy: getRoleLabel(userRole),
    userName,
  })
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    // Backend returns plain text, not JSON
    const result = await resp.text()
    return { success: true, message: result }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}
async function getClusterHeadSubmissionHistory(
  keycloak,
  siteId,
  verticalId,
  financialYear,
) {
  const url = `${Config.CaseEngineUrl}/task/cluster-head-submission-audit-trail/${siteId}/${verticalId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const data = await json(keycloak, resp)

    return data
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getClusterHeadApproveRejectAuditTrail(
  keycloak,
  siteId,
  verticalId,
  financialYear,
) {
  const url = `${Config.CaseEngineUrl}/task/cluster-head-approve-reject-audit-trail/${siteId}/${verticalId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const data = await json(keycloak, resp)

    return data
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ========================================================================
// ============ RESET WORKFLOW API ============
// ========================================================================

async function resetWorkflow(
  keycloak,
  siteId,
  financialYear,
  userRole,
  verticalId,
) {
  const url = `${Config.CaseEngineUrl}/task/delete-process-instance/${verticalId}/${siteId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'DELETE',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    // Backend returns plain text, not JSON
    const result = await resp.text()
    return { success: true, message: result }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ========================================================================
// ============ AUDIT TRAIL API ============
// ========================================================================

async function getAuditTrail(keycloak, verticalId, siteId, financialYear) {
  const url = `${Config.CaseEngineUrl}/task/audit-trail/${verticalId}/${siteId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await resp.json()
    return result
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}
