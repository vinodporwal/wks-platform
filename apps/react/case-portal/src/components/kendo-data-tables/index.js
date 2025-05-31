import React, { useState } from 'react'
import { Grid, GridColumn } from '@progress/kendo-react-grid'
import { filterBy } from '@progress/kendo-data-query'
import '@progress/kendo-theme-default/dist/all.css'
import '@progress/kendo-font-icons/dist/index.css'
import { filterIcon } from '@progress/kendo-svg-icons'
import { ColumnMenu } from 'components/data-tables/Reports/columnMenu'
import { EditDescriptor } from '@progress/kendo-react-data-tools'

// import PropTypes from 'prop-types'
import '../../kendo-data-grid.css'
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  MenuItem,
  TextField,
} from '../../../node_modules/@mui/material/index'
import Notification from 'components/Utilities/Notification'
// import DeleteCell from './Utilities-Kendo/DefaultColumn'
import { SvgIcon } from '../../../node_modules/@progress/kendo-react-common/index'
import { trashIcon } from '../../../node_modules/@progress/kendo-svg-icons/dist/index'
import { DateTimePicker } from '@progress/kendo-react-dateinputs'
import { truncateRemarks } from 'utils/remarksUtils'

const KendoDataTables = ({
  // setUpdatedRows = () => {},
  rows = [],
  // updatedRows = [],
  setRows,
  columns,
  loading = false,
  // pageSizes = [10, 20, 50],
  // onRowChange,
  // disableColor = false,
  permissions = {},
  setSnackbarOpen = () => {},
  snackbarData = { message: '', severity: 'info' },
  snackbarOpen = false,
  unsavedChangesRef = { current: { unsavedRows: {}, rowsBeforeChange: {} } },
  setRemarkDialogOpen = () => {},
  currentRemark = '',
  // editedRows = [],
  setCurrentRemark = () => {},
  currentRowId = null,
  // modifiedCells = [],
  NormParameterIdCell = () => {},

  setModifiedCells = () => {},
  remarkDialogOpen = false,
  handleDeleteSelected = () => {},
  saveChanges = () => {},
  deleteRowData = () => {},
  handleAddPlantSite = () => {},
  handleCalculate = () => {},
  fetchData = () => {},
  handleUnitChange = () => {},
  handleRemarkCellClick = () => {},
  selectedUsers = [],
  // allRedCell = [],
}) => {
  const [filter, setFilter] = useState({ logic: 'and', filters: [] })
  const [openDeleteDialogeBox, setOpenDeleteDialogeBox] = useState(false)
  // const [resizedColumns, setResizedColumns] = useState({})
  const [isButtonDisabled, setIsButtonDisabled] = useState(false)
  const [edit, setEdit] = React.useState({})

  // const [searchText, setSearchText] = useState('')
  // const isFilterActive = false
  const [selectedUnit, setSelectedUnit] = useState()
  const [openSaveDialogeBox, setOpenSaveDialogeBox] = useState(false)
  const [paramsForDelete, setParamsForDelete] = useState([])
  // const closeDeleteDialogeBox = () => setOpenDeleteDialogeBox(false)
  const closeSaveDialogeBox = () => setOpenSaveDialogeBox(false)
  // const localApiRef = useGridApiRef()
  // const finalExternalApiRef = apiRef ?? localApiRef
  // const handleSearchChange = (event) => {
  //   setSearchText(event.target.value)
  // }
  // // console.log(columns)
  const handleEditChange = (e) => {
    console.log(e)
    setEdit(e.edit)
  }
  const rowRender = (trElement, props) => {
    if (!props.dataItem.isEditable) {
      return React.cloneElement(trElement, {
        ...trElement.props,
        className: (trElement.props.className || '')
          .split(' ')
          .concat('disabled-row')
          .join(' '),
      })
    }
    return trElement
  }
  const hiddenFields = [
    'maintenanceId',
    'id',
    'plantFkId',
    'aopCaseId',
    'aopType',
    'aopYear',
    'avgTph',
    'NormParameterMonthlyTransactionId',
    'aopStatus',
    'idFromApi',
    'isEditable',
    'period',
  ]
  // //  const toggleColumn = field => {
  // //   setColumnVisibility(vis => ({
  //     ...vis,
  //     [field]: !vis[field],
  //   }));
  // };
  // cell update
  const itemChange = (e) => {
    // console.log(e)
    let updated = rows.map((r) =>
      r.id === e.dataItem.id ? { ...r, [e.field]: e.value } : r,
    )
    setRows(updated)

    setModifiedCells((updated = updated.filter((row) => row.inEdit == true)))
    console.log(updated)

    // onRowChange(e.dataItem, e.field, e.value)
  }
  // console.log(unsavedChangesRef)
  // const rowRender = disableColor
  //   ? (trElement, props) => {
  //       const shouldDisable = props.dataItem.status === 'inactive'
  //       return React.cloneElement(trElement, {
  //         ...trElement.props,
  //         className: `${trElement.props.className || ''} ${shouldDisable ? 'disabled-row' : ''}`,
  //       })
  //     }
  //   : undefined

  const handleRemarkSave = () => {
    setRows((prevRows) => {
      let updatedRow = null
      let keyToUpdate = ''
      const updatedRows = prevRows.map((row) => {
        // console.log(currentRowId, row.id)
        if (row.id === currentRowId) {
          const keysToUpdate = ['aopRemarks', 'remarks', 'remark'].filter(
            (key) => key in row,
          )
          //          console.log(keysToUpdate)
          keyToUpdate = keysToUpdate[0] || 'remark'
          //          console.log([keyToUpdate])
          updatedRow = { ...row, [keyToUpdate]: currentRemark, inEdit: true }
          return updatedRow
        }
        return row
      })
      // console.log(updatedRow)

      if (updatedRow) {
        setModifiedCells((prev) => ({
          ...prev,
          [updatedRow.id]: updatedRow,
        }))
      }

      return updatedRows
    })

    setRemarkDialogOpen(false)
  }
  // console.log(rows)
  // console.log(columns)
  const handleAddRow = () => {
    if (isButtonDisabled) return
    setIsButtonDisabled(true)
    const newRowId = rows.length
      ? Math.max(...rows.map((row) => row.id)) + 1
      : 1
    const newRow = {
      id: newRowId,
      isNew: true,
      ...Object.fromEntries(columns.map((col) => [col.field, ''])),
    }

    setRows((prevRows) => [newRow, ...prevRows])
    // onAddRow?.(newRow)
    // setProduct('')
    // setRowModesModel((oldModel) => ({
    //   ...oldModel,
    //   [newRowId]: { mode: GridRowModes.Edit, fieldToFocus: 'discription' },
    // }))
    // focusFirstField()
    setTimeout(() => {
      setIsButtonDisabled(false)
    }, 500)
  }
  const saveConfirmation = async () => {
    saveChanges()
    setOpenSaveDialogeBox(false)
  }
  const handleDeleteClick = async (params) => {
    setParamsForDelete(params)
    setOpenDeleteDialogeBox(true)
  }
  const deleteTheRecord = async () => {
    deleteRowData(paramsForDelete)
    setOpenDeleteDialogeBox(false)
  }
  const ActionsCell = ({ dataItem }) => {
    return (
      <td style={{ textAlign: 'center', verticalAlign: 'middle' }}>
        <SvgIcon
          onClick={() => handleDeleteClick(dataItem)}
          icon={trashIcon}
          themeColor='dark'
        />
      </td>
    )
  }
  const saveModalOpen = async () => {
    setIsButtonDisabled(true)
    setOpenSaveDialogeBox(true)
    setTimeout(() => {
      setIsButtonDisabled(false)
    }, 500)
  }
  const handleCalculateBtn = async () => {
    setIsButtonDisabled(true)
    handleCalculate()
    setTimeout(() => {
      setIsButtonDisabled(false)
    }, 500)
  }
  const handleRefresh = async () => {
    try {
      fetchData()
    } catch (error) {
      console.error('Error saving refresh data:', error)
    }
  }
  const handleRowClick = (e) => {
    console.log('22', e)

    // setRows(
    //   rows.map((r) => ({
    //     ...r,
    //     inEdit: r.id === e.dataItem.id, // only that row goes into edit mode
    //   })),
    // )
    if (columns.some((col) => col.field === 'remark')) {
      handleRemarkCellClick(e.dataItem)
    }
  }
  const showDeleteAll = permissions?.deleteAllBtn && selectedUsers.length > 1
  // const DateTimePickerCell = (props) => {
  //   const { dataItem, field } = props
  //   const value = dataItem[field] ? new Date(dataItem[field]) : null

  //   return (
  //     <td>
  //       <DateTimePicker
  //         value={value}
  //         onChange={(e) => props.onChange({ ...props, value: e.value })}
  //         format='dd/MM/yyyy hh:mm tt'
  //       />
  //     </td>
  //   )
  // }
  const DateTimePickerEditor = ({ dataItem, field, onChange }) => {
    const value = dataItem[field] ? new Date(dataItem[field]) : null
    return (
      <td>
        <DateTimePicker
          value={value}
          onChange={(e) =>
            onChange({
              dataItem,
              field,
              value: e.value,
              syntheticEvent: e.syntheticEvent,
            })
          }
          format='dd/MM/yyyy hh:mm tt'
        />
      </td>
    )
  }

  const particulars = [
    'normParameterId',
    'normParametersFKId',
    'NormParameterFKId',
    'materialFkId',
    'normParameterFKId',
  ]
  const RemarkCell = (props) => {
    const { dataItem, field, onRemarkClick, ...tdProps } = props

    // Compute: the truncated display text (or placeholder)
    const rawValue = dataItem[field]
    const displayText = truncateRemarks(rawValue)
    const editable = Boolean(dataItem.isEditable)

    return (
      <td
        {...tdProps}
        style={{
          cursor: editable ? 'pointer' : 'not-allowed',
          color: rawValue ? 'inherit' : 'gray',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
        onClick={() => {
          if (editable) {
            onRemarkClick(dataItem)
          }
        }}
      >
        {displayText || (editable ? 'Click to add remark' : '')}
      </td>
    )
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
      {(permissions?.allAction ?? true) && (
        <Box className='action-box'>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'flex-end',
              width: '100%', // make sure container is full width
              p: 1,
              gap: 1,
            }}
          >
            {permissions?.UnitToShow && (
              <Chip
                label={permissions.UnitToShow}
                variant='outlined'
                className='unit-chip'
              />
            )}
            {/* {permissions?.showCalculate && (
              <Tooltip title='Calculate'>
                <span>
                  <Button
                    variant='contained'
                    onClick={handleCalculateBtn}
                    disabled={isButtonDisabled}
                    sx={{
                      minWidth: '40px',
                      padding: '8px',
                      backgroundColor: '#0100cb',
                      '&:hover': {
                        backgroundColor: '#0100cb',
                        opacity: 0.9,
                      },
                    }}
                  >
                    <CalculateOutlinedIcon sx={{ color: '#fff' }} />
                  </Button>
                </span>
              </Tooltip>
            )} */}

            {permissions?.showCalculate && (
              <Button
                variant='contained'
                onClick={handleCalculateBtn}
                disabled={isButtonDisabled}
                className='btn-save'
              >
                Calculate
              </Button>
            )}
            {permissions?.showRefresh && (
              <Button
                variant='contained'
                onClick={handleCalculateBtn}
                disabled={isButtonDisabled}
                className='btn-save'
              >
                Refresh
              </Button>
            )}

            {permissions?.showRefreshBtn && false && (
              <Button
                variant='contained'
                onClick={handleRefresh}
                className='btn-save'
              >
                Refresh
              </Button>
            )}

            {permissions?.showUnit && (
              <TextField
                select
                value={selectedUnit || permissions?.units?.[0]}
                onChange={(e) => {
                  setSelectedUnit(e.target.value)
                  handleUnitChange(e.target.value)
                }}
                sx={{ width: '150px', backgroundColor: '#FFFFFF' }}
                variant='outlined'
                label='Select UOM'
              >
                <MenuItem value='' disabled>
                  Select UOM
                </MenuItem>

                {/* Render the correct unit options dynamically */}
                {permissions?.units.map((unit) => (
                  <MenuItem key={unit} value={unit}>
                    {unit}
                  </MenuItem>
                ))}
              </TextField>
            )}

            {/* </Box> */}
          </Box>
        </Box>
      )}
      <div className='kendo-data-grid'>
        <Grid
          data={filterBy(rows, filter)}
          sortable
          dataItemKey='id'
          editField='inEdit'
          editable={{ mode: 'incell' }}
          // onRowClick={(e) => {
          //   const id = e.dataItem.id
          //   setRows(rows.map((r) => (r.id === id ? { ...r, inEdit: true } : r)))
          // }}
          onEditChange={handleEditChange}
          autoProcessData={true}
          edit={edit}
          scrollable='scrollable'
          filter={filter}
          // filterable={true}
          onFilterChange={(e) => setFilter(e.filter)}
          onItemChange={itemChange}
          rowRender={rowRender}
          resizable={true}
          defaultSkip={0}
          defaultTake={100}
          columnMenuIcon={filterIcon}
          contextMenu={true}
          pageable={
            rows?.length > 100
              ? {
                  buttonCount: 4,
                  pageSizes: [10, 50, 100],
                }
              : false
          }
          onBlur={() => {
            // whenever the Grid loses focus, clear every row’s inEdit flag
            // setRows(rows.map((r) => ({ ...r, inEdit: false })))
            setEdit({})
          }}
          // cellClick={(e) => {
          //   console.log('Cell clicked:', e)
          //   if (e.field === 'remark') {
          //     handleRemarkCellClick(e.dataItem)
          //   }
          // }}
          // onRowClick={handleRowClick}
        >
          {columns
            .filter((col) => !hiddenFields.includes(col.field))
            .map((col) => {
              if (
                col.field === 'maintStartDateTime' ||
                col.field === 'maintEndDateTime'
              ) {
                return (
                  <GridColumn
                    key={col.field}
                    field={col.field}
                    title={col.title || col.headerName}
                    width={col.width}
                    filter='date'
                    format='{0:dd/MM/yyyy hh:mm tt}'
                    cells={{
                      // data: DateTimePickerCell,
                      edit: {
                        date: DateTimePickerEditor,
                      },
                    }}
                    columnMenu={ColumnMenu}
                    editor='date'
                  />
                )
              }
              if (col.field === 'remark' || col.field === 'remarks') {
                return (
                  <GridColumn
                    key={col.field}
                    field={col.field}
                    title={col.title || col.headerName}
                    width={col.width}
                    editor={true}
                    cells={{
                      data: (cellProps) => (
                        <RemarkCell
                          {...cellProps}
                          onRemarkClick={handleRemarkCellClick}
                        />
                      ),
                    }}
                    columnMenu={ColumnMenu}
                    // editor='date'
                  />
                )
              }

              if (particulars.includes(col.field)) {
                return (
                  <GridColumn
                    key={col.field}
                    field={col.field}
                    title={col.title || col.headerName}
                    width={col.width}
                    editable={false}
                    filterable={true}
                    cells={{
                      data: NormParameterIdCell,
                    }}
                    columnMenu={ColumnMenu}
                  />
                )
              }

              return (
                <GridColumn
                  key={col.field}
                  field={col.field}
                  title={col.title || col.headerName}
                  width={col.width}
                  columnMenu={ColumnMenu}
                />
              )
            })}

          {permissions?.deleteButton && (
            <GridColumn
              key='actions'
              field='actions'
              title='Action'
              width={80}
              className='k-text-center'
              filterable={false}
              editable={false}
              cells={{
                data: ActionsCell,
              }}
            />
          )}
        </Grid>
      </div>
      {(permissions?.allActionOfBottomBtns ?? true) && (
        <Box
          sx={{
            marginTop: 2,
            display: 'flex',
            gap: 2,
          }}
        >
          {permissions?.addButton && (
            <Button
              variant='contained'
              className='btn-save'
              onClick={handleAddRow}
              disabled={isButtonDisabled}
            >
              Add Item
            </Button>
          )}

          {permissions?.saveBtn && (
            <Button
              variant='contained'
              className='btn-save'
              onClick={saveModalOpen}
              disabled={isButtonDisabled}
              // loading={loading}
              // loadingposition='start'
              {...(loading ? {} : {})}
            >
              Save
            </Button>
          )}
          {/* {permissions?.showCreateCasebutton && (
            <Button
              variant='contained'
              onClick={createCase}
              disabled={isCreatingCase || !showCreateCasebutton}
              className='btn-save'
            >
              {isCreatingCase ? 'Submitting…' : 'Submit'}
            </Button>
          )} */}

          {permissions?.approveBtn && (
            <Button
              variant='contained'
              className='btn-save'
              onClick={saveModalOpen}
              disabled={isButtonDisabled}
              // loading={loading}
              // loadingposition='start'
              {...(loading ? {} : {})}
            >
              Approve
            </Button>
          )}
          {permissions?.nextBtn && (
            <Button
              variant='contained'
              className='btn-save'
              onClick={() => {
                // Write any additional logic here before navigating.
                // console.log('Navigating to dashboard')
                // navigate('/user-form')
                handleAddPlantSite()
              }}
              disabled={isButtonDisabled}
              loading={loading} // Use the loading prop to trigger loading state
              loadingposition='start' // Use loadingPosition to control where the spinner appears
            >
              Next
            </Button>
          )}
          {showDeleteAll && (
            <Button
              variant='contained'
              className='btn-save'
              onClick={handleDeleteSelected}
              disabled={isButtonDisabled}
              loading={loading} // Use the loading prop to trigger loading state
              loadingposition='start' // Use loadingPosition to control where the spinner appears
            >
              Delete
            </Button>
          )}
        </Box>
      )}

      <Notification
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
      />

      <Dialog
        open={openDeleteDialogeBox}
        onClose={() => setOpenDeleteDialogeBox(false)}
        aria-labelledby='alert-dialog-title'
        aria-describedby='alert-dialog-description'
      >
        <DialogTitle id='alert-dialog-title'>{'Delete ?'}</DialogTitle>
        <DialogContent>
          <DialogContentText id='alert-dialog-description'>
            Are you sure you want to delete this row?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDeleteDialogeBox(false)}>Cancel</Button>
          <Button onClick={deleteTheRecord} autoFocus>
            Delete
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={openSaveDialogeBox}
        onClose={closeSaveDialogeBox}
        aria-labelledby='alert-dialog-title'
        aria-describedby='alert-dialog-description'
      >
        <DialogTitle id='alert-dialog-title'>{'Save ?'}</DialogTitle>
        <DialogContent>
          <DialogContentText id='alert-dialog-description'>
            Are you sure you want to save these changes?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeSaveDialogeBox}>Cancel</Button>
          <Button onClick={saveConfirmation} autoFocus>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={!!remarkDialogOpen}
        onClose={() => setRemarkDialogOpen(false)}
      >
        <DialogTitle>Add Remark</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin='dense'
            id='remark'
            label='Remark'
            type='text'
            fullWidth
            variant='outlined'
            sx={{ width: '100%', minWidth: '600px' }}
            value={currentRemark || ''}
            // value={remark}
            onChange={(e) => setCurrentRemark(e.target.value)}
            multiline
            rows={8}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRemarkDialogOpen(false)}>Cancel</Button>
          {/* <Button onClick={handleCloseRemark}>Cancel</Button> */}
          <Button onClick={handleRemarkSave} disabled={!currentRemark?.trim()}>
            Add
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  )
}

export default KendoDataTables
