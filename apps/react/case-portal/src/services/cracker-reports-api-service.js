import Config from '../consts'
import { json } from './request'
export const CrackerReportsApiDataService = {
  spyroInputReport,
  spyroOutputReport,
  finalNormsReport,
  finalNormsProductionReport,
  configurationIntermediateValues,
  getRawDataSetvalues,
  getRawutilitymonthly,
  getRawCatcame,
  getRawatcammonthly,
  getRawasteam,
  getRawasfindingteam,
  getConfigurationExecutionDetails,
  findingModel,
  miisData,
  furnaceRawData,
  runLengthDataSet,
  calculateMonthWiseRawData,
  getSpyroInputMinMaxData,
  saveSpyroInputMinMaxData,
  exportSpyroInputMinMax,
  importSpyroInputMinMax,
}

async function runLengthDataSet(keycloak, reportType, PLANT_ID, AOP_YEAR) {
  let url = `${Config.CaseEngineUrl}/task/run-length-data-set?plantId=${PLANT_ID}&year=${AOP_YEAR}&reportType=${reportType}`

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
    return Promise.reject(e)
  }
}

async function miisData(
  keycloak,
  reportType,
  periodFrom,
  periodTo,
  mode,
  PLANT_ID,
  AOP_YEAR,
) {
  let url = `${Config.CaseEngineUrl}/task/miis-data?plantId=${PLANT_ID}&year=${AOP_YEAR}`

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
    return Promise.reject(e)
  }
}
async function findingModel(
  keycloak,
  reportType,
  periodFrom,
  periodTo,
  mode,
  PLANT_ID,
  AOP_YEAR,
) {
  let url = `${Config.CaseEngineUrl}/task/finding-model?plantId=${PLANT_ID}&year=${AOP_YEAR}`

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
    return Promise.reject(e)
  }
}

async function furnaceRawData(keycloak, reportType, PLANT_ID, AOP_YEAR) {
  let url = `${Config.CaseEngineUrl}/task/report-furnace?plantId=${PLANT_ID}&year=${AOP_YEAR}&reportType=${reportType}`

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
    return Promise.reject(e)
  }
}

async function configurationIntermediateValues(keycloak, PLANT_ID, AOP_YEAR) {
  let url = `${Config.CaseEngineUrl}/task/configuration-intermediate-values?plantId=${PLANT_ID}&year=${AOP_YEAR}`

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
    return Promise.reject(e)
  }
}

async function getRawDataSetvalues(
  keycloak,
  periodFrom,
  periodTo,
  PLANT_ID,
  AOP_YEAR,
) {
  let url = `${Config.CaseEngineUrl}/task/report-best-achieved-raw?plantId=${PLANT_ID}&year=${AOP_YEAR}&periodFrom=${periodFrom}&periodTo=${periodTo}`

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
    return Promise.reject(e)
  }
}
async function getRawCatcame(
  keycloak,
  periodFrom,
  periodTo,
  PLANT_ID,
  AOP_YEAR,
) {
  let url = `${Config.CaseEngineUrl}/task/report-best-achieved-catcam?plantId=${PLANT_ID}&year=${AOP_YEAR}&periodFrom=${periodFrom}&periodTo=${periodTo}`

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
    return Promise.reject(e)
  }
}
async function calculateMonthWiseRawData(keycloak, PLANT_ID, AOP_YEAR) {
  let url = `${Config.CaseEngineUrl}/task/calculate-month-wise-raw-data?plantId=${PLANT_ID}&year=${AOP_YEAR}`

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
    return Promise.reject(e)
  }
}

async function getRawutilitymonthly(
  keycloak,
  periodFrom,
  periodTo,
  PLANT_ID,
  AOP_YEAR,
) {
  let url = `${Config.CaseEngineUrl}/task/report-best-achieved-mis-utility-monthly?plantId=${PLANT_ID}&year=${AOP_YEAR}&periodFrom=${periodFrom}&periodTo=${periodTo}`

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
    return Promise.reject(e)
  }
}

async function getRawatcammonthly(
  keycloak,
  periodFrom,
  periodTo,
  PLANT_ID,
  AOP_YEAR,
) {
  let url = `${Config.CaseEngineUrl}/task/report-best-achieved-stg-catcam-monthly?plantId=${PLANT_ID}&year=${AOP_YEAR}&periodFrom=${periodFrom}&periodTo=${periodTo}`

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
    return Promise.reject(e)
  }
}
///task/report-best-achieved-raw-steam?
async function getRawasteam(
  keycloak,
  periodFrom,
  periodTo,
  mode,
  PLANT_ID,
  AOP_YEAR,
) {
  // ✅ Encode mode to handle special characters like '+'
  const encodedMode = encodeURIComponent(mode)

  let url = `${Config.CaseEngineUrl}/task/report-best-achieved-raw-steam?plantId=${PLANT_ID}&year=${AOP_YEAR}&periodFrom=${periodFrom}&periodTo=${periodTo}&mode=${encodedMode}`

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
    return Promise.reject(e)
  }
}

async function getRawasfindingteam(keycloak, mode, PLANT_ID, AOP_YEAR) {
  const encodedMode = encodeURIComponent(mode)

  let url = `${Config.CaseEngineUrl}/task/report-best-achieved-finding-steam?plantId=${PLANT_ID}&year=${AOP_YEAR}&mode=${encodedMode}`

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
    return Promise.reject(e)
  }
}
async function getConfigurationExecutionDetails(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/configuration-execution?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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

async function finalNormsReport(keycloak, reportType, PLANT_ID, AOP_YEAR) {
  let url = `${Config.CaseEngineUrl}/task/final-norms-report?plantId=${PLANT_ID}&year=${AOP_YEAR}&reportType=${reportType}`

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
    return Promise.reject(e)
  }
}
async function finalNormsProductionReport(
  keycloak,
  reportType,
  PLANT_ID,
  AOP_YEAR,
) {
  let url = `${Config.CaseEngineUrl}/task/final-norms-production-report?plantId=${PLANT_ID}&year=${AOP_YEAR}&reportType=${reportType}`

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
    return Promise.reject(e)
  }
}
async function spyroOutputReport(
  keycloak,
  reportType,
  mode,
  PLANT_ID,
  AOP_YEAR,
) {
  let url = `${Config.CaseEngineUrl}/task/spyro-output-report?plantId=${PLANT_ID}&year=${AOP_YEAR}&mode=${mode}`

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
    return Promise.reject(e)
  }
}

async function spyroInputReport(
  keycloak,
  reportType,
  mode,
  PLANT_ID,
  AOP_YEAR,
) {
  let url = `${Config.CaseEngineUrl}/task/spyro-input-report?plantId=${PLANT_ID}&year=${AOP_YEAR}&mode=${mode}`

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
    return Promise.reject(e)
  }
}
async function getSpyroInputMinMaxData(keycloak, VERTICAL_ID, SITE_ID, PLANT_ID, AOP_YEAR, mode) {
  let url = `${Config.CaseEngineUrl}/task/spyro-input-min-max?verticalId=${VERTICAL_ID}&siteId=${SITE_ID}&plantId=${PLANT_ID}&aopYear=${AOP_YEAR}&mode=${mode}`

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
    return Promise.reject(e)
  }
}
async function saveSpyroInputMinMaxData(keycloak, PLANT_ID, AOP_YEAR, payload) {
  const url = `${Config.CaseEngineUrl}/task/spyro-input-min-max?plantFKId=${PLANT_ID}&year=${AOP_YEAR}`
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
    return Promise.reject(e)
  }
}

async function exportSpyroInputMinMax(
  keycloak,
  PLANT_ID,
  SITE_ID,
  VERTICAL_ID,
  AOP_YEAR,
  mode,
  EXCEL_NAME,
) {
  const url = `${Config.CaseEngineUrl}/task/spyro-input-min-max-export?plantId=${encodeURIComponent(PLANT_ID)}&siteId=${encodeURIComponent(SITE_ID)}&verticalId=${encodeURIComponent(VERTICAL_ID)}&aopYear=${encodeURIComponent(AOP_YEAR)}&mode=${encodeURIComponent(mode)}`
  const headers = {
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = EXCEL_NAME || `Spyro_Input_Min_Max_${AOP_YEAR}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Spyro Input Min Max Excel:', e)
    return Promise.reject(e)
  }
}

async function importSpyroInputMinMax(
  keycloak,
  PLANT_ID,
  SITE_ID,
  VERTICAL_ID,
  AOP_YEAR,
  mode,
  file,
) {
  const url = `${Config.CaseEngineUrl}/task/spyro-input-min-max-import?plantId=${encodeURIComponent(PLANT_ID)}&siteId=${encodeURIComponent(SITE_ID)}&verticalId=${encodeURIComponent(VERTICAL_ID)}&aopYear=${encodeURIComponent(AOP_YEAR)}&mode=${encodeURIComponent(mode)}`
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
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error importing Spyro Input Min Max Excel:', e)
    return Promise.reject(e)
  }
}