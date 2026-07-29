import Config from 'consts/index'
import { json } from 'services/request'

export const DocumentUploadApiService = {
    getOtherDocuments,
    uploadOrUpdateDocument,
    deleteDocument,
    getOtherDocumentInformation,
    saveOrUpdateOtherDocumentInformation,
}

async function getOtherDocuments(keycloak, verticalId, aopYear) {
    let url = `${Config.CaseEngineUrl}/task/other-documents?verticalId=${encodeURIComponent(verticalId)}&aopYear=${encodeURIComponent(aopYear)}`
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
        console.error('Error fetching document list:', e)
        return await Promise.reject(e)
    }
}

async function uploadOrUpdateDocument(keycloak, { transactionId, masterId, verticalId, aopYear, file }) {
    let url = `${Config.CaseEngineUrl}/task/other-documents?verticalId=${encodeURIComponent(verticalId)}&aopYear=${encodeURIComponent(aopYear)}`
    if (masterId) {
        url += `&masterId=${encodeURIComponent(masterId)}`
    }
    if (transactionId) {
        url += `&transactionId=${encodeURIComponent(transactionId)}`
    }
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
        if (!resp.ok) {
            let errorMsg = `HTTP error! Status: ${resp.status}`
            try {
                const errJson = await resp.json()
                if (errJson?.message) {
                    errorMsg = errJson.message
                }
            } catch (_) {
                // Ignore JSON parse error if response body is not JSON
            }
            if (resp.status === 500 && errorMsg.startsWith('HTTP error')) {
                errorMsg = 'Server Error (500): The uploaded file exceeds the maximum allowed server limit (5 MB).'
            }
            throw new Error(errorMsg)
        }
        return json(keycloak, resp)
    } catch (e) {
        console.error('Error uploading/updating document:', e)
        return await Promise.reject(e)
    }
}

async function deleteDocument(keycloak, transactionId) {
    const url = `${Config.CaseEngineUrl}/task/other-documents?transactionId=${encodeURIComponent(transactionId)}`
    const headers = {
        Accept: 'application/json',
        Authorization: `Bearer ${keycloak.token}`,
    }
    try {
        const resp = await fetch(url, {
            method: 'DELETE',
            headers,
        })
        if (!resp.ok) {
            throw new Error(`Failed to delete document: ${resp.status} ${resp.statusText}`)
        }
        return json(keycloak, resp)
    } catch (e) {
        console.error('Error deleting document:', e)
        return await Promise.reject(e)
    }
}

async function getOtherDocumentInformation(keycloak, verticalId, aopYear) {
    let url = `${Config.CaseEngineUrl}/task/other-document-information?verticalId=${encodeURIComponent(verticalId)}&aopYear=${encodeURIComponent(aopYear)}`
    const headers = {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak?.token}`,
    }
    try {
        const resp = await fetch(url, { method: 'GET', headers })
        if (!resp.ok) {
            throw new Error(`HTTP error! Status: ${resp.status}`)
        }
        return json(keycloak, resp)
    } catch (e) {
        console.error('Error fetching other document information:', e)
        return await Promise.reject(e)
    }
}

async function saveOrUpdateOtherDocumentInformation(keycloak, verticalId, aopYear, informationList) {
    let url = `${Config.CaseEngineUrl}/task/other-document-information?verticalId=${encodeURIComponent(verticalId)}&aopYear=${encodeURIComponent(aopYear)}`
    const headers = {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${keycloak?.token}`,
    }
    try {
        const resp = await fetch(url, {
            method: 'POST',
            headers,
            body: JSON.stringify(informationList),
        })
        if (!resp.ok) {
            throw new Error(`HTTP error! Status: ${resp.status}`)
        }
        return json(keycloak, resp)
    } catch (e) {
        console.error('Error saving other document information:', e)
        return await Promise.reject(e)
    }
}
