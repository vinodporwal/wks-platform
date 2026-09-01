import Config from 'consts/index'
import { json } from 'services/request'

export const OutputApiService = {
  // Used in: Outputs/heat-rate/index.js (Final Heat Rate output grid)
  getFinalHeatRate,
  exportFinalHeatRateExcel,

  // Used in: Outputs/sr-mapping/index.js (SR Mapping output grid)
  getSRMappingOutput,
  exportSRMappingOutputExcel,

  // Used in: Outputs/average-asset-loading/index.js (Average Asset Loading output grid)
  getAverageAssetLoading,
  exportAverageAssetLoadingExcel,
}

// ===================== GENERIC HELPERS ===================== //

function buildPlantIdsParam(plantIds) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return plantIdArray.join(',')
}

// ===================== || FINAL HEAT RATE OUTPUT APIs || ===================== //
// GET /task/jmd/final-heat-rate?plantIds=...&aopYear=...
// Returns aggregated final heat rate rows across all asset types for the given plant(s).
// Each row is expected to contain: siteName, cppPlantName, assetType, assetName,
// utilityId, load, finalHeatRate.
async function getFinalHeatRate(keycloak, plantIds, aopYear) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/final-heat-rate?plantIds=${queryParams}&aopYear=${aopYear}`
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

// GET /task/jmd/final-heat-rate/export?plantIds=...&aopYear=...
async function exportFinalHeatRateExcel(
  keycloak,
  plantIds,
  aopYear,
  EXCEL_NAME,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/final-heat-rate/export?plantIds=${queryParams}&aopYear=${aopYear}`
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

// ===================== || AVERAGE ASSET LOADING OUTPUT APIs || ===================== //
// GET /task/jmd/average-asset-loading?plantIds=...&aopYear=...
// Returns average asset loading rows for the given plant(s).
// Each row is expected to contain: cppPlantName, generatingPlantName, generatingUom,
// generatingUtility, issuingMaterial, issuingPlantName, and 12 monthly fields (apr → mar).
async function getAverageAssetLoading(keycloak, plantIds, aopYear) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/average-asset-loading?plantIds=${queryParams}&aopYear=${aopYear}`
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
    console.error('Error fetching average asset loading data:', e)
    return await Promise.reject(e)
  }
}

// GET /task/jmd/average-asset-loading/export?plantIds=...&aopYear=...
async function exportAverageAssetLoadingExcel(
  keycloak,
  plantIds,
  aopYear,
  EXCEL_NAME,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/average-asset-loading/export?plantIds=${queryParams}&aopYear=${aopYear}`
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
    console.error('Error exporting average asset loading Excel:', e)
    return Promise.reject(e)
  }
}

// ===================== || SR MAPPING OUTPUT APIs || ===================== //
// GET /task/sr-mapping-qty?plantIds=...&financialYear=...
// Calls SP CPP_GetSRMappingQTY.
// Returns SR mapping output rows with monthly QTY values for the given plant(s).
// Field names follow SenderReceiverMapping.js convention:
//   id, cppPlantId, cppPlantName,
//   senderPlantName, senderPlantCode, senderPlantId,
//   senderUtilityId, senderUtilityName, senderUtilityCode, senderUtilityUOM,
//   senderCostCenterId, senderCostCenterName, senderCostCenterCode,
//   receiverPlantName, receiverPlantCode, receiverPlantId,
//   receiverUtilityId, receiverUtilityName, receiverUtilityCode, receiverUtilityUOM,
//   receiverCostCenterId, receiverCostCenterName, receiverCostCenterCode,
//   remarks, and 12 monthly QTY fields (apr → mar).
async function getSRMappingOutput(keycloak, plantIds, financialYear) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/sr-mapping-qty?plantIds=${queryParams}&financialYear=${financialYear}`
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
    console.error('Error fetching SR mapping output data:', e)
    return await Promise.reject(e)
  }
}

// GET /task/sr-mapping-qty/export?plantIds=...&financialYear=...
async function exportSRMappingOutputExcel(
  keycloak,
  plantIds,
  financialYear,
  EXCEL_NAME,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/sr-mapping-qty/export?plantIds=${queryParams}&financialYear=${financialYear}`
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
    console.error('Error exporting SR mapping output Excel:', e)
    return Promise.reject(e)
  }
}
