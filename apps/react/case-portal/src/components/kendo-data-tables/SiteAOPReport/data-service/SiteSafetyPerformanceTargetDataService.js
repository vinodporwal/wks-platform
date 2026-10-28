import Config from 'consts'

export const SiteSafetyPerformanceTargetDataService = {
  getSiteSafetyPerformanceTargets,
  saveSiteSafetyPerformanceTargets,
  handleLoadSiteSafetyTarget,
  SiteSafetyPerformanceExport,
  ImportSiteSafetyPerformanceExcel,
}

export async function getSiteSafetyPerformanceTargets(
  keycloak,
  siteId,
  aopYear,
) {
  const url = `${Config.CaseEngineUrl}/task/site-safety-performance?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Site Safety Performance data:', e)
    return Promise.reject(e)
  }
}

export async function saveSiteSafetyPerformanceTargets(keycloak, payload) {
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
      body: JSON.stringify(payload),
    })
    return await resp.json()
  } catch (e) {
    console.error('Error saving Site Safety Performance data:', e)
    return Promise.reject(e)
  }
}

export async function handleLoadSiteSafetyTarget(keycloak, siteId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/site-safety-performance-load?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error loading Site Safety Performance SP data:', e)
    return Promise.reject(e)
  }
}

export async function SiteSafetyPerformanceExport(
  keycloak,
  siteId,
  aopYear,
  EXCEL_EXPORT_TITLE,
) {
  const url = `${Config.CaseEngineUrl}/task/site-safety-performance-export?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
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
    a.download = `${EXCEL_EXPORT_TITLE || 'Site_Safety_Performance_Targets'}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Site Safety Performance Excel:', e)
    return Promise.reject(e)
  }
}

export async function ImportSiteSafetyPerformanceExcel(
  file,
  keycloak,
  siteId,
  aopYear,
) {
  const url = `${Config.CaseEngineUrl}/task/site-safety-performance-import?siteId=${encodeURIComponent(siteId)}&aopYear=${encodeURIComponent(aopYear)}`
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
    console.error('Error importing Site Safety Performance Excel:', e)
    return Promise.reject(e)
  }
}
