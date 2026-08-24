/**
 * Filters out unit dropdown options that are already selected in other rows.
 *
 * @param {Object} dataItem - Current row object
 * @param {Array} unitDropdown - Complete list of unit dropdown options
 * @param {Array} rows - Current table rows state
 * @returns {Array} Filtered unit options for the current row
 */
export const getUnitOptions = (dataItem, unitDropdown = [], rows = []) => {
     if (!Array.isArray(unitDropdown)) return []

     const currentUnit = String(dataItem?.unit || dataItem?.Unit || '').trim().toLowerCase()
     const currentRowId = String(dataItem?.id || '')

     const selectedUnitsInOtherRows = new Set()
     const rowList = Array.isArray(rows) ? rows : []
     rowList.forEach((r) => {
          const rRowId = String(r.id || '')
          const rUnit = String(r.unit || r.Unit || '').trim().toLowerCase()

          if (rRowId !== currentRowId && rUnit) {
               selectedUnitsInOtherRows.add(rUnit)
          }
     })

     return unitDropdown.filter((opt) => {
          const optLabel = String(opt.label || opt.value || '').trim().toLowerCase()
          const optVal = String(opt.value || opt.unit || '').trim().toLowerCase()

          if (currentUnit && (optLabel === currentUnit || optVal === currentUnit)) return true

          return !selectedUnitsInOtherRows.has(optLabel) && !selectedUnitsInOtherRows.has(optVal)
     })
}
