import Config from '../consts'
import { json } from './request'

export const CatChemRecipeChemGradeApiService = {
  getCatChemChemGradeData,
  saveCatChemChemGradeData,
  exportCatChemChemGradeExcel,
  importCatChemChemGradeExcel,
}

async function getCatChemChemGradeData(keycloak, plantId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/chem-grade-data?plantId=${plantId}&aopYear=${aopYear}`
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

async function saveCatChemChemGradeData(keycloak, plantId, aopYear, payload) {
  const url = `${Config.CaseEngineUrl}/task/chem-grade-data?plantId=${plantId}&aopYear=${aopYear}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: JSON.stringify(payload) })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function exportCatChemChemGradeExcel(keycloak, plantId, aopYear, fileName) {
  const url = `${Config.CaseEngineUrl}/task/chem-grade-export?plantId=${plantId}&aopYear=${aopYear}`
  const headers = {
    Accept: 'application/octet-stream',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) throw new Error('Export failed')
    const blob = await resp.blob()
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.setAttribute('download', `${fileName}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(link.href)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function importCatChemChemGradeExcel(file, keycloak, plantId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/chem-grade-import?plantId=${plantId}&aopYear=${aopYear}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: formData })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}
