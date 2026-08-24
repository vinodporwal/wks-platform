/**
 * Helpers for Fixed Bed & Lab Cost Screen & Dropdown
 */

/**
 * Format raw dropdown data from API into standard options
 * @param {Array} rawList
 * @returns {Array} Formatted options [{ label, value, masterId, costCenterCode, costCenterDescription, displayLabel }]
 */
export const formatCostCenterDropdownOptions = (rawList = []) => {
  if (!Array.isArray(rawList)) return []

  return rawList.map((item) => {
    const id = item.masterId || item.value || item.id || ''
    const desc = item.costCenterDescription || item.label || ''
    const code = item.costCenterCode || item.costCenter || ''
    const displayLabel = item.displayLabel || (desc ? (code ? `${code} - ${desc}` : desc) : code)

    return {
      value: id,
      masterId: id,
      label: displayLabel,
      displayLabel: displayLabel,
      costCenterCode: code,
      costCenterDescription: desc,
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
  const otherRows = (existingRows || []).filter((r) => r.id !== dataItem?.id)

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
  })

  return costCenterOptions.filter((opt) => {
    const valKey = String(opt.value || opt.masterId || '').trim().toLowerCase()
    const descKey = String(opt.costCenterDescription || opt.label || '').trim().toLowerCase()
    const dispKey = String(opt.displayLabel || '').trim().toLowerCase()

    // If current row already has this item selected, keep it visible in its own dropdown
    const isCurrentRowSelection =
      (dataItem?.masterId && String(dataItem.masterId).trim().toLowerCase() === valKey) ||
      (dataItem?.costCenterDescription && String(dataItem.costCenterDescription).trim().toLowerCase() === descKey)

    if (isCurrentRowSelection) {
      return true
    }

    // Exclude if already chosen in any other row
    return (
      !usedIdentifiers.has(valKey) &&
      !usedIdentifiers.has(descKey) &&
      !usedIdentifiers.has(dispKey)
    )
  })
}

/**
 * Helper to find cost center description from MasterId
 * @param {string} masterId
 * @param {Array} costCenterOptions
 * @returns {string}
 */
export const getCostCenterDescriptionById = (masterId, costCenterOptions = []) => {
  if (!masterId || !Array.isArray(costCenterOptions)) return ''
  const searchKey = String(masterId).trim().toLowerCase()
  const match = costCenterOptions.find(
    (opt) =>
      String(opt.value || '').trim().toLowerCase() === searchKey ||
      String(opt.masterId || '').trim().toLowerCase() === searchKey
  )
  return match?.costCenterDescription || match?.label || masterId
}

/**
 * Helper to find MasterId from cost center description, label, or ID
 * @param {string} descriptionOrId
 * @param {Array} costCenterOptions
 * @returns {string}
 */
export const getMasterIdByDescription = (descriptionOrId, costCenterOptions = []) => {
  if (!descriptionOrId || !Array.isArray(costCenterOptions)) return ''
  const searchKey = String(descriptionOrId).trim().toLowerCase()
  const match = costCenterOptions.find(
    (opt) =>
      String(opt.value || '').trim().toLowerCase() === searchKey ||
      String(opt.masterId || '').trim().toLowerCase() === searchKey ||
      String(opt.costCenterDescription || '').trim().toLowerCase() === searchKey ||
      String(opt.label || '').trim().toLowerCase() === searchKey ||
      String(opt.displayLabel || '').trim().toLowerCase() === searchKey
  )
  return match?.masterId || match?.value || descriptionOrId
}
