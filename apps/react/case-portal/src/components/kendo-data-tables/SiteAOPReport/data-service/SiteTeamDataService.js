import Config from 'consts'

export const SiteTeamDataService = {
  getSiteTeamDetails,
  saveSiteTeam,
  SiteTeamExport,
  ImportSiteTeamExcel,
}

export async function getSiteTeamDetails(keycloak, siteId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/site-team-transaction?siteId=${encodeURIComponent(siteId)}&year=${encodeURIComponent(aopYear)}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Site Team data:', e)
    return Promise.reject(e)
  }
}

export async function saveSiteTeam(keycloak, siteId, aopYear, data) {
  const url = `${Config.CaseEngineUrl}/task/site-team-transaction?siteId=${encodeURIComponent(siteId)}&year=${encodeURIComponent(aopYear)}`
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
