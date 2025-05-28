import React, { useState } from 'react'
import PropTypes from 'prop-types'
import { Grid, GridColumn as Column } from '@progress/kendo-react-grid'
import { filterBy } from '@progress/kendo-data-query'
import { Button, Box, TextField, MenuItem } from '@mui/material'
import '@progress/kendo-theme-default/dist/all.css'
import '../../kendo-data-grid.css'
import Notification from 'components/Utilities/Notification'

const KendoDataTables = ({
  rows,
  setRows,
  columns,
  loading = false,
  pageSizes = [10, 20, 50],
  onRowChange,
  disableColor = false,
  setSnackbarOpen = () => {},
  snackbarData = { message: '', severity: 'info' },
  snackbarOpen = false,
  unsavedChangesRef = { current: { unsavedRows: {}, rowsBeforeChange: {} } },
  setRemarkDialogOpen = () => {},
  currentRemark = '',
  setCurrentRemark = () => {},
  currentRowId = null,
  remarkDialogOpen = false,
  handleCalculate = () => {},
  fetchData = () => {},
  handleUnitChange = () => {},
  deleteRowData = () => {},
  handleAddPlantSite = () => {},
  selectedUsers = [],
  allRedCell = [],
  permissions = {},
  // selectedUnit = '', // Add selectedUnit prop
  // setSelectedUnit = () => {}, // Add setSelectedUnit prop
  jioColors = { background: '#ffffff' }, // Add jioColors prop with default
}) => {
  const [filter, setFilter] = useState({ logic: 'and', filters: [] })
  const [isButtonDisabled, setIsButtonDisabled] = useState(false)
const [selectedUnit, setSelectedUnit] = useState()
  // ✅ Snackbar state
  const [localSnackbarOpen, setLocalSnackbarOpen] = useState(false)
  const [localSnackbarData, setLocalSnackbarData] = useState({
    message: '',
    severity: 'success',
  })

  const handleItemChange = (e) => {
    const updated = rows.map((r) =>
      r.id === e.dataItem.id ? { ...r, [e.field]: e.value } : r,
    )
    onRowChange?.(updated, e)
  }

  const rowRender = disableColor
    ? (trElement, props) => {
        const shouldDisable = props.dataItem.status === 'inactive'
        return React.cloneElement(trElement, {
          ...trElement.props,
          className: `${trElement.props.className || ''} ${
            shouldDisable ? 'disabled-row' : ''
          }`,
        })
      }
    : undefined

  const handleCalculateBtn = async () => {
    setIsButtonDisabled(true)
    handleCalculate()
    setTimeout(() => {
      setIsButtonDisabled(false)
    }, 500)
  }

  const handleRemarkSave = () => {
    setRows((prevRows) => {
      let updatedRow = null

      const updatedRows = prevRows.map((row) => {
        if (row.id === currentRowId) {
          const keysToUpdate = ['aopRemarks', 'remarks', 'remark'].filter(
            (key) => key in row,
          )
          const keyToUpdate = keysToUpdate[0] || 'remark'
          updatedRow = { ...row, [keyToUpdate]: currentRemark }
          return updatedRow
        }
        return row
      })

      if (updatedRow) {
        unsavedChangesRef.current.unsavedRows[currentRowId] = updatedRow
      }

      return updatedRows
    })

    setRemarkDialogOpen(false)
  }

  return (
    <div style={{ position: 'relative' }}>
      {loading && (
        <div className='k-loading-mask'>
          <span className='k-loading-text'>Loading...</span>
          <div className='k-loading-image' />
          <div className='k-loading-color' />
        </div>
      )}

      {/* Button and Unit Selection Section */}
      {(permissions?.showCalculate || permissions?.showUnit) && (
        <Box display="flex" justifyContent="flex-end" alignItems="center" gap={2} p={1}>
          {/* Calculate Button */}
          {permissions?.showCalculate && (
            <Button
              variant="contained"
              onClick={handleCalculateBtn}
              disabled={isButtonDisabled}
              sx={{
                backgroundColor: '#0100cb',
                '&:hover': {
                  backgroundColor: '#0100cb',
                  opacity: 0.9,
                },
              }}
            >
              Calculate
            </Button>
          )}

          {/* Unit Selection TextField */}
          {permissions?.showUnit && (
            <TextField
              select
              value={selectedUnit || permissions?.units?.[0] || ''}
              onChange={(e) => {
                setSelectedUnit(e.target.value)
                handleUnitChange(e.target.value)
              }}
              sx={{ 
                width: '150px', 
                backgroundColor: jioColors.background 
              }}
              variant='outlined'
              label='Select UOM'
              size="small"
            >
              <MenuItem value='' disabled>
                Select UOM
              </MenuItem>

              {/* Render the correct unit options dynamically */}
              {permissions?.units?.map((unit) => (
                <MenuItem key={unit} value={unit}>
                  {unit}
                </MenuItem>
              ))}
            </TextField>
          )}
        </Box>
      )}

      {/* Grid Section */}
      <div className='kendo-data-grid'>
        <Grid
          data={filterBy(rows, filter)}
          filterable={true}
          sortable
          dataItemKey='id'
          pageable={{ pageSizes, buttonCount: 5 }}
          editField='inEdit'
          filter={filter}
          onFilterChange={(e) => setFilter(e.filter)}
          onItemChange={handleItemChange}
          rowRender={rowRender}
          resizable={true}
        >
          {columns.map(
            ({ field, title, width, cell, format, filterable = true }) => (
              <Column
                key={field}
                field={field}
                title={title}
                width={width}
                filterable={filterable}
                cell={cell}
                format={format}
              />
            ),
          )}
        </Grid>
      </div>

      {/* ✅ Custom Snackbar Notification */}
      <Notification
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
      />
    </div>
  )
}


export default KendoDataTables