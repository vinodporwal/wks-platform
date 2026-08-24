/**
 * Helpers for Fixed Bed & Lab Cost Screen & Dropdown
 */

/**
 * Format raw dropdown data from API into standard options
 * Uses displayLabel (e.g. "10L_1010061 - JM Club Township") for both value & label.
 * @param {Array} rawList
 * @returns {Array} Formatted options
 */
export const formatCostCenterDropdownOptions = (rawList = []) => {
  if (!Array.isArray(rawList)) return []

  return rawList.map((item) => {
    const id = item.masterId || item.id || item.value || ''
    const rawDesc = item.costCenterDescription || item.label || ''
    const code = item.costCenterCode || item.costCenter || ''
    const displayLabel = item.displayLabel || (rawDesc ? (code ? `${code} - ${rawDesc}` : rawDesc) : code)

    return {
      value: displayLabel,
      label: displayLabel,
      displayLabel: displayLabel,
      id: id,
      masterId: id,
      costCenterCode: code,
      costCenterDescription: displayLabel,
      rawDescription: rawDesc,
    }
  })
}

/**
 * Helper to get available options for Kendo Dropdown cell
 * Filters out all Cost Centers already chosen in other rows so duplicates cannot be selected.
 * @param {Object} dataItem current row data
 * @param {Array} costCenterOptions full dropdown list
 * @param {Array} existingRows all current rows in the table
 * @returns {Array} Available options excluding duplicates
 */
export const getCostCenterOptions = (dataItem, costCenterOptions = [], existingRows = []) => {
  if (!Array.isArray(costCenterOptions)) return []

  // Other rows excluding current active row
  const currentRowId = String(dataItem?.id || '')
  const otherRows = (existingRows || []).filter((r) => String(r.id || '') !== currentRowId)

  const usedIdentifiers = new Set()

  otherRows.forEach((r) => {
    if (r.masterId) {
      usedIdentifiers.add(String(r.masterId).trim().toLowerCase())
    }
    if (r.id && !String(r.id).startsWith('new_row_')) {
      usedIdentifiers.add(String(r.id).trim().toLowerCase())
    }
    if (r.costCenterDescription) {
      usedIdentifiers.add(String(r.costCenterDescription).trim().toLowerCase())
    }
    if (r.displayLabel) {
      usedIdentifiers.add(String(r.displayLabel).trim().toLowerCase())
    }
  })

  const currentMasterId = String(dataItem?.masterId || '').trim().toLowerCase()
  const currentCostCenterDesc = String(dataItem?.costCenterDescription || dataItem?.displayLabel || '').trim().toLowerCase()

  return costCenterOptions.filter((opt) => {
    const masterIdKey = String(opt.masterId || opt.id || '').trim().toLowerCase()
    const descKey = String(opt.rawDescription || '').trim().toLowerCase()
    const dispKey = String(opt.displayLabel || opt.label || opt.value || '').trim().toLowerCase()

    // If current row already has this item selected, keep it visible in its own dropdown
    const isCurrentRowSelection =
      (currentMasterId && currentMasterId === masterIdKey) ||
      (currentCostCenterDesc && (currentCostCenterDesc === dispKey || (descKey && currentCostCenterDesc === descKey)))

    if (isCurrentRowSelection) {
      return true
    }

    // Exclude if already chosen in any other row
    return (
      (!masterIdKey || !usedIdentifiers.has(masterIdKey)) &&
      (!dispKey || !usedIdentifiers.has(dispKey)) &&
      (!descKey || !usedIdentifiers.has(descKey))
    )
  })
}

/**
 * Helper to find cost center displayLabel from MasterId
 * @param {string} masterId
 * @param {Array} costCenterOptions
 * @returns {string}
 */
export const getCostCenterDescriptionById = (masterId, costCenterOptions = []) => {
  if (!masterId || !Array.isArray(costCenterOptions)) return ''
  const searchKey = String(masterId).trim().toLowerCase()
  const match = costCenterOptions.find(
    (opt) =>
      String(opt.masterId || '').trim().toLowerCase() === searchKey ||
      String(opt.id || '').trim().toLowerCase() === searchKey ||
      String(opt.value || '').trim().toLowerCase() === searchKey
  )
  return match?.displayLabel || match?.label || match?.costCenterDescription || masterId
}

/**
 * Helper to find MasterId from cost center displayLabel, description, label, or ID
 * @param {string} descriptionOrId
 * @param {Array} costCenterOptions
 * @returns {string}
 */
export const getMasterIdByDescription = (descriptionOrId, costCenterOptions = []) => {
  if (!descriptionOrId || !Array.isArray(costCenterOptions)) return ''
  const searchKey = String(descriptionOrId).trim().toLowerCase()
  const match = costCenterOptions.find(
    (opt) =>
      String(opt.displayLabel || '').trim().toLowerCase() === searchKey ||
      String(opt.label || '').trim().toLowerCase() === searchKey ||
      String(opt.value || '').trim().toLowerCase() === searchKey ||
      String(opt.masterId || '').trim().toLowerCase() === searchKey ||
      String(opt.id || '').trim().toLowerCase() === searchKey ||
      String(opt.costCenterDescription || '').trim().toLowerCase() === searchKey ||
      String(opt.rawDescription || '').trim().toLowerCase() === searchKey ||
      String(opt.costCenterCode || '').trim().toLowerCase() === searchKey
  )
  return match?.masterId || match?.id || match?.value || descriptionOrId
}
