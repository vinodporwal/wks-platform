import NumericInputOnly from 'utils/NumericInputOnly'
const getEnhancedAnnualAopCostReport = ({
  headerMap,
  type,
  headers2 = [],
  keys2 = [],
}) => {
  // console.log('headers2', headers2)

  const formatValueToThreeDecimals = (params) =>
    params ? parseFloat(params).toFixed(3) : ''

  let annual_aop_cost_report
  switch (type) {
    case 'Production':
      annual_aop_cost_report = require('../../../assets/annual_aop_cost_report_production.json')
      break
    case 'Price':
      annual_aop_cost_report = require('../../../assets/annual_aop_cost_report_price.json')
      break
    case 'Norm':
      annual_aop_cost_report = require('../../../assets/annual_aop_cost_report_norm.json')
      break
    case 'Quantity':
      annual_aop_cost_report = require('../../../assets/annual_aop_cost_report_quantity.json')
      break
    case 'NormCost':
      annual_aop_cost_report = require('../../../assets/annual_aop_cost_report_norm_cost.json')
      break
    default:
      throw new Error('Invalid type provided')
  }

  if (type == 'Price') {
    // console.log('annual_aop_cost_report', annual_aop_cost_report)

    const updatedColumns = annual_aop_cost_report.map((col, index) => {
      const header = headers2[index]
      const key = keys2[index]

      return {
        ...col,
        field: key,
        headerName: header,
        flex: 1,
        renderEditCell: NumericInputOnly,
        valueFormatter: formatValueToThreeDecimals,
      }
    })
    return updatedColumns

    // console.log('updatedColumns updated ', updatedColumns)
  }

  return annual_aop_cost_report.map((col) => {
    let updatedCol = { ...col }
    if (headerMap && headerMap[col.headerName]) {
      updatedCol.headerName = headerMap[col.headerName]
    }
    if (headerMap && headerMap[col.headerName]) {
      return {
        ...col,
        renderEditCell: NumericInputOnly,
        valueFormatter: formatValueToThreeDecimals,
        headerName: headerMap[col.headerName],
        flex: 1,
      }
    }
    return updatedCol
  })
}

export default getEnhancedAnnualAopCostReport
