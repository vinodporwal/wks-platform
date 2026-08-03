import Config from 'consts/index'
import { json } from 'services/request'

export const JswBudgetSourceAPIService = {
     getJswBudgetSourceData,
     saveJswBudgetSourceData,
     getDropdownUnit,

}

async function getJswBudgetSourceData(keycloak, SITE_ID, year) {
     let url = `${Config.CaseEngineUrl}/task/profit-center-data?siteId=${SITE_ID}&aopYear=${year}`
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
async function saveJswBudgetSourceData(
     payload,
     keycloak,
     year,
) {
     const url = `${Config.CaseEngineUrl}/task/profit-center-data?&aopYear=${year}`
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
async function getDropdownUnit(keycloak, SITE_ID) {
     let url = `${Config.CaseEngineUrl}/task/profit-center-uom-dropdown?siteId=${SITE_ID}`
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