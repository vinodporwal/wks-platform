import Config from 'consts/index'
import { json } from 'services/request'

export const PlantsCapacitiesApiService = {
     getPlantsCapacitiesData,
     savePlantsCapacitiesData,
}

async function getPlantsCapacitiesData(keycloak, plantId, year) {
     let url = `${Config.CaseEngineUrl}/task/plant-capacities-transcation?plantId=${plantId}&aopYear=${year}`
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
async function savePlantsCapacitiesData(
     PLANT_ID,
     payload,
     keycloak,
     AOP_YEAR,
) {
     const url = `${Config.CaseEngineUrl}/task/plant-capacities-transcation`
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
