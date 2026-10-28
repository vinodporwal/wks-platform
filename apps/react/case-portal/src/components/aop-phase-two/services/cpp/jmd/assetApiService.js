import Config from 'consts/index'
import { json } from 'services/request'

export const AssetApiService = {
  // Used in: Inputs/components/AddAssetDialog.js (ShutdownAndOperational)
  addAsset,
  updateAsset,
  deleteAsset,
}
// ===================== || Add / Update / Delete Asset APIs || ===================== //
// POST /task/jmd/assets/add
// Adds a new asset (Power or Steam) for operational hours
async function addAsset(keycloak, assetData) {
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/add`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(assetData)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error adding asset:', e)
    return await Promise.reject(e)
  }
}

// PUT /task/jmd/assets/update/{assetId}
// Updates an existing asset
async function updateAsset(keycloak, assetId, updateData) {
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/update/${assetId}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(updateData)
  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error updating asset:', e)
    return await Promise.reject(e)
  }
}

// DELETE /task/jmd/assets/delete/{assetId}
// Deletes an asset
async function deleteAsset(keycloak, assetId) {
  const url = `${Config.CaseEngineUrl}/task/jmd/assets/delete/${assetId}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'DELETE',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const text = await resp.text()
    return text ? JSON.parse(text) : { success: true }
  } catch (e) {
    console.error('Error deleting asset:', e)
    return await Promise.reject(e)
  }
}
