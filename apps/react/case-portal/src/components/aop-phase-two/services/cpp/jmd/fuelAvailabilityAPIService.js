import Config from 'consts/index'
import { json } from 'services/request'

const BASE = '/task/jmd/fuel-availability'
const BASE_COST = '/task/jmd/fuel-availability-cost'

export const FuelAvailabilityAPIService = {
  getFuelAvailability,
  saveFuelAvailability,
  exportFuelAvailability,
  importFuelAvailability,
  getFuels,
  deleteFuelAvailability,
  getFuelAvailabilityCost,
  getMappedFuelAvailability,
  saveFuelAvailabilityCost,
  exportFuelAvailabilityCost,
  importFuelAvailabilityCost,
}

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

// ===================== || FUEL AVAILABILITY GET || ===================== //
// GET {base}?siteId=...&plantIds=...&financialYear=...&type=...
async function getFuelAvailabilityBase(
  keycloak,
  plantIds,
  financialYear,
  type,
  base,
  siteId,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  let url = `${Config.CaseEngineUrl}${base}?financialYear=${financialYear}`
  if (queryParams) {
    url += `&plantIds=${queryParams}`
  }
  if (siteId) {
    url += `&siteId=${encodeURIComponent(siteId)}`
  }
  if (type) {
    url += `&type=${encodeURIComponent(type)}`
  }
  const headers = buildHeaders(keycloak)
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching fuel availability data:', e)
    return await Promise.reject(e)
  }
}

function getFuelAvailability(keycloak, plantIds, financialYear, type) {
  return getFuelAvailabilityBase(keycloak, plantIds, financialYear, type, BASE)
}

function getFuelAvailabilityCost(
  keycloak,
  plantIds,
  financialYear,
  type,
  siteId,
) {
  return getFuelAvailabilityBase(
    keycloak,
    plantIds,
    financialYear,
    type,
    BASE_COST,
    siteId,
  )
}

// GET {base}/mapped?plantIds=...&financialYear=...&type=...
// Fuel availability expanded through CPPPlantFuelSourceMapping — rows declared
// under source plants (e.g. C2-CPP, DTA-PCG-CPP) come back attributed to the
// consuming CPP plant (e.g. DTA-CPP). Used by the Fuel Priority screen.
async function getMappedFuelAvailability(
  keycloak,
  plantIds,
  financialYear,
  type,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  let url = `${Config.CaseEngineUrl}${BASE}/mapped?plantIds=${queryParams}&financialYear=${financialYear}`
  if (type) {
    url += `&type=${encodeURIComponent(type)}`
  }
  const headers = buildHeaders(keycloak)
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching mapped fuel availability data:', e)
    return await Promise.reject(e)
  }
}

// ===================== || FUEL AVAILABILITY SAVE OR UPDATE || ===================== //
// POST {base}?plantIds=...&financialYear=...
async function saveFuelAvailabilityBase(
  keycloak,
  plantIds,
  financialYear,
  payload,
  base,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}${base}?plantIds=${queryParams}&financialYear=${financialYear}`
  const headers = buildHeaders(keycloak)
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
    console.error('Error saving fuel availability data:', e)
    return await Promise.reject(e)
  }
}

function saveFuelAvailability(keycloak, plantIds, financialYear, payload) {
  return saveFuelAvailabilityBase(
    keycloak,
    plantIds,
    financialYear,
    payload,
    BASE,
  )
}

function saveFuelAvailabilityCost(keycloak, plantIds, financialYear, payload) {
  return saveFuelAvailabilityBase(
    keycloak,
    plantIds,
    financialYear,
    payload,
    BASE_COST,
  )
}

// ===================== || FUEL AVAILABILITY EXCEL IMPORT || ===================== //
// POST {base}/import?plantIds=...&financialYear=... (multipart)
async function importFuelAvailabilityBase(
  file,
  keycloak,
  plantIds,
  financialYear,
  base,
  minimal,
  type,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  let url = `${Config.CaseEngineUrl}${base}/import?plantIds=${queryParams}&financialYear=${financialYear}`
  if (minimal) {
    url += `&minimal=true`
  }
  if (type) {
    url += `&type=${encodeURIComponent(type)}`
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
    console.error('Error importing Fuel Availability Excel data:', e)
    return Promise.reject(e)
  }
}

function importFuelAvailability(
  file,
  keycloak,
  plantIds,
  financialYear,
  minimal,
  type,
) {
  return importFuelAvailabilityBase(
    file,
    keycloak,
    plantIds,
    financialYear,
    BASE,
    minimal,
    type,
  )
}

function importFuelAvailabilityCost(
  file,
  keycloak,
  plantIds,
  financialYear,
  type,
) {
  return importFuelAvailabilityBase(
    file,
    keycloak,
    plantIds,
    financialYear,
    BASE_COST,
    true,
    type,
  )
}

// ===================== || FUEL AVAILABILITY EXPORT || ===================== //
// GET {base}/export?plantIds=...&financialYear=...&type=...
async function exportFuelAvailabilityBase(
  keycloak,
  plantIds,
  financialYear,
  type,
  fileName,
  base,
  siteId,
  minimal,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  let url = `${Config.CaseEngineUrl}${base}/export?financialYear=${financialYear}`
  if (queryParams) {
    url += `&plantIds=${queryParams}`
  }
  if (siteId) {
    url += `&siteId=${encodeURIComponent(siteId)}`
  }
  if (type) {
    url += `&type=${encodeURIComponent(type)}`
  }
  if (minimal) {
    url += `&minimal=true`
  }
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
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl

    // Extract filename from Content-Disposition header if available
    const contentDisposition = resp.headers.get('content-disposition')
    let downloadFileName = fileName
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename="?([^";\n]+)"?/i)
      if (filenameMatch && filenameMatch[1]) {
        downloadFileName = filenameMatch[1]
      }
    }

    link.download = downloadFileName || `FuelAvailability_${financialYear}.xlsx`
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(downloadUrl)
  } catch (e) {
    console.error('Error exporting Fuel Availability data:', e)
    return Promise.reject(e)
  }
}

function exportFuelAvailability(
  keycloak,
  plantIds,
  financialYear,
  type,
  fileName,
  siteId,
  minimal,
) {
  return exportFuelAvailabilityBase(
    keycloak,
    plantIds,
    financialYear,
    type,
    fileName,
    BASE,
    siteId,
    minimal,
  )
}

function exportFuelAvailabilityCost(
  keycloak,
  plantIds,
  financialYear,
  type,
  fileName,
  siteId,
) {
  return exportFuelAvailabilityBase(
    keycloak,
    plantIds,
    financialYear,
    type,
    fileName,
    BASE_COST,
    siteId,
    true,
  )
}

// ===================== || FUELS LIST (with category) || ===================== //
// GET /task/jmd/fuel-availability/fuels?type=FUEL
// Returns { data: [{ id, fuelName, fuelDisplayName, type, categoryFkId, categoryName, categoryDisplayName }] }
async function getFuels(keycloak, type) {
  let url = `${Config.CaseEngineUrl}${BASE}/fuels`
  if (type) {
    url += `?type=${encodeURIComponent(type)}`
  }
  const headers = buildHeaders(keycloak)
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching fuels list:', e)
    return await Promise.reject(e)
  }
}

// ===================== || FUEL AVAILABILITY DELETE || ===================== //
// DELETE /task/jmd/fuel-availability/{id}
async function deleteFuelAvailability(keycloak, id) {
  const url = `${Config.CaseEngineUrl}${BASE}/${id}`
  const headers = buildHeaders(keycloak)
  try {
    const resp = await fetch(url, { method: 'DELETE', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error deleting fuel availability record:', e)
    return await Promise.reject(e)
  }
}
