/**
 * Formats unit dropdown API response data into dropdown option objects.
 *
 * @param {Array} data - Raw unit dropdown response items
 * @returns {Array} Formatted unit dropdown options
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
 * Filters raw material response list by profitId/unitName and formats into dropdown options.
 *
 * @param {Array} rawList - Raw material items from API
 * @param {string} profitId - Unit/Profit ID filter
 * @param {string} unitName - Unit name filter
 * @returns {Array} Formatted material dropdown options
 */
export const formatMaterialDropdownOptions = (rawList = [], profitId = '', unitName = '') => {
     const filtered = rawList.filter((item) => {
          const uId = item?.UnitId || item?.unitId || item?.profitId
          const uName = item?.Unit || item?.unit
          if (profitId) {
               return String(uId).toLowerCase() === String(profitId).toLowerCase()
          }
          if (unitName) {
               return String(uName).toLowerCase() === String(unitName).toLowerCase()
          }
          return true
     })

     const formatted = []
     const seenMaterialKeys = new Set()
     filtered.forEach((item) => {
          const dName = item?.displayName || item?.DisplayName || item?.name || ''
          const mId = item?.materialId || item?.MaterialId || item?.id || item?.Id || dName
          const key = String(dName).trim().toLowerCase()
          if (dName && !seenMaterialKeys.has(key)) {
               seenMaterialKeys.add(key)
               formatted.push({
                    label: dName,
                    value: dName,
                    id: mId,
                    materialId: mId,
                    unitId: item?.unitId || item?.UnitId || profitId,
                    unit: item?.unit || item?.Unit || unitName || '',
                    uom: item?.uom || item?.UOM || '',
               })
          }
     })
     return formatted
}

/**
 * Helper function to filter and deduplicate material dropdown options for a given row in ThroughputNorms grid.
 *
 * @param {Object} dataItem - Current row data item
 * @param {Object} materialDropdownMap - Map of profitId / unitName to options array
 * @param {Object} materialDropdownMapRef - Ref pointing to materialDropdownMap
 * @param {Array} rows - Current table rows
 * @returns {Array} Filtered and deduplicated material options
 */
export const getMaterialOptions = (dataItem, materialDropdownMap = {}, materialDropdownMapRef = { current: {} }, rows = []) => {
     const profitId = dataItem?.unitId || dataItem?.profitId || dataItem?.profitCenter_FK_Id || dataItem?.profitCenterFkId
     const unitName = dataItem?.unit || dataItem?.Unit
     const rawOptions =
          materialDropdownMap[profitId] ||
          materialDropdownMap[unitName] ||
          (profitId ? materialDropdownMapRef?.current?.[profitId] : null) ||
          (unitName ? materialDropdownMapRef?.current?.[unitName] : null) ||
          []

     // 1. Deduplicate raw options by label / materialId
     const uniqueOptionsMap = new Map()
     rawOptions.forEach((opt) => {
          const key = String(opt.label || opt.value || opt.id || opt.materialId).trim().toLowerCase()
          if (key && !uniqueOptionsMap.has(key)) {
               uniqueOptionsMap.set(key, opt)
          }
     })
     const uniqueRawOptions = Array.from(uniqueOptionsMap.values())

     // 2. Filter out materials already selected in other rows under the SAME unit
     const currentUnit = String(unitName || '').toLowerCase()
     const currentProfitId = String(profitId || '').toLowerCase()
     const currentRowId = String(dataItem?.id || '')
     const currentDisplayName = String(dataItem?.displayName || '').trim().toLowerCase()

     const selectedMaterialsInSameUnit = new Set()
     rows.forEach((r) => {
          const rUnit = String(r.unit || '').toLowerCase()
          const rProfitId = String(r.unitId || r.profitId || '').toLowerCase()
          const rRowId = String(r.id || '')
          const rDisplayName = String(r.displayName || '').trim().toLowerCase()

          const isSameUnit =
               (currentProfitId && rProfitId && currentProfitId === rProfitId) ||
               (currentUnit && rUnit && currentUnit === rUnit)

          if (isSameUnit && rRowId !== currentRowId && rDisplayName) {
               selectedMaterialsInSameUnit.add(rDisplayName)
          }
     })

     return uniqueRawOptions.filter((opt) => {
          const optLabel = String(opt.label || opt.value || '').trim().toLowerCase()
          if (optLabel === currentDisplayName) return true
          return !selectedMaterialsInSameUnit.has(optLabel)
     })
}

/**
 * Formats initial Throughput Norms API data into row objects for table state.
 *
 * @param {Array} data - Raw API throughput norms items
 * @param {Array} unitsList - Available unit dropdown options
 * @returns {Array} Formatted row objects
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

          return {
               ...item,
               id: item?.id || item?.materialId || index + 1,
               materialId: item?.materialId || item?.id,
               unit: uName,
               unitId: resolvedUnitId,
               profitId: resolvedUnitId,
               displayName: item.displayName || item.DisplayName || '',
               uom: item.uom || item.UOM || '',
               UOM: item.UOM || item.uom || '',
               remarks: item.remarks || '',
               isEditable: true,
          }
     })
}

/**
 * Extracts unique unit identifiers to pre-fetch material dropdown options.
 *
 * @param {Array} rows - Current formatted grid rows
 * @returns {Array} Array of unique unit objects ({ profitId, unitName })
 */
export const getUniqueUnitsToFetch = (rows = []) => {
     const unitsToFetch = rows
          .map((r) => ({
               profitId: r.unitId || r.profitId || r.profitCenter_FK_Id || r.profitCenterFkId,
               unitName: r.unit,
          }))
          .filter((u) => u.profitId || u.unitName)

     return unitsToFetch.filter(
          (item, index, self) =>
               index === self.findIndex(
                    (t) =>
                         (t.profitId && t.profitId === item.profitId) ||
                         (t.unitName && t.unitName === item.unitName),
               ),
     )
}
