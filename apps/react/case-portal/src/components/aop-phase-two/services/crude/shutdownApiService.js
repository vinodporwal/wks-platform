import Config from 'consts/index'
import { json } from 'services/request'

export const ShutdownApiService = {
     getShutdownData,
     saveShutdownData,
     exportShutdownData,
     importShutdownData,
     getSitePlantDropdownData,
}

async function getShutdownData(keycloak, plantId, year) {
     let url = `${Config.CaseEngineUrl}/task/refinery-shutdown-data?plantId=${plantId}&aopYear=${year}`
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
async function saveShutdownData(
     payload,
     keycloak,
) {
     const url = `${Config.CaseEngineUrl}/task/refinery-shutdown-data`
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

async function exportShutdownData(keycloak, plantId, year, EXCEL_NAME) {
     const url = `${Config.CaseEngineUrl}/task/refinery-shutdown-data-export?aopYear=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
     const headers = {
          'Content-Type': 'application/json',
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
          a.download = EXCEL_NAME
          document.body.appendChild(a)
          a.click()
          a.remove()
          window.URL.revokeObjectURL(urlBlob)
     } catch (e) {
          console.log(e)
          return await Promise.reject(e)
     }
}

async function importShutdownData(file, keycloak, plantId, year) {
     const url = `${Config.CaseEngineUrl}/task/refinery-shutdown-data-import?aopYear=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
     const formData = new FormData()
     formData.append('file', file)
     const headers = {
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
          console.log(e)
          return await Promise.reject(e)
     }
}

async function getSitePlantDropdownData(keycloak, PLANT_ID) {
     let url = `${Config.CaseEngineUrl}/task/site-dropdown-data?plantId=${PLANT_ID}`
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