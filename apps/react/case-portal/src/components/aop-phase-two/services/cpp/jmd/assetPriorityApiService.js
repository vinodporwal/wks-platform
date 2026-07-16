import Config from 'consts/index'
import { json } from 'services/request'

export const AssetPriorityApiService = {
  getAssetPriority,
  saveAssetPriority,
  saveAssetPriorityExcel,
  exportAssetPriorityExcel,
  exportPowerAssetPriority,
  exportSteamAssetPriority,
  importPowerAssetPriority,
  importSteamAssetPriority,
}

// ========================|| Asset Priority APIs ||=====================================//

async function getAssetPriority(keycloak, PLANT_ID_LIST, year) {
  const plantIdArray = Array.isArray(PLANT_ID_LIST)
    ? PLANT_ID_LIST
    : [PLANT_ID_LIST]
  const queryParams = plantIdArray.map((id) => id).join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/priorities?plantIds=${queryParams}&aopYear=${year}`
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

async function saveAssetPriority(keycloak, PLANT_ID_LIST, AOP_YEAR, payload) {
  const plantIdArray = Array.isArray(PLANT_ID_LIST)
    ? PLANT_ID_LIST
    : [PLANT_ID_LIST]
  const queryParams = plantIdArray.map((id) => id).join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/priorities?plantIds=${queryParams}&aopYear=${AOP_YEAR}`
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

// Asset Priority Excel Import
async function saveAssetPriorityExcel(file, keycloak, PLANT_ID_LIST, AOP_YEAR) {
  const plantIdArray = Array.isArray(PLANT_ID_LIST)
    ? PLANT_ID_LIST
    : [PLANT_ID_LIST]
  const queryParams = plantIdArray.map((id) => id).join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/priorities/import?plantIds=${queryParams}&aopYear=${AOP_YEAR}`
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
    console.error(`Error importing Asset Priority Excel:`, e)
    return Promise.reject(e)
  }
}

// Asset Priority Excel Export
async function exportAssetPriorityExcel(
  keycloak,
  PLANT_ID_LIST,
  AOP_YEAR,
  EXCEL_NAME,
) {
  const plantIdArray = Array.isArray(PLANT_ID_LIST)
    ? PLANT_ID_LIST
    : [PLANT_ID_LIST]
  const queryParams = plantIdArray.map((id) => id).join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/priorities/export?plantIds=${queryParams}&aopYear=${AOP_YEAR}`

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
    console.error(`Error exporting Asset Priority Excel:`, e)
    return Promise.reject(e)
  }
}

// Power Asset Priority Excel Export
async function exportPowerAssetPriority(
  keycloak,
  PLANT_ID_LIST,
  AOP_YEAR,
  EXCEL_NAME,
) {
  const plantIdArray = Array.isArray(PLANT_ID_LIST)
    ? PLANT_ID_LIST
    : [PLANT_ID_LIST]
  const queryParams = plantIdArray.map((id) => id).join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/power-priority/export?plantIds=${queryParams}&aopYear=${AOP_YEAR}`

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
    console.error(`Error exporting Power Asset Priority Excel:`, e)
    return Promise.reject(e)
  }
}

// Steam Asset Priority Excel Export
async function exportSteamAssetPriority(
  keycloak,
  PLANT_ID_LIST,
  AOP_YEAR,
  EXCEL_NAME,
) {
  const plantIdArray = Array.isArray(PLANT_ID_LIST)
    ? PLANT_ID_LIST
    : [PLANT_ID_LIST]
  const queryParams = plantIdArray.map((id) => id).join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/steam-priority/export?plantIds=${queryParams}&aopYear=${AOP_YEAR}`

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
    console.error(`Error exporting Steam Asset Priority Excel:`, e)
    return Promise.reject(e)
  }
}

// Power Asset Priority Excel Import
async function importPowerAssetPriority(
  file,
  keycloak,
  PLANT_ID_LIST,
  AOP_YEAR,
) {
  const plantIdArray = Array.isArray(PLANT_ID_LIST)
    ? PLANT_ID_LIST
    : [PLANT_ID_LIST]
  const queryParams = plantIdArray.map((id) => id).join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/power-priority/import?plantIds=${queryParams}&aopYear=${AOP_YEAR}`
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
    console.error(`Error importing Power Asset Priority Excel:`, e)
    return Promise.reject(e)
  }
}

// Steam Asset Priority Excel Import
async function importSteamAssetPriority(
  file,
  keycloak,
  PLANT_ID_LIST,
  AOP_YEAR,
) {
  const plantIdArray = Array.isArray(PLANT_ID_LIST)
    ? PLANT_ID_LIST
    : [PLANT_ID_LIST]
  const queryParams = plantIdArray.map((id) => id).join(',')
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/steam-priority/import?plantIds=${queryParams}&aopYear=${AOP_YEAR}`
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
    console.error(`Error importing Steam Asset Priority Excel:`, e)
    return Promise.reject(e)
  }
}
