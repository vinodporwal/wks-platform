import { ConsumptionNormsApiService } from 'services/consumption-norms-api-service'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'

/**
 * Utility functions for Material Grouped Selection Pop-Up operations and APIs
 */

/**
 * Checks if the Material Grouped Selection Pop-up is required for the given plant.
 * @param {Object} keycloak - Keycloak session object
 * @param {string|number} plantId - Plant ID
 * @returns {Promise<boolean>} - Resolves to true if popup is required, false otherwise
 */
export const checkMaterialGroupedSelectionRequired = async (keycloak, plantId) => {
  if (!plantId || !keycloak) {
    return false
  }
  try {
    const response =
      await ConsumptionNormsApiService.checkMaterialGroupedSelectionPopup(
        keycloak,
        plantId,
      )
    if (
      response &&
      (response.data === true ||
        response.data === 1 ||
        response.data === 'true')
    ) {
      return true
    }
    return false
  } catch (error) {
    console.error('Error checking grouped selection popup status:', error)
    return false
  }
}

/**
 * Fetches grouped selection data for the popup grid.
 * @param {Object} keycloak - Keycloak session object
 * @param {string|number} plantId - Plant ID
 * @param {string} aopYear - AOP Year
 * @returns {Promise<any>}
 */
export const fetchGroupedSelectionData = async (keycloak, plantId, aopYear) => {
  if (!plantId || !aopYear || !keycloak) {
    return null
  }
  try {
    return await PlantAopReportApiService.getGroupedSelection(
      keycloak,
      plantId,
      aopYear,
    )
  } catch (error) {
    console.error('Error fetching grouped selection popup data:', error)
    throw error
  }
}

/**
 * Saves grouped selection data submitted from the popup.
 * @param {Object} keycloak - Keycloak session object
 * @param {Array|Object} payload - Data payload to save
 * @returns {Promise<any>}
 */
export const saveGroupedSelectionData = async (keycloak, payload) => {
  if (!keycloak) {
    return null
  }
  try {
    return await PlantAopReportApiService.saveGroupedSelection(keycloak, payload)
  } catch (error) {
    console.error('Error saving grouped selection popup data:', error)
    throw error
  }
}

export const MaterialGroupedSelectionPopupUtils = {
  checkMaterialGroupedSelectionRequired,
  fetchGroupedSelectionData,
  saveGroupedSelectionData,
}

export default MaterialGroupedSelectionPopupUtils
