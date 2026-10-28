import Config from '../consts'
import { json } from './request'

export const SpyroInputValueApiService = {
  importSpyroInputExcelValue,
  exportSpyroInputExcelValue,
}

async function importSpyroInputExcelValue(file, keycloak, mode, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/spyro-input-import-excel-value?plantId=${PLANT_ID}&year=${AOP_YEAR}&mode=${encodeURIComponent(mode)}`
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
    console.error('Error importing Optimizer Input Excel V2:', e)
    return await Promise.reject(e)
  }
}

async function exportSpyroInputExcelValue(
  keycloak,
  mode,
  PLANT_ID,
  AOP_YEAR,
  EXCEL_NAME,
) {
  const url = `${Config.CaseEngineUrl}/task/spyro-input-export-excel-value?year=${encodeURIComponent(AOP_YEAR)}&plantId=${encodeURIComponent(PLANT_ID)}&mode=${encodeURIComponent(mode)}`

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
    a.download = `${EXCEL_NAME}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Optimizer Input Excel V2:', e)
    return Promise.reject(e)
  }
}
