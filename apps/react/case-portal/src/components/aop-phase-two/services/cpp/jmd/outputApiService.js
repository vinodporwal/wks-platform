import Config from 'consts/index'
import { json } from 'services/request'

export const OutputApiService = {
  // Used in: Outputs/heat-rate/index.js (Final Heat Rate output grid)
  getFinalHeatRate,
  exportFinalHeatRateExcel,
}

// ===================== || FINAL HEAT RATE OUTPUT APIs || ===================== //
// GET /task/jmd/final-heat-rate?siteId=...&aopYear=...
// Returns aggregated final heat rate rows across all asset types for the given site.
// Each row is expected to contain: siteName, cppPlantName, assetType, assetName,
// utilityId, load, finalHeatRate.
async function getFinalHeatRate(keycloak, siteId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/jmd/final-heat-rate?siteId=${siteId}&aopYear=${aopYear}`
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
    console.error('Error fetching final heat rate data:', e)
    return await Promise.reject(e)
  }
}

// GET /task/jmd/final-heat-rate/export?siteId=...&aopYear=...
async function exportFinalHeatRateExcel(keycloak, siteId, aopYear, EXCEL_NAME) {
  const url = `${Config.CaseEngineUrl}/task/jmd/final-heat-rate/export?siteId=${siteId}&aopYear=${aopYear}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(
        `Failed to export Excel: ${resp.status} ${resp.statusText}`,
      )
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob

    // Extract filename from Content-Disposition header if available
    const contentDisposition = resp.headers.get('content-disposition')
    let downloadFileName = EXCEL_NAME
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename="?([^";\n]+)"?/i)
      if (filenameMatch && filenameMatch[1]) {
        downloadFileName = filenameMatch[1]
      }
    }

    a.download = downloadFileName
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)

    return { success: true, message: 'Excel exported successfully' }
  } catch (e) {
    console.error('Error exporting final heat rate Excel:', e)
    return Promise.reject(e)
  }
}
