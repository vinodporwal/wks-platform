import productionColDefs from '../../../assets/kendo_shutdown_rate_new.json'

const getEnhancedColDefsShutdownRate = ({ headerMap, valueFormat }) => {
  let cols

  cols = productionColDefs

  const enhancedColDefs = cols.map((col) => {
    let updatedCol = { ...col }

    if (headerMap && headerMap[col.title] !== undefined) {
      updatedCol.title = headerMap[col.title]
    }

    if (col.type === 'number' && valueFormat) {
      updatedCol.format = valueFormat
    }

    return updatedCol
  })

  return enhancedColDefs
}

export default getEnhancedColDefsShutdownRate
