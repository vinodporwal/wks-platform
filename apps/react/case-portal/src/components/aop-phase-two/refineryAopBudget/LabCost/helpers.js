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
 * Format raw Material dropdown data from API into standard options
 * @param {Array} rawList
 * @returns {Array} Formatted options
 */
export const formatMaterialDropdownOptions = (rawList = []) => {
  if (!Array.isArray(rawList)) return []

  const formatted = []
  const seenKeys = new Set()

  rawList.forEach((item) => {
    const id = item.MaterialMasterId || item.materialMasterId || item.id || ''
    const mat = item.Material || item.material || ''
    const uom = item.UOM || item.uom || ''

    const key = String(mat || '').trim().toLowerCase()
    if (key && !seenKeys.has(key)) {
      seenKeys.add(key)
      formatted.push({
        label: mat,
        value: mat,
        materialMasterId: id,
        material: mat,
        uom: uom,
      })
    }
  })

  return formatted
}

/**
 * Helper to get available options for Cost Center Dropdown cell
 * Rule: Excludes Cost Centers that already have the current row's Material in another row.
 * @param {Object} dataItem current row data
 * @param {Array} costCenterOptions full dropdown list
 * @param {Array} existingRows all current rows in the table
 * @returns {Array} Available options
 */
export const getCostCenterOptions = (dataItem, costCenterOptions = [], existingRows = []) => {
  if (!Array.isArray(costCenterOptions)) return []

  const currentRowId = String(dataItem?.id || '')
  const currentMaterial = String(dataItem?.material || '').trim().toLowerCase()
  const currentCostCenter = String(dataItem?.costCenterDescription || '').trim().toLowerCase()

  if (!currentMaterial) {
    return costCenterOptions
  }

  const usedCostCentersForThisMaterial = new Set()
  const rowList = Array.isArray(existingRows) ? existingRows : []

  rowList.forEach((r) => {
    if (String(r.id || '') === currentRowId) return
    const rCostCenter = String(r.costCenterDescription || '').trim().toLowerCase()
    const rMaterial = String(r.material || '').trim().toLowerCase()

    if (rCostCenter && rMaterial && rMaterial === currentMaterial) {
      usedCostCentersForThisMaterial.add(rCostCenter)
    }
  })

  return costCenterOptions.filter((opt) => {
    const optLabel = String(opt.label || opt.value || opt.costCenterDescription || '').trim().toLowerCase()
    if (currentCostCenter && optLabel === currentCostCenter) return true
    return !usedCostCentersForThisMaterial.has(optLabel)
  })
}

/**
 * Helper to get available options for Material Dropdown cell
 * Rule: Excludes Materials that are already paired with the current row's Cost Center in another row.
 * @param {Object} dataItem current row data
 * @param {Array} materialOptions full dropdown list
 * @param {Array} existingRows all current rows in the table
 * @returns {Array} Available options
 */
export const getMaterialOptions = (dataItem, materialOptions = [], existingRows = []) => {
  if (!Array.isArray(materialOptions)) return []

  const currentRowId = String(dataItem?.id || '')
  const currentCostCenter = String(dataItem?.costCenterDescription || '').trim().toLowerCase()
  const currentMaterial = String(dataItem?.material || '').trim().toLowerCase()

  if (!currentCostCenter) {
    return materialOptions
  }

  const usedMaterialsInThisCostCenter = new Set()
  const rowList = Array.isArray(existingRows) ? existingRows : []

  rowList.forEach((r) => {
    if (String(r.id || '') === currentRowId) return
    const rCostCenter = String(r.costCenterDescription || '').trim().toLowerCase()
    const rMaterial = String(r.material || '').trim().toLowerCase()

    if (rCostCenter && rMaterial && rCostCenter === currentCostCenter) {
      usedMaterialsInThisCostCenter.add(rMaterial)
    }
  })

  return materialOptions.filter((opt) => {
    const optLabel = String(opt.label || opt.value || opt.material || '').trim().toLowerCase()
    if (currentMaterial && optLabel === currentMaterial) return true
    return !usedMaterialsInThisCostCenter.has(optLabel)
  })
}

