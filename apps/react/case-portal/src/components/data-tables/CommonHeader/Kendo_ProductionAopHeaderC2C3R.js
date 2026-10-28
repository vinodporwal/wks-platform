import productionColDefs from '../../../assets/kendo_production_aop_cracker_c2c3r.json'
import { shouldLockColumn } from 'utils/columnLockUtils'

const getEnhancedColDefsC2C3R = ({ headerMap, valueFormat }) => {
  let cols

  cols = productionColDefs

  const enhancedColDefs = cols.map((col) => {
    let updatedCol = { ...col }

    if (headerMap && headerMap[col.title] !== undefined) {
      updatedCol.title = headerMap[col.title]
    }

    if (col.type === 'number' && valueFormat) {
      updatedCol.format = valueFormat
      updatedCol.minWidth = 100
    }
    if (shouldLockColumn(col)) {
      updatedCol.locked = true
    }

    return updatedCol
  })

  return enhancedColDefs
}

export default getEnhancedColDefsC2C3R
