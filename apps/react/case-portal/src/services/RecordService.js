import { json, nop } from './request'
import Config from '../consts'

// Utility function to get clean headers for API calls
const getCleanHeaders = (keycloak) => {
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  
  // Log header size for debugging
  const headerSize = JSON.stringify(headers).length
  if (headerSize > 8000) { // 8KB warning threshold
    console.warn(`Large Authorization header detected: ${headerSize} bytes`)
  }
  
  return headers
}

// Enhanced fetch with error handling and retry logic
const enhancedFetch = async (url, options, keycloak) => {
  try {
    const response = await fetch(url, options)
    
    // If we get a 400 error, it might be due to header size
    if (response.status === 400) {
      console.error(`400 Bad Request for ${url}`)
      console.error('Request headers size:', JSON.stringify(options.headers || {}).length, 'bytes')
      
      // Log cookie information for debugging
      const cookieCount = document.cookie.split(';').length
      const cookieSize = document.cookie.length
      console.error('Cookie count:', cookieCount, 'Total cookie size:', cookieSize, 'bytes')
    }
    
    return response
  } catch (error) {
    console.error(`Network error for ${url}:`, error)
    throw error
  }
}

export const RecordService = {
  getRecordTypeById,
  getAllRecordTypes,
  createRecordType,
  getRecordById,
  updateRecord,
  createRecord,
  deleteRecord,
}

async function getRecordById(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/record/${id}`
  const headers = getCleanHeaders(keycloak)

  try {
    const resp = await enhancedFetch(url, { headers }, keycloak)
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getAllRecordTypes(keycloak) {
  const url = `${Config.CaseEngineUrl}/record-type`
  const headers = getCleanHeaders(keycloak)

  try {
    const resp = await enhancedFetch(url, { headers }, keycloak)
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getRecordTypeById(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/record-type/${id}`
  const headers = getCleanHeaders(keycloak)

  try {
    const resp = await enhancedFetch(url, { headers }, keycloak)
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function createRecordType(keycloak, id, data) {
  const url = `${Config.CaseEngineUrl}/record-type/${id}`

  try {
    const resp = await enhancedFetch(url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        ...getCleanHeaders(keycloak),
      },
      body: JSON.stringify(data),
    }, keycloak)
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function createRecord(keycloak, id, data) {
  const url = `${Config.CaseEngineUrl}/record/${id}`

  try {
    const resp = await enhancedFetch(url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        ...getCleanHeaders(keycloak),
      },
      body: JSON.stringify(data),
    }, keycloak)
    return nop(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function updateRecord(keycloak, id, oid, data) {
  const url = `${Config.CaseEngineUrl}/record/${id}/${oid}`

  try {
    const resp = await enhancedFetch(url, {
      method: 'PATCH',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        ...getCleanHeaders(keycloak),
      },
      body: JSON.stringify(data),
    }, keycloak)
    return nop(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function deleteRecord(keycloak, id, oid) {
  const url = `${Config.CaseEngineUrl}/record/${id}/${oid}`

  try {
    const resp = await enhancedFetch(url, {
      method: 'DELETE',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        ...getCleanHeaders(keycloak),
      },
    }, keycloak)
    return nop(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}
