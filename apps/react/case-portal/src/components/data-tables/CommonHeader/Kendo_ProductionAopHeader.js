import { useSelector } from 'react-redux'
import TextField from '@mui/material/TextField'

import productionColDefs from '../../../assets/production_aop_meg.json'
import productionColDefsPE from '../../../assets/production_aop_pe.json'

import NumericInputOnly from 'utils/NumericInputOnly'

const getEnhancedColDefs = ({
  allProducts,
  headerMap,
  handleRemarkCellClick, // unused but preserved for API compatibility
  showOnlyMonths = true,
}) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { verticalChange } = dataGridStore
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || 'meg'

  const monthFields = [
    'april', 'may', 'june', 'july', 'aug', 'sep',
    'oct', 'nov', 'dec', 'jan', 'feb', 'march',
  ]

  const formatValueToTwoDecimals = (params) =>
    params?.value ? parseFloat(params.value).toFixed(2) : ''

  // Load base columns
  let cols = lowerVertName === 'pe' ? productionColDefsPE : productionColDefs

  // Optionally filter only month columns
  if (showOnlyMonths) {
    cols = cols.filter((col) => monthFields.includes(col.field))
  }

  // Add "Total" column for summary row
  cols.push({
    field: 'total',
    headerName: headerMap?.['Total'] || 'Total',
    valueGetter: (params) => (params.row.id === 'total' ? params.row.total : null),
    valueFormatter: formatValueToTwoDecimals,
    align: 'right',
    renderEditCell: NumericInputOnly,
    cellClassName: (params) => (params.row.id === 'total' ? 'total-row-style' : ''),
  })

  // Transform all month/metric columns
  const enhancedCols = cols.map((col) => {
    let updatedCol = { ...col }

    // Custom averageTPH logic
    if (col.field === 'averageTPH') {
      updatedCol.valueGetter = (params) => {
        const row = params.row
        const total = monthFields.reduce((acc, field) => {
          const val = parseFloat(row[field])
          return acc + (isNaN(val) ? 0 : val)
        }, 0)
        return total / 8760
      }
    }

    // Apply header map, formatting and styling
    if (headerMap && headerMap[col.headerName] !== undefined) {
      updatedCol = {
        ...updatedCol,
        headerName: headerMap[col.headerName],
        valueFormatter: formatValueToTwoDecimals,
        renderEditCell: NumericInputOnly,
        align: 'right',
      }
    }

    updatedCol.cellClassName = (params) =>
      params.row.id === 'total' ? 'total-row-style' : ''

    return updatedCol
  })

  // Add Particulars column at the beginning
  enhancedCols.unshift({
    field: 'normParametersFKId',
    headerName: headerMap?.['Particulars'] || 'Particulars',
    valueGetter: (params) => {
      if (params?.row?.id === 'total') return 'Total'
      const product = allProducts.find(
        (p) => p.id === params?.row?.normParametersFKId
      )
      return product ? product.displayName : ''
    },
    renderEditCell: (params) => {
      const { value, id, api } = params
      return (
        <select
          value={value || ''}
          onChange={(event) => {
            api.setEditCellValue({
              id,
              field: 'normParametersFKId',
              value: event.target.value,
            })
          }}
          style={{
            width: '100%',
            padding: '5px',
            border: 'none',
            outline: 'none',
            background: 'transparent',
          }}
        >
          <option value='' disabled>
            Select
          </option>
          {allProducts.map((product) => (
            <option key={product.id} value={product.id}>
              {product.displayName}
            </option>
          ))}
        </select>
      )
    },
    cellClassName: (params) =>
      params.row.id === 'total' ? 'total-row-style' : '',
  })

  return enhancedCols
}

export default getEnhancedColDefs
