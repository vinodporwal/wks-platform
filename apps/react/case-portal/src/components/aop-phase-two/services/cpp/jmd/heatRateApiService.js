import Config from 'consts/index'
import { json } from 'services/request'

// ===================== GENERIC HELPERS ===================== //

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

// ===================== API FACTORY ===================== //

/**
 * Creates API methods for a specific asset type
 * @param {string} assetType - 'GT', 'STG', 'HRSG', or 'AUXBOILER'
 * @param {Object} config - Configuration for the asset type
 * @param {string} config.dropdownEndpoint - Endpoint for dropdown (e.g., 'heat-rate/drop-down' or 'hrsg/drop-down')
 * @param {string} config.dataEndpoint - Endpoint for data (e.g., 'gt-heat-rate')
 * @param {string} config.exportEndpoint - Endpoint for export (may differ from dataEndpoint)
 * @param {string} config.paramName - Parameter name for year used in GET/SAVE (e.g., 'year' or 'aopYear')
 * @param {string} config.exportParamName - Parameter name for year used in EXPORT (defaults to paramName)
 * @param {boolean} config.hasSeparateDropdown - Whether asset has its own dropdown endpoint (true for HRSG, AUXBOILER)
 */
function createHeatRateApi(assetType, config) {
  const {
    dropdownEndpoint,
    dataEndpoint,
    exportEndpoint = dataEndpoint,
    paramName = 'year',
    exportParamName = paramName,
    hasSeparateDropdown = false,
  } = config

  const assetKey = assetType.toLowerCase()
  const assetLabel =
    assetType.charAt(0).toUpperCase() + assetType.slice(1).toLowerCase()

  // Helper to build data endpoint URL
  function buildDataUrl(endpoint, queryParams) {
    return `${Config.CaseEngineUrl}/task/jmd/${endpoint}${queryParams ? `?${queryParams}` : ''}`
  }

  // Helper to handle API response
  async function handleResponse(keycloak, resp) {
    const result = await json(keycloak, resp)
    return result
  }

  // ----- DROPDOWN -----
  async function getAssetDropdown(keycloak, plantIds, assetTypeParam) {
    const queryParams = buildPlantIdsParam(plantIds)
    let url

    if (hasSeparateDropdown) {
      url = `${Config.CaseEngineUrl}/task/jmd/${dropdownEndpoint}?plantIds=${queryParams}&assetType=${assetTypeParam || assetType}`
    } else {
      // GT/STG share the same dropdown
      url = `${Config.CaseEngineUrl}/task/jmd/${dropdownEndpoint}?plantIds=${queryParams}&assetType=${assetTypeParam || assetType}`
    }

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
      console.error(`Error fetching ${assetLabel} asset dropdown:`, e)
      return await Promise.reject(e)
    }
  }

  // ----- FETCH DATA -----
  async function getHeatRateData(
    keycloak,
    assetId,
    aopYear,
    startDate,
    endDate,
    plantIds,
  ) {
    const queryParams = new URLSearchParams()
    queryParams.append('assetId', assetId)
    queryParams.append(paramName, aopYear)
    if (startDate && endDate) {
      queryParams.append('startDate', startDate)
      queryParams.append('endDate', endDate)
    }
    if (plantIds) {
      const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
      queryParams.append('plantIds', plantIdArray.join(','))
    }

    const url = buildDataUrl(dataEndpoint, queryParams.toString())
    try {
      const resp = await fetch(url, {
        method: 'GET',
        headers: buildHeaders(keycloak),
      })
      if (!resp.ok) {
        throw new Error(`HTTP error! Status: ${resp.status}`)
      }
      return await handleResponse(keycloak, resp)
    } catch (e) {
      console.error(`Error fetching ${assetLabel} heat rate data:`, e)
      return await Promise.reject(e)
    }
  }

  // ----- SAVE DATA -----
  async function saveHeatRateData(keycloak, aopYear, payload) {
    const url = `${Config.CaseEngineUrl}/task/jmd/${dataEndpoint}/${aopYear}`
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
      console.error(`Error saving ${assetLabel} heat rate data:`, e)
      return await Promise.reject(e)
    }
  }

  // ----- IMPORT EXCEL -----
  async function saveHeatRateExcel(
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
      const plantIdsStr = Array.isArray(plantIds)
        ? plantIds.join(',')
        : plantIds
      queryParams.append('plantIds', plantIdsStr)
    }

    const url = `${Config.CaseEngineUrl}/task/jmd/${dataEndpoint}/import?${queryParams.toString()}`
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
          `Failed to import ${assetLabel} heat rate data: ${resp.status} ${resp.statusText}`,
        )
      }
      return responseData
    } catch (e) {
      console.error(`Error importing ${assetLabel} Heat Rate Excel:`, e)
      return Promise.reject(e)
    }
  }

  // ----- EXPORT EXCEL -----
  async function exportHeatRateExcel(
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
    queryParams.append(exportParamName, aopYear)
    if (startDate) queryParams.append('startDate', startDate)
    if (endDate) queryParams.append('endDate', endDate)
    if (plantIds && plantIds.length > 0) {
      const plantIdsStr = Array.isArray(plantIds)
        ? plantIds.join(',')
        : plantIds
      queryParams.append('plantIds', plantIdsStr)
    }

    const endpoint = `jmd/${exportEndpoint}/export?${queryParams.toString()}`
    const fileName = assetDisplayName
      ? `${assetDisplayName}_${aopYear}.xlsx`
      : `${assetLabel}_Heat_Rate_${aopYear}.xlsx`

    const url = `${Config.CaseEngineUrl}/task/${endpoint}`
    const headers = {
      'Content-Type': 'application/json',
      Accept:
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
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
        const filenameMatch = contentDisposition.match(
          /filename=?"([^";\n]+)"?/i,
        )
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

  return {
    getAssetDropdown,
    getHeatRateData,
    saveHeatRateData,
    saveHeatRateExcel,
    exportHeatRateExcel,
  }
}

// ===================== CREATE API INSTANCES ===================== //

// GT - uses shared dropdown with STG
const gtApi = createHeatRateApi('GT', {
  dropdownEndpoint: 'power-heat-rate/drop-down',
  dataEndpoint: 'gt-heat-rate',
  paramName: 'year',
  hasSeparateDropdown: false,
})

// STG - uses shared dropdown with GT
// NOTE: GET data uses 'aopYear' but export/import endpoints use 'year'
const stgApi = createHeatRateApi('STG', {
  dropdownEndpoint: 'power-heat-rate/drop-down',
  dataEndpoint: 'stg-heat-rate',
  paramName: 'aopYear',
  exportParamName: 'year',
  hasSeparateDropdown: false,
})

// HRSG - has its own dropdown
const hrsgApi = createHeatRateApi('HRSG', {
  dropdownEndpoint: 'steam-heat-rate/drop-down',
  dataEndpoint: 'hrsg-heat-rate',
  paramName: 'year',
  hasSeparateDropdown: true,
})

// AUXBOILER - follows HRSG pattern
const auxBoilerApi = createHeatRateApi('AUXBOILER', {
  dropdownEndpoint: 'steam-heat-rate/drop-down',
  dataEndpoint: 'hrsg-heat-rate',
  paramName: 'year',
  hasSeparateDropdown: true,
})

// ===================== EXPORT ===================== //

export const HeatRateApiService = {
  // GT
  getGTAssetDropdown: gtApi.getAssetDropdown,
  getGTHeatRateData: gtApi.getHeatRateData,
  saveGTHeatRateData: gtApi.saveHeatRateData,
  saveGTHeatRateExcel: gtApi.saveHeatRateExcel,
  exportGTHeatRateExcel: gtApi.exportHeatRateExcel,

  // STG
  getSTGHeatRateData: stgApi.getHeatRateData,
  saveSTGHeatRateData: stgApi.saveHeatRateData,
  saveSTGHeatRateExcel: stgApi.saveHeatRateExcel,
  exportSTGHeatRateExcel: stgApi.exportHeatRateExcel,

  // HRSG
  getHRSGAssetDropdown: hrsgApi.getAssetDropdown,
  getHRSGHeatRateData: hrsgApi.getHeatRateData,
  saveHRSGHeatRateData: hrsgApi.saveHeatRateData,
  saveHRSGHeatRateExcel: hrsgApi.saveHeatRateExcel,
  exportHRSGHeatRateExcel: hrsgApi.exportHeatRateExcel,

  // AUXBOILER
  getAuxBoilerAssetDropdown: auxBoilerApi.getAssetDropdown,
  getAuxBoilerHeatRateData: auxBoilerApi.getHeatRateData,
  saveAuxBoilerHeatRateData: auxBoilerApi.saveHeatRateData,
  saveAuxBoilerHeatRateExcel: auxBoilerApi.saveHeatRateExcel,
  exportAuxBoilerHeatRateExcel: auxBoilerApi.exportHeatRateExcel,
}
