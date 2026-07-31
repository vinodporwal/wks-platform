import Config from '../consts'
import { json } from './request'

export const ReliabilityPerformancePlantWiseFunctionalApiService = {
  exportReliabilityExcelPlantWise,
  saveReliabilityPerformancePlantWise,
  getReliabilityPerformancePlantWise,
  importReliabilityPerformanceExcelPlantWise,
}

async function getReliabilityPerformancePlantWise(
  keycloak,
  plantId,
  year,
  type,
) {
  const baseUrl = `${Config.CaseEngineUrl}/task/reliability-performance-plant-wise`
  const queryParams = new URLSearchParams({
    plantId,
    year,
    type,
  })

  const url = `${baseUrl}?${queryParams.toString()}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function saveReliabilityPerformancePlantWise(payloadData, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/reliability-performance-plant-wise`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(payloadData),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function exportReliabilityExcelPlantWise(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/reliability-performance-export-excel-plant-wise?year=${encodeURIComponent(AOP_YEAR)}&plantId=${encodeURIComponent(PLANT_ID)}`
  const fileName = 'Reliability_Performance.xlsx'
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
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Reliability Performance Excel:', e)
    return Promise.reject(e)
  }
}

async function importReliabilityPerformanceExcelPlantWise(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  rawFile,
) {
  const url = `${Config.CaseEngineUrl}/task/reliability-performance-import-excel-plant-wise?plantId=${PLANT_ID}&year=${AOP_YEAR}`
  const formData = new FormData()
  formData.append('file', rawFile)

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
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error importing Reliability Performance Excel:', e)
    return await Promise.reject(e)
  }
}
