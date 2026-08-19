import Config from 'consts/index'
import { json } from 'services/request'

export const FuelAvailabilityAPIService = {
  getFuelAvailability,
  saveFuelAvailability,
  exportFuelAvailability,
  importFuelAvailability,
  getFuels,
  deleteFuelAvailability,
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
// GET /task/jmd/fuel-availability?plantIds=...&financialYear=...&type=...
async function getFuelAvailability(keycloak, plantIds, financialYear, type) {
  const queryParams = buildPlantIdsParam(plantIds)
  let url = `${Config.CaseEngineUrl}/task/jmd/fuel-availability?plantIds=${queryParams}&financialYear=${financialYear}`
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

// ===================== || FUEL AVAILABILITY SAVE OR UPDATE || ===================== //
// POST /task/jmd/fuel-availability?plantIds=...&financialYear=...
async function saveFuelAvailability(
  keycloak,
  plantIds,
  financialYear,
  payload,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/fuel-availability?plantIds=${queryParams}&financialYear=${financialYear}`
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

// ===================== || FUEL AVAILABILITY EXCEL IMPORT || ===================== //
// POST /task/jmd/fuel-availability/import?plantIds=...&financialYear=... (multipart)
async function importFuelAvailability(file, keycloak, plantIds, financialYear) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/fuel-availability/import?plantIds=${queryParams}&financialYear=${financialYear}`
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

// ===================== || FUEL AVAILABILITY EXPORT || ===================== //
// GET /task/jmd/fuel-availability/export?plantIds=...&financialYear=...&type=...
async function exportFuelAvailability(
  keycloak,
  plantIds,
  financialYear,
  type,
  fileName,
) {
  const queryParams = buildPlantIdsParam(plantIds)
  let url = `${Config.CaseEngineUrl}/task/jmd/fuel-availability/export?plantIds=${queryParams}&financialYear=${financialYear}`
  if (type) {
    url += `&type=${encodeURIComponent(type)}`
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

// ===================== || FUELS LIST (with category) || ===================== //
// GET /task/jmd/fuel-availability/fuels?type=FUEL
// Returns { data: [{ id, fuelName, fuelDisplayName, type, categoryFkId, categoryName, categoryDisplayName }] }
async function getFuels(keycloak, type) {
  let url = `${Config.CaseEngineUrl}/task/jmd/fuel-availability/fuels`
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
  const url = `${Config.CaseEngineUrl}/task/jmd/fuel-availability/${id}`
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
