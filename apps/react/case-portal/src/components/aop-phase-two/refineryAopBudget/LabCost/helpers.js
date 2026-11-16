/**
 * Helpers for Lab Cost Screen & Dropdown
 */

/**
 * Format raw Cost Center dropdown data from API into standard options
 * @param {Array} rawList
 * @returns {Array} Formatted options
 */
export const formatCostCenterDropdownOptions = (rawList = []) => {
  if (!Array.isArray(rawList)) return []

  const formatted = []
  const seenKeys = new Set()

  rawList.forEach((item) => {
    const id = item.CostCenterMasterId || item.costCenterMasterId || item.id || ''
    const desc = item.CostCenterDescription || item.costCenterDescription || ''

    const key = String(desc || '').trim().toLowerCase()
    if (key && !seenKeys.has(key)) {
      seenKeys.add(key)
      formatted.push({
        label: desc,
        value: desc,
        costCenterMasterId: id,
        costCenterDescription: desc,
      })
    }
  })

  return formatted
}

/**
 * Helper to get available options for Cost Center Dropdown cell
 * Rule: Excludes Cost Centers that are already selected in another row.
 * @param {Object} dataItem current row data
 * @param {Array} costCenterOptions full dropdown list
 * @param {Array} existingRows all current rows in the table
 * @returns {Array} Available options
 */
export const getCostCenterOptions = (dataItem, costCenterOptions = [], existingRows = []) => {
  if (!Array.isArray(costCenterOptions)) return []

  const currentRowId = String(dataItem?.id || '')
  const currentCostCenter = String(dataItem?.costCenterDescription || '').trim().toLowerCase()

  const usedCostCenters = new Set()
  const rowList = Array.isArray(existingRows) ? existingRows : []

  rowList.forEach((r) => {
    if (String(r.id || '') === currentRowId) return
    const rCostCenter = String(r.costCenterDescription || '').trim().toLowerCase()
    if (rCostCenter) {
      usedCostCenters.add(rCostCenter)
    }
  })

  return costCenterOptions.filter((opt) => {
    const optLabel = String(opt.label || opt.value || opt.costCenterDescription || '').trim().toLowerCase()
    if (currentCostCenter && optLabel === currentCostCenter) return true
    return !usedCostCenters.has(optLabel)
  })
}
