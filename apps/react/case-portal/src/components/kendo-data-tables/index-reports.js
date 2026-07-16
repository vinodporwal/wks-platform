import '@progress/kendo-font-icons/dist/index.css'
import {
  Grid,
  GridColumn,
  isColumnMenuFilterActive,
  isColumnMenuSortActive,
} from '@progress/kendo-react-grid'
import '@progress/kendo-theme-default/dist/all.css'
import { getColumnMenuCheckboxFilter } from 'components/data-tables/Reports/ColumnMenu1'
import Notification from 'components/Utilities/Notification'
import React, { useCallback, useState, useEffect, useRef, useMemo } from 'react'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  MenuItem,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  TextField,
  Typography,
  Collapse,
} from '../../../node_modules/@mui/material/index'
import { styled } from '@mui/material/styles'
import { SvgIcon } from '../../../node_modules/@progress/kendo-react-common/index'

import { trashIcon } from '../../../node_modules/@progress/kendo-svg-icons/dist/index'
import '../../kendo-data-grid.css'
import { NoSpinnerNumericEditor } from './Utilities-Kendo/numbericColumns'
import { Tooltip } from '../../../node_modules/@progress/kendo-react-tooltip/index'
import {
  DurationDisplayWithTooltipCell,
  DurationEditor,
} from './Utilities-Kendo/numericViewCells'
import {
  recalcDuration,
  recalcEndDate,
} from './Utilities-Kendo/durationHelpers'
import DateOnlyPicker from './Utilities-Kendo/DatePicker'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import { useSelector } from 'react-redux'

import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import {
  FileExportIcon,
  FileImportIcon,
  SaveIcon,
  CalculateIcon,
} from 'assets/images/icons'

const CompactDialog = styled(Dialog)(({ theme }) => ({
  '& .MuiPaper-root': {
    borderRadius: '12px',
    width: '600px',
    boxShadow: '0 12px 40px rgba(0,0,0,0.15)',
  },
}))

const CompactTextField = styled(TextField)({
  '& .MuiOutlinedInput-root': {
    fontSize: '0.85rem',
    backgroundColor: '#fff',
    '& fieldset': { borderColor: '#e2e8f0' },
    '&:hover fieldset': { borderColor: '#cbd5e1' },
    '&.Mui-focused fieldset': { borderColor: '#0100cb' },
  },
})

export const particulars = [
  'normParameterId',
  'normParametersFKId',
  'NormParameterFKId',
  'materialFkId',
  'normParameterFKId',
]
export const hiddenFields = [
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
export const dateFields = [
  'maintStartDateTime',
  'maintEndDateTime',
  'endDateTA',
  'startDateTA',
  'endDateSD',
  'startDateSD',
  'endDateIBR',
  'startDateIBR',
  'toDate',
  'fromDate',
  'tentativeMonth',
  'ibrDueDate',
  'shutdownDate',
  'startupDate',
]
export const monthMap = {
  january: 1,
  february: 2,
  march: 3,
  april: 4,
  may: 5,
  june: 6,
  july: 7,
  august: 8,
  september: 9,
  october: 10,
  november: 11,
  december: 12,
}
const KendoDataTablesReports = ({
  allRedCell = [],
  modifiedCells = [],
  title = '',
  rows = [],
  setRows,
  columns,
  loading = false,
  permissions = {},
  setSnackbarOpen = () => { },
  snackbarData = { message: '', severity: 'info' },
  snackbarOpen = false,
  setRemarkDialogOpen = () => { },
  currentRemark = '',
  setCurrentRemark = () => { },
  currentRowId = null,
  setModifiedCells = () => { },
  remarkDialogOpen = false,
  saveChanges = () => { },
  fetchData = () => { },
  deleteRowData = () => { },
  handleCalculate = () => { },
  handleUnitChange = () => { },
  handleRemarkCellClick = () => { },
  handleExport = () => { },
  handleExcelUpload = () => { },
  groupBy = null,
  grades = [],
  handleGradeChange = () => { },
  handleRelease = () => { },
  isReleaseDisabled = true,
  supressGridHeight = false,
  isProposedAOP = false,
}) => {
  const grid = React.useRef(null)
  const minGridWidth = useRef(0)
  const [filter, setFilter] = useState({ logic: 'and', filters: [] })
  const [openDeleteDialogeBox, setOpenDeleteDialogeBox] = useState(false)
  const [isButtonDisabled, setIsButtonDisabled] = useState(false)
  const [openSaveDialogeBox, setOpenSaveDialogeBox] = useState(false)
  const [paramsForDelete, setParamsForDelete] = useState([])
  const closeSaveDialogeBox = () => setOpenSaveDialogeBox(false)
  const [edit, setEdit] = useState({})
  const [sort, setSort] = useState([])
  const [issRowEdited, setIsRowEdited] = useState(false)
  const [gridExpanded, setGridExpanded] = useState(true)
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const [selectedGrade, setSelectedGrade] = useState()
  const [applyMinWidth, setApplyMinWidth] = useState(false)
  const [gridCurrent, setGridCurrent] = useState(0)

  const keycloak = useSession()
  const { verticalChange, plantObject, oldYear } = dataGridStore
  const IS_OLD_YEAR = oldYear?.oldYear
  const plantID = plantObject?.id
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const ADJUST_PADDING = 4
  const COLUMN_MIN = 4

  React.useEffect(() => {
    grid.current = document.querySelector('.k-grid')
    window.addEventListener('resize', handleResize)
    minGridWidth.current = columns
      .filter((col) => col?.isVisible !== false)
      .reduce((sum, col) => sum + (col?.minWidth || 100), 0)
    setGridCurrent(grid.current.offsetWidth)
    setApplyMinWidth(grid.current.offsetWidth < minGridWidth.current)
  }, [])

  const handleResize = () => {
    if (grid.current.offsetWidth < minGridWidth.current && !applyMinWidth) {
      setApplyMinWidth(true)
    } else if (grid.current.offsetWidth > minGridWidth.current) {
      setGridCurrent(grid.current.offsetWidth)
      setApplyMinWidth(false)
    }
  }

  const setWidth = (minWidth = 0) => {
    const visibleCols = columns.filter((col) => col?.isVisible !== false)

    const totalMinWidth = visibleCols.reduce(
      (sum, col) => sum + (col.minWidth || 0),
      0,
    )

    // 🔥 Decide behavior based on available space
    const hasExtraSpace = gridCurrent > totalMinWidth

    let width

    if (!hasExtraSpace) {
      // ✅ Not enough space → respect minWidth → enable scroll
      width = minWidth
    } else {
      // ✅ Extra space → distribute nicely (but controlled)
      const extraPerCol = (gridCurrent - totalMinWidth) / visibleCols.length

      // 🔥 limit expansion so it doesn't look ugly
      const MAX_GROWTH = 80 // tweak if needed

      width = minWidth + Math.min(extraPerCol, MAX_GROWTH)
    }

    // optional padding adjustment
    if (width >= COLUMN_MIN) {
      width -= ADJUST_PADDING
    }

    return Math.max(minWidth, width)
  }

  const TextCellEditor = (props) => (
    <td>
      <input
        type='text'
        className='k-textbox text-cell-editor-input'
        value={props.dataItem[props.field] || ''}
        onChange={(e) =>
          props.onChange({
            dataItem: props.dataItem,
            field: props.field,
            value: e.target.value,
          })
        }
        autoFocus
      />
    </td>
  )
  const initialGroup = groupBy
    ? [
      {
        field: groupBy,
        dir: undefined,
      },
    ]
    : []

  const handleEditChange = useCallback((e) => {
    setEdit(e.edit)
    // }
  }, [])

  const handleRowClick = (e) => {
    if (!e.dataItem?.isEditable && e.dataItem?.isEditable !== undefined) {
      setEdit({})
      return
    }

    setRows(
      rows.map((r) => ({
        ...r,
        inEdit: r.id === e.dataItem.id,
      })),
    )
  }
  const itemChange = useCallback(
    (e) => {


      const { dataItem, field, value } = e
      const itemId = dataItem.id


      // Ignore group header expand/collapse events — they are not real edits
      if (!field || dataItem?.items) {
        return
      }

      setIsRowEdited(true)


      setRows((prev) =>
        prev.map((r) => {
          if (r.id !== itemId) return r
          const updated = { ...r, [field]: value }

          if (
            'fromDate' in updated &&
            'toDate' in updated &&
            'durationInHrs' in updated
          ) {
            if (field === 'fromDate' || field === 'toDate') {
              updated.durationInHrs = recalcDuration(
                updated.fromDate,
                updated.toDate,
              )
            } else if (field === 'durationInHrs') {
              const newEnd = recalcEndDate(
                updated.fromDate,
                value, // string like “10.20”
              )
              if (newEnd) {
                updated.toDate = newEnd
              }
            }
          }
          return updated
        }),
      )

      setModifiedCells((prev) => {
        const base = { ...dataItem, [field]: value }
        if ('fromDate' in base && 'toDate' in base && 'durationInHrs' in base) {
          if (field === 'fromDate' || field === 'toDate') {
            base.durationInHrs = recalcDuration(base.fromDate, base.toDate)
          } else if (field === 'durationInHrs') {
            const newEnd = recalcEndDate(base.fromDate, value)
            if (newEnd) base.toDate = newEnd.toISOString()
          }
        }
        return { ...prev, [itemId]: base }
      })
    },
    [setRows, setModifiedCells],
  )

  const handleRemarkSave = () => {
    setRows((prevRows) => {
      let updatedRow = null
      let keyToUpdate = ''
      const updatedRows = prevRows.map((row) => {
        if (row.id === currentRowId) {
          const keysToUpdate = [
            'aopRemarks',
            'remarks',
            'remark',
            'Remark',
          ].filter((key) => key in row)
          keyToUpdate = keysToUpdate[0] || 'remark'
          updatedRow = { ...row, [keyToUpdate]: currentRemark, inEdit: true }
          return updatedRow
        }
        return row
      })
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

  const toggleGrid = () => {
    setGridExpanded(!gridExpanded)
  }

  const fileInputRef = useRef(null)
  const triggerFileUpload = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click()
    }
  }

  const onFileChange = (event) => {
    const file = event.target.files[0]
    if (!file) return

    handleExcelUpload(file)
    event.target.value = ''
  }
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

    setTimeout(() => {
      setIsButtonDisabled(false)
    }, 500)
  }

  const saveConfirmation = async () => {
    saveChanges()
    setOpenSaveDialogeBox(false)
    setEdit({})
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
      <td className='center-cell'>
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

  const RemarkCell = (props) => {
    const { dataItem, field, onRemarkClick, ...tdProps } = props
    const rawValue = dataItem[field]
    const displayText = String(rawValue ?? '')
    const isDisabled = READ_ONLY

    return (
      <td
        {...tdProps}
        className='remark-cell'
        style={{
          color: rawValue ? 'inherit' : 'gray',
          background: isDisabled ? '#f1f5f9' : undefined,
          fontFamily: 'Honeywell Sans Web, Inter, sans-serif',
          fontSize: '15px',
          fontWeight: 500,
        }}
        onClick={(e) => {
          e.preventDefault()
          e.stopPropagation()
        }}
        onDoubleClick={(e) => {
          e.preventDefault()
          e.stopPropagation()
          if (!isDisabled) {
            onRemarkClick(dataItem)
            setEdit?.({})
          }
        }}
      >
        {displayText || 'Add remark'}
      </td>
    )
  }

  const CustomRow = useCallback(
    ({ dataItem, className, ...rest }) => {
      const isDisabled =
        READ_ONLY ||
        (!dataItem.isEditable && dataItem?.isEditable !== undefined)

      const dataIndex = (isProposedAOP && rows) ? rows.indexOf(dataItem) : -1
      const isEvenRow = dataIndex !== -1 && dataIndex % 2 === 1

      const rowClassName = [
        className,
        isEvenRow ? 'k-alt' : '',
        isDisabled ? 'custom-disabled-row' : '',
        dataItem.isBold ? 'custom-bold-row' : '',
      ]
        .filter(Boolean)
        .join(' ')
      return (
        <tr {...rest?.trProps} className={rowClassName}>
          {rest.children}
        </tr>
      )
    },
    [IS_OLD_YEAR, READ_ONLY, rows, isProposedAOP],
  )
  useEffect(() => {
    if (!permissions?.showG || !grades?.length) return
    setSelectedGrade((prev) => {
      if (prev) {
        return prev
      }
      const firstGrade = grades[0]

      handleGradeChange(
        firstGrade.gradeId,
        firstGrade?.displayName,
        firstGrade?.name,
      )
      return firstGrade.gradeId
    })
  }, [grades, permissions?.showG])

  useEffect(() => {
    setSelectedGrade(null)
  }, [plantID])

  const SimpleHeaderWithTooltip = (props) => {
    const { ariaSort, ...restThProps } = props.thProps || {}

    return (
      <th
        {...restThProps}
        aria-sort={ariaSort}
        title={props.title}
        style={{
          ...restThProps?.style,
          fontFamily: "'Honeywell Sans Web', 'Inter', Arial, sans-serif",
        }}
      >
        <Tooltip
          position='top'
          anchorElement='target'
          parentTitle={true}
          className='test'
        >
          {props.children}
        </Tooltip>
      </th>
    )
  }

  const ColumnMenuCheckboxFilter = getColumnMenuCheckboxFilter(rows)

  const isColumnActive = (field, filter, sort) => {
    return (
      isColumnMenuFilterActive(field, filter) ||
      isColumnMenuSortActive(field, sort)
    )
  }

  const renderColumns = (cols, filter, sort) =>
    cols.map((col, idx) => {
      const isEditable = col.editable === true
      const isActive = isColumnActive(col.field, filter, sort)

      // console.log('col', col)

      const headerColorClass = undefined

      const budgetDividerClass =
        col.parent === 'Procurment Budget' && idx === 0 ? 'budget-divider' : ''

      if (col.children) {
        return (
          <GridColumn
            locked={col.locked || false}
            key={col.title || idx}
            title={col.title}
            // headerClassName={`center-group-header ${isActive ? 'active-column' : ''} ${headerColorClass} ${budgetDividerClass}`}
            headerClassName='center-group-header'
          >
            {renderColumns(col.children, filter, sort)}
          </GridColumn>
        )
      }

      if (['aopRemarks', 'remarks', 'remark', 'Remark'].includes(col.field)) {
        return (
          <GridColumn
            locked={col.locked || false}
            key={col.field}
            field={col.field}
            title={col.title || col.headerName}
            // width={col.fixedWidth || undefined}
            width={setWidth(
              col?.fixedWidth || col?.minWidth || col?.widthT || 200,
            )}
            cells={{
              data: (cellProps) => (
                <RemarkCell
                  {...cellProps}
                  onRemarkClick={handleRemarkCellClick}
                />
              ),
            }}
            columnMenu={ColumnMenuCheckboxFilter}
            headerClassName={isActive ? 'active-column' : ''}
          />
        )
      }
      if (col.field === 'particular' || col.type === 'text') {
        return (
          <GridColumn
            locked={col.locked || false}
            key={col.field}
            field={col.field}
            title={col.title || col.headerName}
            editable={col.editable || false}
            cells={{
              edit: { text: TextCellEditor }, // <-- Use text editor for particulars
              data: toolTipRenderer,
              headerCell: SimpleHeaderWithTooltip,
            }}
            className={!isEditable ? 'non-editable-cell' : ''}
            columnMenu={ColumnMenuCheckboxFilter}
            headerClassName={isActive ? 'active-column' : ''}
            width={setWidth(col?.widthT || col?.fixedWidth || col?.minWidth)}
          />
        )
      }
      if (dateFields.includes(col.field)) {
        return (
          <GridColumn
            locked={col.locked || false}
            key={col.field}
            field={col.field}
            title={col.title || col.headerName}
            cells={{
              edit: { date: DateOnlyPicker },
              data: toolTipRenderer,
              headerCell: SimpleHeaderWithTooltip,
            }}
            format='{0:dd-MM-yyyy}'
            editor='date'
            editable={col.editable || false}
            hidden={col.hidden}
            className={!isEditable ? 'non-editable-cell' : ''}
            width={setWidth(col?.widthT || col?.fixedWidth || col?.minWidth)}
          />
        )
      }
      if (col.field.includes('durationInHrs')) {
        return (
          <GridColumn
            locked={col.locked || false}
            key={col.field}
            field={col.field}
            title={col.title || col.headerName}
            editable={col.editable || false}
            columnMenu={ColumnMenuCheckboxFilter}
            hidden={col.hidden}
            format={'{0:n2}'}
            className={!isEditable ? 'non-editable-cell' : ''}
            width={setWidth(col?.widthT || col?.fixedWidth || col?.minWidth)}
            cells={{
              edit: { text: DurationEditor },
              data: DurationDisplayWithTooltipCell,
            }}
          />
        )
      }
      if (col.type === 'numberNonGrey') {
        return (
          <GridColumn
            locked={col.locked || false}
            key={col.field}
            field={col.field}
            title={col.title || col.headerName}
            hidden={col.hidden}
            className={'k-number-right'}
            editable={col?.editable ? true : false}
            headerClassName={isActive ? 'active-column' : ''}
            cells={{
              edit: { text: NoSpinnerNumericEditor },
              data: toolTipRenderer,
              headerCell: SimpleHeaderWithTooltip,
            }}
            columnMenu={ColumnMenuCheckboxFilter}
            filter='numeric'
            format={col.format}
            width={setWidth(
              col?.widthT || col?.fixedWidth || col?.minWidth || 130,
            )}
          />
        )
      }

      if (col.type === 'number') {
        return (
          <GridColumn
            locked={col.locked || false}
            key={col.field}
            field={col.field}
            title={col.title || col.headerName}
            width={setWidth(
              col?.fixedWidth ||
              col?.width ||
              col?.widthT ||
              col?.minWidth ||
              130,
            )}
            hidden={col.hidden}
            className={'k-number-right-disabled'}
            editable={col?.editable ? true : false}
            headerClassName={isActive ? 'active-column' : ''}
            cells={{
              edit: { text: NoSpinnerNumericEditor },
              data: toolTipRenderer,
              headerCell: SimpleHeaderWithTooltip,
            }}
            columnMenu={ColumnMenuCheckboxFilter}
            filter='numeric'
            format={col.format}
          />
        )
      }

      if (col.type === 'number1') {
        return (
          <GridColumn
            locked={col.locked || false}
            key={col.field}
            field={col.field}
            title={col.title || col.headerName}
            hidden={col.hidden}
            editable={col?.editable ? true : false}
            className={!col?.editable ? 'k-right-disabled' : undefined}
            headerClassName={`${isActive ? 'active-column' : ''} ${headerColorClass}`}
            cells={{
              edit: { text: NoSpinnerNumericEditor },
              data: toolTipRenderer,
              headerCell: SimpleHeaderWithTooltip,
            }}
            columnMenu={ColumnMenuCheckboxFilter}
            filter='numeric'
            format={col.format}
            width={setWidth(col?.widthT || col?.fixedWidth || col?.minWidth)}
          />
        )
      }

      return (
        <GridColumn
          locked={col.locked || false}
          key={col.field}
          field={col.field}
          title={col.title || col.headerName}
          editable={col.editable || false}
          format={col.format}
          cells={{
            edit: { text: NoSpinnerNumericEditor },
            data: toolTipRenderer,
            headerCell: SimpleHeaderWithTooltip,
          }}
          className={!isEditable ? 'non-editable-cell' : ''}
          columnMenu={ColumnMenuCheckboxFilter}
          headerClassName={isActive ? 'active-column' : ''}
          width={setWidth(col?.widthT || col?.fixedWidth || col?.minWidth)}
        />
      )
    })

  const toolTipRenderer = (props) => {
    const value = props.dataItem[props.field]
    const month = monthMap[props.field?.toLowerCase()]
    const normId = props.dataItem.materialFkId

    const isRedFromAllRedCell = allRedCell.some(
      (cell) =>
        cell.month === month &&
        cell.normParameterFKId?.toLowerCase() === normId?.toLowerCase(),
    )

    const isRed = isRedFromAllRedCell

    return (
      <td
        {...props.tdProps}
        title={value}
        //className={isRed ? 'orange-text' : ''}
        className={`${props.tdProps?.className || ''} ${isRed ? 'edited-cell' : ''}`.trim()}
      >
        {props.children}
      </td>
    )
  }

  const rowHeightVH = 5 // each row ~4vh
  const headerVH = 10 // grid’s own header/filter area
  const pageHeaderVH = 20 // top app bar + stepper + controls
  const maxVH = 60 // cap grid height

  const calculatedVH = React.useMemo(() => {
    if (!rows || rows?.length === 0) return 20
    const needed = rows?.length * rowHeightVH + headerVH
    const available = 100 - pageHeaderVH
    return Math.round(Math.min(needed, maxVH, available))
  }, [rows?.length])

  return (
    <div className='k-table-box'>
      <LoaderBackdrop open={!!loading} />

      {(permissions?.allAction ?? true) && (
        <Box className='action-box'>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              width: '100%',
              ...(permissions?.marginTop && { marginTop: '10px' }),
            }}
          >
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                paddingBottom: 0.25,
              }}
            >
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: 32,
                  height: 32,
                  borderRadius: '6px',
                  backgroundColor: '#ECEEFF',
                  color: '#1e293b',
                  ml: 1,
                  cursor: 'pointer',
                  padding: '8px',
                }}
                onClick={toggleGrid}
              >
                <KeyboardArrowUpIcon
                  sx={{
                    fontSize: 20,
                    transition: '0.2s',
                    transform: gridExpanded ? 'rotate(0deg)' : 'rotate(180deg)',
                  }}
                />
              </Box>
              {permissions?.showTitle && (
                <Typography
                  component='div'
                  className='grid-title'
                  style={{ whiteSpace: 'pre-line' }}
                >
                  {title}
                </Typography>
              )}
              {permissions?.showG && (
                <TextField
                  select
                  value={selectedGrade || ''}
                  onChange={(e) => {
                    const selectedGradeId = e.target.value
                    const selectedGradeObj = grades.find(
                      (g) => g.gradeId === selectedGradeId,
                    )
                    setSelectedGrade(selectedGradeId)
                    handleGradeChange(
                      selectedGradeObj?.gradeId,
                      selectedGradeObj?.displayName,
                      selectedGradeObj?.name,
                    )
                  }}
                  variant='outlined'
                  size='small'
                  className='custom-select-textfield'
                  InputProps={{
                    startAdornment: (
                      <Typography
                        variant='caption'
                        sx={{
                          mr: 0.5,
                          color: '#606060',
                          fontWeight: 500,
                          fontSize: '14px',
                          textTransform: 'uppercase',
                          letterSpacing: '0.4px',
                          lineHeight: 1,
                          fontFamily:
                            "'Honeywell Sans Web', 'Inter', sans-serif",
                        }}
                      >
                        {permissions?.dropdownLabel || 'Grade'}:
                      </Typography>
                    ),
                  }}
                  SelectProps={{
                    MenuProps: {
                      disableScrollLock: true,
                    },
                  }}
                >
                  <MenuItem value='' disabled className='menu-item-style'>
                    {permissions?.dropdownLabel || 'Select'}
                  </MenuItem>

                  {grades?.map((unit) => (
                    <MenuItem
                      key={unit.gradeId}
                      value={unit.gradeId}
                      className='menu-item-style'
                    >
                      {unit.displayName}
                    </MenuItem>
                  ))}
                </TextField>
              )}
              <Box
                sx={{
                  p: '4px 8px',
                  borderRadius: '100px',
                  backgroundColor: '#ECEEFF',
                  border: '1px solid #41424D',
                }}
              >
                <Typography
                  sx={{
                    fontSize: '12px',
                    fontWeight: 700,
                    color: '#41424D',
                    fontFamily: "'Honeywell Cond Web',  'Inter', sans-serif",
                  }}
                >
                  {rows?.length || 0} {rows?.length === 1 ? 'Row' : 'Rows'}
                </Typography>
              </Box>
            </Box>

            {/* RIGHT: Buttons */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              {permissions?.addButton && (
                <Button
                  variant='contained'
                  className='btn-add'
                  onClick={handleAddRow}
                  disabled={READ_ONLY}
                  startIcon={<AddIcon sx={{ color: '#4A4DDA !important' }} />}
                >
                  Add Item
                </Button>
              )}
              {permissions?.showExport && (
                <Button
                  variant='contained'
                  onClick={handleExport}
                  className='btn-export'
                  startIcon={
                    <Box
                      component='img'
                      src={FileExportIcon}
                      className='w16-icon'
                    />
                  }
                >
                  Export
                </Button>
              )}

              {permissions?.showImport && (
                <Button
                  variant='contained'
                  onClick={handleExcelUpload}
                  startIcon={
                    <Box
                      component='img'
                      src={FileImportIcon}
                      className='w16-icon'
                    />
                  }
                  disabled={isButtonDisabled || READ_ONLY}
                  className='btn-import'
                >
                  Import
                </Button>
              )}
              {permissions?.uploadExcelBtn && (
                <>
                  <Button
                    variant='contained'
                    onClick={triggerFileUpload}
                    startIcon={
                      <Box
                        component='img'
                        src={FileImportIcon}
                        className='w16-icon'
                      />
                    }
                    disabled={isButtonDisabled || READ_ONLY}
                    className='btn-save'
                  >
                    Import
                  </Button>

                  <input
                    type='file'
                    accept='.xlsx,.xls'
                    onChange={onFileChange}
                    ref={fileInputRef}
                    style={{ display: 'none' }}
                  />
                </>
              )}
              {permissions?.saveBtn && (
                <Button
                  variant='contained'
                  className='btn-save'
                  onClick={saveModalOpen}
                  disabled={isButtonDisabled || READ_ONLY}
                  startIcon={
                    <Box component='img' src={SaveIcon} className='w16-icon' />
                  }
                  {...(loading ? {} : {})}
                >
                  Save
                </Button>
              )}

              {permissions?.showCalculate && (
                <Button
                  variant='contained'
                  startIcon={
                    <Box
                      component='img'
                      src={CalculateIcon}
                      className='w16-icon'
                    />
                  }
                  onClick={handleCalculateBtn}
                  // disabled={isButtonDisabled || READ_ONLY}
                  className='btn-calculate'
                  disabled={
                    READ_ONLY ||
                    (rows?.length === 0
                      ? false
                      : isButtonDisabled ||
                      !permissions?.showCalculateVisibility)
                  }
                >
                  Calculate
                </Button>
              )}

              {permissions?.showFinalSubmit && (
                <Button
                  variant='contained'
                  onClick={handleRelease}
                  disabled={isReleaseDisabled || READ_ONLY}
                  className='btn-save'
                  sx={{ color: '#bfa161ff' }}
                  startIcon={
                    <TrendingUpIcon sx={{ fontSize: 16, color: '#bfa161ff' }} />
                  }
                >
                  {/* Submit */}
                  Release
                </Button>
              )}

              {/* {permissions?.showWorkFlowBtns && (
                      <Stack direction='row' spacing={1} alignItems='center'>
                        {taskId && (
                          <Button
                            variant='contained'
                            onClick={handleRejectClick}
                            disabled={isButtonDisabled|| READ_ONLY}
                          >
                            Accept
                          </Button>
                        )}
                        <Button variant='outlined'                           
                        disabled={isButtonDisabled|| READ_ONLY}
                        onClick={handleAuditOpen}>
                          Audit Trail
                        </Button>
                      </Stack>
                    )} */}
            </Box>
          </Box>
        </Box>
      )}

      <Collapse in={gridExpanded}>
        <div className='kendo-data-grid'>
          <Tooltip openDelay={50} position='auto' anchorElement='target'>
            <Grid
              style={{
                flex: 1,
                overflow: 'auto',
                height:
                  supressGridHeight == true
                    ? undefined
                    : rows?.length > 10
                      ? `${calculatedVH}vh`
                      : undefined,
              }}
              modifiedCells={modifiedCells}
              data={rows}
              rows={{ data: CustomRow }}
              sortable={{
                mode: 'multiple',
              }}
              autoProcessData={true}
              {...(initialGroup.length > 0
                ? { defaultGroup: initialGroup }
                : {})}
              dataItemKey='id'
              editField='inEdit'
              editable={{ mode: 'incell' }}
              onEditChange={handleEditChange}
              edit={edit}
              filter={filter}
              onFilterChange={(e) => setFilter(e.filter)}
              onItemChange={itemChange}
              resizable={true}
              defaultSkip={0}
              defaultTake={100}
              contextMenu={true}
              filterable={columns.some((col) => dateFields.includes(col.field))}
              size='small'
              pageable={
                rows?.length > 100
                  ? {
                    buttonCount: 4,
                    pageSizes: [10, 50, 100],
                  }
                  : false
              }

              onRowClick={handleRowClick}
              lockGroups={true}
            >
              {renderColumns(
                columns.filter((col) => !hiddenFields.includes(col.field)),
                filter,
                sort,
              )}

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
          </Tooltip>
        </div>
      </Collapse>

      <Notification
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
      />

      <CompactDialog
        open={openDeleteDialogeBox}
        onClose={() => setOpenDeleteDialogeBox(false)}
        disableScrollLock
        slotProps={{ backdrop: { disableScrollLock: true } }}
      >
        {/* Header */}
        <DialogTitle
          sx={{
            p: 1.5,
            px: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            bgcolor: '#fef2f2',
            borderBottom: '1px solid #fee2e2',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <DeleteOutlineIcon sx={{ fontSize: '1rem', color: '#dc2626' }} />
            <Typography
              sx={{
                fontWeight: 800,
                fontSize: '0.8rem',
                color: '#7f1d1d',
                letterSpacing: '0.4px',
              }}
            >
              CONFIRM DELETE
            </Typography>
          </Box>

          <IconButton
            size='small'
            onClick={() => setOpenDeleteDialogeBox(false)}
            sx={{ color: '#7f1d1d' }}
          >
            <CloseIcon fontSize='small' />
          </IconButton>
        </DialogTitle>

        {/* Content */}
        <DialogContent sx={{ p: 1.5, pt: '12px !important' }}>
          <Typography
            sx={{
              fontSize: '0.75rem',
              color: '#7f1d1d',
              lineHeight: 1.5,
              fontWeight: 600,
            }}
          >
            Are you sure you want to delete this row?
          </Typography>

          <Typography
            sx={{
              mt: 1,
              fontSize: '0.7rem',
              color: '#991b1b',
              fontWeight: 600,
            }}
          >
            This action cannot be undone.
          </Typography>
        </DialogContent>

        {/* Actions */}
        <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
          <Button
            onClick={() => setOpenDeleteDialogeBox(false)}
            className='btn-no'
          >
            Cancel
          </Button>

          <Button
            onClick={deleteTheRecord}
            variant='contained'
            size='small'
            disabled={READ_ONLY}
            className='btn-yes'
          >
            Delete
          </Button>
        </DialogActions>
      </CompactDialog>

      <CompactDialog
        open={openSaveDialogeBox}
        onClose={closeSaveDialogeBox}
        disableScrollLock
        slotProps={{ backdrop: { disableScrollLock: true } }}
      >
        {/* Header */}
        <DialogTitle
          sx={{
            p: 1.5,
            px: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            bgcolor: '#f8fafc',
            borderBottom: '1px solid #e2e8f0',
          }}
        >
          <Typography
            sx={{
              fontWeight: 800,
              fontSize: '0.8rem',
              color: '#334155',
              letterSpacing: '0.5px',
            }}
          >
            CONFIRM SAVE
          </Typography>

          <IconButton
            size='small'
            onClick={closeSaveDialogeBox}
            sx={{ color: '#64748b' }}
          >
            <CloseIcon fontSize='small' />
          </IconButton>
        </DialogTitle>

        {/* Content */}
        <DialogContent sx={{ p: 1.5, pt: '12px !important' }}>
          <Typography
            sx={{
              fontSize: '0.75rem',
              color: '#475569',
              lineHeight: 1.5,
              fontWeight: 500,
            }}
          >
            Are you sure you want to save these changes?
          </Typography>
        </DialogContent>

        {/* Actions */}
        <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
          <Button onClick={closeSaveDialogeBox} className='btn-no'>
            Cancel
          </Button>

          <Button
            onClick={saveConfirmation}
            variant='contained'
            size='small'
            autoFocus
            className='btn-yes'
          >
            Save
          </Button>
        </DialogActions>
      </CompactDialog>

      <CompactDialog
        open={!!remarkDialogOpen}
        onClose={() => setRemarkDialogOpen(false)}
        disableScrollLock
        slotProps={{ backdrop: { disableScrollLock: true } }}
      >
        {/* Compact Header */}
        <DialogTitle
          sx={{
            p: 1.5,
            px: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            bgcolor: '#f8fafc',
            borderBottom: '1px solid #e2e8f0',
          }}
        >
          <Typography
            sx={{
              fontWeight: 800,
              fontSize: '0.8rem',
              color: '#334155',
              letterSpacing: '0.5px',
            }}
          >
            ADD REMARK
          </Typography>
          <IconButton
            size='small'
            onClick={() => setRemarkDialogOpen(false)}
            sx={{ color: '#64748b' }}
          >
            <CloseIcon fontSize='small' />
          </IconButton>
        </DialogTitle>

        <DialogContent sx={{ p: 1.5, pt: '12px !important' }}>
          <CompactTextField
            autoFocus
            placeholder='Type your remarks here...'
            fullWidth
            multiline
            rows={6}
            value={currentRemark || ''}
            disabled={READ_ONLY}
            onChange={(e) => setCurrentRemark(e.target.value)}
            onKeyDown={(e) => {
              if (
                e.ctrlKey &&
                e.key === 'Enter' &&
                currentRemark?.trim() &&
                !READ_ONLY
              ) {
                handleRemarkSave()
              }
            }}
          />
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 0.5 }}>
            <Typography
              variant='caption'
              sx={{
                fontSize: '0.65rem',
                color: 'text.disabled',
                fontWeight: 600,
              }}
            >
              {currentRemark?.length || 0} characters | Ctrl+Enter to save
            </Typography>
          </Box>
        </DialogContent>

        <DialogActions className='compact-dialog-actions'>
          <Button onClick={() => setRemarkDialogOpen(false)} className='btn-no'>
            Cancel
          </Button>
          <Button
            onClick={handleRemarkSave}
            variant='contained'
            size='small'
            disabled={READ_ONLY || !currentRemark?.trim()}
            className='btn-yes'
          >
            Add
          </Button>
        </DialogActions>
      </CompactDialog>
    </div>
  )
}

export default KendoDataTablesReports
