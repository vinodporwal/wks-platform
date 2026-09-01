/**
 * Filters out unit dropdown options that are already selected in other rows.
 *
 * @param {Object} dataItem - Current row object
 * @param {Array} unitDropdown - Complete list of unit dropdown options
 * @param {Array} rows - Current table rows state
 * @returns {Array} Filtered unit options for the current row
 */
export const getUnitOptions = (dataItem, unitDropdown = [], rows = []) => {
     const currentUnit = String(dataItem?.unit || dataItem?.Unit || '').trim().toLowerCase()
     const currentUnitId = String(dataItem?.unitId || dataItem?.id || '').trim().toLowerCase()
     const currentRowId = String(dataItem?.id || '')

     const selectedUnitsInOtherRows = new Set()
     rows.forEach((r) => {
          const rRowId = String(r.id || '')
          const rUnit = String(r.unit || r.Unit || '').trim().toLowerCase()
          const rUnitId = String(r.unitId || r.id || '').trim().toLowerCase()

          if (rRowId !== currentRowId) {
               if (rUnit) selectedUnitsInOtherRows.add(rUnit)
               if (rUnitId) selectedUnitsInOtherRows.add(rUnitId)
          }
     })

     return unitDropdown.filter((opt) => {
          const optLabel = String(opt.label || opt.value || '').trim().toLowerCase()
          const optVal = String(opt.value || opt.id || opt.unitId || '').trim().toLowerCase()

          if (currentUnit && (optLabel === currentUnit || optVal === currentUnit)) return true
          if (currentUnitId && (optLabel === currentUnitId || optVal === currentUnitId)) return true

          return !selectedUnitsInOtherRows.has(optLabel) && !selectedUnitsInOtherRows.has(optVal)
     })
}
