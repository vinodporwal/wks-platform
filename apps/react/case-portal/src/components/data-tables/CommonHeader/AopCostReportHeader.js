// Modify getEnhancedAnnualAopCostReport to include the type as a parameter

import NumericInputOnly from 'utils/NumericInputOnly'

const getEnhancedAnnualAopCostReport = ({ headerMap, type }) => {
  // Function to format values to 3 decimals
  const formatValueToThreeDecimals = (params) =>
    params ? parseFloat(params).toFixed(3) : ''

  // Conditionally import the right JSON file based on the type
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

  // Map through the data and apply the headerMap and other transformations
  return annual_aop_cost_report.map((col) => {
    let updatedCol = { ...col }

    // Update the column name if a headerMap is provided
    if (headerMap && headerMap[col.headerName]) {
      updatedCol.headerName = headerMap[col.headerName]
    }

    // Return the updated column with additional properties if headerMap is available
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
