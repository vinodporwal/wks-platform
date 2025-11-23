import { json } from './request'
import Config from 'consts/index'

export const ProcessDefService = {
  start,
  find,
  getBPMNXml,
  getTaskByTaskId,
  completeTask,
  processExistsForBusinessKey,
  isTaskActive,
  taskExists,
  completeTaskWithbusinessKey,
}

// async function start(keycloak, procDefKey, businessKey) {
//   const url = `${Config.CaseEngineUrl}/process-definition/key/${procDefKey}/start`

//   try {
//     const resp = await fetch(url, {
//       method: 'POST',
//       headers: {
//         Accept: 'application/json',
//         'Content-Type': 'application/json',
//         Authorization: `Bearer ${keycloak.token}`,
//       },
//       body: JSON.stringify({ businessKey: businessKey }),
//     })
//     return json(keycloak, resp)
//   } catch (err) {
//     console.log(err)
//     return await Promise.reject(err)
//   }
// }


async function start(keycloak, procDefKey, businessKey) {
  const url = `${Config.CaseEngineUrl}/process-definition/key/${procDefKey}/start`

  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
      },
      body: JSON.stringify({ businessKey }),
    })

    if (!resp.ok) {
      // Parse error body and reject so .catch() runs
      const errorBody = await resp.json().catch(() => ({}))
      return Promise.reject({
        status: resp.status,
        message: resp.statusText,
        body: errorBody
      })
    }

    return json(keycloak, resp) // success case
  } catch (err) {
    return Promise.reject(err) // network errors
  }
}


async function find(keycloak) {
  if (keycloak.isTokenExpired()) {
    keycloak.logout({ redirectUri: window.location.origin })
  }

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }

  var url = `${Config.CaseEngineUrl}/process-definition`

  try {
    const resp = await fetch(url, { headers })
    return json(keycloak, resp)
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}

async function getBPMNXml(keycloak, processDefId) {
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }

  var url = `${Config.CaseEngineUrl}/process-definition/${processDefId}/xml`

  try {
    const resp = await fetch(url, { headers })
    return json(keycloak, resp.text())
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}

async function getTaskByTaskId(keycloak, taskId) { 

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
}
  var url = `${Config.CaseEngineUrl}/process-definition/task/${taskId}`
  try {
    const resp = await fetch(url, { headers })
    return json(keycloak, resp)
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}

async function completeTask(keycloak, taskId, body) {
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
    Accept: 'application/json',
    'Content-Type': 'application/json'
  }
  var url = `${Config.CaseEngineUrl}/process-definition/complete-task/${taskId}`
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: JSON.stringify(body) })
  //  return json(keycloak, resp)

     if (!resp.ok) throw new Error("Request failed");
     return;
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}

async function processExistsForBusinessKey(keycloak, businessKey) {
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  var url = `${Config.CaseEngineUrl}/process-definition/exists-for-business-key/${businessKey}` 
  try {
    const resp = await fetch(url, { headers })
    return json(keycloak, resp)
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}

async function isTaskActive(keycloak, businessKey, taskDefinitionKey) { 
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  var url = `${Config.CaseEngineUrl}/process-definition/is-task-active/${businessKey}/${taskDefinitionKey}`
  try {
    const resp = await fetch(url, { headers })
    //return json(keycloak, resp)
    const data = await resp.json();
    return data;
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}

async function taskExists(keycloak, taskId) {
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  var url = `${Config.CaseEngineUrl}/process-definition/task-exists/${taskId}`
  try {
    const resp = await fetch(url, { headers })
    //return json(keycloak, resp)
    const data = await resp.json();
    return data;
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}

async function completeTaskWithbusinessKey(keycloak, businessKey, taskDefKey, body) {
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
     Accept: 'application/json',
    'Content-Type': 'application/json'
  }
  var url = `${Config.CaseEngineUrl}/process-definition/complete-task-with-business-key/${businessKey}/${taskDefKey}`
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: JSON.stringify(body) })
  //   const resp = await fetch(url, { method: 'POST', headers })
 //   return json(keycloak, resp)

 if (!resp.ok) throw new Error("Request failed");
 return;
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}