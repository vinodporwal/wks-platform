import Config from '../consts'
import { json } from './request'

export const OptimizerOutputApiService = {
  importExcelWithPilotFurnace,
  exportSpyroOutputReportWithPilotFurnace,
}

async function importExcelWithPilotFurnace(
  file,
  keycloak,
  mode,
  PLANT_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/optimizer-output-import?plantId=${encodeURIComponent(PLANT_ID)}&year=${encodeURIComponent(AOP_YEAR)}&mode=${encodeURIComponent(mode)}`
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
    console.error('Error importing Optimizer Output with Pilot Furnace Excel:', e)
    return await Promise.reject(e)
  }
}

async function exportSpyroOutputReportWithPilotFurnace(
  keycloak,
  mode,
  PLANT_ID,
  AOP_YEAR,
  ExcelName,
) {
  const url = `${Config.CaseEngineUrl}/task/optimizer-output-export?year=${encodeURIComponent(AOP_YEAR)}&plantId=${encodeURIComponent(PLANT_ID)}&mode=${encodeURIComponent(mode)}`

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
    a.download = `${ExcelName}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Optimizer Output with Pilot Furnace Excel:', e)
    return Promise.reject(e)
  }
}
