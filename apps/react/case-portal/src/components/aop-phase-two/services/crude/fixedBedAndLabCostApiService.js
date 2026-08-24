import Config from 'consts/index'
import { json } from 'services/request'

export const FixedBedAndLabCostApiService = {
  getFixedBedAndLabCostData,
  getFixedBedCostCentersDropdown,
  saveFixedBedAndLabCostData,
  deleteFixedBedAndLabCostData,
  exportFixedBedAndLabCost,
}

/**
 * 1) GET API - SP_GetFixedBedAndLabCostData
 */
async function getFixedBedAndLabCostData(keycloak, year) {
  const url = `${Config.CaseEngineUrl}/task/fixed-bed-and-lab-cost?aopYear=${encodeURIComponent(year || '')}`
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

/**
 * 2) Dropdown API - Sp_GetFixedBedCostCentersDropdowns
 */
async function getFixedBedCostCentersDropdown(keycloak) {
  const url = `${Config.CaseEngineUrl}/task/fixed-bed-cost-centers-dropdown`
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

/**
 * 3) SAVE API - If MasterId available in txn tbl update else insert
 */
async function saveFixedBedAndLabCostData(payload, keycloak, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/fixed-bed-and-lab-cost?aopYear=${encodeURIComponent(aopYear || '')}`
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

/**
 * 4) DELETE API - Delete transaction data for a masterId and AOP year
 */
async function deleteFixedBedAndLabCostData(keycloak, masterId, year) {
  const url = `${Config.CaseEngineUrl}/task/fixed-bed-and-lab-cost?masterId=${encodeURIComponent(masterId || '')}&aopYear=${encodeURIComponent(year || '')}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'DELETE', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Export to Excel
 */
async function exportFixedBedAndLabCost(keycloak, year, excelName) {
  const url = `${Config.CaseEngineUrl}/task/fixed-bed-and-lab-cost-export?aopYear=${encodeURIComponent(year || '')}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const blob = await resp.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.setAttribute('download', `${excelName}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.parentNode.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
    return true
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}
