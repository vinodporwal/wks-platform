import Config from '../consts'
import { json } from './request'

export const SiteReportDataService = {
  getSiteTeamDetails,
  saveSiteTeam,
  getEnergyPerformanceDetails,
  saveEnergyPerformance,
  exportEnergyPerformance,
  importEnergyPerformance,
  getPerformanceHighlightsSummary,
  savePerformanceHighlightsSummary,
  getSlowdownPlan,
  saveSlowdownPlan,
  getFixedExpensesData,
  saveFixedExpensesData,
  getCapexData,
  saveCapexData,
  getTechnicalAvailability,
  saveTechnicalAvailability,
  getMajorSafetyInitiative,
  saveMajorSafetyInitiative,
  deleteMajorSafetyInitiative,
  getPlantDropdownForSiteAOPReport,
  exportMajorSafetyInitiative,
  importMajorSafetyInitiative,
  getMajorProfitImprovement,
  saveMajorProfitImprovement,
  getMajorReliabilityImprovement,
  saveMajorReliabilityImprovement,
  getMajorPeopleInitiative,
  saveMajorPeopleInitiative,
  deleteMajorPeopleInitiative,
  exportMajorPeopleInitiative,
  importMajorPeopleInitiative,
  getMCUCapacityUtilization,
  saveMCUCapacityUtilization,
  getSitesafetyPerformance,
  saveSitesafetyPerformance,
  getConversionVariableCost,
  saveConversionVariableCost,
  getSiteAopReportTabs,
  SiteTeamExport,
  ImportSiteTeamExcel,
  deleteMajorReliabilityImprovement,
  exportMajorReliabilityImprovement,
  importMajorReliabilityImprovement,
  deleteMajorProfitImprovement,
  exportMajorProfitImprovement,
  importMajorProfitImprovement,
  exportFixedExpensesData,
  importFixedExpensesData,
  exportCapexData,
  importCapexData,
  deleteCapex,
}
export async function getSiteTeamDetails(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/site-team-transaction?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Plant Team data:', e)
    return Promise.reject(e)
  }
}
export async function saveSiteTeam(keycloak, SITE_ID, AOP_YEAR, data) {
  const url = `${Config.CaseEngineUrl}/task/site-team-transaction?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Site Team data:', e)
    return Promise.reject(e)
  }
}
export async function getEnergyPerformanceDetails(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/energy-performance?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Energy Performance data:', e)
    return Promise.reject(e)
  }
}
export async function saveEnergyPerformance(keycloak, SITE_ID, AOP_YEAR, data) {
  const url = `${Config.CaseEngineUrl}/task/energy-performance?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Energy Performance data:', e)
    return Promise.reject(e)
  }
}

export async function exportEnergyPerformance(
  keycloak,
  siteId,
  aopYear,
  excelName,
) {
  const url = `${Config.CaseEngineUrl}/task/energy-performance-export?siteId=${encodeURIComponent(siteId)}&year=${encodeURIComponent(aopYear)}&excelName=${encodeURIComponent(excelName || 'energy_performance')}`
  const headers = {
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${excelName || 'energy_performance'}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Energy Performance Excel:', e)
    return Promise.reject(e)
  }
}

export async function importEnergyPerformance(file, keycloak, siteId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/energy-performance-import?siteId=${encodeURIComponent(siteId)}&year=${encodeURIComponent(aopYear)}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: formData,
    })
    return await resp.json()
  } catch (e) {
    console.error('Error importing Energy Performance Excel:', e)
    return Promise.reject(e)
  }
}
export async function getPerformanceHighlightsSummary(
  keycloak,
  SITE_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/performance-highlights?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Performance Highlights Summary data:', e)
    return Promise.reject(e)
  }
}

export async function savePerformanceHighlightsSummary(
  keycloak,
  SITE_ID,
  AOP_YEAR,
  data,
) {
  const url = `${Config.CaseEngineUrl}/task/performance-highlights?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Performance Highlights Summary data:', e)
    return Promise.reject(e)
  }
}
export async function getSlowdownPlan(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-slowdown-plan?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Shutdown / Slowdown Plan data:', e)
    return Promise.reject(e)
  }
}
export async function saveSlowdownPlan(keycloak, SITE_ID, AOP_YEAR, data) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-slowdown-plan?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Shutdown / Slowdown Plan data:', e)
    return Promise.reject(e)
  }
}
export async function getFixedExpensesData(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/report-fixed-expenses?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Fixed Expenses data:', e)
    return Promise.reject(e)
  }
}
export async function saveFixedExpensesData(keycloak, SITE_ID, AOP_YEAR, data) {
  const url = `${Config.CaseEngineUrl}/task/report-fixed-expenses?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Fixed Expenses data:', e)
    return Promise.reject(e)
  }
}

export async function exportFixedExpensesData(
  keycloak,
  siteId,
  aopYear,
  excelName,
) {
  const url = `${Config.CaseEngineUrl}/task/report-fixed-expenses-export?siteId=${encodeURIComponent(siteId)}&year=${encodeURIComponent(aopYear)}`
  const headers = {
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${excelName || 'fixed_expenses'}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Fixed Expenses Excel:', e)
    return Promise.reject(e)
  }
}

export async function importFixedExpensesData(file, keycloak, siteId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/report-fixed-expenses-import?siteId=${encodeURIComponent(siteId)}&year=${encodeURIComponent(aopYear)}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: formData,
    })
    return await resp.json()
  } catch (e) {
    console.error('Error importing Fixed Expenses Excel:', e)
    return Promise.reject(e)
  }
}
export async function getCapexData(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/capex-pio?plantId=${PLANT_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Capex data:', e)
    return Promise.reject(e)
  }
}
export async function saveCapexData(keycloak, PLANT_ID, AOP_YEAR, data) {
  const url = `${Config.CaseEngineUrl}/task/capex-pio?plantId=${PLANT_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Capex data:', e)
    return Promise.reject(e)
  }
}
export async function deleteCapex(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/report-capex-pioplan/${id}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error deleting Fixed Expense record:', e)
    return Promise.reject(e)
  }
}

export async function exportCapexData(
  keycloak,
  siteId,
  aopYear,
  excelName,
) {
  const url = `${Config.CaseEngineUrl}/task/report-capex-pioplan-export?siteId=${encodeURIComponent(siteId)}&year=${encodeURIComponent(aopYear)}&excelName=${encodeURIComponent(excelName || 'capex_pioplan')}`
  const headers = {
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${excelName || 'fixed_expenses'}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Fixed Expenses Excel:', e)
    return Promise.reject(e)
  }
}

export async function importCapexData(file, keycloak, siteId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/report-capex-pioplan-import?siteId=${encodeURIComponent(siteId)}&year=${encodeURIComponent(aopYear)}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: formData,
    })
    return await resp.json()
  } catch (e) {
    console.error('Error importing Fixed Expenses Excel:', e)
    return Promise.reject(e)
  }
}
export async function getTechnicalAvailability(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/technical-availability?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Technical Availability data:', e)
    return Promise.reject(e)
  }
}
export async function saveTechnicalAvailability(
  keycloak,
  SITE_ID,
  AOP_YEAR,
  data,
) {
  const url = `${Config.CaseEngineUrl}/task/technical-availability?siteId=${SITE_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Technical Availability data:', e)
    return Promise.reject(e)
  }
}

// Major Safety Improvement Initiative
export async function getMajorSafetyInitiative(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/major-safety-improvement-initiative?siteId=${SITE_ID}&aopYear=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Major Safety Initiative data:', e)
    return Promise.reject(e)
  }
}
export async function saveMajorSafetyInitiative(
  keycloak,
  SITE_ID,
  AOP_YEAR,
  data,
) {
  const url = `${Config.CaseEngineUrl}/task/major-safety-improvement-initiative?aopYear=${AOP_YEAR}&siteId=${SITE_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Major Safety Initiative data:', e)
    return Promise.reject(e)
  }
}

export async function deleteMajorSafetyInitiative(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/major-safety-improvement-initiative/${id}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error deleting Major Safety Initiative record:', e)
    return Promise.reject(e)
  }
}

export async function getPlantDropdownForSiteAOPReport(keycloak, siteId) {
  const url = `${Config.CaseEngineUrl}/task/plant-dropdown-for-site-aop-report?siteId=${siteId}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching plant dropdown for Site AOP Report:', e)
    return Promise.reject(e)
  }
}

export async function exportMajorSafetyInitiative(
  keycloak,
  siteId,
  aopYear,
  excelName,
) {
  const url = `${Config.CaseEngineUrl}/task/major-safety-improvement-initiative-export?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${excelName || 'Major_Safety_Initiative'}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Major Safety Initiative Excel:', e)
    return Promise.reject(e)
  }
}

export async function importMajorSafetyInitiative(
  file,
  keycloak,
  siteId,
  aopYear,
) {
  const url = `${Config.CaseEngineUrl}/task/major-safety-improvement-initiative-import?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: formData,
    })
    return await resp.json()
  } catch (e) {
    console.error('Error importing Major Safety Initiative Excel:', e)
    return Promise.reject(e)
  }
}

// Major Profit Improvement
export async function getMajorProfitImprovement(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/major-profit-improvement?siteId=${SITE_ID}&aopYear=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Major Profit Improvement data:', e)
    return Promise.reject(e)
  }
}
export async function saveMajorProfitImprovement(
  keycloak,
  SITE_ID,
  AOP_YEAR,
  data,
) {
  const url = `${Config.CaseEngineUrl}/task/major-profit-improvement?aopYear=${AOP_YEAR}&siteId=${SITE_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Major Profit Improvement data:', e)
    return Promise.reject(e)
  }
}

export async function deleteMajorProfitImprovement(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/major-profit-improvement/${id}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error deleting Major Profit Improvement record:', e)
    return Promise.reject(e)
  }
}

export async function exportMajorProfitImprovement(
  keycloak,
  siteId,
  aopYear,
  excelName,
) {
  const url = `${Config.CaseEngineUrl}/task/major-profit-improvement-export?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${excelName || 'Major_Profit_Improvement'}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Major Profit Improvement Excel:', e)
    return Promise.reject(e)
  }
}

export async function importMajorProfitImprovement(
  file,
  keycloak,
  siteId,
  aopYear,
) {
  const url = `${Config.CaseEngineUrl}/task/major-profit-improvement-import?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: formData,
    })
    return await resp.json()
  } catch (e) {
    console.error('Error importing Major Profit Improvement Excel:', e)
    return Promise.reject(e)
  }
}
// Major Reliability Improvement
export async function getMajorReliabilityImprovement(
  keycloak,
  SITE_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/major-reliability-improvement?siteId=${SITE_ID}&aopYear=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Major Reliability Improvement data:', e)
    return Promise.reject(e)
  }
}
export async function saveMajorReliabilityImprovement(
  keycloak,
  SITE_ID,
  AOP_YEAR,
  data,
) {
  const url = `${Config.CaseEngineUrl}/task/major-reliability-improvement?aopYear=${AOP_YEAR}&siteId=${SITE_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Major Reliability Improvement data:', e)
    return Promise.reject(e)
  }
}
export async function deleteMajorReliabilityImprovement(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/major-reliability-improvement/${id}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error deleting Major Safety Initiative record:', e)
    return Promise.reject(e)
  }
}
export async function exportMajorReliabilityImprovement(
  keycloak,
  siteId,
  aopYear,
  excelName,
) {
  const url = `${Config.CaseEngineUrl}/task/major-reliability-improvement-export?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${excelName || 'Major_Reliability_Improvement'}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Major Reliability Improvement Excel:', e)
    return Promise.reject(e)
  }
}
export async function importMajorReliabilityImprovement(
  file,
  keycloak,
  siteId,
  aopYear,
) {
  const url = `${Config.CaseEngineUrl}/task/major-reliability-improvement-import?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: formData,
    })
    return await resp.json()
  } catch (e) {
    console.error('Error importing Major Reliability Improvement Excel:', e)
    return Promise.reject(e)
  }
}

// Major People Initiative
export async function getMajorPeopleInitiative(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/major-people-initiative?siteId=${SITE_ID}&aopYear=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Major People Initiative data:', e)
    return Promise.reject(e)
  }
}
export async function saveMajorPeopleInitiative(
  keycloak,
  SITE_ID,
  AOP_YEAR,
  data,
) {
  const url = `${Config.CaseEngineUrl}/task/major-people-initiative?aopYear=${AOP_YEAR}&siteId=${SITE_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Major People Initiative data:', e)
    return Promise.reject(e)
  }
}

export async function deleteMajorPeopleInitiative(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/major-people-initiative/${id}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error deleting Major People Initiative record:', e)
    return Promise.reject(e)
  }
}

export async function exportMajorPeopleInitiative(
  keycloak,
  siteId,
  aopYear,
  excelName,
) {
  const url = `${Config.CaseEngineUrl}/task/major-people-initiative-export?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${excelName || 'Major_People_Initiative'}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Major People Initiative Excel:', e)
    return Promise.reject(e)
  }
}

export async function importMajorPeopleInitiative(
  file,
  keycloak,
  siteId,
  aopYear,
) {
  const url = `${Config.CaseEngineUrl}/task/major-people-initiative-import?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: formData,
    })
    return await resp.json()
  } catch (e) {
    console.error('Error importing Major People Initiative Excel:', e)
    return Promise.reject(e)
  }
}

// MCU Capacity Utilization
export async function getMCUCapacityUtilization(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/mcu-capacity-utilization?siteId=${SITE_ID}&aopYear=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching MCU Capacity Utilization data:', e)
    return Promise.reject(e)
  }
}
export async function saveMCUCapacityUtilization(
  keycloak,
  SITE_ID,
  AOP_YEAR,
  data,
) {
  const url = `${Config.CaseEngineUrl}/task/mcu-capacity-utilization?aopYear=${AOP_YEAR}&siteId=${SITE_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving MCU Capacity Utilization data:', e)
    return Promise.reject(e)
  }
}
export async function getSitesafetyPerformance(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/site-safety-performance?siteId=${SITE_ID}&aopYear=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching MCU Capacity Utilization data:', e)
    return Promise.reject(e)
  }
}
export async function saveSitesafetyPerformance(keycloak, data) {
  const url = `${Config.CaseEngineUrl}/task/site-safety-performance`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving MCU Capacity Utilization data:', e)
    return Promise.reject(e)
  }
}
export async function getConversionVariableCost(keycloak, SITE_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/conversion-variable-cost?siteId=${SITE_ID}&aopYear=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Conversion Variable Cost data:', e)
    return Promise.reject(e)
  }
}
export async function saveConversionVariableCost(
  keycloak,
  SITE_ID,
  AOP_YEAR,
  data,
) {
  const url = `${Config.CaseEngineUrl}/task/conversion-variable-cost`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Conversion Variable Cost data:', e)
    return Promise.reject(e)
  }
}

export async function getSiteAopReportTabs(keycloak, SITE_ID) {
  const url = `${Config.CaseEngineUrl}/task/site-aop-report-tabs${SITE_ID ? `?siteId=${encodeURIComponent(SITE_ID)}` : ''}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Site AOP Report tabs:', e)
    return Promise.reject(e)
  }
}

export async function SiteTeamExport(
  keycloak,
  siteId,
  year,
  EXCEL_EXPORT_TITLE,
) {
  const url = `${Config.CaseEngineUrl}/task/site-team-export?siteId=${encodeURIComponent(siteId)}&year=${encodeURIComponent(year)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE || 'Site_Team'}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Site Team Excel:', e)
    return Promise.reject(e)
  }
}

export async function ImportSiteTeamExcel(file, keycloak, siteId, year) {
  const url = `${Config.CaseEngineUrl}/task/site-team-import?siteId=${encodeURIComponent(siteId)}&year=${encodeURIComponent(year)}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: formData,
    })
    return await resp.json()
  } catch (e) {
    console.error('Error importing Site Team Excel:', e)
    return Promise.reject(e)
  }
}

