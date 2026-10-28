import Config from 'consts/index'
import { json } from 'services/request'

export const QuantityApiService = {
  getQuantity,
  saveQuantityData,
  saveQuantityExcel,
  exportQuantityDetailed,
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

// ===================== || QUANTITY GET || ===================== //
async function getQuantity(keycloak, plantIds, financialYear) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/quantity?cppPlantIds=${queryParams}&financialYear=${financialYear}`
  const headers = buildHeaders(keycloak)
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

// ===================== || QUANTITY SAVE OR UPDATE || ===================== //
async function saveQuantityData(keycloak, payload, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/jmd-saveOrUpdateQuantityMonths/${AOP_YEAR}`
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
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ===================== || QUANTITY EXCEL IMPORT || ===================== //
async function saveQuantityExcel(file, keycloak, plantIds, AOP_YEAR) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/quantity/import?cppPlantIds=${queryParams}&financialYear=${AOP_YEAR}`
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
    console.error(`Error importing Quantity Excel data:`, e)
    return Promise.reject(e)
  }
}

// ===================== || QUANTITY DETAILED EXPORT || ===================== //
async function exportQuantityDetailed(keycloak, plantIds, AOP_YEAR, fileName) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/quantity/detailed/export?cppPlantIds=${queryParams}&financialYear=${AOP_YEAR}`
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
    link.download = fileName || `Quantity_Detailed_${AOP_YEAR}.xlsx`
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(downloadUrl)
  } catch (e) {
    console.error('Error exporting Quantity data:', e)
    return Promise.reject(e)
  }
}
