/**
 * Helper functions and utilities for ThroughputNorms screen.
 */

/**
 * Formats unit dropdown API response data into dropdown option objects.
 */
export const formatUnitDropdownOptions = (data = []) => {
     const formattedOptions = []
     const seenUnitKeys = new Set()
     if (Array.isArray(data)) {
          data.forEach((item) => {
               const labelVal = item?.Unit || item?.unit || item?.name || item?.displayName || ''
               const idVal = item?.Id || item?.id || item?.unitId || item?.profitId || labelVal
               const key = String(labelVal).trim().toLowerCase()
               if (labelVal && !seenUnitKeys.has(key)) {
                    seenUnitKeys.add(key)
                    formattedOptions.push({
                         label: labelVal,
                         value: labelVal,
                         id: idVal,
                         unitId: idVal,
                         profitId: idVal,
                         uom: item?.UOM || item?.uom || '',
                    })
               }
          })
     }
     return formattedOptions
}

/**
 * Formats raw material response list into dropdown options.
 */
export const formatMaterialDropdownOptions = (rawList = []) => {
     if (!Array.isArray(rawList)) return []

     const formatted = []
     const seenMaterialKeys = new Set()
     rawList.forEach((item) => {
          const dName = item?.displayName || item?.DisplayName || item?.materialName || item?.MaterialName || item?.name || ''
          const mId = item?.materialId || item?.MaterialId || item?.id || item?.Id || dName
          const key = String(dName).trim().toLowerCase()
          if (dName && !seenMaterialKeys.has(key)) {
               seenMaterialKeys.add(key)
               formatted.push({
                    label: dName,
                    value: dName,
                    id: mId,
                    materialId: mId,
                    unitId: item?.unitId || item?.UnitId || item?.profitId || '',
                    unit: item?.unit || item?.Unit || '',
                    uom: item?.uom || item?.UOM || '',
               })
          }
     })
     return formatted
}

/**
 * Helper function to get available Material Code dropdown options for a given row.
 *
 * Rules:
 * - If the row has NO Unit selected yet: show all materials (no filtering by duplicate).
 * - If the row has a Unit selected: exclude any Material already used with that same Unit
 *   in another row (unless it is this row's own current selection).
 * - Blank new rows (no unit, no displayName) NEVER block each other's options.
 */
export const getMaterialOptions = (
     dataItem,
     materialOptionsList = [],
     materialDropdownMapRef = { current: {} },
     rows = [],
) => {
     let allOptions = []
     if (Array.isArray(materialOptionsList)) {
          allOptions = materialOptionsList
     } else if (typeof materialOptionsList === 'object' && materialOptionsList !== null) {
          allOptions = Object.values(materialOptionsList).flat()
     }

     if (allOptions.length === 0 && materialDropdownMapRef?.current) {
          allOptions = Object.values(materialDropdownMapRef.current).flat()
     }

     // Deduplicate options list by label
     const uniqueMap = new Map()
     allOptions.forEach((opt) => {
          const key = String(opt.label || opt.value || opt.displayName || '').trim().toLowerCase()
          if (key && !uniqueMap.has(key)) {
               uniqueMap.set(key, opt)
          }
     })
     const uniqueList = Array.from(uniqueMap.values())

     const currentRowId = String(dataItem?.id || '')
     const currentUnit = String(dataItem?.unit || dataItem?.Unit || '').trim().toLowerCase()
     // Only treat as "real" materialId if it doesn't look like a numeric index
     const rawMaterialId = dataItem?.materialId
     const currentMaterialId = (rawMaterialId && isNaN(rawMaterialId))
          ? String(rawMaterialId).trim().toLowerCase()
          : ''
     const currentDisplayName = String(dataItem?.displayName || dataItem?.DisplayName || '').trim().toLowerCase()

     // If no unit is selected on this row yet — show all options, no restriction
     if (!currentUnit) {
          return uniqueList
     }

     // Collect all Materials already committed to this Unit by OTHER rows
     const usedMaterialsInCurrentUnit = new Set()
     const rowList = Array.isArray(rows) ? rows : []
     rowList.forEach((r) => {
          const rRowId = String(r.id || '')
          if (rRowId === currentRowId) return // skip self

          const rUnit = String(r.unit || r.Unit || '').trim().toLowerCase()
          const rDisplayName = String(r.displayName || r.DisplayName || '').trim().toLowerCase()

          // Only block if other row has same unit AND a real material selected
          if (rUnit && rUnit === currentUnit && rDisplayName) {
               usedMaterialsInCurrentUnit.add(rDisplayName)
          }
     })

     return uniqueList.filter((opt) => {
          const optName = String(opt.label || opt.value || opt.displayName || '').trim().toLowerCase()

          // Keep this row's own current selection always visible
          if (currentDisplayName && optName === currentDisplayName) return true
          if (currentMaterialId && String(opt.materialId || '').trim().toLowerCase() === currentMaterialId) return true

          // Exclude if already selected in the same Unit by another row
          return !usedMaterialsInCurrentUnit.has(optName)
     })
}

/**
 * Helper to get available Unit dropdown options for a given row.
 *
 * Rules:
 * - If the row has NO Material Code selected yet: show all units.
 * - If the row has a Material Code selected: exclude any Unit that already has
 *   that Material in another row (unless it is this row's own current unit).
 * - Blank rows NEVER restrict each other.
 */
export const getUnitOptions = (dataItem, unitOptions = [], rows = []) => {
     if (!Array.isArray(unitOptions)) return []

     const currentRowId = String(dataItem?.id || '')
     const currentDisplayName = String(dataItem?.displayName || dataItem?.DisplayName || '').trim().toLowerCase()
     const currentUnit = String(dataItem?.unit || dataItem?.Unit || '').trim().toLowerCase()

     // If no material is chosen yet — show all units
     if (!currentDisplayName) {
          return unitOptions
     }

     // Collect all Units already using this Material in other rows
     const unitsHavingMaterial = new Set()
     const rowList = Array.isArray(rows) ? rows : []
     rowList.forEach((r) => {
          const rRowId = String(r.id || '')
          if (rRowId === currentRowId) return // skip self

          const rUnit = String(r.unit || r.Unit || '').trim().toLowerCase()
          const rDisplayName = String(r.displayName || r.DisplayName || '').trim().toLowerCase()

          if (rUnit && rDisplayName && rDisplayName === currentDisplayName) {
               unitsHavingMaterial.add(rUnit)
          }
     })

     return unitOptions.filter((u) => {
          const uLabel = String(u.label || u.value || '').trim().toLowerCase()
          // Always show current row's own unit
          if (currentUnit && uLabel === currentUnit) return true
          // Exclude if another row already uses this unit+material
          return !unitsHavingMaterial.has(uLabel)
     })
}

/**
 * Formats initial Throughput Norms API data into row objects for table state.
 */
export const formatNormsInitialRows = (data = [], unitsList = []) => {
     if (!Array.isArray(data)) return []
     return data.map((item, index) => {
          const uName = item.unit || item.Unit || ''
          const matchedUnit = unitsList.find(
               (u) =>
                    String(u.label || '').toLowerCase() === String(uName).toLowerCase() ||
                    String(u.value || '').toLowerCase() === String(uName).toLowerCase(),
          )
          const resolvedUnitId =
               item.unitId ||
               item.UnitId ||
               item.profitId ||
               item.profitFKId ||
               item.profitCenter_FK_Id ||
               item.profitCenterFkId ||
               matchedUnit?.unitId ||
               matchedUnit?.id ||
               ''

          const matId = item?.materialId || item?.id || ''
          const uniqueRowId = `row_${matId}_${resolvedUnitId}_${index}_${Date.now()}`

          return {
               ...item,
               id: uniqueRowId,
               originalMaterialId: matId,
               materialId: matId,
               unit: uName,
               unitId: resolvedUnitId,
               profitId: resolvedUnitId,
               displayName: item.displayName || item.DisplayName || item.materialCode || '',
               uom: item.uom || item.UOM || '',
               UOM: item.UOM || item.uom || '',
               remarks: item.remarks || '',
               isEditable: true,
          }
     })
}
