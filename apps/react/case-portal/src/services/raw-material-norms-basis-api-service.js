import Config from '../consts'
import { json } from './request'
export const RawMaterialNormsBasisApiService = {
  getData,
  postData,
  importExcel,
  exportExcel,

  getRawMaterialData,
  postRawMaterialData,
  importRawMaterialExcel,
  exportRawMaterialExcel,
  getNormsConfigurationData,
  handleCalculateNormsConfiguration,
}

async function getData(keycloak, PLANT_ID, AOP_YEAR, type) {
  const url = `${Config.CaseEngineUrl}/task/production-norms-manual-entry?year=${AOP_YEAR}&plantFKId=${PLANT_ID}&type=${type}`
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
async function postData(keycloak, payload, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/production-norms?year=${AOP_YEAR}&plantFKId=${PLANT_ID}`
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
    if (!resp.ok) {
      throw new Error(`Failed to save data: ${resp.status} ${resp.statusText}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error saving production norms data:', e)
    return Promise.reject(e)
  }
}

async function importExcel(file, keycloak, PLANT_ID, AOP_YEAR, type) {
  const url = `${Config.CaseEngineUrl}/task/production-norms-manual-entry-import?plantId=${PLANT_ID}&year=${AOP_YEAR}&type=${type}`
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
    console.error('Error importing Losses Excel:', e)
    return await Promise.reject(e)
  }
}
async function exportExcel(keycloak, PLANT_ID, AOP_YEAR, title, type) {
  const url = `${Config.CaseEngineUrl}/task/production-norms-manual-entry-export?year=${encodeURIComponent(AOP_YEAR)}&plantId=${encodeURIComponent(PLANT_ID)}&type=${type}`

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
    a.download = `${title}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Losses Excel:', e)
    return Promise.reject(e)
  }
}

// Handlers for stoichiometry and rawmaterial types (IIR/CIIR/BIIR columns)
async function getRawMaterialData(keycloak, PLANT_ID, AOP_YEAR, type) {
  const url = `${Config.CaseEngineUrl}/task/grade-wise-norm-configuration?year=${AOP_YEAR}&plantId=${PLANT_ID}&type=${type}`
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

async function postRawMaterialData(
  keycloak,
  payload,
  PLANT_ID,
  AOP_YEAR,
  type,
) {
  const url = `${Config.CaseEngineUrl}/task/grade-wise-norm-configuration?year=${AOP_YEAR}&plantId=${PLANT_ID}&type=${type}`
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
    if (!resp.ok) {
      throw new Error(`Failed to save data: ${resp.status} ${resp.statusText}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error saving production norms columns data:', e)
    return Promise.reject(e)
  }
}

async function importRawMaterialExcel(
  file,
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  type,
) {
  const url = `${Config.CaseEngineUrl}/task/production-norms-columns/import?plantFKId=${PLANT_ID}&year=${AOP_YEAR}&type=${type}`
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
    console.error('Error importing Columns Excel:', e)
    return await Promise.reject(e)
  }
}

async function exportRawMaterialExcel(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  title,
  type,
) {
  const url = `${Config.CaseEngineUrl}/task/production-norms-columns/export?year=${encodeURIComponent(AOP_YEAR)}&plantFKId=${encodeURIComponent(PLANT_ID)}&type=${type}`

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
    a.download = `${title}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Columns Excel:', e)
    return Promise.reject(e)
  }
}
async function getNormsConfigurationData(keycloak, PLANT_ID, AOP_YEAR, type) {
  const url = `${Config.CaseEngineUrl}/task/norm-configuration?year=${AOP_YEAR}&plantId=${PLANT_ID}&type=${type}`
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
async function handleCalculateNormsConfiguration(plantId, year, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/calculate-norm-configuration?plantId=${plantId}&year=${year}`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const data = await resp.json() // Parse JSON response
    return data
  } catch (e) {
    console.error('Error fetching calculation data:', e)
    return Promise.reject(e)
  }
}
