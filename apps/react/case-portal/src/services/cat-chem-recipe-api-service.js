import Config from '../consts'
import { json } from './request'

export const CatChemRecipeApiService = {
  getCatChemRecipeData,
  saveCatChemRecipeData,
  exportCatChemRecipeExcel,
  importCatChemRecipeExcel,
}

async function getCatChemRecipeData(keycloak, plantId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/makeup-batch-recipe-data?plantId=${plantId}&aopYear=${aopYear}`
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

async function saveCatChemRecipeData(keycloak, plantId, aopYear, payload) {
  const url = `${Config.CaseEngineUrl}/task/makeup-batch-recipe-data?plantId=${plantId}&aopYear=${aopYear}`
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

async function exportCatChemRecipeExcel(keycloak, plantId, aopYear, fileName) {
  const url = `${Config.CaseEngineUrl}/task/makeup-batch-recipe-export?plantId=${plantId}&aopYear=${aopYear}`
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

async function importCatChemRecipeExcel(file, keycloak, plantId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/makeup-batch-recipe-import?plantId=${plantId}&aopYear=${aopYear}`
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
