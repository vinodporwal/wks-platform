import Config from '../consts'
import { json } from './request'

export const DtaDataService = {
  getLineDetails,
  exportShutdownLineWise,
  ImportShutdownLineWise,
  exportSlowdownLineWise,
  ImportSlowdownLineWise,
  ExportShutdownHistoryConfig,
  importShutdownHistoryConfig,
  exportShutdownElastomerjmd,
  ImportShutdownElastomerjmd,
  exportSlowdownElastomerJmd,
  ImportSlowdownElastomerJmd,
  exportShutdownPEC2,
  ImportShutdownPEC2,
  exportShutdownMonthLineWise,
  ImportShutdownLineWisePP,
  ExcelSlowdownConfiguration,
  ImportSlowdownConfiguration,
  getShutdownHistoryConfigData,
  saveShutdownHistoryConfig,
  deleteShutdownHistoryConfigData,
  exportShutdownHistoryConfig,
  ImportShutdownHistoryConfig,
}
export async function getLineDetails(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/line-details?plantId=${PLANT_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Plant Team data:', e)
    return Promise.reject(e)
  }
}
export async function exportShutdownLineWise(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
) {
  const maintenanceTypeName = 'Shutdown'
  const url = `${Config.CaseEngineUrl}/task/shutdown-export-line?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}&maintenanceTypeName=${encodeURIComponent(maintenanceTypeName)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Shutdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function ImportShutdownLineWise(file, keycloak, plantId, year) {
  const maintenanceTypeName = 'Shutdown'
  const url = `${Config.CaseEngineUrl}/task/shutdown-import-line?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}&maintenanceTypeName=${encodeURIComponent(maintenanceTypeName)}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error importing Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function exportSlowdownLineWise(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
) {
  const maintenanceTypeName = 'Slowdown'
  const url = `${Config.CaseEngineUrl}/task/slowdown-export-line?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}&maintenanceTypeName=${encodeURIComponent(maintenanceTypeName)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Slowdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Slowdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function ImportSlowdownLineWise(file, keycloak, plantId, year) {
  const maintenanceTypeName = 'Slowdown'
  const url = `${Config.CaseEngineUrl}/task/slowdown-import-line?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}&maintenanceTypeName=${encodeURIComponent(maintenanceTypeName)}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error importing Slowdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function ExportShutdownHistoryConfig(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
) {
  const maintenanceTypeName = 'Shutdown'
  const url = `${Config.CaseEngineUrl}/task/shutdown-history-pta-export-excel?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Shutdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function importShutdownHistoryConfig(
  file,
  keycloak,
  plantId,
  year,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-history-pta-import-excel?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error importing Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function exportShutdownElastomerjmd(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-export-hiir?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Shutdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function ImportShutdownElastomerjmd(
  file,
  keycloak,
  plantId,
  year,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-import-hiir?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error importing Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function exportSlowdownElastomerJmd(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-export-hiir?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Slowdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Slowdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function ImportSlowdownElastomerJmd(
  file,
  keycloak,
  plantId,
  year,
) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-import-hiir?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error importing Slowdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function exportShutdownPEC2(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
) {
  const maintenanceTypeName = 'Shutdown'
  const url = `${Config.CaseEngineUrl}/task/shutdown-export-excel?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}&maintenanceTypeName=${encodeURIComponent(maintenanceTypeName)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Shutdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function ImportShutdownPEC2(file, keycloak, plantId, year) {
  const maintenanceTypeName = 'Shutdown'
  const url = `${Config.CaseEngineUrl}/task/shutdown-export-excel-import?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}&maintenanceTypeName=${encodeURIComponent(maintenanceTypeName)}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error importing Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function exportShutdownMonthLineWise(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
) {
  const maintenanceTypeName = 'Shutdown'
  const url = `${Config.CaseEngineUrl}/task/shutdown-export-line-pp?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}&maintenanceTypeName=${encodeURIComponent(maintenanceTypeName)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Shutdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function ImportShutdownLineWisePP(file, keycloak, plantId, year) {
  const maintenanceTypeName = 'Shutdown'
  const url = `${Config.CaseEngineUrl}/task/shutdown-import-line-pp?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}&maintenanceTypeName=${encodeURIComponent(maintenanceTypeName)}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error importing Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function ExcelSlowdownConfiguration(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-configuration-export?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Slowdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Slowdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function ImportSlowdownConfiguration(
  file,
  keycloak,
  plantId,
  year,
) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-configuration-import?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error importing Slowdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function getShutdownHistoryConfigData(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-history-config?plantId=${PLANT_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return await resp.json()
  } catch (e) {
    console.error('Error fetching Plant Team data:', e)
    return Promise.reject(e)
  }
}
async function saveShutdownHistoryConfig(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  payload,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-history-config?plantId=${PLANT_ID}&year=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}
async function deleteShutdownHistoryConfigData(maintenanceId, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-history-config?Id=${maintenanceId}`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'DELETE',
      headers,
    })
    if (!resp.ok) {
      throw new Error(
        `Failed to delete data: ${resp.status} ${resp.statusText}`,
      )
    }
    return await resp.text() // Handle text response from the backend
  } catch (e) {
    console.error('Error deleting slowdown data:', e)
    return Promise.reject(e)
  }
}
export async function exportShutdownHistoryConfig(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-history-config-export-excel?year=${year}&plantId=${plantId}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Shutdown History Config.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
export async function ImportShutdownHistoryConfig(
  file,
  keycloak,
  plantId,
  year,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-history-config-import-excel?year=${year}&plantId=${plantId}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error importing Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
