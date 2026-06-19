import Config from 'consts/index'
import { json } from 'services/request'

export const InputApiService = {
  // Used in: Inputs/ShutdownAndOperational/PowerGrid.js, STGGrid.js
  getOperationHoursData,
  saveOperationHours,

  // Used in: Inputs/ShutdownAndOperational/PowerGrid.js
  exportPowerResponseExcel,
  savePowerResponseExcel,

  // Used in: Inputs/ShutdownAndOperational/STGGrid.js
  exportSteamResponseExcel,
  saveSteamResponseExcel,

  // Used in: Inputs/ImportPower.js
  getImportPowerCapacity,
  saveImportPowerCapacity,
  saveImportPowerCapacityExcel,
  exportImportPowerCapacityExcel,

  // Used in: Inputs/components/AddImportPowerRowDialog.js
  getImportProcurementPlants,
  addSource,
  updateSource,
  deleteSource,

  // Used in: Inputs/AssetCapacity/PowerAssetCapacity.js & SteamAssetCapacity.js
  getAssetCapacities,
  saveAssetCapacities,
  // Power-specific export/import
  exportPowerAssetCapacityExcel,
  importPowerAssetCapacityExcel,
  // Steam-specific export/import
  exportSteamAssetCapacityExcel,
  importSteamAssetCapacityExcel,

  // Used in: Inputs/HeatRate/GTHeatRate.js
  getPlantList,
  getHeatRateData,
  saveHeatRateData,
  saveHeatRateExcel,
  exportHeatRateExcel,

  // Used in: Inputs/HeatRate/STGHeatRate.js
  getSTGHeatRateData,
  saveSTGHeatRateData,
  saveSTGHeatRateExcel,
  exportSTGHeatRateExcel,

  // Used in: Inputs/HeatRate/HRSGHeatRate.js
  getHRSGHeatRateData,
  saveHRSGHeatRateData,
  saveHRSGHeatRateExcel,
  exportHRSGHeatRateExcel,
  getHRSGHeatRateDropdown,

  // Used in: Inputs/FixedNorms.js
  getNormBasedUtilityBudget,
  saveNormsData,
  exportCPPNormsExcel,
  saveCPPNormsExcel,

  // Used in: Inputs/Fuel/FuelAvailability.js
  saveFuelAvailabilityData,
  saveFuelAvailabilityExcel,
  exportFuelAvailabilityExcel,

  // Used in: Inputs/FuelPriority/PlantFuelAvailability.js
  getFuelMaster,
  getFuelPriorityData,
  saveFuelPriorityData,
  importFuelPriorityExcel,
  exportFuelPriorityExcel,

  // Used in: Inputs/FuelPriority/AssetFuelPriority.js
  getAssetFuelPriority,
  saveAssetFuelPriority,

  // Used in: Inputs/Fuel/JCBFuel.js
  getFuelAvailabilityDataJCB,
  saveFuelAvailabilityDataJCB,
  saveFuelAvailabilityExcelJCB,
  exportFuelAvailabilityExcelJCB,
}

// ===================== ||Shutdown and Operational hrs APIs || ===================== //
async function getOperationHoursData(keycloak, plantIds, year) {
  // Support both single plantId (string/UUID) and multiple plantIds (array)
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.map((id) => id).join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/operational-hours?plantIds=${queryParams}&financialYear=${year}`
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

async function exportCPPNormsExcel(
  keycloak,
  PLANT_ID,
  financialYear,
  startDate,
  endDate,
) {
  return exportExcelData(keycloak, {
    endpoint: `cpp-norms/export`,
    queryParams: { cppPlantId: PLANT_ID, financialYear, startDate, endDate },
    fileName: `CPPNorms_${financialYear}.xlsx`,
    method: 'GET',
  })
}

async function saveCPPNormsExcel(file, keycloak, PLANT_ID, financialYear) {
  return saveExcelData(file, keycloak, 'cpp-norms/import', null, null, {
    cppPlantId: PLANT_ID,
    financialYear,
  })
}

async function saveOperationHours(keycloak, plantIds, AOP_YEAR, payload) {
  // Support both single plantId (string/UUID) and multiple plantIds (array)
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.map((id) => id).join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/operational-hours?plantIds=${queryParams}&financialYear=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// Power Response Excel Import
async function savePowerResponseExcel(file, keycloak, PLANT_ID_LIST, AOP_YEAR) {
  return saveExcelData(
    file,
    keycloak,
    'jmd/assets/power-operational-hours/import',
    PLANT_ID_LIST,
    AOP_YEAR,
  )
}

// Power Response Excel Export
async function exportPowerResponseExcel(
  keycloak,
  PLANT_ID_LIST,
  AOP_YEAR,
  EXCEL_NAME,
) {
  return exportExcelData(keycloak, {
    endpoint: `jmd/assets/power-operational-hours/export`,
    queryParams: {
      plantIds: PLANT_ID_LIST,
      financialYear: AOP_YEAR,
    },
    fileName: EXCEL_NAME,
    method: 'GET',
  })
}

// Steam Response Excel Import
async function saveSteamResponseExcel(file, keycloak, PLANT_ID_LIST, AOP_YEAR) {
  return saveExcelData(
    file,
    keycloak,
    'jmd/assets/steam-operational-hours/import',
    PLANT_ID_LIST,
    AOP_YEAR,
  )
}

// Steam Response Excel Export
async function exportSteamResponseExcel(
  keycloak,
  PLANT_ID_LIST,
  AOP_YEAR,
  EXCEL_NAME,
) {
  return exportExcelData(keycloak, {
    endpoint: `jmd/assets/steam-operational-hours/export`,
    queryParams: {
      plantIds: PLANT_ID_LIST,
      financialYear: AOP_YEAR,
    },
    fileName: EXCEL_NAME,
    method: 'GET',
  })
}

// ========================|| Import Power APIs ||=====================================//

async function getImportPowerCapacity(keycloak, plantIds, aopYear) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/imported-power-plans?plantIds=${queryParams}&aopYear=${aopYear}`
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

async function getImportProcurementPlants(keycloak, cppPlant) {
  const url = `${Config.CaseEngineUrl}/task/jmd/imported-power-plants/procurement-plants?cppPlant=${cppPlant}`
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

async function saveImportPowerCapacity(keycloak, plantIds, aopYear, payload) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/imported-power-plans?plantIds=${queryParams}&aopYear=${aopYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// POST /task/jmd/imported-power-plans/source
// Adds a new source row (without month values) for Import Power
async function addSource(keycloak, sourceData) {
  const url = `${Config.CaseEngineUrl}/task/jmd/imported-power-plans/source`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(sourceData)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error adding source:', e)
    return await Promise.reject(e)
  }
}

// PUT /task/jmd/imported-power-plans/source/{normParameterId}
// Updates an existing source row
async function updateSource(keycloak, normParameterId, updateData) {
  const url = `${Config.CaseEngineUrl}/task/jmd/imported-power-plans/source/${normParameterId}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(updateData)
  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error updating source:', e)
    return await Promise.reject(e)
  }
}

// DELETE /task/jmd/imported-power-plans/source/{normParameterId}?procurementPlant={uuid}
// Deletes a source row from Import Power
async function deleteSource(keycloak, normParameterId, procurementPlant) {
  const url = `${Config.CaseEngineUrl}/task/jmd/imported-power-plans/source/${normParameterId}?procurementPlant=${procurementPlant}`
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
    // Delete endpoints often return empty bodies, so we handle it gracefully
    const text = await resp.text()
    return text ? JSON.parse(text) : { success: true }
  } catch (e) {
    console.error('Error deleting source:', e)
    return await Promise.reject(e)
  }
}

async function saveImportPowerCapacityExcel(file, keycloak, plantIds, aopYear) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return saveExcelData(
    file,
    keycloak,
    'jmd/imported-power-plans/import',
    null,
    null,
    {
      plantIds: plantIdArray.join(','),
      aopYear,
    },
  )
}

async function exportImportPowerCapacityExcel(
  keycloak,
  plantIds,
  aopYear,
  EXCEL_NAME,
) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return exportExcelData(keycloak, {
    endpoint: 'jmd/imported-power-plans/export',
    queryParams: { plantIds: plantIdArray.join(','), aopYear },
    fileName: EXCEL_NAME,
    method: 'GET',
  })
}

// ========================|| Plant List APIs ||=====================================//
async function getPlantList(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/heat-rate/drop-down/${plantId}`
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

// ========================|| Heat Rate APIs ||=====================================//

async function getHeatRateData(keycloak, assetId, year, startDate, endDate) {
  const url = `${Config.CaseEngineUrl}/task/heat-rate/${assetId}/${year}/${startDate}/${endDate}`

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

async function saveHeatRateData(keycloak, PLANT_ID, AOP_YEAR, payload) {
  const url = `${Config.CaseEngineUrl}/task/heat-rate/${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ========================|| STG Heat Rate APIs ||=====================================//
async function getSTGHeatRateData(keycloak, financialYear, startDate, endDate) {
  let url = `${Config.CaseEngineUrl}/task/stg-heat-rate/${financialYear}`

  if (startDate && endDate) {
    url += `/${startDate}/${endDate}`
  }
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

async function saveSTGHeatRateData(keycloak, financialYear, payload) {
  const url = `${Config.CaseEngineUrl}/task/stg-heat-rate/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ========================|| HRSG Heat Rate APIs ||=====================================//
async function getHRSGHeatRateData(
  keycloak,
  assetId,
  financialYear,
  startDate,
  endDate,
) {
  let url = `${Config.CaseEngineUrl}/task/hrsg-heat-rate/${assetId}/${financialYear}`

  if (startDate && endDate) {
    url += `/${startDate}/${endDate}`
  }

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

async function saveHRSGHeatRateData(keycloak, PLANT_ID, AOP_YEAR, payload) {
  const url = `${Config.CaseEngineUrl}/task/hrsg-heat-rate/${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getHRSGHeatRateDropdown(keycloak, cppId) {
  const url = `${Config.CaseEngineUrl}/task/hrsg-heat-rate/drop-down/${cppId}`
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

//====================|| NORM BASED UTILITY BUDGET APIs ||====================//
async function getNormBasedUtilityBudget(
  keycloak,
  PLANT_ID,
  financialYear,
  startDate,
  endDate,
) {
  let url = `${Config.CaseEngineUrl}/task/cpp-norms?cppPlantId=${PLANT_ID}&financialYear=${financialYear}`
  if (startDate && endDate) {
    url += `&startDate=${startDate}&endDate=${endDate}`
  }
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

async function saveNormsData(keycloak, payload, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/cpp-norms/${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ===================== || GENERIC EXCEL IMPORT FUNCTION || ===================== //
/**
 * Generic function to upload Excel file to any CPP Input endpoint
 * @param {File} file - The Excel file to upload
 * @param {Object} keycloak - Keycloak session object
 * @param {string} endpoint - The API endpoint path (e.g., 'import-power/import')
 * @param {string|Array} plantIds - Single Plant ID or array of Plant IDs
 * @param {string} AOP_YEAR - Financial year
 * @returns {Promise} API response
 */
async function saveExcelData(
  file,
  keycloak,
  endpoint,
  plantIds,
  AOP_YEAR,
  customQueryParams = null,
) {
  let url
  if (customQueryParams) {
    // Allow callers to pass arbitrary query params (e.g. aopYear, cppPlantId)
    const qs = new URLSearchParams(customQueryParams).toString()
    url = `${Config.CaseEngineUrl}/task/${endpoint}${qs ? `?${qs}` : ''}`
  } else {
    // Support both single plantId (string/UUID) and multiple plantIds (array)
    const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
    const queryParams = plantIdArray.map((id) => id).join(',')
    url = `${Config.CaseEngineUrl}/task/${endpoint}?plantIds=${queryParams}&financialYear=${AOP_YEAR}`
  }
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

    const responseData = await json(keycloak, resp)

    // Return response data for both success and 400 (partial success with error file)
    // Components will handle error file download based on code and data
    if (resp.status === 400 || resp.status === 200) {
      return responseData
    }

    if (!resp.ok) {
      throw new Error(
        `Failed to import data: ${resp.status} ${resp.statusText}`,
      )
    }

    return responseData
  } catch (e) {
    console.error(`Error importing Excel data to ${endpoint}:`, e)
    return Promise.reject(e)
  }
}

// ===================== || GENERIC EXCEL EXPORT FUNCTION || ===================== //
/**
 * Generic function to export Excel file from backend
 * @param {Object} keycloak - Keycloak session object
 * @param {Object} params - Export parameters
 * @param {string} params.endpoint - The API endpoint path (e.g., 'export-excel')
 * @param {Object} params.queryParams - Query parameters (e.g., { year: '2024', plantId: '123', type: 'Production' })
 * @param {string|Array} params.plantIds - Single Plant ID or array of Plant IDs (optional, for JMD endpoints)
 * @param {string} params.financialYear - Financial year (optional, for JMD endpoints)
 * @param {Object|null} params.payload - Optional POST body payload
 * @param {string} params.fileName - Downloaded file name (e.g., 'plant_production_plan.xlsx')
 * @param {string} params.method - HTTP method ('GET' or 'POST'), defaults to 'GET'
 * @returns {Promise} Success/error response
 */
async function exportExcelData(keycloak, params) {
  const {
    endpoint,
    queryParams = {},
    plantIds = null,
    financialYear = null,
    payload = null,
    fileName,
    method = 'GET',
  } = params

  // Build query parameters
  let finalQueryParams = { ...queryParams }

  // If plantIds is provided (for JMD endpoints), add it to query params
  if (plantIds) {
    const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
    finalQueryParams.plantIds = plantIdArray.join(',')
  }

  // If financialYear is provided (for JMD endpoints), add it to query params
  if (financialYear) {
    finalQueryParams.financialYear = financialYear
  }

  const queryString = new URLSearchParams(finalQueryParams).toString()
  const url = `${Config.CaseEngineUrl}/task/${endpoint}${queryString ? `?${queryString}` : ''}`

  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const fetchOptions = {
      method,
      headers,
    }

    if (payload && (method === 'POST' || method === 'PUT')) {
      fetchOptions.body = JSON.stringify(payload)
    }

    const resp = await fetch(url, fetchOptions)

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
    let downloadFileName = fileName
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
    console.error(`Error exporting Excel from ${endpoint}:`, e)
    return Promise.reject(e)
  }
}

// ===================== || SPECIFIC EXCEL IMPORT FUNCTIONS || ===================== //

// ========================|| Asset Capacity Common APIs ||=====================================//

// GET /task/jmd/asset/capacities?plantIds=...&aopYear=...
// Returns { data: { PowerAssetCapacities: [...], SteamAssetCapacities: [...] } }
async function getAssetCapacities(keycloak, plantIds, aopYear) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const url = `${Config.CaseEngineUrl}/task/jmd/asset/capacities?plantIds=${plantIdArray.join(',')}&aopYear=${aopYear}`
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
    return Promise.reject(e)
  }
}

// POST /task/jmd/asset/capacities?plantIds=...&aopYear=...
// payload: { powerResponse: [...] } or { steamResponse: [...] }
async function saveAssetCapacities(keycloak, plantIds, aopYear, payload) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const url = `${Config.CaseEngineUrl}/task/jmd/asset/capacities?plantIds=${plantIdArray.join(',')}&aopYear=${aopYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, { method: 'POST', headers, body })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.log(e)
    return Promise.reject(e)
  }
}

// GET /task/jmd/asset/capacities/power/export?plantIds=...&aopYear=...
async function exportPowerAssetCapacityExcel(
  keycloak,
  plantIds,
  aopYear,
  fileName,
) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return exportExcelData(keycloak, {
    endpoint: 'jmd/asset/capacities/power/export',
    queryParams: { plantIds: plantIdArray.join(','), aopYear },
    fileName,
    method: 'GET',
  })
}

// POST /task/jmd/asset/capacities/power/import?plantIds=...&aopYear=... (multipart)
async function importPowerAssetCapacityExcel(
  file,
  keycloak,
  plantIds,
  aopYear,
) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return saveExcelData(
    file,
    keycloak,
    'jmd/asset/capacities/power/import',
    null,
    null,
    { plantIds: plantIdArray.join(','), aopYear },
  )
}

// GET /task/jmd/asset/capacities/steam/export?plantIds=...&aopYear=...
async function exportSteamAssetCapacityExcel(
  keycloak,
  plantIds,
  aopYear,
  fileName,
) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return exportExcelData(keycloak, {
    endpoint: 'jmd/asset/capacities/steam/export',
    queryParams: { plantIds: plantIdArray.join(','), aopYear },
    fileName,
    method: 'GET',
  })
}

// POST /task/jmd/asset/capacities/steam/import?plantIds=...&aopYear=... (multipart)
async function importSteamAssetCapacityExcel(
  file,
  keycloak,
  plantIds,
  aopYear,
) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return saveExcelData(
    file,
    keycloak,
    'jmd/asset/capacities/steam/import',
    null,
    null,
    { plantIds: plantIdArray.join(','), aopYear },
  )
}

// Heat Rate Excel Import (GT Heat Rate)
async function saveHeatRateExcel(file, keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/heat-rate/import`
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

    if (!resp.ok) {
      throw new Error(
        `Failed to import data: ${resp.status} ${resp.statusText}`,
      )
    }

    return { success: true }
  } catch (e) {
    console.error(`Error importing Heat Rate Excel:`, e)
    return Promise.reject(e)
  }
}

// Heat Rate Excel Export
async function exportHeatRateExcel(
  keycloak,
  assetId,
  financialYear,
  startDate = null,
  endDate = null,
) {
  // Construct endpoint with optional date range
  let endpoint = `heat-rate/export/${assetId}/${financialYear}`

  // If both dates are provided, add them as path variables
  if (startDate && endDate) {
    endpoint = `heat-rate/export/${assetId}/${financialYear}/${startDate}/${endDate}`
  }

  return exportExcelData(keycloak, {
    endpoint: endpoint,
    queryParams: {},
    fileName: `Heat_Rate_${financialYear}.xlsx`,
    method: 'GET',
  })
}

// STG Heat Rate Excel Import
async function saveSTGHeatRateExcel(file, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/stg-heat-rate/import`
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

    if (!resp.ok) {
      throw new Error(
        `Failed to import data: ${resp.status} ${resp.statusText}`,
      )
    }

    return { success: true }
  } catch (e) {
    console.error(`Error importing STG Heat Rate Excel:`, e)
    return Promise.reject(e)
  }
}

// STG Heat Rate Excel Export
async function exportSTGHeatRateExcel(
  keycloak,
  financialYear,
  startDate = null,
  endDate = null,
) {
  let endpoint = `stg-heat-rate/export/${financialYear}`

  if (startDate && endDate) {
    endpoint = `stg-heat-rate/export/${financialYear}/${startDate}/${endDate}`
  }

  return exportExcelData(keycloak, {
    endpoint,
    queryParams: {},
    fileName: `STG_Heat_Rate_${financialYear}.xlsx`,
    method: 'GET',
  })
}

// HRSG Heat Rate Excel Import
async function saveHRSGHeatRateExcel(file, keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/hrsg-heat-rate/import`
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

    if (!resp.ok) {
      throw new Error(
        `Failed to import HRSG heat rate data: ${resp.status} ${resp.statusText}`,
      )
    }

    return { success: true }
  } catch (e) {
    console.error(`Error importing HRSG Heat Rate Excel:`, e)
    return Promise.reject(e)
  }
}

// HRSG Heat Rate Excel Export
async function exportHRSGHeatRateExcel(
  keycloak,
  assetId,
  financialYear,
  startDate = null,
  endDate = null,
) {
  // Construct endpoint with optional date range
  let endpoint = `hrsg-heat-rate/export/${assetId}/${financialYear}`

  // If both dates are provided, add them as path variables
  if (startDate && endDate) {
    endpoint = `hrsg-heat-rate/export/${assetId}/${financialYear}/${startDate}/${endDate}`
  }

  return exportExcelData(keycloak, {
    endpoint: endpoint,
    queryParams: {},
    fileName: `HRSG_Heat_Rate_${financialYear}.xlsx`,
    method: 'GET',
  })
}

// ========================|| Fuel Availability APIs ||=====================================//

async function saveFuelAvailabilityData(keycloak, AOP_YEAR, payload) {
  const url = `${Config.CaseEngineUrl}/task/fuel-availability/${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function saveFuelAvailabilityExcel(file, keycloak, PLANT_ID, AOP_YEAR) {
  return saveExcelData(
    file,
    keycloak,
    'fuel-availability/import',
    PLANT_ID,
    AOP_YEAR,
  )
}

async function exportFuelAvailabilityExcel(keycloak, PLANT_ID, AOP_YEAR) {
  return exportExcelData(keycloak, {
    endpoint: `fuel-availability/export/${PLANT_ID}/${AOP_YEAR}`,
    queryParams: {},
    fileName: `Fuel_Availability_${AOP_YEAR}.xlsx`,
    method: 'GET',
  })
}

// ========================|| JCB Fuel Availability APIs ||=====================================//
async function getFuelAvailabilityDataJCB(
  keycloak,
  cppId,
  financialYear,
  fuelType = null,
) {
  let url = `${Config.CaseEngineUrl}/task/fuel-availability/${cppId}/${financialYear}`

  if (fuelType) {
    url += `?fuelType=${encodeURIComponent(fuelType)}`
  }

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

async function saveFuelAvailabilityDataJCB(
  keycloak,
  cppId,
  financialYear,
  payload,
) {
  const url = `${Config.CaseEngineUrl}/task/fuel-availability/${cppId}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function saveFuelAvailabilityExcelJCB(file, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/fuel-availability/import`
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

    const responseData = await json(keycloak, resp)

    if (resp.status === 400 || resp.status === 200) {
      return responseData
    }

    if (!resp.ok) {
      throw new Error(
        `Failed to import JCB fuel availability data: ${resp.status} ${resp.statusText}`,
      )
    }

    return responseData
  } catch (e) {
    console.error(`Error importing JCB Fuel Availability Excel:`, e)
    return Promise.reject(e)
  }
}

async function exportFuelAvailabilityExcelJCB(
  keycloak,
  cppId,
  financialYear,
  fuelType = null,
) {
  const queryParams = {}
  if (fuelType) {
    queryParams.fuelType = fuelType
  }

  return exportExcelData(keycloak, {
    endpoint: `fuel-availability/export/${cppId}/${financialYear}`,
    queryParams: queryParams,
    fileName: `JCB_Fuel_Availability_${financialYear}.xlsx`,
    method: 'GET',
  })
}

// ========================|| Fuel Priority APIs ||=====================================//

async function getFuelMaster(keycloak) {
  const url = `${Config.CaseEngineUrl}/task/fuel-master`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result
  } catch (e) {
    console.error('Error fetching fuel master:', e)
    return await Promise.reject(e)
  }
}

async function getFuelPriorityData(keycloak, plantIds, financialYear) {
  const queryParams = Array.isArray(plantIds) ? plantIds.join(',') : plantIds
  const url = `${Config.CaseEngineUrl}/task/plant-wise-fuel-priority/${queryParams}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result
  } catch (e) {
    console.error('Error fetching fuel priority data:', e)
    return await Promise.reject(e)
  }
}

async function saveFuelPriorityData(keycloak, plantIds, financialYear, payload) {
  const url = `${Config.CaseEngineUrl}/task/plant-fuel-availability`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error saving fuel priority data:', e)
    return await Promise.reject(e)
  }
}

async function importFuelPriorityExcel(file, keycloak, plantIds, financialYear) {
  return saveExcelData(
    file,
    keycloak,
    'plant-fuel-availability/import',
    plantIds,
    financialYear
  )
}

async function exportFuelPriorityExcel(keycloak, plantIds, financialYear, EXCEL_NAME) {
  return exportExcelData(keycloak, {
    endpoint: `plant-fuel-availability/export`,
    plantIds,
    financialYear,
    fileName: EXCEL_NAME,
    method: 'GET',
  })
}

// GET /task/asset-fuel-priority/{plantIds}/{financialYear}
async function getAssetFuelPriority(keycloak, plantIds, financialYear) {
  const queryParams = Array.isArray(plantIds) ? plantIds.join(',') : plantIds
  const url = `${Config.CaseEngineUrl}/task/asset-fuel-priority/${queryParams}/${financialYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result
  } catch (e) {
    console.error('Error fetching asset fuel priority data:', e)
    return await Promise.reject(e)
  }
}

// PUT /task/asset-fuel-priority
async function saveAssetFuelPriority(keycloak, payload) {
  const url = `${Config.CaseEngineUrl}/task/asset-fuel-priority`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, { method: 'PUT', headers, body })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error saving asset fuel priority data:', e)
    return await Promise.reject(e)
  }
}
