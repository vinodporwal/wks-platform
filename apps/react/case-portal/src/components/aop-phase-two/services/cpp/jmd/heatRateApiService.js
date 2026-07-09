import Config from 'consts/index'
import { json } from 'services/request'

export const HeatRateApiService = {
  // GT Heat Rate
  getGTAssetDropdown,
  getGTHeatRateData,
  saveGTHeatRateData,
  saveGTHeatRateExcel,
  exportGTHeatRateExcel,

  // HRSG Heat Rate
  getHRSGAssetDropdown,
  getHRSGHeatRateData,
  saveHRSGHeatRateData,
  saveHRSGHeatRateExcel,
  exportHRSGHeatRateExcel,

  // STG Heat Rate
  getSTGHeatRateData,
  saveSTGHeatRateData,
  saveSTGHeatRateExcel,
  exportSTGHeatRateExcel,
}

// ===================== || GENERIC HELPERS || ===================== //

function buildHeaders(keycloak) {
  return {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
}

function buildPlantIdsParam(plantIds) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return plantIdArray.join(',')
}

// ===================== || GT HEAT RATE APIs || ===================== //
// Used by: GTHeatRate.js
// Java Controller: JMDHeatRateController.java

// -----------------------------------------------------------------------
// [GT] DROPDOWN
// Used by   : GTHeatRate.js → getPlantList()
//             STGHeatRate.js → getPlantList()  ← NOTE: STG also reuses this GT dropdown
// Endpoint  : GET /task/jmd/heat-rate/drop-down?plantIds=...&assetType=...
// Controller: getGTAssetDropdown() — @GetMapping("/jmd/heat-rate/drop-down")
// -----------------------------------------------------------------------
// GET /task/jmd/heat-rate/drop-down?plantIds=...
async function getGTAssetDropdown(keycloak, plantIds, assetType) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/heat-rate/drop-down?plantIds=${queryParams}&assetType=${assetType}`
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: buildHeaders(keycloak),
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result?.data ?? result
  } catch (e) {
    console.error('Error fetching GT asset dropdown:', e)
    return await Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [GT] FETCH DATA
// Used by   : GTHeatRate.js → fetchHeatRateData()
// Endpoint  : GET /task/jmd/gt-heat-rate?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
// Controller: getGTHeatRateData() — @GetMapping("/jmd/gt-heat-rate")
// -----------------------------------------------------------------------
// GET /task/jmd/gt-heat-rate?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
async function getGTHeatRateData(
  keycloak,
  assetId,
  aopYear,
  startDate,
  endDate,
  plantIds,
) {
  const queryParams = new URLSearchParams()
  queryParams.append('assetId', assetId)
  queryParams.append('year', aopYear)
  if (startDate && endDate) {
    queryParams.append('startDate', startDate)
    queryParams.append('endDate', endDate)
  }
  if (plantIds) {
    const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
    queryParams.append('plantIds', plantIdArray.join(','))
  }
  const url = `${Config.CaseEngineUrl}/task/jmd/gt-heat-rate?${queryParams.toString()}`
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: buildHeaders(keycloak),
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result?.data ?? result
  } catch (e) {
    console.error('Error fetching GT heat rate data:', e)
    return await Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [GT] SAVE DATA
// Used by   : GTHeatRate.js → saveChanges()
// Endpoint  : POST /task/jmd/gt-heat-rate?year=...
// Controller: saveGTHeatRateData() — @PostMapping("/jmd/gt-heat-rate")
// -----------------------------------------------------------------------
// POST /task/jmd/gt-heat-rate?year=...
async function saveGTHeatRateData(keycloak, aopYear, payload) {
  const queryParams = new URLSearchParams()
  queryParams.append('year', aopYear)
  const url = `${Config.CaseEngineUrl}/task/jmd/gt-heat-rate?${queryParams.toString()}`
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: buildHeaders(keycloak),
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error saving GT heat rate data:', e)
    return await Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [GT] IMPORT EXCEL
// Used by   : GTHeatRate.js → handleExcelUpload()
// Endpoint  : POST /task/jmd/gt-heat-rate/import?year=...&assetId=...&startDate=...&endDate=...&plantIds=...
// Controller: importGTHeatRateData() — @PostMapping("/jmd/gt-heat-rate/import", consumes = multipart/form-data)
// -----------------------------------------------------------------------
// POST /task/jmd/gt-heat-rate/import?year=...&assetId=...&startDate=...&endDate=...&plantIds=...
async function saveGTHeatRateExcel(
  file,
  keycloak,
  aopYear,
  assetId,
  startDate,
  endDate,
  plantIds,
) {
  const queryParams = new URLSearchParams()
  queryParams.append('year', aopYear)
  if (assetId) queryParams.append('assetId', assetId)
  if (startDate) queryParams.append('startDate', startDate)
  if (endDate) queryParams.append('endDate', endDate)
  if (plantIds && plantIds.length > 0) {
    const plantIdsStr = Array.isArray(plantIds) ? plantIds.join(',') : plantIds
    queryParams.append('plantIds', plantIdsStr)
  }
  const url = `${Config.CaseEngineUrl}/task/jmd/gt-heat-rate/import?${queryParams.toString()}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: formData })
    const responseData = await json(keycloak, resp)
    if (resp.status === 400 || resp.status === 200) {
      return responseData
    }
    if (!resp.ok) {
      throw new Error(
        `Failed to import GT heat rate data: ${resp.status} ${resp.statusText}`,
      )
    }
    return responseData
  } catch (e) {
    console.error('Error importing GT Heat Rate Excel:', e)
    return Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [GT] EXPORT EXCEL
// Used by   : GTHeatRate.js → handleExport()
// Endpoint  : GET  /task/jmd/gt-heat-rate/export?assetId=...&year=...&startDate=...&endDate=...&plantIds=...&isAfterSave=false
//             POST /task/jmd/gt-heat-rate/export?...&isAfterSave=true  (sends dtoList in body after save)
// Controller: exportGTHeatRate() — @GetMapping("/jmd/gt-heat-rate/export")
// -----------------------------------------------------------------------
// GET /task/jmd/gt-heat-rate/export?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
async function exportGTHeatRateExcel(
  keycloak,
  assetId,
  aopYear,
  startDate,
  endDate,
  plantIds,
  assetDisplayName,
  isAfterSave = false,
  dtoList = null,
) {
  const queryParams = new URLSearchParams()
  queryParams.append('assetId', assetId)
  queryParams.append('year', aopYear)
  if (startDate) queryParams.append('startDate', startDate)
  if (endDate) queryParams.append('endDate', endDate)
  if (plantIds && plantIds.length > 0) {
    const plantIdsStr = Array.isArray(plantIds) ? plantIds.join(',') : plantIds
    queryParams.append('plantIds', plantIdsStr)
  }
  queryParams.append('isAfterSave', isAfterSave)

  const endpoint = `jmd/gt-heat-rate/export?${queryParams.toString()}`
  const fileName = assetDisplayName
    ? `${assetDisplayName}_${aopYear}.xlsx`
    : `GT_Heat_Rate_${aopYear}.xlsx`

  const url = `${Config.CaseEngineUrl}/task/${endpoint}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }

  const fetchOptions = { method: 'GET', headers }
  if (isAfterSave && dtoList) {
    fetchOptions.method = 'POST'
    fetchOptions.body = JSON.stringify(dtoList)
  }

  try {
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

    const contentDisposition = resp.headers.get('content-disposition')
    let downloadFileName = fileName
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename=?"([^";\n]+)"?/i)
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

// ===================== || HRSG HEAT RATE APIs || ===================== //
// Used by: HRSGHeatRate.js
// Java Controller: JMDHeatRateController.java

// -----------------------------------------------------------------------
// [HRSG] DROPDOWN
// Used by   : HRSGHeatRate.js → getPlantList()
// Endpoint  : GET /task/jmd/hrsg/drop-down?plantIds=...&assetType=HRSG
// Controller: getHRSGAssetDropdown(List<UUID>, String) — @GetMapping("/jmd/hrsg/drop-down")
// NOTE: There is a second (older) overload @GetMapping("/jmd/hrsg-heat-rate/drop-down")
//       that takes only plantIds — that one is NOT used by the UI.
// -----------------------------------------------------------------------
// GET /task/jmd/hrsg/drop-down?plantIds=...&assetType=HRSG
async function getHRSGAssetDropdown(keycloak, plantIds, assetType) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/hrsg/drop-down?plantIds=${queryParams}&assetType=${assetType}`
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: buildHeaders(keycloak),
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result?.data ?? result
  } catch (e) {
    console.error('Error fetching HRSG asset dropdown:', e)
    return await Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [HRSG] FETCH DATA
// Used by   : HRSGHeatRate.js → fetchHeatRateData()
// Endpoint  : GET /task/jmd/hrsg-heat-rate?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
// Controller: getHRSGHeatRateData() — @GetMapping("/jmd/hrsg-heat-rate")
// -----------------------------------------------------------------------
// GET /task/jmd/hrsg-heat-rate?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
async function getHRSGHeatRateData(
  keycloak,
  assetId,
  aopYear,
  startDate,
  endDate,
  plantIds,
) {
  const queryParams = new URLSearchParams()
  queryParams.append('assetId', assetId)
  queryParams.append('year', aopYear)
  if (startDate && endDate) {
    queryParams.append('startDate', startDate)
    queryParams.append('endDate', endDate)
  }
  if (plantIds) {
    const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
    queryParams.append('plantIds', plantIdArray.join(','))
  }
  const url = `${Config.CaseEngineUrl}/task/jmd/hrsg-heat-rate?${queryParams.toString()}`
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: buildHeaders(keycloak),
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result
  } catch (e) {
    console.error('Error fetching HRSG heat rate data:', e)
    return await Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [HRSG] SAVE DATA
// Used by   : HRSGHeatRate.js → saveChanges()
// Endpoint  : POST /task/jmd/hrsg-heat-rate/{aopYear}
// Controller: updateHRSGHeatRate() — @PostMapping("/jmd/hrsg-heat-rate/{aopYear}")
// -----------------------------------------------------------------------
// POST /task/jmd/hrsg-heat-rate/{aopYear}
async function saveHRSGHeatRateData(keycloak, aopYear, payload) {
  const url = `${Config.CaseEngineUrl}/task/jmd/hrsg-heat-rate/${aopYear}`
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: buildHeaders(keycloak),
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error saving HRSG heat rate data:', e)
    return await Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [HRSG] IMPORT EXCEL
// Used by   : HRSGHeatRate.js → handleExcelUpload()
// Endpoint  : POST /task/jmd/hrsg-heat-rate/import?year=...&assetId=...&startDate=...&endDate=...&plantIds=...
// Controller: importHRSGHeatRate() — @PostMapping("/jmd/hrsg-heat-rate/import")
// -----------------------------------------------------------------------
// POST /task/jmd/hrsg-heat-rate/import?year=...&assetId=...&startDate=...&endDate=...&plantIds=...
async function saveHRSGHeatRateExcel(
  file,
  keycloak,
  aopYear,
  assetId,
  startDate,
  endDate,
  plantIds,
) {
  const queryParams = new URLSearchParams()
  queryParams.append('year', aopYear)
  if (assetId) queryParams.append('assetId', assetId)
  if (startDate) queryParams.append('startDate', startDate)
  if (endDate) queryParams.append('endDate', endDate)
  if (plantIds && plantIds.length > 0) {
    const plantIdsStr = Array.isArray(plantIds) ? plantIds.join(',') : plantIds
    queryParams.append('plantIds', plantIdsStr)
  }
  const url = `${Config.CaseEngineUrl}/task/jmd/hrsg-heat-rate/import?${queryParams.toString()}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: formData })
    const responseData = await json(keycloak, resp)
    if (resp.status === 400 || resp.status === 200) {
      return responseData
    }
    if (!resp.ok) {
      throw new Error(
        `Failed to import HRSG heat rate data: ${resp.status} ${resp.statusText}`,
      )
    }
    return responseData
  } catch (e) {
    console.error('Error importing HRSG Heat Rate Excel:', e)
    return Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [HRSG] EXPORT EXCEL
// Used by   : HRSGHeatRate.js → handleExport()
// Endpoint  : GET /task/jmd/hrsg-heat-rate/export?assetId=...&year=...&startDate=...&endDate=...&plantIds=...&isAfterSave=false
// Controller: exportHRSGHeatRate() — @GetMapping("/jmd/hrsg-heat-rate-export")
// -----------------------------------------------------------------------
// GET /task/jmd/hrsg-heat-rate/export?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
async function exportHRSGHeatRateExcel(
  keycloak,
  assetId,
  aopYear,
  startDate,
  endDate,
  plantIds,
  assetDisplayName,
) {
  const queryParams = new URLSearchParams()
  queryParams.append('assetId', assetId)
  queryParams.append('year', aopYear)
  if (startDate) queryParams.append('startDate', startDate)
  if (endDate) queryParams.append('endDate', endDate)
  if (plantIds && plantIds.length > 0) {
    const plantIdsStr = Array.isArray(plantIds) ? plantIds.join(',') : plantIds
    queryParams.append('plantIds', plantIdsStr)
  }

  const endpoint = `jmd/hrsg-heat-rate-export?${queryParams.toString()}`
  const fileName = assetDisplayName
    ? `${assetDisplayName}_${aopYear}.xlsx`
    : `HRSG_Heat_Rate_${aopYear}.xlsx`

  const url = `${Config.CaseEngineUrl}/task/${endpoint}`
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

    const contentDisposition = resp.headers.get('content-disposition')
    let downloadFileName = fileName
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename=?"([^";\n]+)"?/i)
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

// ===================== || STG HEAT RATE APIs || ===================== //
// Used by: STGHeatRate.js
// Java Controller: JMDHeatRateController.java
// NOTE: STG Dropdown reuses getGTAssetDropdown() → GET /task/jmd/heat-rate/drop-down

// -----------------------------------------------------------------------
// [STG] FETCH DATA
// Used by   : STGHeatRate.js → fetchHeatRateData()
// Endpoint  : GET /task/jmd/stg-heat-rate?assetId=...&aopYear=...&startDate=...&endDate=...&plantIds=id1,id2
// Controller: getSTGHeatRate() — @GetMapping("/jmd/stg-heat-rate")
// NOTE: STG uses 'aopYear' param (not 'year' like GT/HRSG) — keep consistent with controller.
// -----------------------------------------------------------------------
// GET /task/jmd/stg-heat-rate?assetId=...&aopYear=...&startDate=...&endDate=...&plantIds=id1,id2
async function getSTGHeatRateData(
  keycloak,
  assetId,
  aopYear,
  startDate,
  endDate,
  plantIds,
) {
  const queryParams = new URLSearchParams()
  queryParams.append('assetId', assetId)
  queryParams.append('aopYear', aopYear)
  if (startDate && endDate) {
    queryParams.append('startDate', startDate)
    queryParams.append('endDate', endDate)
  }
  if (plantIds && plantIds.length > 0) {
    const plantIdsStr = Array.isArray(plantIds) ? plantIds.join(',') : plantIds
    queryParams.append('plantIds', plantIdsStr)
  }
  const url = `${Config.CaseEngineUrl}/task/jmd/stg-heat-rate?${queryParams.toString()}`
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: buildHeaders(keycloak),
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result
  } catch (e) {
    console.error('Error fetching STG heat rate data:', e)
    return await Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [STG] SAVE DATA
// Used by   : STGHeatRate.js → saveChanges()
// Endpoint  : POST /task/jmd/stg-heat-rate/{aopYear}
// Controller: updateSTGHeatRate() — @PostMapping("/jmd/stg-heat-rate/{aopYear}")
// -----------------------------------------------------------------------
// POST /task/jmd/stg-heat-rate/{aopYear}
async function saveSTGHeatRateData(keycloak, aopYear, payload) {
  const url = `${Config.CaseEngineUrl}/task/jmd/stg-heat-rate/${aopYear}`
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: buildHeaders(keycloak),
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error saving STG heat rate data:', e)
    return await Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [STG] IMPORT EXCEL
// Used by   : STGHeatRate.js → handleExcelUpload()
// Endpoint  : POST /task/jmd/stg-heat-rate/import?year=...&assetId=...&startDate=...&endDate=...&plantIds=...
// Controller: importSTGHeatRateData() — @PostMapping("/jmd/stg-heat-rate/import")
// -----------------------------------------------------------------------
async function saveSTGHeatRateExcel(
  file,
  keycloak,
  aopYear,
  assetId,
  startDate,
  endDate,
  plantIds,
) {
  const queryParams = new URLSearchParams()
  queryParams.append('year', aopYear)
  if (assetId) queryParams.append('assetId', assetId)
  if (startDate) queryParams.append('startDate', startDate)
  if (endDate) queryParams.append('endDate', endDate)
  if (plantIds && plantIds.length > 0) {
    const plantIdsStr = Array.isArray(plantIds) ? plantIds.join(',') : plantIds
    queryParams.append('plantIds', plantIdsStr)
  }
  const url = `${Config.CaseEngineUrl}/task/jmd/stg-heat-rate/import?${queryParams.toString()}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: formData })
    const responseData = await json(keycloak, resp)
    if (resp.status === 400 || resp.status === 200) {
      return responseData
    }
    if (!resp.ok) {
      throw new Error(
        `Failed to import STG heat rate data: ${resp.status} ${resp.statusText}`,
      )
    }
    return responseData
  } catch (e) {
    console.error('Error importing STG Heat Rate Excel:', e)
    return Promise.reject(e)
  }
}

// -----------------------------------------------------------------------
// [STG] EXPORT EXCEL
// Used by   : STGHeatRate.js → handleExport()
// Endpoint  : GET /task/jmd/stg-heat-rate/export?assetId=...&year=...&startDate=...&endDate=...&plantIds=id1&plantIds=id2
// Controller: exportSTGHeatRate() — @GetMapping("/jmd/stg-heat-rate/export")
// -----------------------------------------------------------------------
async function exportSTGHeatRateExcel(
  keycloak,
  assetId,
  aopYear,
  startDate,
  endDate,
  plantIds,
  assetDisplayName,
) {
  const queryParams = new URLSearchParams()
  queryParams.append('assetId', assetId)
  queryParams.append('year', aopYear)
  if (startDate) queryParams.append('startDate', startDate)
  if (endDate) queryParams.append('endDate', endDate)
  if (plantIds && plantIds.length > 0) {
    const plantIdsStr = Array.isArray(plantIds) ? plantIds.join(',') : plantIds
    queryParams.append('plantIds', plantIdsStr)
  }

  const endpoint = `jmd/stg-heat-rate/export?${queryParams.toString()}`
  const fileName = assetDisplayName
    ? `${assetDisplayName}_${aopYear}.xlsx`
    : `STG_Heat_Rate_${aopYear}.xlsx`

  const url = `${Config.CaseEngineUrl}/task/${endpoint}`
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

    const contentDisposition = resp.headers.get('content-disposition')
    let downloadFileName = fileName
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename=?"([^";\n]+)"?/i)
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

// ===================== || GENERIC EXCEL EXPORT FUNCTION || ===================== //

async function exportExcelData(keycloak, params) {
  const { endpoint, queryParams = {}, fileName, method = 'GET' } = params

  const queryString = new URLSearchParams(queryParams).toString()
  const url = `${Config.CaseEngineUrl}/task/${endpoint}${queryString ? `?${queryString}` : ''}`

  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, { method, headers })

    if (!resp.ok) {
      throw new Error(
        `Failed to export Excel: ${resp.status} ${resp.statusText}`,
      )
    }

    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob

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
