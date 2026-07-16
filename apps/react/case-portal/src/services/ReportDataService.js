import Config from '../consts'
import { json } from './request'

export const ReportDataService = {
  getShutdownData,
  saveShutdownPlannedData,
  saveShutdownPreviousYearsData,
  deleteRoutineShutdownData,
  deletePlannedShutdownData,
  getShutdownSummaryLastFourYearData,
  getMonthwiseOperatingHours,
  saveMonthwiseOperatingHours,
  getPlantShutdownSlowdownNormsDuration,
  savePlantShutdownSlowdownNormsDuration,
  deletePlantShutdownSlowdownNormsDuration,
  saveShutdownRoutineData,
  saveShutdownSummaryLastFourYearData,
  deleteShutdownLastFourYears,
  deleteRoutineShutdownsMonthwiseData,
  getMonthlyProductionReportData,
  getOptimizerInputOutput,
}

async function getShutdownData(keycloak, PLANT_ID, AOP_YEAR, type) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-details?plantId=${PLANT_ID}&year=${AOP_YEAR}&type=${type}`
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
async function saveShutdownPlannedData(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  type,
  payload,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-details?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
async function saveShutdownPreviousYearsData(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  type,
  payload,
) {
  const url = `${Config.CaseEngineUrl}/task/routine-shutdown-previous-years?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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

async function deleteRoutineShutdownData(Id, keycloak, PLANT_ID) {
  const url = `${Config.CaseEngineUrl}/task/routine-shutdown-previous-years?id=${Id}`
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
    console.error('Error deleting routine shutdown data:', e)
    return Promise.reject(e)
  }
}
async function deletePlannedShutdownData(Id, keycloak, PLANT_ID) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-details?id=${Id}`
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
    console.error('Error deleting planned shutdown data:', e)
    return Promise.reject(e)
  }
}
async function getShutdownSummaryLastFourYearData(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-summary-last-four-year?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
async function getMonthwiseOperatingHours(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/monthwise-operating-hours?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
async function saveMonthwiseOperatingHours(
  keycloak,
  plantId,
  year,
  monthwiseOperatingHoursDTOs,
) {
  const url = `${Config.CaseEngineUrl}/task/monthwise-operating-hours?plantId=${plantId}&year=${year}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(monthwiseOperatingHoursDTOs),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error saving monthwise operating hours:', e)
    return await Promise.reject(e)
  }
}

async function getPlantShutdownSlowdownNormsDuration(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/plant-shutdown-slowdown-norms-duration?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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

async function savePlantShutdownSlowdownNormsDuration(
  keycloak,
  plantId,
  year,
  payload,
) {
  const url = `${Config.CaseEngineUrl}/task/plant-shutdown-slowdown-norms-duration?plantId=${plantId}&year=${year}`
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
    console.error('Error saving monthwise operating hours:', e)
    return await Promise.reject(e)
  }
}

async function deletePlantShutdownSlowdownNormsDuration(id, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/plant-shutdown-slowdown-norms-duration?id=${id}`
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
async function saveShutdownRoutineData(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  type,
  payload,
) {
  const url = `${Config.CaseEngineUrl}/task/routine-shutdown?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
async function saveShutdownSummaryLastFourYearData(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  payload,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-summary-last-four-year?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
async function deleteShutdownLastFourYears(id, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-summary-last-four-year?id=${id}`
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
    console.error('Error deleting shutdown data:', e)
    return Promise.reject(e)
  }
}
async function deleteRoutineShutdownsMonthwiseData(Id, keycloak, PLANT_ID) {
  const url = `${Config.CaseEngineUrl}/task/routine-shutdown?id=${Id}`
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
    console.error('Error deleting routine shutdown data:', e)
    return Promise.reject(e)
  }
}
async function getMonthlyProductionReportData(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/monthly-production-report?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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

async function getOptimizerInputOutput(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/data-set-optimizer?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
