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

  // Used in: Outputs/monthly-calculated-norms/index.js (Monthly Calculated Norms output grid)
  getMonthlyCalculatedNorms,
  exportMonthlyCalculatedNormsExcel,

  // Used in: Outputs/utility-rate/UtilityRateGrid.js (Utility Rate output grid)
  getUtilityRateData,
  exportUtilityRateExcel,

  // Used in: Outputs/spinning-margin-report/index.js (Spinning Margin report grid)
  getSpinningMarginReport,
  exportSpinningMarginReportExcel,

  // Used in: Outputs/power-purchase-sell/PowerDemandGrid.js (Power Demand output grid)
  getPowerDemand,
  exportPowerDemand: exportPowerDemandExcel,
  exportPowerDemandExcel,
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
// GET /task/sr-mapping-qty?plantIds=...&financialYear=...&dataType=...
// Calls SP CPP_GetSRMappingQTY (dataType=utility, default) or
//         CPP_GetSRPlantFixedSummary (dataType=all — JMD: utility+process+fixed).
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
async function getSRMappingOutput(keycloak, plantIds, financialYear, dataType = 'utility') {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/sr-mapping-qty?plantIds=${queryParams}&financialYear=${financialYear}&dataType=${dataType}`
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

// GET /task/sr-mapping-qty/export?plantIds=...&financialYear=...&dataType=...
async function exportSRMappingOutputExcel(
  keycloak,
  plantIds,
  financialYear,
  EXCEL_NAME,
  dataType = 'utility',
) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/sr-mapping-qty/export?plantIds=${queryParams}&financialYear=${financialYear}&dataType=${dataType}`
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

// ===================== || MONTHLY CALCULATED NORMS OUTPUT APIs || ===================== //
// GET /task/jmd/monthly-calculated-norms?plantIds=...&aopYear=...&fromDate=...&toDate=...
// Calls SP CPP_GetMonthWiseFixedCalculatedUtilityNorms.
// Returns monthly calculated norms rows for the given plant(s).
// Each row is expected to contain: generatingPlantName, utilityName, utilityId,
// uom, accountName, materialName, materialId, issuingPlantName, issuingUom,
// normTypeName, and 12 monthly fields (aprNorms → marNorms).
async function getMonthlyCalculatedNorms(
  keycloak,
  plantIds,
  aopYear,
  fromDate,
  toDate,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/monthly-calculated-norms?plantIds=${queryParams}&aopYear=${aopYear}&fromDate=${fromDate}&toDate=${toDate}`
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
    console.error('Error fetching monthly calculated norms data:', e)
    return await Promise.reject(e)
  }
}

// GET /task/jmd/monthly-calculated-norms/export?plantIds=...&aopYear=...&fromDate=...&toDate=...
async function exportMonthlyCalculatedNormsExcel(
  keycloak,
  plantIds,
  aopYear,
  EXCEL_NAME,
  fromDate,
  toDate,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/monthly-calculated-norms/export?plantIds=${queryParams}&aopYear=${aopYear}&fromDate=${fromDate}&toDate=${toDate}`
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
    console.error('Error exporting monthly calculated norms Excel:', e)
    return Promise.reject(e)
  }
}

// ===================== || UTILITY RATE OUTPUT APIs || ===================== //
// GET /task/jmd/cpp-utility-rates?plantIds=...&aopYear=...
// Returns utility rate rows across all selected CPP plants for the given AOP year (multiplant).
// Each row is expected to contain: siteDescription, utilityPlant, utilityPlantId,
// utilityName, utilityId, uom, weightedAvgPrice, and 12 monthly price fields (apr → mar).
async function getUtilityRateData(keycloak, plantIds, aopYear) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/cpp-utility-rates?plantIds=${queryParams}&aopYear=${aopYear}`
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
    console.error('Error fetching utility rate data:', e)
    return await Promise.reject(e)
  }
}

// GET /task/jmd/cpp-utility-rates/export?plantIds=...&aopYear=...
async function exportUtilityRateExcel(keycloak, plantIds, aopYear, EXCEL_NAME) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/cpp-utility-rates/export?plantIds=${queryParams}&aopYear=${aopYear}`
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
    console.error('Error exporting utility rate Excel:', e)
    return Promise.reject(e)
  }
}

// ===================== || SPINNING MARGIN REPORT APIs || ===================== //
// GET /task/jmd/spinning-margin-report?aopYear=...[&plantIds=...]
// Returns Spinning Margin Maintained rows from dbo.CPPMonthlySpinningMarginMaintained
// for the given AOP year. plantIds is optional — when omitted, all rows for the
// year are returned; rows where Plant_FK_Id IS NULL are common to all plants.
// Each row contains: id, aopYear, marginType, plantFkId, plantName,
// 12 monthly margin fields (apr → mar).
async function getSpinningMarginReport(keycloak, plantIds, aopYear) {
  const plantParams = plantIds?.length
    ? `&plantIds=${buildPlantIdsParam(plantIds)}`
    : ''
  const url = `${Config.CaseEngineUrl}/task/jmd/spinning-margin-report?aopYear=${aopYear}${plantParams}`
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
    console.error('Error fetching Spinning Margin report data:', e)
    return await Promise.reject(e)
  }
}

// GET /task/jmd/spinning-margin-report/export?aopYear=...[&plantIds=...]
async function exportSpinningMarginReportExcel(
  keycloak,
  plantIds,
  aopYear,
  EXCEL_NAME,
) {
  const plantParams = plantIds?.length
    ? `&plantIds=${buildPlantIdsParam(plantIds)}`
    : ''
  const url = `${Config.CaseEngineUrl}/task/jmd/spinning-margin-report/export?aopYear=${aopYear}${plantParams}`
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
    console.error('Error exporting Spinning Margin report Excel:', e)
    return Promise.reject(e)
  }
}

// ===================== || POWER DEMAND OUTPUT APIs || ===================== //
// GET /task/jmd/power-demand?plantIds=...&financialYear=...
// Returns Power Demand rows for the given plant(s) and financial year.
// Each row contains: id, cppPlantId, cppPlantName, normParameterId, utilityName,
// utilityId, utilityUOM, financialYear, type, apr..mar, remarks.
async function getPowerDemand(keycloak, plantIds, financialYear) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/power-demand?plantIds=${queryParams}&financialYear=${financialYear}`
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
    console.error('Error fetching Power Demand data:', e)
    return await Promise.reject(e)
  }
}

// GET /task/jmd/power-demand/export?plantIds=...&financialYear=...
async function exportPowerDemandExcel(
  keycloak,
  plantIds,
  financialYear,
  EXCEL_NAME,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/power-demand/export?plantIds=${queryParams}&financialYear=${financialYear}`
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
    console.error('Error exporting Power Demand Excel:', e)
    return Promise.reject(e)
  }
}

export default OutputApiService


