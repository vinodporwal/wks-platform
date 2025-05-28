import production_coldefs_pe from '../../../assets/production_coldefs_pe.json' // adjust the path as needed
import production_coldefs_meg from '../../../assets/production_coldefs_meg.json' // adjust the path as needed
import { useSelector } from 'react-redux'
import Tooltip from '@mui/material/Tooltip'
import { truncateRemarks } from 'utils/remarksUtils'
import NumericInputOnly from 'utils/NumericInputOnly'

import TextField from '@mui/material/TextField'

const getEnhancedProductionColDefs = ({
  allProducts,
  headerMap,
  handleRemarkCellClick,
  findAvg,
}) => {
  const formatValueToThreeDecimals = (params) =>
    params ? parseFloat(params).toFixed(3) : ''

  const getProductDisplayName = (id) => {
    if (!id) return
    const product = allProducts.find((p) => p.id === id)
    return product ? product.displayName : ''
  }

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { verticalChange } = dataGridStore
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || 'meg'
  return (
    lowerVertName === 'meg' ? production_coldefs_meg : production_coldefs_pe
  ).map((col) => {
    let updatedCol = { ...col }

    // Override headerName using headerMap if available
    if (headerMap && headerMap[col.headerName]) {
      updatedCol.headerName = headerMap[col.headerName]
    }

    // Enhance the Product column with custom functions
   if (col.field === 'normParametersFKId') {
  updatedCol = {
    ...updatedCol,
    title: headerMap?.[col.headerName] || col.headerName,
    field: 'normParametersFKId',

    // Display displayName instead of id
    cell: (props) => {
      const product = allProducts.find((p) => p.id === props.dataItem.normParametersFKId);
      return <td>{product ? product.displayName : ''}</td>;
    },

    // Custom filterCell for "contains" logic
    filterCell: (props) => {
      const handleChange = (e) => {
        props.onChange({
          value: e.target.value,
          operator: 'contains',
          syntheticEvent: e,
        });
      };

      return (
        <td>
          <TextField
            size="small"
            label="Contains"
            value={props.filter?.value || ''}
            onChange={handleChange}
            fullWidth
          />
        </td>
      );
    },

    // Custom cell editor with dropdown
    editCell: (props) => {
      const handleChange = (e) => {
        props.onChange({
          dataItem: props.dataItem,
          field: 'normParametersFKId',
          value: e.target.value,
        });
      };

      return (
        <td>
          <select
            value={props.dataItem.normParametersFKId || ''}
            onChange={handleChange}
            style={{ width: '100%', padding: '5px' }}
          >
            <option value="" disabled>
              Select
            </option>
            {allProducts.map((product) => (
              <option key={product.id} value={product.id}>
                {product.displayName}
              </option>
            ))}
          </select>
        </td>
      );
    },
  };
}

    if (col.field === 'avgTph') {
      updatedCol.valueGetter = findAvg
    }

    if (headerMap && headerMap[col.headerName]) {
      return {
        ...col,
        renderEditCell: NumericInputOnly,
        valueFormatter: formatValueToThreeDecimals,
        headerName: headerMap[col.headerName],
        align: 'right',
      }
    }

    if (col.field === 'remarks') {
      updatedCol.renderCell = (params) => {
        const displayText = truncateRemarks(params.value)
        const isEditable = !params.row.Particulars
        return (
          <Tooltip title={params.value || ''} arrow>
            <div
              style={{
                cursor: 'pointer',
                color: params.value ? 'inherit' : 'gray',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                width: ' 100%',
              }}
              onClick={() => handleRemarkCellClick(params.row)}
            >
              {displayText || (isEditable ? 'Click to add remark' : '')}
            </div>
          </Tooltip>
        )
      }
    }

    return updatedCol
  })
}

export default getEnhancedProductionColDefs
