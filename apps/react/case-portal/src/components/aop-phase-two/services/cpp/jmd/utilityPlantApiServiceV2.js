import Config from 'consts/index'
import { json } from 'services/request'
export const UtilityPlantApiServiceV2 = {
  //  Fixed Consumption APIs
  getFixedConsumptionData,
  saveFixedConsumptionData,
  saveFixedConsumptionExcel,
  exportFixedConsumptionExcel,
  addFixedConsumptionRow,
  updateFixedConsumptionRow,
  deleteFixedConsumptionRow,

  //   Plant requirement APIs
  getPlantRequirementData,
  savePlantRequirementData,
  savePlantRequirementExcel,
  exportPlantRequirementExcel,

  // Import Consumption APIs
  getImportConsumptionData,
  saveImportConsumptionData,

  //Norm Based Utility Budget APIs
  getNormBasedUtilityBudget,
  saveNormsData,
  saveNormsExcel,
  exportNormsExcel,
  calculateNormsData,

  // SR Mapping APIs
  getSRMapping,
  saveSRMapping,
  importSRMappingExcel,
  exportSRMappingExcel,
  deleteSRMapping,

  updateSRMappingsByPlant,
  getSRMappingByPlant,
  getSRMappingPlants,
  getSRMappingCostCenters,
  getNormParameters,

  // Generic Excel Import/Export
  saveExcelData,
  exportExcelData,
}

// ===================== || Fixed Consumption APIs || ===================== //
async function getFixedConsumptionData(keycloak, plantIds, financialYear) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/fixed-consumption?plantIds=${queryParams}&financialYear=${financialYear}`
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
async function saveFixedConsumptionData(
  keycloak,
  plantIds,
  payload,
  financialYear,
) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/fixed-consumption?plantIds=${queryParams}&financialYear=${financialYear}`
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

// ===================== || Fixed Consumption Row CRUD APIs || ===================== //

// Create a new fixed consumption row
async function addFixedConsumptionRow(keycloak, rowData, financialYear) {
  const url = `${Config.CaseEngineUrl}/task/jmd/fixed-consumption/create`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify({
    ...rowData,
    aopYear: financialYear,
  })
  try {
    const resp = await fetch(url, { method: 'POST', headers, body })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error creating fixed consumption row:', e)
    return await Promise.reject(e)
  }
}

// Update an existing fixed consumption row
async function updateFixedConsumptionRow(keycloak, rowData, financialYear) {
  const url = `${Config.CaseEngineUrl}/task/jmd/fixed-consumption/update`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify({
    ...rowData,
    aopYear: financialYear,
  })
  try {
    const resp = await fetch(url, { method: 'PUT', headers, body })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error updating fixed consumption row:', e)
    return await Promise.reject(e)
  }
}

// Delete a fixed consumption row by id
async function deleteFixedConsumptionRow(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/jmd/fixed-consumption/delete/${id}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'DELETE', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const text = await resp.text()
    return text ? JSON.parse(text) : { success: true }
  } catch (e) {
    console.error('Error deleting fixed consumption row:', e)
    return await Promise.reject(e)
  }
}

// ===================== || Plant Requirement APIs || ===================== //
async function getPlantRequirementData(keycloak, plantIds, financialYear) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd-process-demand?plantIds=${queryParams}&financialYear=${financialYear}`
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

async function savePlantRequirementData(keycloak, financialYear, payload) {
  const url = `${Config.CaseEngineUrl}/task/jmd-process-demand/${financialYear}`
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

// ===================== || Import  Consumption APIs || ===================== //
async function getImportConsumptionData(keycloak, YEAR) {
  const url = `${Config.CaseEngineUrl}/task/asset-import-mapping?financialYear=${YEAR}`
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
async function saveImportConsumptionData(keycloak, PLANT_ID, payload) {
  const url = `${Config.CaseEngineUrl}/task/consumption/update-import-power`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = payload
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

//====================|| NORM BASED UTILITY BUDGET APIs ||====================//
async function getNormBasedUtilityBudget(keycloak, plantIds, financialYear) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/norm-based-utility-budget?cppPlantIds=${queryParams}&financialYear=${financialYear}`
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

async function calculateNormsData(keycloak, plantIds, financialYear) {
  const url = `${Config.CaseEngineUrl}/task/jmd-run-full-year`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  let financial_year = financialYear.split('-')[0]
  const body = JSON.stringify({
    financial_year: financial_year,
    save_to_db: true,
    cpp_ids: plantIdArray,
  })
  try {
    const resp = await fetch(url, { method: 'POST', headers, body })
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
  const url = `${Config.CaseEngineUrl}/task/saveOrUpdateNormsMonths/${AOP_YEAR}`
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
 * Generic function to upload Excel file to any CPP endpoint
 * @param {File} file - The Excel file to upload
 * @param {Object} keycloak - Keycloak session object
 * @param {string} endpoint - The API endpoint path (e.g., 'jmd-consumption/import')
 * @param {string|null} PLANT_ID - Plant ID (optional, for backward compatibility)
 * @param {string|null} AOP_YEAR - Financial year (optional, for backward compatibility)
 * @param {Object|null} customQueryParams - Custom query parameters (e.g., { plantIds: '...', financialYear: '...' })
 * @returns {Promise} API response
 */
async function saveExcelData(
  file,
  keycloak,
  endpoint,
  PLANT_ID,
  AOP_YEAR,
  customQueryParams = null,
) {
  let url
  if (customQueryParams) {
    // Use custom query params (e.g., { plantIds: '...', financialYear: '...' })
    const qs = new URLSearchParams(customQueryParams).toString()
    url = `${Config.CaseEngineUrl}/task/${endpoint}${qs ? `?${qs}` : ''}`
  } else {
    // Legacy: use PLANT_ID and AOP_YEAR
    url = `${Config.CaseEngineUrl}/task/${endpoint}`
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
    if (!resp.ok) {
      throw new Error(
        `Failed to import data: ${resp.status} ${resp.statusText}`,
      )
    }
    return json(keycloak, resp)
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
 * @param {string} params.endpoint - The API endpoint path
 * @param {Object} params.queryParams - Query parameters
 * @param {Object|null} params.payload - Optional POST body payload
 * @param {string} params.fileName - Downloaded file name
 * @param {string} params.method - HTTP method ('GET' or 'POST'), defaults to 'GET'
 * @returns {Promise} Success/error response
 */
async function exportExcelData(keycloak, params) {
  const {
    endpoint,
    queryParams = {},
    payload = null,
    fileName,
    method = 'GET',
  } = params

  const queryString = new URLSearchParams(queryParams).toString()
  const url = `${Config.CaseEngineUrl}/task/${endpoint}?${queryString}`

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

// Fixed Consumption Excel Import
async function saveFixedConsumptionExcel(
  file,
  keycloak,
  plantIds,
  financialYear,
) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.join(',')
  return saveExcelData(
    file,
    keycloak,
    `jmd/fixed-consumption/import`,
    null,
    null,
    { plantIds: queryParams, financialYear },
  )
}

// Fixed Consumption Excel Export
async function exportFixedConsumptionExcel(
  keycloak,
  plantIds,
  financialYear,
  EXCEL_NAME,
) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.join(',')
  return exportExcelData(keycloak, {
    endpoint: `jmd/fixed-consumption/export`,
    queryParams: { plantIds: queryParams, financialYear },
    fileName: EXCEL_NAME || `FixedConsumption_${financialYear}.xlsx`,
    method: 'GET',
  })
}

// Plant Requirement Excel Import
async function savePlantRequirementExcel(
  file,
  keycloak,
  plantIds,
  financialYear,
) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return saveExcelData(file, keycloak, 'jmd-consumption/import', null, null, {
    plantIds: plantIdArray.join(','),
    financialYear,
  })
}

// Plant Requirement Excel Export
async function exportPlantRequirementExcel(
  keycloak,
  plantIds,
  financialYear,
  EXCEL_NAME,
) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return exportExcelData(keycloak, {
    endpoint: 'jmd-consumption/export',
    queryParams: { plantIds: plantIdArray.join(','), financialYear },
    fileName: EXCEL_NAME,
    method: 'GET',
  })
}

// Norms Excel Import
async function saveNormsExcel(file, keycloak, plantIds, AOP_YEAR) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.join(',')
  return saveExcelData(
    file,
    keycloak,
    `jmd/norm-based-utility-budget/import?cppPlantIds=${queryParams}&financialYear=${AOP_YEAR}`,
    null,
    null,
  )
}

// Norms Excel Export
async function exportNormsExcel(keycloak, plantIds, AOP_YEAR, fileName) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.join(',')
  return exportExcelData(keycloak, {
    endpoint: `jmd/norm-based-utility-budget/export`,
    queryParams: { cppPlantIds: queryParams, financialYear: AOP_YEAR },
    fileName: fileName || `Norms_${AOP_YEAR}.xlsx`,
    method: 'GET',
  })
}

//====================|| SR Mapping APIs ||====================//
async function getSRMapping(keycloak, plantFkIds, aopYear) {
  // Convert to array if single value passed
  const plantIdArray = Array.isArray(plantFkIds) ? plantFkIds : [plantFkIds]
  // Join array elements with comma
  const queryParams = plantIdArray.map((id) => id).join(',')

  const url = `${Config.CaseEngineUrl}/task/sr-mapping?aopYear=${aopYear}&plantFkId=${queryParams}`
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

async function saveSRMapping(keycloak, payload) {
  const url = `${Config.CaseEngineUrl}/task/sr-mapping`
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

async function exportSRMappingExcel(keycloak, plantFkIds, aopYear) {
  // Convert to array if single value passed
  const plantIdArray = Array.isArray(plantFkIds) ? plantFkIds : [plantFkIds]
  // Join array elements with comma
  const queryParams = plantIdArray.map((id) => id).join(',')

  return exportExcelData(keycloak, {
    endpoint: `sr-mapping/export?aopYear=${aopYear}&plantFkId=${queryParams}`,
    queryParams: {},
    fileName: `CPP_SRMapping_${aopYear}.xlsx`,
    method: 'GET',
  })
}

async function importSRMappingExcel(file, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/sr-mapping/import`
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
        `Failed to import data: ${resp.status} ${resp.statusText}`,
      )
    }

    return responseData
  } catch (e) {
    console.error(`Error importing SR Mapping Excel:`, e)
    return Promise.reject(e)
  }
}

// Update (create/update) SR Mappings By Plant
// Calls PUT /task/sr-mapping/by-plant
async function updateSRMappingsByPlant(keycloak, payload, financialYear) {
  const url = `${Config.CaseEngineUrl}/task/sr-mapping/by-plant?financialYear=${financialYear}`
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
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// Get SR Mapping By Plant (New endpoint)
async function getSRMappingByPlant(keycloak, plantIds, financialYear) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  const queryParams = plantIdArray.map((id) => id).join(',')

  const url = `${Config.CaseEngineUrl}/task/sr-mapping/by-plant?plantIds=${queryParams}${financialYear ? `&financialYear=${financialYear}` : ''}`
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

// Get Plants for SR Mapping Dropdown
async function getSRMappingPlants(keycloak, sourceNames = null) {
  const plantIdArray = Array.isArray(sourceNames) ? sourceNames : [sourceNames]
  const queryParams = plantIdArray.map((id) => id).join(',')
  let url = `${Config.CaseEngineUrl}/task/sr-mapping/plants`
  if (sourceNames) {
    url += `?sourceNames=${queryParams}`
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

// Get Cost Centers for SR Mapping Dropdown
async function getSRMappingCostCenters(keycloak, plantIds = null) {
  let url = `${Config.CaseEngineUrl}/task/sr-mapping/cost-centers`
  if (plantIds) {
    const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
    url += `?plantIds=${plantIdArray.map((id) => id).join(',')}`
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
// Delete a single SR Mapping record by id
async function deleteSRMapping(keycloak, id, year) {
  const url = `${Config.CaseEngineUrl}/task/sr-mapping/${id}?financialYear=${year}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'DELETE', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const text = await resp.text()
    return text ? JSON.parse(text) : { success: true }
  } catch (e) {
    console.error('Error deleting SR mapping:', e)
    return await Promise.reject(e)
  }
}

// Get Norm Parameters by Source Plant
async function getNormParameters(keycloak, plantId, type = null) {
  let url = `${Config.CaseEngineUrl}/task/cpp-norm-parameters?plantId=${plantId}`
  if (type != null) {
    url += `&type=${type}`
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
    console.error('Error fetching norm parameters:', e)
    return await Promise.reject(e)
  }
}
