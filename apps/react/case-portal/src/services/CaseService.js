import Config from '../consts'
import i18n from '../i18n'
import { json, nop } from './request'
import moment from 'moment'

export const CaseService = {
  getAllByStatus,
  getCaseDefinitions,
  getCaseDefinitionsById,
  getCaseById,
  getCaseByBusinessKey,
  getSingleCaseByBusinessKey,
  filterCase,
  filterCaseByAssetName,
  createCase,
  createdSaveCase,
  patch,
  addDocuments,
  addComment,
  updateComment,
  deleteComment,
  getFaultCategories,
  getCaseStatus,
  saveCase,
  getCasesById,
  saveRecommendation,
  saveAnalysis,
  updateEventIds
}

async function getAllByStatus(keycloak, status, limit) {
  if (!status) {
    return Promise.resolve([])
  }

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }

  var url = `${Config.CaseEngineUrl}/case?status=${status}&limit=${limit}`

  try {
    const resp = await fetch(url, { headers })
    const data = await json(keycloak, resp)
    return mapperToCase(data)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getCaseStatus(keycloak) {
  const url = `${Config.CaseEngineUrl}/case-definition/case-status`;

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
    Accept: 'application/json',
  };

  try {
    const resp = await fetch(url, { headers });
    const data = await json(keycloak, resp);  
    return data;
  } catch (e) {
    console.log('Error fetching case status:', e);
    return await Promise.reject(e);
  }
}


async function getFaultCategories(keycloak) {
  const url = `${Config.CaseEngineUrl}/case-definition/fault-category`;

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
    Accept: 'application/json',
  };

  try {
    const resp = await fetch(url, { headers });
    const data = await json(keycloak, resp);
    return data;
  } catch (e) {
    console.log('Error fetching fault categories:', e);
    return await Promise.reject(e);
  }
}


async function getCaseDefinitions(keycloak) {
  const url = `${Config.CaseEngineUrl}/case-definition?deployed=true`

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, { headers })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getCaseDefinitionsById(keycloak, caseDefId) {
  const url = `${Config.CaseEngineUrl}/case-definition/${caseDefId || ''}`

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, { headers })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getCaseById(keycloak, id) {
  let url = `${Config.CaseEngineUrl}/case/${id}`

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, { headers })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}
async function getCasesById(keycloak, caseDefId = '', assetName = '', hierarchyName = '') {
  console.log('caseDefId', caseDefId)
  // Use '/cases' in the URL directly, not appending the caseDefId
  let url = `${Config.CaseEngineUrl}/case-definition/cases`;

  // Append query parameters if provided
  const queryParams = new URLSearchParams();
  if (assetName) queryParams.append('assetName', assetName);
  if (hierarchyName) queryParams.append('hierarchyName', hierarchyName);

  // Add query parameters to the URL if they exist
  if (queryParams.toString()) {
    url += `?${queryParams.toString()}`;
  }

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  };

  try {
    const resp = await fetch(url, { headers });
    return json(keycloak, resp);
  } catch (e) {
    console.error(e);
    return await Promise.reject(e);
  }
}



async function filterCase(keycloak, caseDefId, status, cursor) {
  let url = `${Config.CaseEngineUrl}/case?`
  url = url + (status ? `status=${status}` : '')
  url = url + (caseDefId ? `&caseDefinitionId=${caseDefId}` : '')
  url = url + `&before=${cursor.before || ''}`
  url = url + `&after=${cursor.after || ''}`
  url = url + `&sort=${cursor.sort || 'DESC'}`
  url = url + `&limit=${cursor.limit || 10}`

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }


  try {
    const resp = await fetch(url, { headers })
    const data = await json(keycloak, resp)
    return mapperToCase(data)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function filterCaseByAssetName(keycloak, caseDefId, status, cursor, assetName, eventIds) {
  // let url = `${Config.CaseEngineUrl}/case/asset-name?`
  // url = url + (assetName ? `assetName=${assetName}` : '')
  // url = url + (status ? `status=${status}` : '')
  // url = url + (caseDefId ? `&caseDefinitionId=${caseDefId}` : '')
  // url = url + (eventIds ? `&eventIds=${eventIds}` : '')
  // url = url + `&before=${cursor.before || ''}`
  // url = url + `&after=${cursor.after || ''}`
  // url = url + `&sort=${cursor.sort || 'DESC'}`
  // url = url + `&limit=${cursor.limit || 10}`

  console.log('filterCaseByAssetName eventIds: ', eventIds)
  const params = new URLSearchParams()

if (assetName) params.append('assetName', assetName)
if (status) params.append('status', status)
if (caseDefId) params.append('caseDefinitionId', caseDefId)
if (eventIds) params.append('eventIds', eventIds)

params.append('before', cursor.before || '')
params.append('after', cursor.after || '')
params.append('sort', cursor.sort || 'DESC')
params.append('limit', cursor.limit || 10)

const url = `${Config.CaseEngineUrl}/case-definition/cases-to-link?${params.toString()}`

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, { headers })
    const data = await json(keycloak, resp)
    return mapperToCase(data)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function updateEventIds(keycloak, businessKeys, eventIds, eventTrendUrls, eventReportUrls) {
  const url = `${Config.CaseEngineUrl}/case/update-event-ids`

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
    'Content-Type': 'application/json',
  }

  try {
    const body = { businessKeys, eventIds, eventTrendUrls, eventReportUrls }

    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    })

   // return json(keycloak, resp)

   if (resp.status === 204) {
    return null
  }

  return await resp.json()
  } catch (e) {
    console.log(e)
    return Promise.reject(e)
  }
}

async function patch(keycloak, id, body) {
  const url = `${Config.CaseEngineUrl}/case/${id}`

  try {
    const resp = await fetch(url, {
      method: 'PATCH',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/merge-patch+json',
        Authorization: `Bearer ${keycloak.token}`,
      },
      body: body,
    })
    return nop(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function createCase(keycloak, body) {
  const url = `${Config.CaseEngineUrl}/case`

  try {
   // const bodyWithDates = {...body, lastUpdated: moment().toISOString()}; 
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
      },
      //body: bodyWithDates,
      body: body,
    })
    return json(keycloak, resp)
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}
async function createdSaveCase(keycloak, body) {
  const url = `${Config.CaseEngineUrl}/case/save`

  try {
   // const bodyWithDates = {...body, lastUpdated: moment().toISOString()}; 
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
      },
      //body: bodyWithDates,
      body: body,
    })
    return json(keycloak, resp)
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}
async function saveCase(keycloak, body) {
  const url = `${Config.CaseEngineUrl}/case-definition/save-case`

  const hardcodedOwner = {
    id: '417136bb-d82a-4ae3-98b5-4bfc3ccbf07d',
    name: 'demo demo',
    email: 'demo@demo.com',
  }

  const parsedBody = typeof body === 'string' ? JSON.parse(body) : body
  const bodyWithOwner = JSON.stringify({ ...parsedBody, owner: hardcodedOwner })

  try {
    //const bodyWithDates = {...body, lastUpdated: moment().toISOString()}; 
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
      },
      body: bodyWithOwner,
    })
    return json(keycloak, resp)
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}


async function saveRecommendation(keycloak, body) {
  const url = `${Config.CaseEngineUrl}/case-definition/save-recommendation`;

  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
      },
      body: JSON.stringify(body),
    });
    return json(keycloak, resp)
  } catch (err) {
    console.error('Error in saveRecommendation API:', err);
    throw err;
  }
}

async function addDocuments(keycloak, businessKey, document) {
  const url = `${Config.CaseEngineUrl}/case/${businessKey}/document`

  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
      },
      body: JSON.stringify(document),
    })
    return nop(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function addComment(keycloak, text, parentId, businessKey) {
  const url = `${Config.CaseEngineUrl}/case/${businessKey}/comment`

  const comment = {
    body: text,
    parentId,
    userId: keycloak.tokenParsed.preferred_username,
    userName: keycloak.tokenParsed.given_name,
    caseId: businessKey,
  }

  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
      },
      body: JSON.stringify(comment),
    })
    return nop(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function updateComment(keycloak, text, commentId, businessKey) {
  const url = `${Config.CaseEngineUrl}/case/${businessKey}/comment/${commentId}`

  const comment = {
    id: commentId,
    body: text,
    userId: keycloak.tokenParsed.preferred_username,
    caseId: businessKey,
  }

  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
      },
      body: JSON.stringify(comment),
    })
    return nop(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function deleteComment(keycloak, commentId, businessKey) {
  const url = `${Config.CaseEngineUrl}/case/${businessKey}/comment/${commentId}`

  try {
    const resp = await fetch(url, {
      method: 'DELETE',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
      },
    })
    return nop(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

function mapperToCase(resp) {
  const { data, paging } = resp

  if (!data.length) {
    return Promise.resolve({ data: [], paging: {} })
  }

  const toStatus = (status) => {
    const mapper = {
      WIP_CASE_STATUS: i18n.t('general.case.status.wip'),
      CLOSED_CASE_STATUS: i18n.t('general.case.status.closed'),
      ARCHIVED_CASE_STATUS: i18n.t('general.case.status.archived'),
    }

    return mapper[status] || '-'
  }

  const toCase = data.map((element) => {
    const createdAt = element?.attributes?.find(
      (attribute) => attribute.name === 'createdAt',
    )
    element.createdAt = createdAt ? createdAt.value : ''
    element.statusDescription = toStatus(element.status)
    return element
  })

  const toPaging = {
    cursors: paging.cursors,
    hasPrevious: paging.hasPrevious,
    hasNext: paging.hasNext,
  }

  return Promise.resolve({ data: toCase, paging: toPaging })
}

async function saveAnalysis(keycloak, body) {
  const url = `${Config.CaseEngineUrl}/case-definition/analysis`

  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
      },
      body: body,
    })
    return json(keycloak, resp)
  } catch (err) {
    console.log(err)
    return await Promise.reject(err)
  }
}

// Fetch a single case by businessKey using the dedicated GET /case/{businessKey} endpoint
async function getCaseByBusinessKey(keycloak, caseDefId = '', businessKey) {
  if (!businessKey) {
    return Promise.resolve(null)
  }

  const url = `${Config.CaseEngineUrl}/case/${encodeURIComponent(businessKey)}`

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, { headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching case by businessKey:', e)
    return await Promise.reject(e)
  }
}

async function getSingleCaseByBusinessKey(keycloak, caseDefId = '', businessKey) {
 

  if (!businessKey) {
    return Promise.resolve({ data: [], paging: {} })
  }

  let url = `${Config.CaseEngineUrl}/case/${encodeURIComponent(businessKey)}`

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, { headers })
    const data = await json(keycloak, resp)

    console.log("API raw response:", data)

  //   return mapperToCase(data)
  // } catch (e) {
  //   console.error('Error fetching case by businessKey:', e)
  //   throw e
  // }

  return mapperToCase({
    data: data ? [data] : [],
    paging: {}
  })
} catch (e) {
    console.error('Error fetching case by businessKey:', e)
    throw e
  }
}