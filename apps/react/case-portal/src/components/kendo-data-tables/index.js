import HelpIcon from '@mui/icons-material/Help'
import InfoIcon from '@mui/icons-material/Info'
import { Divider, Tooltip as MuiTooltip } from '@mui/material'
import '@progress/kendo-font-icons/dist/index.css'
import { Grid, GridColumn } from '@progress/kendo-react-grid'
import { Tooltip } from '@progress/kendo-react-tooltip'
import { process } from '@progress/kendo-data-query'
import '@progress/kendo-theme-default/dist/all.css'
import { getColumnMenuCheckboxFilter } from 'components/data-tables/Reports-kendo/ColumnMenu1'
import { DateColumnMenu } from 'components/Utilities/DateColumnMenu'
import Notification from 'components/Utilities/Notification'
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import PropaneDropdown from './Utilities-Kendo/PropaneDropdown'
import RestartAltIcon from '@mui/icons-material/RestartAlt'
import PublishIcon from '@mui/icons-material/Publish'
import { useSelector } from 'react-redux'
import YearDropdownEditor from './Utilities-Kendo/YearDropdownEditor'
import CloseIcon from '@mui/icons-material/Close'
import SDDaysDropdownEditorWrapper from './Utilities-Kendo/SdDaysDropdownEditor'

import ModeEditIcon from '@mui/icons-material/ModeEdit'
import { styled } from '@mui/material/styles'

import AddIcon from '@mui/icons-material/Add'
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import CategoryDropdownEditor from './Utilities-Kendo/CategoryDropdown'
import LineDropdownEditor from './Utilities-Kendo/LineDropdownEditor'
import { useIntegerDaysEditor } from './Utilities-Kendo/NoSpinnerIntegerEditorForDays'
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  MenuItem,
  TextField,
  Typography,
} from '../../../node_modules/@mui/material/index'
import { SvgIcon } from '../../../node_modules/@progress/kendo-react-common/index'
import {
  ExcelExport,
  ExcelExportColumn,
} from '../../../node_modules/@progress/kendo-react-excel-export/index'
import {
  isColumnMenuFilterActive,
  isColumnMenuSortActive,
} from '../../../node_modules/@progress/kendo-react-grid/index'
import { Checkbox } from '../../../node_modules/@progress/kendo-react-inputs/index'
import { trashIcon } from '../../../node_modules/@progress/kendo-svg-icons/dist/index'
import { arrowRotateCcwIcon } from '../../../node_modules/@progress/kendo-svg-icons/dist/index'
import '../../kendo-data-grid.css'
import BudgetConstrainsCellEditor from './Utilities-Kendo/BudgetConstrainsCellEditor'
import DateOnlyPicker from './Utilities-Kendo/DatePicker'
import DateTimePickerEditor from './Utilities-Kendo/DatePickeronSelectedYr'
import DatePickerNoLimit from './Utilities-Kendo/DatePickerNoLimit'

import { descLimit } from './Utilities-Kendo/descLimit'
import {
  calculateMonthDuration,
  getMonthStartEndDate,
  recalcDuration,
  recalcEndDate,
} from './Utilities-Kendo/durationHelpers'
import LimitCellEditor from './Utilities-Kendo/LimitCellEditor'
import MonthCell from './Utilities-Kendo/MonthCell'
import MonthDropdownEditor from './Utilities-Kendo/MonthDropdownEditor'
import MonthDropdownPEPP from './Utilities-Kendo/MonthDropdownPEPP'
import { NoSpinnerNumericEditorNegative } from './Utilities-Kendo/negativeNumbericColumns'
import { NoSpinnerNumericEditor } from './Utilities-Kendo/numbericColumns'
import { DurationEditor } from './Utilities-Kendo/numericViewCells'
import ProductCell from './Utilities-Kendo/ProductCell'
import { RemarkCell } from './Utilities-Kendo/RemarkCell'
import { TextCellEditor } from './Utilities-Kendo/TextCellEditor'
import { NoSpinnerNumericEditorWithUOMValidation } from './Utilities-Kendo/numbericColumnsWithUOMValidation'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { getColumnMenuDateFilter } from 'components/data-tables/Reports-kendo/ColumnMenuDateFilter'
import DateTimePickerEditor24HourFormat from './Utilities-Kendo/DatePickeronSelectedYr24HourFomat'
import { NoSpinnerNumericEditorCrackerValidation } from './Utilities-Kendo/numbericColumnsCrackerValidation'
import DynamicDropdown from './Utilities-Kendo/DynamicDropdown'
import ShutdownRateDropdown from './Utilities-Kendo/ShutdownRateDropdown'
import MonthDropdownPEPP1 from './Utilities-Kendo/MonthDropdownPEPP1'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import Collapse from '@mui/material/Collapse'
import {
  FileExportIcon,
  FileImportIcon,
  SaveIcon,
  CalculateIcon,
} from 'assets/images/icons'
import { DashboardColors } from 'themes/colors'
import SwitchEditor from './Utilities-Kendo/SwitchEditor'
import { NoSpinnerNumericIntegerEditor } from './Utilities-Kendo/numbericIntegerColumns'
import DisabledUOM from './Utilities-Kendo/DisabledUOM'

// A stable editor component to prevent focus loss during table re-renders.
const ON_OFF_CONDITION = (dataItem) => dataItem?.UOM === 'ON/OFF'
const OnOffSwitchEditCell = (props) => {
  return (
    <SwitchEditor
      {...props}
      condition={ON_OFF_CONDITION}
      editable={props.column?.editable}
      isDisabled={props.column?.isDisabled}
    />
  )
}

export const dateFields = [
  'maintStartDateTime',
  'maintEndDateTime',
  'periodTo',
  'periodFrom',
  'toDateReport',
  'fromDateReport',
]
export const dateFields2 = ['fromDate', 'toDate']
export const dateFields1 = [
  'ibrSD',
  'ibrED',
  'taSD',
  'taED',
  'sdED',
  'sdSD',
  'targetDate',
  'exclusionEndDate',
  'exclusionStartDate',
  'shutdownDate',
  'StartDate',
  'EndDate',
  'date',
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

const KendoDataTables = ({
  resetEditSignal,
  setEditResetKey,
  showCatChemUtilityCheckbox = false,
  showCatChemUtilityCheckbox2 = false,
  screenType = 'slowdown',
  rows = [],
  plantID = null,
  grades = [],
  allRedCell = [],
  allRedCell2 = [],
  modifiedCells = [],
  setRows,
  columns,
  summaryEdited,
  revision,
  loading = false,
  supressGridHeight = false,
  typeRank = {},
  permissions = {},
  errorRows = new Set(),
  setSnackbarOpen = () => {},
  snackbarData = { message: '', severity: 'info', duration: 3000 },
  snackbarOpen = false,
  setRemarkDialogOpen = () => {},
  currentRemark = '',
  setCurrentRemark = () => {},
  currentRowId = null,
  NormParameterIdCell = () => {},
  setModifiedCells = () => {},
  remarkDialogOpen = false,
  handleDeleteSelected = (selectedItems) => {},
  saveChanges = () => {},
  deleteRowData = () => {},
  handleAddPlantSite = () => {},
  handleCalculate = () => {},
  handleLoad = () => {},
  fetchData = () => {},
  handleUnitChange = () => {},
  handleYearChange = () => {},
  handleGradeChange = () => {},
  handleRemarkCellClick = () => {},
  calculatebtnClicked = () => {},
  selectedUsers = [],
  groupBy = null,
  totalRowConfiguration = null,
  selectedUOM = 'MT/Month',
  selectedPackagingYear = 'Budget',
  note = '',
  titleName = '',
  gridName,
  onGlobalCheckboxChange,
  allProducts = [],
  allDescriptionDrpdwn = [],
  allMonths = [],
  selectMode,
  setSelectMode = () => {},
  handleExcelUpload = () => {},
  downloadExcelForConfiguration = () => {},
  onLoad = () => {},
  disableRedHighlight = false,
  showThreeColors = false,
  resetDataChanges = () => {},
  noteOnSaveDialogeBox = '',
  deleteNoteOnDeleteDialogeBox = '',
  shutdownMonths = [],
  slowdownMonths = [],
  sdDaysValues = [],
  allLines = [],
  startDate,
  endDate,
  mcuMaxCapValues = [],
  key = [],
  isReleaseDisabled = true,
  handleRelease = () => {},
  customItemChange = null,
  configType,
  isEditable = false,
  currentTabDisplayName,
}) => {
  const _export = useRef(null)

  const _grid = React.useRef(undefined)
  const minGridWidth = useRef(0)
  // Always-current rows ref — lets itemChange read the latest rows without
  // adding rows to its useCallback deps (avoids recreating the handler on every edit).
  const rowsRef = useRef(rows)
  useEffect(() => {
    rowsRef.current = rows
  }, [rows])
  const grid = React.useRef(null)
  const gridRef = useRef(null)
  const [gridExpanded, setGridExpanded] = useState(true)
  const [openDeleteDialogeBox, setOpenDeleteDialogeBox] = useState(false)
  const [openResetDialogeBox, setOpenResetDialogeBox] = useState(false)
  const [isButtonDisabled, setIsButtonDisabled] = useState(false)
  const [selectedUnit, setSelectedUnit] = useState(permissions?.units?.[0])
  const [selectedGrade, setSelectedGrade] = useState()
  const [openSaveDialogeBox, setOpenSaveDialogeBox] = useState(false)
  const [openResetDataDialogeBox, setOpenResetDataDialogeBox] = useState(false)
  const [paramsForDelete, setParamsForDelete] = useState([])
  const closeSaveDialogeBox = () => setOpenSaveDialogeBox(false)
  const closeResetDataDialogeBox = () => setOpenResetDataDialogeBox(false)
  const [edit, setEdit] = useState({})
  const [filter, setFilter] = useState({ logic: 'and', filters: [] })
  const [sort, setSort] = useState([])

  const processedRows = useMemo(() => {
    try {
      return process(rows, { filter, sort }).data
    } catch (err) {
      return rows
    }
  }, [rows, filter, sort])

  const GroupedColumnCell = (props) => {
    const { dataItem, field, tdProps } = props
    const value = dataItem[field]

    const gName = dataItem.groupName
    if (!gName) {
      return (
        <td
          {...tdProps}
          style={{
            ...tdProps?.style,
            textAlign: 'right',
          }}
        >
          {value !== null && value !== undefined ? value : ''}
        </td>
      )
    }

    const groupRows = processedRows.filter((r) => r.groupName === gName)
    const indexInGroup = groupRows.findIndex((r) => r.id === dataItem.id)

    if (indexInGroup > 0) {
      return (
        <td
          {...tdProps}
          style={{
            ...tdProps?.style,
            display: 'none',
          }}
        />
      )
    }

    const rowSpan = groupRows.length

    return (
      <td
        {...tdProps}
        rowSpan={rowSpan}
        style={{
          ...tdProps?.style,
          verticalAlign: 'middle',
          textAlign: 'right',
          backgroundColor: '#FFFFFF',
        }}
      >
        {value !== null && value !== undefined ? value : ''}
      </td>
    )
  }

  const [issRowEdited, setIsRowEdited] = useState(false)
  const [isDateFilterActive, setIsDateFilterActive] = useState([])
  const ColumnMenuCheckboxFilter = getColumnMenuCheckboxFilter(rows)
  const ColumnMenuCheckboxFilterDate = getColumnMenuDateFilter(rows)
  const [customModifiedCells, setCustomModifiedCells] = useState({})
  const [selectedRows, setSelectedRows] = useState([])
  const [deleteMultipleConfirms, setDeleteMultipleConfirms] = useState(false)
  const [applyMinWidth, setApplyMinWidth] = useState(false)
  const [gridCurrent, setGridCurrent] = useState(0)
  const dataGridStore = useSelector((state) => state.dataGridStore)

  const ADJUST_PADDING = 4
  const COLUMN_MIN = 4

  const keycloak = useSession()
  const showDeleteAll =
    (permissions?.deleteAllBtn && selectedUsers.length > 1) ||
    (permissions?.deleteMultiple && selectedRows.length > 0)
  const { verticalChange, oldYear, year, plantObject, siteObject } =
    dataGridStore
  const IS_OLD_YEAR = oldYear?.oldYear
  const AOP_YEAR = year?.selectedYear
  const PLANT_ID = plantObject?.id
  const plantName = plantObject?.name
  const SiteName = siteObject?.name
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const IntegerDaysEditor = useIntegerDaysEditor(configType, AOP_YEAR)
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const lowerSiteName = SiteName?.toLowerCase()
  const isPEPP = ['pe', 'pp'].includes(lowerVertName)
  const IS_VCM_VERTICAL = ['vcm'].includes(lowerVertName)
  const lowerPlantName = plantName?.toLowerCase()

  const IS_CHEMICAL_VMD_BENZENE =
    lowerVertName === 'chemical' &&
    lowerSiteName === 'vmd' &&
    lowerPlantName === 'benzene'

  const RED_HIGHLIGHT_PRODUCT_NAMES = [
    'C5 Cut to NCP',
    'FC Column Bottom to RARFS',
    'VA Stream to BZ',
    'PYROLYSIS GASOLINE',
    'Benzene Content in feed for PyGas',
  ].map((n) => n.trim().toLowerCase())

  const RED_HIGHLIGHT_PRODUCT_NAMES_STEADY_STATE_NORMS_NP = [
    'Tatoray Hydrogen',
    'PACOL HYDROGEN',
    'PX Hydrogen',
  ].map((n) => n.trim().toLowerCase())

  const RED_HIGHLIGHT_PRODUCT_NAMES_STEADY_STATE_NORMS_LAB = [
    'PX Hydrogen',
    'HYDROGENT',
  ].map((n) => n.trim().toLowerCase())

  const RED_HIGHLIGHT_PRODUCT_NAMES_STEADY_STATE_NORMS =
    lowerPlantName == 'np'
      ? RED_HIGHLIGHT_PRODUCT_NAMES_STEADY_STATE_NORMS_NP
      : RED_HIGHLIGHT_PRODUCT_NAMES_STEADY_STATE_NORMS_LAB

  const toggleGrid = () => {
    setGridExpanded((prev) => !prev)
  }

  // console.log('columns', columns)

  // ...inside columns?.map((col) => { ... })...
  const fieldToMonthNumber = {
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
  // Custom cell for VCM/DMD month highlighting
  const VcmDmdMonthHighlightCell = ({
    dataItem,
    field,
    tdProps,
    children,
    shutdownMonths = [],
    slowdownMonths = [],
  }) => {
    const value = dataItem[field]
    const monthNumber = fieldToMonthNumber[field]
    const isShutdown = shutdownMonths.includes(monthNumber)
    const isSlowdown = slowdownMonths.includes(monthNumber)
    let color = ''
    if (isShutdown || isSlowdown) color = '#E3E3E3'

    return (
      <td
        {...tdProps}
        title={value}
        style={{
          ...tdProps.style,
          backgroundColor: color || undefined,
          textAlign: 'right',
        }}
      >
        {children}
      </td>
    )
  }
  const isMcuMaxCapRedCell = useCallback(
    (productName, field) => {
      if (!mcuMaxCapValues?.aopMaxCapMCValueList?.length) return false
      // console.log('mcuMaxCapValues received:', mcuMaxCapValues)
      const monthNameMap = {
        april: 'April',
        may: 'May',
        june: 'June',
        july: 'July',
        august: 'August',
        september: 'September',
        october: 'October',
        november: 'November',
        december: 'December',
        january: 'January',
        february: 'February',
        march: 'March',
      }

      const monthName = monthNameMap[field?.toLowerCase()]
      if (!monthName) return false
      console.log(
        'Checking:',
        productName,
        monthName,
        mcuMaxCapValues.aopMaxCapMCValueList,
      )

      return mcuMaxCapValues.aopMaxCapMCValueList.some(
        (item) =>
          item.isValid === 1 &&
          item.monthName?.toLowerCase() === monthName.toLowerCase() &&
          item.productName?.toLowerCase() === productName?.toLowerCase(),
      )
    },
    [mcuMaxCapValues],
  )
  const monthFields = [
    'april',
    'may',
    'june',
    'july',
    'august',
    'september',
    'october',
    'november',
    'december',
    'january',
    'february',
    'march',
  ]

  const initialGroup = groupBy
    ? [
        {
          field: groupBy,
          aggregates: totalRowConfiguration,
          dir: undefined,
        },
      ]
    : []

  const MyFooterCustomCell = (props) => {
    const { tdProps } = props
    const { dataItem } = props
    const groupName = dataItem?.value
    // Skip footer for non-Production groups
    if (groupName !== 'Production') {
      return
    }

    const field = props.field
    const labelColumn = 'displayName'
    if (field === labelColumn) {
      return (
        <td {...tdProps}>
          <b>Total</b>
        </td>
      )
    }

    let cellContent = ''

    // Calculate sum the same way as validation - sum scaled integers from raw data
    const items = dataItem?.items || []
    if (items.length > 0 && field) {
      const SCALE = 10000
      const TOLERANCE = 1 // allows 0.0001 difference
      const EXPECTED = 100 * SCALE

      const toPreciseInt = (num) => {
        if (num === null || num === undefined || num === '') return 0
        const n = Number(num)
        if (isNaN(n)) return 0
        return Math.round(Number(n || 0) * SCALE)
      }

      const formatFromIntRobust = (intVal) => {
        const sign = intVal < 0 ? '-' : ''
        const abs = Math.abs(intVal)
        const integerPart = Math.floor(abs / SCALE)
        const remainder = abs % SCALE
        if (remainder === 0) return sign + String(integerPart)
        const scaleDigits = String(SCALE).length - 1
        let fracStr = String(remainder).padStart(scaleDigits, '0')
        fracStr = fracStr.replace(/0+$/, '')
        return sign + `${integerPart}.${fracStr}`
      }

      const sumInt = items.reduce(
        (acc, row) => acc + toPreciseInt(row[field]),
        0,
      )

      const isWithinTolerance = Math.abs(sumInt - EXPECTED) <= TOLERANCE

      // If sum is within tolerance of 100, display as exactly 100
      if (isWithinTolerance) {
        cellContent = '100'
      } else {
        cellContent = formatFromIntRobust(sumInt)
      }
    }
    return (
      <td {...props.tdProps} colSpan={1}>
        {cellContent}
      </td>
    )
  }
  const formatTo12Hr = (raw) => {
    if (!raw) return ''
    const d = new Date(raw)
    const dd = String(d.getDate()).padStart(2, '0')
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const yyyy = d.getFullYear()
    let hours = d.getHours()
    const minutes = String(d.getMinutes()).padStart(2, '0')
    const ampm = hours >= 12 ? 'PM' : 'AM'
    hours = hours % 12 || 12
    const hh = String(hours).padStart(2, '0')
    return `${dd}-${mm}-${yyyy} ${hh}:${minutes} ${ampm}`
  }

  const handleEditChange = useCallback((e) => {
    setEdit(e.edit)
  }, [])

  const fileInputRef = useRef(null)
  const onSelectionChange = (event) => {
    const newSelectedRows = event?.dataItems?.map((item) => item?.idFromApi)
    setSelectedRows(newSelectedRows)
  }

  const numericHeaderClass = (isActive, col) =>
    [isActive ? 'active-column' : '', 'k-number-right']
      .filter(Boolean)
      .join(' ')

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
  const onHeaderSelectionChange = (event) => {
    const checked = event?.syntheticEvent?.target?.checked
    if (checked) {
      const allRowIds = rows.map((item) => item?.idFromApi)
      setSelectedRows(allRowIds)
    } else {
      setSelectedRows([])
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
  const handleDeleteMultiple = () => {
    if (permissions?.deleteMultiple && selectedRows?.length > 0) {
      handleDeleteSelected(selectedRows)
      setSelectedRows([])
      setDeleteMultipleConfirms(false)
    } else {
      handleDeleteSelected()
      setDeleteMultipleConfirms(false)
    }
  }

  const handleOpenDeleteMultipleDialog = () => {
    setDeleteMultipleConfirms(true)
  }

  const excelExport = () => {
    if (_export.current !== null) {
      _export.current.save()
    }
  }

  const handleRowClick = (e) => {
    if (READ_ONLY) {
      setEdit({})
      return
    }

    if (e?.dataItem?.aggregates) {
      setEdit({})
      return
    }

    if (!e.dataItem?.isEditable && e.dataItem?.isEditable !== undefined) {
      setEdit({})
      return
    }
    if (e.dataItem?.isTotal) {
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
      const { dataItem, field } = e
      let { value } = e

      if (!field || dataItem?.items) {
        return
      }

      setIsRowEdited(true)

      if (dataItem?.isTotal) {
        return
      }

      if (permissions?.isTotalFooterActive) {
        const monthsForTotalRow = [
          'april',
          'aug',
          'dec',
          'feb',
          'jan',
          'july',
          'june',
          'march',
          'may',
          'nov',
          'oct',
          'sep',
        ]
        if (monthsForTotalRow.includes(field)) {
          if (value === '' || value == null) {
            value = null
          } else {
            value = Number(value)
          }
        }
      }

      if (dataItem?.field === 'Particulars') return
      if (dataItem?.field === 'ParticularsType') return

      const itemId = dataItem.id

      const months = [
        'apr',
        'may',
        'jun',
        'jul',
        'aug',
        'sep',
        'oct',
        'nov',
        'dec',
        'jan',
        'feb',
        'mar',
      ]

      // Helper: return numeric percent if input contains at least one digit, otherwise null.
      const parsePctOrNull = (v) => {
        if (v == null) return null
        const s = String(v).replace('%', '').trim()
        // if there are no digits at all, treat as invalid (e.g. "+", "-", "", "+%")
        if (!/[0-9]/.test(s)) return null
        // remove leading plus sign for parsing, keep minus sign
        const cleaned = s.replace(/^\+/, '')
        const n = Number(cleaned)
        return Number.isFinite(n) ? n : null
      }

      // ✅ Helper: calculate start date from end date and duration
      const recalcStartDate = (endDateTime, durationStr) => {
        if (!endDateTime || !durationStr) return null
        const end = new Date(endDateTime)
        if (isNaN(end)) return null
        const [hoursStr, minsStr = '0'] = String(durationStr).split('.')
        const hours = parseInt(hoursStr, 10) || 0
        const mins = parseInt(minsStr.padEnd(2, '0'), 10) || 0
        const totalMs = (hours * 60 + mins) * 60 * 1000
        return new Date(end.getTime() - totalMs)
      }

      setRows((prev) =>
        prev.map((r) => {
          if (r.id !== itemId) return r
          const updated = { ...r, [field]: value }

          if (
            screenType === 'slowdown' &&
            lowerVertName === 'pta' &&
            lowerSiteName === 'dmd'
          ) {
            if (!updated.rpfDownTime || isNaN(Number(updated.rpfDownTime))) {
              updated.rpfDownTime = 28
            }
            let rpfDownTimeVal =
              field === 'rpfDownTime' ? value : updated.rpfDownTime
            let noOfRPFVal = field === 'noOfRPF' ? value : updated.noOfRPF

            // Accept both "HH:MM" and "HH.MM" input
            let minsPerRPF = 0
            if (
              typeof rpfDownTimeVal === 'string' &&
              rpfDownTimeVal.includes(':')
            ) {
              const [h, m] = rpfDownTimeVal.split(':').map(Number)
              minsPerRPF = (isNaN(h) ? 0 : h) * 60 + (isNaN(m) ? 0 : m)
            } else if (rpfDownTimeVal) {
              const [h, m = '0'] = String(rpfDownTimeVal).split('.')
              minsPerRPF =
                parseInt(h || 0) * 60 + parseInt(m.padEnd(2, '0') || 0)
            }

            const nRPF = parseFloat(noOfRPFVal) || 0 // Use parseFloat for decimals
            const totalMins = minsPerRPF * nRPF

            // Convert back to HH.MM for storage
            const hours = Math.floor(totalMins / 60)
            const mins = Math.round(totalMins % 60)
            updated.durationInHrs = `${hours.toString().padStart(2, '0')}.${mins
              .toString()
              .padStart(2, '0')}`
          }
          if (
            screenType === 'slowdown' &&
            lowerVertName === 'pta' &&
            lowerSiteName === 'dmd'
          ) {
            updated.rate = 154
          }

          if (
            screenType === 'slowdown' &&
            lowerVertName === 'vcm' &&
            field === 'discription'
          ) {
            const desc = (value || '').trim()
            if (
              desc === 'Furnace Decoking H-210' ||
              desc === 'Furnace Decoking H-220'
            ) {
              updated.rate = 27.0833
            } else if (desc === 'Furnace Decoking H-1220') {
              updated.rate = 26.458
            } else if (desc === 'Furnace Decoking') {
              updated.rate = ''
            }
          }
          if (
            screenType === 'slowdown' &&
            lowerVertName === 'vcm' &&
            lowerSiteName === 'hmd' &&
            field === 'discription'
          ) {
            const desc = (value || '').toLowerCase().replace(/\s+/g, ' ').trim()

            const rateMap = {
              'furnace decoking - (eba-6401a)': 32.5,
              'furnace decoking - (eba-6401b)': 32.5,
              'furnace decoking - (eba-6401c)': 22.75,
            }

            if (rateMap[desc] !== undefined) {
              updated.rate = rateMap[desc]
            }
          }

          if (
            screenType === 'shutdown' &&
            lowerVertName === 'pta' &&
            lowerSiteName === 'dmd' &&
            ['discription', 'discriptionDrpdwn'].includes(field)
          ) {
            const desc = (value || '').trim()
            if (desc === 'Flush SD') {
              updated.durationInHrs = '158.00'
            } else if (desc === 'Purif Flush') {
              updated.durationInHrs = '16.00'
            } else if (desc === 'Annual Turn Around') {
              updated.durationInHrs = '684.00'
            } else if (
              desc ===
              'FSD (Catalyst full replacement/Partial Preheater cleaning)'
            ) {
              updated.durationInHrs = '168.00'
            } else if (
              desc ===
              'FSD (Catalyst partial replacement/Partial Preheater cleaning)'
            ) {
              updated.durationInHrs = '158.00'
            }
          }
          if (lowerVertName === 'cracker' && lowerSiteName === 'vmd') {
            if (
              (field === 'StartDate' || field === 'EndDate') &&
              updated.StartDate &&
              updated.EndDate
            ) {
              const start = new Date(updated.StartDate)
              const end = new Date(updated.EndDate)
              const diffMs = end.getTime() - start.getTime()
              const totalDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
              if (totalDays >= 0) {
                updated.Duration = `${totalDays}`
              } else {
                updated.Duration = 'Invalid'
              }
            }
          }

          const lowerPlantName = plantName?.toLowerCase()
          const IS_ELASTOMER_JMD_HIIR =
            lowerVertName === 'elastomer' &&
            lowerSiteName === 'jmd' &&
            lowerPlantName === 'hiir'

          if (
            screenType === 'shutdown' &&
            IS_ELASTOMER_JMD_HIIR &&
            field === 'monthly'
          ) {
            const monthDur = calculateMonthDuration(value, AOP_YEAR)
            const [start, end] = getMonthStartEndDate(value, AOP_YEAR)
            if (monthDur) {
              updated.durationInHrs = monthDur
            }
            if (start && end) {
              updated.maintStartDateTime = start
              updated.maintEndDateTime = end
            }
          }

          // percentChange logic: adjust months if enabled and percentChange field changed
          if (field === 'percentChange' && permissions?.percentChangeLogic) {
            const pct = parsePctOrNull(value)
            if (pct !== null) {
              const factor = 1 + pct / 100
              months.forEach((m) => {
                const original = Number(r[m]) || 0
                updated[m] = Number((original * factor).toFixed(2))
              })
            }
          }

          if (
            'maintStartDateTime' in updated &&
            'maintEndDateTime' in updated &&
            'durationInHrs' in updated
          ) {
            if (!(screenType === 'slowdown' && lowerVertName === 'elastomer')) {
              if (
                field === 'maintStartDateTime' ||
                field === 'maintEndDateTime'
              ) {
                updated.durationInHrs = recalcDuration(
                  updated.maintStartDateTime,
                  updated.maintEndDateTime,
                )
              } else if (field === 'durationInHrs') {
                if (updated.type === 'ramp-down') {
                  // ramp-down: end date is FIXED → move start date
                  const newStart = recalcStartDate(
                    updated.maintEndDateTime,
                    value,
                  )
                  if (newStart) {
                    updated.maintStartDateTime = newStart
                  }
                } else {
                  // all others: start date is FIXED → move end date
                  const newEnd = recalcEndDate(
                    updated.maintStartDateTime,
                    value, // string like “10.20”
                  )
                  if (newEnd) {
                    updated.maintEndDateTime = newEnd
                  }
                }
              }
            }
          }

          if (
            lowerVertName === 'vcm' &&
            [
              'Furnace Decoking',
              'Furnace Decoking H-210',
              'Furnace Decoking H-220',
              'Furnace Decoking H-1220',
            ].includes((updated.discription || '').trim())
          ) {
            if (field === 'maintStartDateTime' && value) {
              const start = new Date(value)
              if (!isNaN(start)) {
                const end = new Date(start)
                end.setHours(end.getHours() + 192)
                // Only update if different to avoid triggering another change
                if (
                  !updated.maintEndDateTime ||
                  new Date(updated.maintEndDateTime).getTime() !== end.getTime()
                ) {
                  updated.maintEndDateTime = end
                  updated.durationInHrs = '192.00'
                }
              }
            }
          }

          return updated
        }),
      )

      if (permissions?.onlyCellUpdate) {
        setModifiedCells((prev) => {
          const updated = { ...(prev[itemId] || {}) }

          updated[field] = value

          if (!updated.NormParameter_FK_Id && dataItem?.NormParameter_FK_Id) {
            updated.NormParameter_FK_Id = dataItem.NormParameter_FK_Id
          }

          // percentChange: only set month fields when percent is numeric
          if (field === 'percentChange' && permissions?.percentChangeLogic) {
            const pct = parsePctOrNull(value)
            if (pct !== null) {
              const factor = 1 + pct / 100
              months.forEach((m) => {
                if (m in dataItem) {
                  const original = Number(dataItem[m]) || 0
                  updated[m] = Number((original * factor).toFixed(2))
                }
              })
            }
          }

          return {
            ...prev,
            [itemId]: updated,
          }
        })
      } else {
        const uniqueItemId = permissions?.showCheckbox
          ? `${gridName}-${itemId}`
          : itemId

        setModifiedCells((prev) => {
          const base = {
            ...(prev[uniqueItemId] || {}),
            ...dataItem,
            [field]: value,
          }

          if (
            'maintStartDateTime' in base &&
            'maintEndDateTime' in base &&
            'durationInHrs' in base
          ) {
            if (!(screenType === 'slowdown' && lowerVertName === 'elastomer')) {
              if (
                field === 'maintStartDateTime' ||
                field === 'maintEndDateTime'
              ) {
                base.durationInHrs = recalcDuration(
                  base.maintStartDateTime,
                  base.maintEndDateTime,
                )
              } else if (field === 'durationInHrs') {
                if (base.type === 'ramp-down') {
                  // ramp-down: end date is FIXED → move start date
                  const newStart = recalcStartDate(base.maintEndDateTime, value)
                  if (newStart) base.maintStartDateTime = newStart.toISOString()
                } else {
                  // all others: start date is FIXED → move end date
                  const newEnd = recalcEndDate(base.maintStartDateTime, value)
                  if (newEnd) base.maintEndDateTime = newEnd.toISOString()
                }
              }
            }
          }

          // percentChange logic: mutate base for all months (only when numeric)
          if (field === 'percentChange' && permissions?.percentChangeLogic) {
            const pct = parsePctOrNull(value)
            if (pct !== null) {
              const factor = 1 + pct / 100
              months.forEach((m) => {
                const original = Number(dataItem[m]) || 0
                base[m] = Number((original * factor).toFixed(2))
              })
            }
          }

          const lowerPlantName = plantName?.toLowerCase()
          const IS_ELASTOMER_JMD_HIIR =
            lowerVertName === 'elastomer' &&
            lowerSiteName === 'jmd' &&
            lowerPlantName === 'hiir'

          if (
            screenType === 'shutdown' &&
            IS_ELASTOMER_JMD_HIIR &&
            field === 'monthly'
          ) {
            const monthDur = calculateMonthDuration(value, AOP_YEAR)
            const [start, end] = getMonthStartEndDate(value, AOP_YEAR)
            if (monthDur) {
              base.durationInHrs = monthDur
            }
            if (start && end) {
              base.maintStartDateTime = start
              base.maintEndDateTime = end
            }
          }

          return { ...prev, [uniqueItemId]: base }
        })
      }

      // customModifiedCells: always set per-row custom changes (include months if percentChange)
      setCustomModifiedCells((prev) => {
        const base = { ...(prev[itemId] || {}), [field]: value }

        if (field === 'percentChange' && permissions?.percentChangeLogic) {
          const pct = parsePctOrNull(value)
          if (pct !== null) {
            const factor = 1 + pct / 100
            months.forEach((m) => {
              const original = Number(dataItem[m]) || 0
              base[m] = Number((original * factor).toFixed(2))
            })
          }
        }

        return {
          ...prev,
          [itemId]: base,
        }
      })

      if (customItemChange) {
        customItemChange(e, {
          setModifiedCells,
          setCustomModifiedCells,
          rows: rowsRef.current,
        })
      }
    },
    [
      setRows,
      setModifiedCells,
      setCustomModifiedCells,
      customItemChange,
      lowerVertName,
      lowerSiteName,
      plantName,
      AOP_YEAR,
      screenType,
    ],
  )

  useEffect(() => {
    const isModifiedCellsEmpty = Object.keys(modifiedCells).length === 0
    const isCustomModifiedCellsEmpty =
      Object.keys(customModifiedCells).length === 0

    if (isModifiedCellsEmpty && !isCustomModifiedCellsEmpty) {
      setCustomModifiedCells({})
    }
  }, [modifiedCells, customModifiedCells])

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
            'Remarks',
          ].filter((key) => key in row)
          keyToUpdate = keysToUpdate[0] || 'remark'
          updatedRow = { ...row, [keyToUpdate]: currentRemark, inEdit: true }
          return updatedRow
        }
        return row
      })

      if (updatedRow) {
        if (permissions?.showCheckbox) {
          const uniqueKey = `${gridName}-${updatedRow.id}`

          setModifiedCells((prev) => ({
            ...prev,
            [uniqueKey]: {
              ...(prev[uniqueKey] || {}),
              ...updatedRow,
              gridName,
              id: updatedRow.id,
            },
          }))
        } else {
          setModifiedCells((prev) => ({
            ...prev,
            [updatedRow.id]: updatedRow,
          }))
        }

        // Mark the remark field as edited in customModifiedCells so RemarkCell highlights orange
        setCustomModifiedCells((prev) => ({
          ...prev,
          [updatedRow.id]: {
            ...(prev[updatedRow.id] || {}),
            [keyToUpdate]: currentRemark,
          },
        }))
      }

      return updatedRows
    })

    setRemarkDialogOpen(false)
  }

  const columnCount = useMemo(() => {
    return (
      columns?.filter(
        (col) =>
          col?.field && col?.hidden !== true && Object.keys(col).length > 0,
      )?.length || 0
    )
  }, [columns])

  const handleAddRow = () => {
    setEdit({})
    if (isButtonDisabled) return
    setIsButtonDisabled(true)
    const newRowId = rows.length
      ? Math.max(...rows.map((row) => row.id)) + 1
      : 1
    const newRow = {
      id: newRowId,
      isNew: true,
      ...Object.fromEntries(columns?.map((col) => [col?.field, ''])),
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

  const resetConfirmation = async () => {
    resetDataChanges()
    setOpenResetDataDialogeBox(false)
    setEdit({})
  }

  const handleDeleteClick = async (params) => {
    if (READ_ONLY) return
    setParamsForDelete(params)
    setOpenDeleteDialogeBox(true)
  }

  const handleResetClick = async (params) => {
    setOpenResetDialogeBox(true)
  }

  const deleteTheRecord = async () => {
    deleteRowData(paramsForDelete)
    setOpenDeleteDialogeBox(false)
  }

  const resetTheRecord = async () => {
    resetRowData(paramsForDelete)
    setOpenResetDialogeBox(false)
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

  const ResetActionsCell = ({ dataItem }) => {
    return (
      <td style={{ textAlign: 'center', verticalAlign: 'middle' }}>
        <SvgIcon
          onClick={() => handleResetClick(dataItem)}
          icon={arrowRotateCcwIcon}
          themeColor='dark'
        />
      </td>
    )
  }

  const saveModalOpen = async () => {
    setEdit({})
    if (READ_ONLY) return
    setIsButtonDisabled(true)
    setOpenSaveDialogeBox(true)
    setTimeout(() => {
      setIsButtonDisabled(false)
    }, 500)
  }

  const resetDataModalOpen = async () => {
    if (READ_ONLY) return
    setIsButtonDisabled(true)
    setOpenResetDataDialogeBox(true)
    setTimeout(() => {
      setIsButtonDisabled(false)
    }, 500)
  }

  const handleCalculateBtn = async () => {
    if (permissions?.showCalulcationPromt) {
      openCalculateDialogBox()
    } else {
      // old code
      setSelectedGrade('')
      setIsButtonDisabled(true)

      handleCalculate()

      setTimeout(() => {
        setIsButtonDisabled(false)
      }, 500)
    }
  }

  const [openCalculateDialogeBox, setOpenCalculateDialogeBox] = useState(false)

  const openCalculateDialogBox = () => {
    setOpenCalculateDialogeBox(true)
  }

  const closeCalculateDialogBox = () => {
    setOpenCalculateDialogeBox(false)
  }

  const handleCalculateConfirmation = async () => {
    closeCalculateDialogBox()
    setSelectedGrade('')
    setIsButtonDisabled(true)

    await handleCalculate()

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

  const isColumnActive = (field, filter, sort) => {
    return (
      isColumnMenuFilterActive(field, filter) ||
      isColumnMenuSortActive(field, sort)
    )
  }

  const ElastomerYearDisplayCell = ({ dataItem, field, tdProps }) => {
    return (
      <td {...tdProps} title={dataItem[field]}>
        {dataItem[field]}
      </td>
    )
  }
  const LineDisplayCell = (props) => {
    const {
      dataItem,
      field,
      tdProps,
      customModifiedCells,
      highlightField,
      highlight,
    } = props
    const rowId = dataItem.id
    const checkField = highlightField || field
    const isEdited = !!(
      customModifiedCells?.[rowId] && checkField in customModifiedCells[rowId]
    )
    const lineObj = props.allLines?.find(
      (l) => l.id === dataItem[field] || l.displayName === dataItem[field],
    )
    const displayLabel = lineObj ? lineObj.displayName : ''
    return (
      <td
        {...tdProps}
        className={`${tdProps?.className || ''} ${highlight && isEdited ? 'edited-cell' : ''}`.trim()}
        title={displayLabel}
      >
        {displayLabel || ''}
      </td>
    )
  }
  const MonthDropdownPEPPDisplayCell = ({ dataItem, field, tdProps }) => {
    return (
      <td {...tdProps} title={dataItem[field]}>
        {dataItem[field]}
      </td>
    )
  }

  const ElastomerSDDaysDisplayCell = (props) => {
    const { dataItem, field, tdProps, sdDaysValues = [] } = props
    const value = dataItem[field]
    const option = sdDaysValues.find((opt) => opt.value === value)
    const displayValue = option ? option.name : `${value} Days`

    return (
      <td {...tdProps} title={displayValue}>
        {displayValue}
      </td>
    )
  }

  const ElastomerMonthDisplayCell = (props) => {
    const { dataItem, field, tdProps } = props
    const value = dataItem[field]

    const monthNames = {
      1: 'January',
      2: 'February',
      3: 'March',
      4: 'April',
      5: 'May',
      6: 'June',
      7: 'July',
      8: 'August',
      9: 'September',
      10: 'October',
      11: 'November',
      12: 'December',
    }

    const displayValue = monthNames[value] || value

    return (
      <td {...tdProps} title={displayValue}>
        {displayValue}
      </td>
    )
  }
  const MonthDisplayCell = (props) => {
    const { dataItem, field, tdProps, children } = props
    const value = dataItem[field]

    const monthNames = {
      1: 'Jan',
      2: 'Feb',
      3: 'Mar',
      4: 'Apr',
      5: 'May',
      6: 'Jun',
      7: 'Jul',
      8: 'Aug',
      9: 'Sep',
      10: 'Oct',
      11: 'Nov',
      12: 'Dec',
    }

    const displayValue = monthNames[value] || value

    return (
      <td {...tdProps} title={displayValue}>
        {displayValue}
      </td>
    )
  }

  const MaterialDisplayNameCell = (props) => {
    const { dataItem, field, tdProps, children } = props
    const value = dataItem[field]
    const method = dataItem.Method || dataItem.method
    let color = 'inherit'

    switch (method) {
      case 'BestAchieved(MinCC)':
        color = '#2e7d32'
        break
      case 'Expression':
        color = '#f51717ff'
        break
      // case 'BestAchieved(Indv)':
      case 'BestAchieved(Indiv)':
        color = '#1565c0'
        break
      default:
        break
    }

    return (
      <td {...tdProps} title={value}>
        <span
          ref={(el) => {
            if (el && color) {
              el.style.setProperty('color', color, 'important')
              // el.style.setProperty('font-weight', 'bold', 'important')
            }
          }}
        >
          {value}
        </span>
      </td>
    )
  }
  const CustomRow = useCallback(
    ({ dataItem, className, ...rest }) => {
      const isDisabled =
        READ_ONLY ||
        (!dataItem.isEditable && dataItem?.isEditable !== undefined)
      const hasError = dataItem?.isError
      const isTotal = dataItem?.isTotal
      const rowClassName = hasError
        ? 'error-row'
        : isDisabled || isTotal
          ? 'custom-disabled-row'
          : className

      return (
        <tr {...rest?.trProps} className={rowClassName}>
          {rest.children}
        </tr>
      )
    },
    [IS_OLD_YEAR, READ_ONLY],
  )

  const resetAllEdits = () => {
    setEditState({
      design: {},
      max: {},
      current: {},
      summary: {},
    })
  }

  const toolTipRendererdescLimit = (props) => {
    const value = props.dataItem[props.field]
    const type = props?.dataItem?.type ?? ''
    // const isDisabled = type === 'ramp-down' || type === 'ramp-up'
    const isDisabled = false

    return (
      <td
        {...props.tdProps}
        title={value}
        style={{
          backgroundColor: isDisabled ? '#f0f0f0' : undefined,
        }}
      >
        {props.children}
      </td>
    )
  }
  //
  const SimpleHighlightCell = ({
    dataItem,
    field,
    tdProps,
    children,
    customModifiedCells,
    highlightField,
    highlight = false,
  }) => {
    const rowId = dataItem.id
    const value = dataItem[field]
    const checkField = highlightField || field
    const isEdited = !!(
      customModifiedCells?.[rowId] && checkField in customModifiedCells[rowId]
    )
    const isBoldFromCells = dataItem?.boldCells?.includes(field)

    const isStart = field === 'maintStartDateTime'
    const isEnd = field === 'maintEndDateTime'
    const type = dataItem?.type
    const isDisabled =
      (isEnd && type === 'ramp-down') || (isStart && type === 'ramp-up')

    return (
      <td
        {...tdProps}
        title={value}
        className={`${tdProps?.className || ''} ${highlight && isEdited ? 'edited-cell' : ''}`.trim()}
        style={{
          fontWeight:
            !(highlight && isEdited) && isBoldFromCells ? 'bold' : undefined,
          backgroundColor: isDisabled ? '#f0f0f0' : undefined,
          color: isDisabled ? '#6e6e6e' : undefined,
          cursor: isDisabled ? 'not-allowed' : undefined,
        }}
      >
        {children}
      </td>
    )
  }
  const RedHighlightCell = (props) => {
    const {
      dataItem,
      field,
      tdProps,
      children,
      customModifiedCells,
      allRedCell,
    } = props

    const rowId = dataItem.id
    const value = dataItem[field]
    const isBoldFromCells = dataItem?.boldCells?.includes(field)
    if (disableRedHighlight) {
      return (
        <td
          {...tdProps}
          title={value}
          style={{ fontWeight: isBoldFromCells ? 'bold' : undefined }}
        >
          {children}
        </td>
      )
    }

    const isEdited = Object.prototype.hasOwnProperty.call(
      customModifiedCells?.[rowId] || {},
      field,
    )

    const normId =
      dataItem.materialFKId ||
      dataItem.materialFkId ||
      dataItem.NormParameter_FK_Id

    const fieldMonthNumber = fieldToMonthNumber[field?.toLowerCase()]

    const isRedFromAllRedCell = allRedCell?.some((cell) => {
      const cellNormId = (
        cell.NormParameter_FK_Id || cell.normParameterFKId
      )?.toLowerCase()

      if (!cellNormId || !normId || cellNormId !== normId.toLowerCase()) {
        return false
      }

      // MEG-style cells carry a ColumnName that matches the dynamic field name directly
      const cellColumnName = cell.ColumnName || cell.columnName
      if (cellColumnName != null) {
        return cellColumnName === field
      }

      // Normal-op-norms-style cells carry a numeric/string month
      const cellMonthNumber =
        typeof cell.month === 'number'
          ? cell.month
          : fieldToMonthNumber[String(cell.month).toLowerCase()]

      return cellMonthNumber === fieldMonthNumber
    })

    const shouldHighlight = isEdited || isRedFromAllRedCell

    return (
      <td
        {...tdProps}
        title={value}
        className={`${tdProps?.className || ''} ${shouldHighlight ? 'edited-cell' : ''}`.trim()}
        style={{
          fontWeight: !shouldHighlight && isBoldFromCells ? 'bold' : undefined,
        }}
      >
        {children}
      </td>
    )
  }

  const RedHighlightCell2 = (props) => {
    const {
      dataItem,
      field,
      tdProps,
      children,
      customModifiedCells,
      allRedCell,
      allRedCell2,
    } = props

    const rowId = dataItem.id
    const value = dataItem[field]
    const isBoldFromCells = dataItem?.boldCells?.includes(field)

    if (disableRedHighlight) {
      return (
        <td
          {...tdProps}
          title={value}
          style={{ fontWeight: isBoldFromCells ? 'bold' : undefined }}
        >
          {children}
        </td>
      )
    }

    const isEdited = Object.prototype.hasOwnProperty.call(
      customModifiedCells?.[rowId] || {},
      field,
    )

    const month = field

    const normId = dataItem.materialFKId || dataItem.NormParameter_FK_Id

    const matchedCell = allRedCell?.find(
      (cell) =>
        cell.month?.toLowerCase() === month?.toLowerCase() &&
        cell.NormParameter_FK_Id?.toLowerCase() === normId?.toLowerCase(),
    )

    const getMonthNumber = (m) => {
      if (m == null) return null
      const map = {
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
      const lower = String(m).trim().toLowerCase()
      return map[lower] || Number(lower) || null
    }

    const isRedFromAllRedCell = allRedCell2?.some((cell) => {
      const cellMonthNum = getMonthNumber(cell.month)
      const fieldMonthNum = getMonthNumber(month)

      const sameMonth = cellMonthNum === fieldMonthNum
      const sameNormId =
        cell.normParameterFKId?.toLowerCase() === normId?.toLowerCase()

      return sameMonth && sameNormId
    })

    let highlightColor
    let highlightColorFullCell = false

    if (isEdited || isRedFromAllRedCell) {
      highlightColor = 'orange'
    } else if (matchedCell?.mode === 'Propane(1Z)') {
      highlightColor = 'red'
    } else if (matchedCell?.mode === 'Propane(2Z)') {
      highlightColor = 'green'
    } else if (matchedCell?.mode === 'Copied') {
      highlightColor = 'purple'
      highlightColorFullCell = true
    }

    return (
      <td
        {...tdProps}
        title={value}
        className={`${tdProps?.className || ''} ${highlightColor ? 'edited-cell' : ''}`.trim()}
        style={{
          color:
            highlightColor && highlightColor !== 'orange'
              ? highlightColor
              : undefined,
          fontWeight: !highlightColor && isBoldFromCells ? 'bold' : undefined,
          // backgroundColor: highlightColorFullCell ? 'lightGrey' : undefined,
        }}
      >
        {children}
      </td>
    )
  }

  const lowerCurrentTabDisplayName = currentTabDisplayName
    ?.toLowerCase()
    ?.trim()
  const isConfigOrConstantTab =
    lowerCurrentTabDisplayName === 'configuration' ||
    configType === 'Configuration'

  const toolTipRenderer = (props) => {
    const value = props.dataItem[props.field]
    const month = props.field
    const normId =
      props.dataItem.materialFkId || props.dataItem.NormParameter_FK_Id

    const isRedFromAllRedCell = allRedCell.some(
      (cell) =>
        cell.month === month &&
        cell.NormParameter_FK_Id?.toLowerCase() === normId?.toLowerCase(),
    )
    const isRed = isRedFromAllRedCell

    const isProductNameTarget =
      props.field === 'productName' &&
      isConfigOrConstantTab &&
      IS_CHEMICAL_VMD_BENZENE &&
      RED_HIGHLIGHT_PRODUCT_NAMES.includes(
        String(value || '')
          .trim()
          .toLowerCase(),
      )

    const isProductNameTargetSSN =
      props.field === 'productName' &&
      permissions?.isShowColoredPartucilars &&
      RED_HIGHLIGHT_PRODUCT_NAMES_STEADY_STATE_NORMS.includes(
        String(value || '')
          .trim()
          .toLowerCase(),
      )

    return (
      <td
        {...props.tdProps}
        title={value}
        className={`${props.tdProps?.className || ''} ${isRed ? 'edited-cell' : ''}`.trim()}
      >
        {isProductNameTarget || isProductNameTargetSSN ? (
          <span
            ref={(el) => {
              if (el) {
                el.style.setProperty('color', 'red', 'important')
                el.style.setProperty('font-weight', 'bold', 'important')
              }
            }}
          >
            {props.children}
          </span>
        ) : (
          props.children
        )}
      </td>
    )
  }

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

  const BlankHeader = (props) => {
    const { ariaSort, ...restThProps } = props.thProps || {}

    return (
      <th
        {...restThProps}
        aria-sort={ariaSort}
        title=''
        style={{ padding: '0px', borderRight: '1px solid #878787' }}
      ></th>
    )
  }

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
  const DurationHighlightCell = ({
    dataItem,
    field,
    tdProps,
    children,
    customModifiedCells,
    highlightField,
    highlight = true,
  }) => {
    const rowId = dataItem.id
    const value = dataItem[field]
    const checkField = highlightField || field
    const isEdited = !!(
      customModifiedCells?.[rowId] && checkField in customModifiedCells[rowId]
    )

    // Format value to HH:MM
    let display = value
    if (value && !isNaN(value)) {
      const [hoursStr, minsStr = '0'] = value.toString().split('.')
      const hours = parseInt(hoursStr, 10)
      const mins = parseInt(minsStr.padEnd(2, '0'), 10)
      display = `${hours.toString().padStart(2, '0')}:${mins
        .toString()
        .padStart(2, '0')}`
    }

    return (
      <td
        {...tdProps}
        title={display}
        className={`${tdProps?.className || ''} ${highlight && isEdited ? 'edited-cell' : ''}`.trim()}
      >
        {display}
      </td>
    )
  }

  const DurationDisplayWithTooltipCell = (props) => {
    const value = props.dataItem[props.field]

    // Format value to HH:MM
    let display = value
    if (value && !isNaN(value)) {
      const [hoursStr, minsStr = '0'] = value.toString().split('.')
      const hours = parseInt(hoursStr, 10)
      const mins = parseInt(minsStr.padEnd(2, '0'), 10)

      display = `${hours.toString().padStart(2, '0')}:${mins
        .toString()
        .padStart(2, '0')}`
    }

    // Tooltip and conditional color logic
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
        title={display}
        className={`${props.tdProps?.className || ''} ${isRed ? 'edited-cell' : ''}`.trim()}
      >
        {display}
      </td>
    )
  }

  // useEffect(() => {
  //   console.log(selectedGrade)

  //   if (permissions?.showG && grades?.length > 0 && !selectedGrade) {
  //     const firstGrade = grades[0]
  //     setSelectedGrade(firstGrade.gradeId)
  //     handleGradeChange(firstGrade.gradeId, firstGrade?.displayName)
  //   }
  // }, [grades, permissions?.showG, selectedGrade])

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

  useEffect(() => {
    setEdit({})
  }, [PLANT_ID, AOP_YEAR])

  useEffect(() => {
    setEdit({})
  }, [revision])

  useEffect(() => {
    if (
      permissions?.units?.length > 0 &&
      (!selectedUnit || !permissions.units.includes(selectedUnit))
    ) {
      const defaultUnit = permissions.units[0]
      setSelectedUnit(defaultUnit)
      handleUnitChange(defaultUnit)
      // setEdit({})
    }
  }, [permissions])

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

  useEffect(() => {
    if (resetEditSignal !== undefined) {
      setEdit({})
    }
  }, [resetEditSignal])

  useEffect(() => {
    const modes = permissions?.modes
    if (Array.isArray(modes) && modes.length && selectMode === undefined) {
      setSelectMode(modes[0])
    }
  }, [permissions?.modes])

  const CHECK_TYPES = ['cat chem', 'utility consumption']
  const CHECK_TYPES2 = ['raw material', 'by products']

  return (
    <div className='k-table-box'>
      {loading && (
        <div className='k-loading-mask'>
          <span className='k-loading-text'>Loading...</span>
          <div className='k-loading-image' />
          <div className='k-loading-color' />
        </div>
      )}

      {permissions?.showReportTitleMain && (
        <Box sx={{ pt: 1, pl: 1 }}>
          <Typography component='div' className='grid-title'>
            {permissions?.titleNameMain}
          </Typography>
        </Box>
      )}

      {permissions?.showNote && (
        <Box sx={{ pt: 1, pl: 1 }}>
          <Typography component='div' className='text-note'>
            {note}
          </Typography>
        </Box>
      )}

      {(permissions?.allAction ?? false) && (
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
            {/* Left side - Note */}
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                paddingBottom: 0.25,
              }}
            >
              {/* CASE 1: Permission TRUE → Full Header UI */}
              {permissions?.showTitleNameBusiness ? (
                <Typography
                  component='div'
                  sx={{
                    fontSize: '16px',
                    fontWeight: 700,
                    color: '#252525',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.5,
                    mb: permissions?.marginBottom ? '12px' : '4px',
                    fontFamily:
                      '"Honeywell Sans Web", "Inter", sans-serif !important',
                  }}
                >
                  {/* TOGGLE ICON */}
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
                        transform: gridExpanded
                          ? 'rotate(0deg)'
                          : 'rotate(180deg)',
                      }}
                    />
                  </Box>

                  {/* TITLE */}
                  {permissions?.titleName}
                </Typography>
              ) : (
                /* CASE 2: Permission FALSE → ONLY ICON */
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
                      transform: gridExpanded
                        ? 'rotate(0deg)'
                        : 'rotate(180deg)',
                    }}
                  />
                </Box>
              )}

              {permissions?.showReportTitle && (
                <Typography component='div' className='grid-title'>
                  {titleName}
                </Typography>
              )}
              {permissions?.showTitleName && (
                <Typography component='div' className='grid-title'>
                  {titleName}
                </Typography>
              )}

              {permissions?.showTitleAndInformation && (
                <Box
                  display='flex'
                  alignItems='center'
                  sx={{
                    mb: permissions?.marginBottom ? '10px' : '2px',
                    gap: 0.5, // Tight gap for density
                  }}
                >
                  <Typography
                    component='div'
                    sx={{
                      // fontSize: '0.85rem',
                      color: '#252525', // Slate 800
                      letterSpacing: '0.2px',
                      position: 'relative',
                      fontFamily:
                        '"Honeywell Sans Web", "Inter", sans-serif !important',
                      fontSize: '16px',
                      fontWeight: 700,
                    }}
                  >
                    {permissions?.titleName}
                  </Typography>

                  <MuiTooltip
                    arrow
                    placement='top'
                    title={
                      permissions?.titleAndInformation ||
                      'No information available'
                    }
                    slotProps={{
                      popper: {
                        sx: {
                          [`& .MuiTooltip-tooltip`]: {
                            bgcolor: 'rgba(15, 23, 42, 0.95)',
                            backdropFilter: 'blur(4px)',
                            padding: '8px 12px',
                            borderRadius: '8px',
                            fontSize: '0.75rem',
                            boxShadow: '0 10px 15px -3px rgba(0,0,0,0.2)',
                            fontFamily:
                              '"Honeywell Sans Web", "Inter", sans-serif !important',
                            fontWeight: 700,
                          },
                        },
                      },
                    }}
                  >
                    <IconButton size='small' className='info-icon-pulse'>
                      <InfoOutlinedIcon sx={{ fontSize: '1rem' }} />
                    </IconButton>
                  </MuiTooltip>
                </Box>
              )}

              {permissions?.titleNameExtra && (
                <Typography
                  component='div'
                  className='grid-title-extra'
                  sx={{
                    fontSize: '0.60rem', // little smaller
                    fontWeight: 700,
                    color: '#336063', // very light grey
                    fontStyle: 'italic',
                    lineHeight: 1.4,
                    fontFamily:
                      '"Honeywell Sans Web", "Inter", sans-serif !important',
                  }}
                >
                  *Enter Number of Continious / Discontinious GCOs per grade for
                  each Month.
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
                  sx={{
                    display:
                      permissions?.IS_PE_C2_HIDE !== false ? 'block' : 'none',
                    minWidth: 140,
                  }}
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
                    {'Select'}
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

              {permissions?.showPackagingYear && (
                <TextField
                  select
                  value={
                    selectedPackagingYear || permissions?.packagingYears?.[0]
                  }
                  onChange={(e) => {
                    handleYearChange(e.target.value)
                  }}
                  variant='outlined'
                  size='small'
                  className='custom-select-textfield'
                  sx={{
                    minWidth: 120,
                  }}
                  InputProps={{
                    startAdornment: (
                      <Typography
                        variant='caption'
                        sx={{
                          mr: 0.5,
                          color: '#606060',
                          fontWeight: 600,
                          fontSize: '14px',
                          textTransform: 'uppercase',
                          letterSpacing: '0.4px',
                          lineHeight: 1,
                        }}
                      >
                        Year:
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
                    Select
                  </MenuItem>

                  {permissions?.packagingYears?.map((year) => (
                    <MenuItem
                      key={year}
                      value={year}
                      className='menu-item-style'
                    >
                      {year}
                    </MenuItem>
                  ))}
                </TextField>
              )}

              {/* ITEMS BADGE */}
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
              {isEditable && (
                <Box
                  sx={{
                    p: '4px 8px',
                    borderRadius: '100px',
                    backgroundColor: '#F3EEE7',
                    border: '1px solid #934403',
                  }}
                >
                  <Typography
                    sx={{
                      fontSize: '12px',
                      fontWeight: 700,
                      color: '#934403',
                      fontFamily: "'Honeywell Cond Web',  'Inter', sans-serif",
                    }}
                  >
                    Editable
                  </Typography>
                </Box>
              )}
            </Box>

            {/* Right side - All other actions */}
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                paddingBottom: 0.25,
              }}
            >
              {permissions?.showUnit && (
                <React.Fragment>
                  <TextField
                    select
                    value={selectedUnit || permissions?.units?.[0]}
                    onChange={(e) => {
                      setEdit({})
                      setEditResetKey((k) => k + 1)
                      setSelectedUnit(e.target.value)
                      handleUnitChange(e.target.value)
                    }}
                    variant='outlined'
                    size='small'
                    disabled={rows?.length === 0}
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
                          Unit:
                        </Typography>
                      ),
                    }}
                    className='custom-select-textfield'
                    sx={{
                      minWidth: 120,
                    }}
                    SelectProps={{
                      MenuProps: {
                        disableScrollLock: true,
                        PaperProps: {
                          sx: {
                            borderRadius: '8px',
                            mt: 0.5,
                            boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
                            '& .MuiMenuItem-root': {
                              fontSize: '14px',
                              fontWeight: 700,
                              minHeight: '26px',
                              margin: '1px 4px',
                              fontFamily:
                                "'Honeywell Sans Web', 'Inter', sans-serif",
                              borderRadius: '7px',
                              '&.Mui-selected': {
                                bgcolor: 'rgba(1, 0, 203, 0.08)',
                                color: '#0100cb',
                                fontWeight: 700,
                                '&:hover': {
                                  bgcolor: 'rgba(1, 0, 203, 0.12)',
                                },
                              },
                            },
                          },
                        },
                      },
                    }}
                    // disabled={rows?.length === 0}
                  >
                    <MenuItem value='' disabled className='menu-item-style'>
                      UOM
                    </MenuItem>

                    {/* Render the correct unit options dynamically */}
                    {permissions?.units?.map((unit) => (
                      <MenuItem
                        key={unit}
                        value={unit}
                        className='menu-item-style'
                      >
                        {unit}
                      </MenuItem>
                    ))}
                  </TextField>
                  <Divider
                    sx={{
                      my: '4px !important',
                      borderColor: `${DashboardColors.divider} !important`,
                      width: 2,
                      borderWidth: 1.5,
                    }}
                    orientation='vertical'
                    flexItem={{ mx: 2 }}
                  />
                </React.Fragment>
              )}

              {permissions?.showDisabledUOM && (
                <DisabledUOM disabledUOM={permissions?.disabledUOM} />
              )}

              {permissions?.showModes && (
                <TextField
                  select
                  value={selectMode ?? ''}
                  onChange={(e) => setSelectMode(e.target.value)}
                  variant='outlined'
                  size='small'
                  className='custom-select-textfield'
                  sx={{
                    minWidth: 140,
                  }}
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
                        Mode:
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
                    Mode
                  </MenuItem>

                  {permissions.modes.map((m) => (
                    <MenuItem
                      key={m.name}
                      value={m.name}
                      className='menu-item-style'
                    >
                      {m.displayName}
                    </MenuItem>
                  ))}
                </TextField>
              )}
              {permissions?.addButton && (
                <Button
                  variant='contained'
                  className='btn-add'
                  startIcon={<AddIcon sx={{ color: '#4A4DDA !important' }} />}
                  onClick={handleAddRow}
                  disabled={isButtonDisabled || READ_ONLY}
                >
                  Add Item
                </Button>
              )}

              {permissions?.downloadExcelBtn && (
                <Button
                  variant='contained'
                  className='btn-export'
                  startIcon={
                    <Box
                      component='img'
                      src={FileExportIcon}
                      className='w16-icon'
                    />
                  }
                  onClick={downloadExcelForConfiguration}
                  // disabled={isButtonDisabled || READ_ONLY || rows?.length === 0}
                  //ANY ONE CAN EXPORT
                  disabled={isButtonDisabled || rows?.length === 0}
                >
                  Export
                </Button>
              )}

              {permissions?.downloadExcelBtnFromUI && (
                <Button
                  variant='contained'
                  className='btn-export'
                  startIcon={
                    <Box
                      component='img'
                      src={FileExportIcon}
                      className='w16-icon'
                    />
                  }
                  onClick={excelExport}
                  // disabled={READ_ONLY || rows?.length === 0}
                  disabled={rows?.length === 0}
                  //ANY ONE CAN EXPORT
                >
                  Export
                </Button>
              )}

              {permissions?.uploadExcelBtn && (
                <>
                  <Button
                    onClick={triggerFileUpload}
                    variant='contained'
                    startIcon={
                      <Box
                        component='img'
                        src={FileImportIcon}
                        className='w16-icon'
                      />
                    }
                    className='btn-import'
                    disabled={isButtonDisabled || READ_ONLY}
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
                  startIcon={
                    <Box component='img' src={SaveIcon} className='w16-icon' />
                  }
                  onClick={saveModalOpen}
                  disabled={
                    isButtonDisabled ||
                    READ_ONLY ||
                    (!summaryEdited && Object.keys(modifiedCells).length === 0)
                  }
                  {...(loading ? {} : {})}
                >
                  Save
                </Button>
              )}

              {permissions?.showCalculate && (
                <Button
                  variant='contained'
                  onClick={handleCalculateBtn}
                  startIcon={
                    <Box
                      component='img'
                      src={CalculateIcon}
                      className='w16-icon'
                    />
                  }
                  disabled={
                    READ_ONLY ||
                    (rows?.length === 0
                      ? false
                      : isButtonDisabled ||
                        !permissions?.showCalculateVisibility)
                  }
                  className='btn-calculate'
                >
                  {permissions?.calculateBtnText || 'Calculate'}
                </Button>
              )}

              {(permissions?.deleteAllBtn || permissions?.deleteMultiple) && (
                <Button
                  variant='contained'
                  className='btn-calculate'
                  onClick={handleOpenDeleteMultipleDialog}
                  disabled={isButtonDisabled || READ_ONLY || !showDeleteAll}
                >
                  Delete
                </Button>
              )}

              {/* {showDeleteAll && (
              <Button
                variant='contained'
                className='btn-save'
                onClick={handleDeleteSelected}
                disabled={isButtonDisabled || READ_ONLY}
                loading={loading} // Use the loading prop to trigger loading state
                loadingposition='start' // Use loadingPosition to control where the spinner appears
              >
                Delete
              </Button>
            )} */}

              {permissions?.showResetButton && (
                <Button
                  variant='contained'
                  className='btn-save'
                  onClick={resetDataModalOpen}
                  disabled={
                    isButtonDisabled ||
                    READ_ONLY ||
                    (!summaryEdited && Object.keys(modifiedCells).length === 0)
                  }
                  startIcon={<RestartAltIcon sx={{ color: '#4A4DDA' }} />}
                >
                  Reset
                </Button>
              )}

              {permissions?.showRefresh && (
                <Button
                  variant='contained'
                  onClick={handleCalculateBtn}
                  disabled={isButtonDisabled || READ_ONLY}
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
                  disabled={isButtonDisabled || READ_ONLY}
                >
                  Refresh
                </Button>
              )}

              {permissions?.showReleaseBtn && (
                <Button
                  variant='contained'
                  className='btn-save'
                  disabled={isReleaseDisabled || READ_ONLY}
                  onClick={handleRelease}
                  startIcon={<PublishIcon sx={{ color: '#4A4DDA' }} />}
                >
                  Release
                </Button>
              )}
            </Box>
          </Box>
        </Box>
      )}

      <Collapse in={gridExpanded}>
        <div className='kendo-data-grid'>
          <Tooltip openDelay={50} position='auto' anchorElement='target'>
            <ExcelExport
              data={rows}
              ref={_export}
              fileName={`${permissions?.ExcelName}.xlsx`}
            >
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
                key={groupBy}
                modifiedCells={modifiedCells}
                autoProcessData={true}
                defaultGroup={initialGroup}
                data={rows}
                rows={{ data: CustomRow }}
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
                defaultTake={50}
                contextMenu={true}
                grade={grades}
                onRowClick={handleRowClick}
                onHeaderSelectionChange={onHeaderSelectionChange}
                onSelectionChange={onSelectionChange}
                groupable={
                  permissions?.isTotalFooterActive
                    ? {
                        enabled: false,
                        footer: 'visible',
                        showGroupPanel: false,
                      }
                    : {
                        enabled: false,
                        footer: 'none',
                        showGroupPanel: false,
                      }
                }
                cells={
                  permissions?.isTotalFooterActive
                    ? {
                        groupFooter: MyFooterCustomCell,
                      }
                    : undefined
                }
                allRedCell={allRedCell}
                allRedCell2={allRedCell2}
                size='small'
                pageable={
                  permissions?.makePagable === false
                    ? false
                    : rows?.length > 100
                      ? {
                          buttonCount: 4,
                          pageSizes: [10, 50, 100],
                        }
                      : false
                }
                sortable={true}
                sort={sort}
                onSortChange={(e) => setSort(e.sort)}
                lockGroups={true}
                columnWidth={150}
              >
                {permissions?.deleteMultiple && (
                  <GridColumn
                    locked={true}
                    field='selected'
                    width='50px'
                    headerSelectionValue={
                      selectedRows?.length > 0 &&
                      selectedRows?.length === rows?.length
                    }
                    cells={{
                      data: (props) => (
                        <td style={{ textAlign: 'center' }}>
                          <Checkbox
                            checked={selectedRows?.includes(
                              props.dataItem?.idFromApi,
                            )}
                            onChange={() => {
                              const id = props.dataItem?.idFromApi
                              if (selectedRows?.includes(id)) {
                                setSelectedRows(
                                  selectedRows?.filter((r) => r !== id),
                                )
                              } else {
                                setSelectedRows([...selectedRows, id])
                              }
                            }}
                          />
                        </td>
                      ),
                      headerCell: () => (
                        <th
                          style={{
                            textAlign: 'center',
                            padding: '0px !important',
                          }}
                        >
                          <Checkbox
                            checked={
                              selectedRows?.length > 0 &&
                              selectedRows?.length === rows?.length
                            }
                            onChange={(e) => {
                              const checked =
                                e?.value ?? e?.target?.checked ?? false
                              if (checked) {
                                setSelectedRows(rows.map((r) => r?.idFromApi))
                              } else {
                                setSelectedRows([])
                              }
                            }}
                          />
                        </th>
                      ),
                    }}
                  />
                )}
                {groupBy && <ExcelExportColumn field={groupBy} title='Type' />}

                {columns?.map((col) => {
                  {
                    permissions?.unitForExcelToadd && (
                      <ExcelExportColumn field={selectedUOM} title='UOM' />
                    )
                  }
                  const isActive = isColumnActive(col?.field, filter, sort)
                  if (
                    IS_VCM_VERTICAL &&
                    (col?.field === 'maintStartDateTime' ||
                      col?.field === 'maintEndDateTime')
                  ) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        cells={{
                          edit: {
                            date: DateTimePickerEditor24HourFormat,
                          },
                          data: (props) => (
                            <SimpleHighlightCell
                              {...props}
                              customModifiedCells={customModifiedCells}
                              highlight={permissions?.highlightDate || false}
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        format={'{0:dd-MM-yyyy HH:mm}'}
                        editor='date'
                        hidden={col?.hidden}
                        filter='date'
                        columnMenu={ColumnMenuCheckboxFilterDate}
                        width={setWidth(col?.minWidth || 150)}
                        headerClassName={
                          isDateFilterActive.includes(col?.field)
                            ? 'active-column'
                            : ''
                        }
                      />
                    )
                  }

                  if (dateFields.includes(col?.field)) {
                    if (
                      screenType === 'ElastomerSlowdown' &&
                      lowerVertName === 'elastomer'
                    ) {
                      return (
                        <GridColumn
                          locked={col.locked || false}
                          key={col?.field}
                          field={col?.field}
                          title={col?.title || col?.headerName}
                          cells={{
                            edit: {
                              date: (props) => (
                                <DatePickerNoLimit
                                  {...props}
                                  min={startDate}
                                  max={endDate}
                                />
                              ),
                            },
                            data: (props) => (
                              <SimpleHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                highlight={permissions?.highlightDate || false} // Add this permission
                              />
                            ),
                            headerCell: SimpleHeaderWithTooltip,
                          }}
                          format={
                            [
                              'fromDate',
                              'toDate',
                              'periodFrom',
                              'periodTo',
                              'toDateReport',
                              'fromDateReport',
                            ].includes(col?.field)
                              ? '{0:dd-MM-yyyy}'
                              : '{0:dd-MM-yyyy hh:mm a}'
                          }
                          editor='date'
                          hidden={col?.hidden}
                          // columnMenu={DateColumnMenu}
                          filter='date'
                          columnMenu={ColumnMenuCheckboxFilterDate}
                          width={setWidth(col?.minWidth || 150)}
                          headerClassName={
                            isDateFilterActive.includes(col?.field)
                              ? 'active-column'
                              : ''
                          }
                        />
                      )
                    }
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        cells={{
                          edit: {
                            date: (props) => {
                              const { dataItem, field } = props
                              const isStart = field === 'maintStartDateTime'
                              const isEnd = field === 'maintEndDateTime'
                              const type = dataItem?.type
                              const isDisabled =
                                (isEnd && type === 'ramp-down') ||
                                (isStart && type === 'ramp-up')
                              if (isDisabled) {
                                return (
                                  <SimpleHighlightCell
                                    {...props}
                                    customModifiedCells={customModifiedCells}
                                    highlight={
                                      permissions?.highlightDate || false
                                    }
                                  />
                                )
                              }
                              return [
                                'fromDate',
                                'toDate',
                                'periodTo',
                                'periodFrom',
                                'toDateReport',
                                'fromDateReport',
                              ].includes(col?.field) ? (
                                <DateOnlyPicker {...props} />
                              ) : (
                                <DateTimePickerEditor {...props} />
                              )
                            },
                          },
                          data: (props) => {
                            const { dataItem, field } = props
                            const isStart = field === 'maintStartDateTime'
                            const isEnd = field === 'maintEndDateTime'
                            const type = dataItem?.type
                            const isCellDisabled =
                              (isEnd && type === 'ramp-down') ||
                              (isStart && type === 'ramp-up')

                            if (isCellDisabled) {
                              // ✅ formatTo12Hr instead of toLocaleString('en-GB')
                              const display = formatTo12Hr(dataItem[field])
                              return (
                                <td
                                  onClick={(e) => e.stopPropagation()}
                                  style={{
                                    backgroundColor: '#f0f0f0',
                                    color: '#888',
                                    cursor: 'not-allowed',
                                  }}
                                  title={display}
                                >
                                  {display}
                                </td>
                              )
                            }

                            return (
                              <SimpleHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                highlight={permissions?.highlightDate || false}
                              />
                            )
                          },
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        format={
                          [
                            'fromDate',
                            'toDate',
                            'periodFrom',
                            'periodTo',
                            'toDateReport',
                            'fromDateReport',
                          ].includes(col?.field)
                            ? '{0:dd-MM-yyyy}'
                            : '{0:dd-MM-yyyy hh:mm a}'
                        }
                        editor='date'
                        hidden={col?.hidden}
                        filter='date'
                        columnMenu={ColumnMenuCheckboxFilterDate}
                        width={setWidth(col?.minWidth || 150)}
                        headerClassName={
                          isDateFilterActive.includes(col?.field)
                            ? 'active-column'
                            : ''
                        }
                      />
                    )
                  }

                  if (dateFields1.includes(col?.field)) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        cells={{
                          edit: {
                            date: [
                              'ibrSD',
                              'ibrED',
                              'taSD',
                              'taED',
                              'sdED',
                              'sdSD',
                              'targetDate',
                              'StartDate',
                              'EndDate',
                            ].includes(col?.field)
                              ? DateOnlyPicker
                              : DateOnlyPicker,
                          },
                          data: (props) => (
                            <RedHighlightCell
                              {...props}
                              customModifiedCells={customModifiedCells}
                              allRedCell={allRedCell}
                              disableRedHighlight={disableRedHighlight}
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        format={
                          [
                            'ibrSD',
                            'ibrED',
                            'taSD',
                            'taED',
                            'sdED',
                            'sdSD',
                            'targetDate',
                            'StartDate',
                            'EndDate',
                          ].includes(col?.field)
                            ? '{0:dd-MM-yyyy}'
                            : '{0:dd-MM-yyyy}'
                        }
                        editor='date'
                        hidden={col?.hidden}
                        filter='date'
                        // columnMenu={DateColumnMenu}
                        columnMenu={ColumnMenuCheckboxFilterDate}
                      />
                    )
                  }
                  if (
                    lowerVertName === 'vcm' &&
                    monthFields.includes(col?.field) &&
                    permissions?.highlightShutdownConsumption
                  ) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: { text: NoSpinnerNumericEditor }, // <-- Add this line for editing
                          data: (props) => (
                            <VcmDmdMonthHighlightCell
                              {...props}
                              shutdownMonths={shutdownMonths}
                              slowdownMonths={slowdownMonths}
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }
                  if (col?.field === 'symbol') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key='symbol'
                        field='symbol'
                        width={setWidth(col?.minWidth || 150)}
                        title={col?.title}
                        editable={col?.editable || true}
                        cells={{
                          data: (cellProps) => (
                            <BudgetConstrainsCellEditor {...cellProps} />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        headerClassName={isActive ? 'active-column' : ''}
                      />
                    )
                  }
                  if (col?.field === 'limit') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key='limit'
                        field='limit'
                        width={setWidth(col?.minWidth || 150)}
                        title={col?.title}
                        editable={col?.editable || true}
                        cells={{
                          data: (cellProps) => (
                            <LimitCellEditor
                              {...cellProps}
                              READ_ONLY={READ_ONLY}
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        headerClassName={isActive ? 'active-column' : ''}
                      />
                    )
                  }

                  if (col?.field === 'discriptionDrpdwn') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key='discriptionDrpdwn'
                        field='discriptionDrpdwn'
                        title={col?.title || col?.headerName || 'Particulars'}
                        editable={col?.editable || true}
                        hidden={col?.hidden}
                        width={setWidth(col?.minWidth || 150)}
                        cells={{
                          data: (cellProps) => (
                            <ProductCell
                              {...cellProps}
                              allProducts={allDescriptionDrpdwn}
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }
                  if (
                    col?.field === 'discription' &&
                    col?.type === 'discriptionDrpdwn'
                  ) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key='discription'
                        field='discription'
                        title={col?.title || col?.headerName || 'Particulars'}
                        editable={col?.editable || true}
                        hidden={col?.hidden}
                        cells={{
                          data: (cellProps) => (
                            <ProductCell
                              {...cellProps}
                              allProducts={allDescriptionDrpdwn}
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        width={setWidth(col?.minWidth || 150)}
                      />
                    )
                  }

                  if (col?.field === 'productName1') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key='productName1'
                        field='productName1'
                        title={col?.title || col?.headerName || 'Particulars'}
                        width={setWidth(col?.minWidth || 150)}
                        editable={col?.editable || true}
                        hidden={col?.hidden}
                        cells={{
                          data: (cellProps) => (
                            <ProductCell
                              {...cellProps}
                              customModifiedCells={customModifiedCells}
                              highlightField='productName1'
                              rowId={cellProps.dataItem.id}
                              allProducts={allProducts}
                              highlight={!!permissions?.highlightProductName1}
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }

                  if (col?.field === 'month') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key='month'
                        field='month'
                        title={col?.title || col?.headerName || 'month'}
                        editable={col?.editable || true}
                        hidden={col?.hidden}
                        width={setWidth(col?.minWidth || 150)}
                        cells={{
                          data: (cellProps) => (
                            <MonthCell {...cellProps} allMonths={allMonths} />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }

                  if (
                    ['discription', 'Name'].includes(col?.field) &&
                    col?.type !== 'dynamicDropdownshared'
                  ) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName || 'Description'}
                        width={setWidth(col?.minWidth || 150)}
                        editable={true}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col?.hidden}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: { text: TextCellEditor },
                          data: (props) =>
                            permissions?.highlightDiscription ? (
                              <SimpleHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                highlight={true}
                              />
                            ) : (
                              toolTipRenderer(props)
                            ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                      />
                    )
                  }

                  if (col?.type === 'descLimit') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: { text: descLimit },
                          data: toolTipRendererdescLimit,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }
                  if (col?.field === 'UOM') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key='UOM'
                        field='UOM'
                        title={col?.title || col?.headerName || 'UOM'}
                        width={setWidth(col?.minWidth || 150)}
                        editable={false}
                        columnMenu={ColumnMenuCheckboxFilter}
                        headerClassName={isActive ? 'active-column' : ''}
                        hidden={col?.hidden}
                        cells={{
                          data: toolTipRenderer,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                      />
                    )
                  }
                  if (col?.field === 'ReceipeName') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key='ReceipeName'
                        field='ReceipeName'
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        editable={false}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col?.hidden}
                        cells={{
                          data: toolTipRenderer,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                      />
                    )
                  }
                  if (col?.type === 'Receipe') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={
                          col?.isDisabled
                            ? 'k-number-right-disabled'
                            : 'k-number-right'
                        }
                        editable={col?.editable ? true : false}
                        headerClassName={numericHeaderClass(isActive, col)}
                        cells={{
                          edit: { text: NoSpinnerNumericEditor },
                          data: toolTipRenderer,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }

                  if (
                    col?.field === 'sapMaterialCode' &&
                    col?.useMethodColors
                  ) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          data: MaterialDisplayNameCell,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }

                  //NEW

                  if (
                    col?.field === 'shutdownRate' &&
                    col?.type === 'shutdownRateDropdown'
                  ) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col.hidden}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: {
                            text: (props) => (
                              <MonthDropdownPEPP1
                                {...props}
                                options={allDescriptionDrpdwn}
                              />
                            ),
                          },
                          data: (props) =>
                            permissions?.MonthDropdownPEPPHighlight ? (
                              <SimpleHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                highlight={true}
                              />
                            ) : (
                              MonthDropdownPEPPDisplayCell(props)
                            ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }

                  // ...existing code...
                  if (col?.type === 'monthDropdownPEPP') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: { text: MonthDropdownPEPP },
                          data: (props) =>
                            permissions?.MonthDropdownPEPPHighlight ? (
                              <SimpleHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                highlight={true}
                              />
                            ) : (
                              MonthDropdownPEPPDisplayCell(props)
                            ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }

                  if (col?.type === 'monthDropdown') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: { text: MonthDropdownEditor },
                          data: ElastomerMonthDisplayCell,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }
                  if (col?.type === 'Categorydropdown') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        editable={!!col?.editable}
                        cells={{
                          edit: { text: CategoryDropdownEditor },
                          data: (props) => {
                            // Show the value as text in display mode
                            const options = [
                              { id: 0, value: '0' },
                              { id: 1, value: '1' },
                              { id: 2, value: '2' },
                            ]
                            const valueObj = options.find(
                              (opt) => opt.id === props.dataItem[props.field],
                            )
                            return (
                              <td {...props.tdProps}>
                                {valueObj ? valueObj.value : ''}
                              </td>
                            )
                          },
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }
                  const YearDropdownEditorWrapper = (props) => (
                    <YearDropdownEditor {...props} AOP_YEAR={AOP_YEAR} />
                  )
                  if (col?.type === 'yeardropdown') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={!!col?.editable}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: { text: YearDropdownEditorWrapper },
                          data: ElastomerYearDisplayCell,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }
                  if (col?.type === 'typesdDropdown') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={!!col?.editable}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: {
                            text: (props) => (
                              <SDDaysDropdownEditorWrapper
                                {...props}
                                sdDaysValues={sdDaysValues}
                              />
                            ),
                          },
                          data: ElastomerSDDaysDisplayCell,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }
                  const LineDropdownEditorWrapper = (props) => (
                    <LineDropdownEditor
                      {...props}
                      allLines={allLines}
                      customModifiedCells={customModifiedCells}
                      highlightField={props.field}
                      highlight={!!permissions?.highlightLine}
                      rowId={props.dataItem?.id}
                    />
                  )
                  if (col?.type === 'lineDropdown') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title}
                        width={setWidth(col?.minWidth || 150)}
                        editable={col?.editable}
                        cells={{
                          edit: { text: LineDropdownEditorWrapper },
                          data: (props) => (
                            <LineDisplayCell
                              {...props}
                              allLines={allLines}
                              customModifiedCells={customModifiedCells}
                              highlightField={col?.field}
                              highlight={!!permissions?.highlightLine}
                              rowId={props.dataItem?.id}
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }
                  if (col?.field === 'DisplayName') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key='DisplayName'
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        editable={false}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col?.hidden}
                        cells={{
                          data: toolTipRenderer,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                      />
                    )
                  }

                  // const isColumnActive = (field, filter, sort) => {
                  //   return (
                  //     isColumnMenuFilterActive(field, filter) ||
                  //     isColumnMenuSortActive(field, sort)
                  //   )
                  // }

                  if (
                    ['aopRemarks', 'remarks', 'remark', 'Remarks'].includes(
                      col?.field,
                    )
                  ) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        // editor={true}
                        // editable={{ mode: 'popup' }}
                        cells={{
                          data: (cellProps, allRedCell) => (
                            <RemarkCell
                              {...cellProps}
                              allRedCell={allRedCell}
                              onRemarkClick={handleRemarkCellClick}
                              customModifiedCells={customModifiedCells}
                              suppressRemarksPlaceholder={
                                permissions?.suppressRemarksPlaceholder
                              }
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col?.hidden}
                        headerClassName={isActive ? 'active-column' : ''}
                        width={setWidth(col?.minWidth || 150)}
                      />
                    )
                  }
                  if (col?.field === 'durationInHrs') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        editable={true}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col?.hidden}
                        format={'{0:n2}'}
                        className={
                          col?.isDisabled
                            ? 'k-number-right-disabled'
                            : 'k-number-right'
                        }
                        cells={{
                          edit: { text: DurationEditor },
                          data: (props) =>
                            permissions?.highlightDuration ? (
                              <DurationHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                              />
                            ) : (
                              DurationDisplayWithTooltipCell(props)
                            ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        headerClassName={isActive ? 'active-column' : ''}
                      />
                    )
                  }

                  if (
                    col.field === 'Duration' &&
                    lowerVertName === 'cracker' &&
                    lowerSiteName === 'vmd'
                  ) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.minWidth || 100)}
                        hidden={col.hidden}
                        editable={false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          data: (props) => (
                            <td
                              {...props.tdProps}
                              title={props.dataItem[props.field]}
                              style={{ textAlign: 'right' }}
                            >
                              {props.dataItem[props.field]}
                            </td>
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }

                  if (col?.field === 'rpfDownTime') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        editable={true}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col?.hidden}
                        format={'{0:n2}'}
                        className='k-number-right'
                        cells={{
                          edit: { text: DurationEditor },
                          data: (props) =>
                            DurationDisplayWithTooltipCell(props),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        headerClassName={isActive ? 'active-column' : ''}
                      />
                    )
                  }

                  if (col?.hideFilter && col?.hideSort) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        hidden={col?.hidden}
                        width={setWidth(col?.minWidth || 150)}
                        className={
                          col?.isDisabled
                            ? 'k-number-right-disabled'
                            : 'k-number-right'
                        }
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: { text: NoSpinnerNumericEditor },
                          data: (props) => (
                            <RedHighlightCell
                              {...props}
                              customModifiedCells={customModifiedCells}
                              allRedCell={allRedCell}
                              disableRedHighlight={disableRedHighlight}
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        format={col?.format}
                        sortable={false}
                      />
                    )
                  }

                  if (col?.type === 'propaneDropdown') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: { text: PropaneDropdown }, // <-- Use your custom editor here
                          data: MonthDisplayCell,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }
                  if (col?.type === 'dynamicDropdown') {
                    const dropdownOptions =
                      permissions?.dynamicDropdownOptions || []

                    const DynamicDisplayCell = (props) => {
                      const { dataItem, field, tdProps } = props
                      const value = dataItem[field]
                      const rowId = dataItem.id

                      const isEdited = Object.prototype.hasOwnProperty.call(
                        customModifiedCells?.[rowId] || {},
                        field,
                      )

                      const shouldHighlight = isEdited

                      return (
                        <td
                          {...tdProps}
                          title={value}
                          className={`${tdProps?.className || ''} ${shouldHighlight ? 'edited-cell' : ''}`.trim()}
                        >
                          {value}
                        </td>
                      )
                    }

                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 100)}
                        hidden={col?.hidden}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: {
                            text: (props) => (
                              <DynamicDropdown
                                {...props}
                                options={
                                  col?.getDropdownOptions
                                    ? col.getDropdownOptions(props.dataItem)
                                    : dropdownOptions
                                }
                                getDropdownOptions={col?.getDropdownOptions}
                              />
                            ),
                          },
                          data: DynamicDisplayCell,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }

                  if (col.type === 'dynamicDropdownshared') {
                    const dropdownOptions =
                      col.dropdownOptions ||
                      permissions?.dynamicDropdownOptions?.[col.field] ||
                      []
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={col.width}
                        hidden={col.hidden}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: {
                            text: (props) => (
                              <DynamicDropdown
                                {...props}
                                options={dropdownOptions}
                              />
                            ),
                          },
                          data: MonthDisplayCell,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }
                  if (col?.type === 'percentChange') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={'k-number-right'}
                        editable={col?.editable ? true : false}
                        headerClassName={numericHeaderClass(isActive, col)}
                        cells={{
                          edit: { text: NoSpinnerNumericEditorNegative },
                          data: toolTipRenderer,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }

                  if (col?.type === 'negativeNumber') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={`
                  ${col?.isDisabled ? 'k-number-right-disabled' : 'k-number-right'}
                  ${col?.isBold ? 'bold-text' : ''}
                `}
                        editable={col?.editable ? true : false}
                        headerClassName={numericHeaderClass(isActive, col)}
                        cells={{
                          edit: { text: NoSpinnerNumericEditorNegative },
                          data: (props) =>
                            showThreeColors ? (
                              <RedHighlightCell2
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                allRedCell2={allRedCell2}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ) : (
                              <RedHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }

                  if (col?.type === 'numberWithUOMValidation') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={`
                  ${col?.isDisabled ? 'k-number-right-disabled' : 'k-number-right'}
                  ${col?.isBold ? 'bold-text' : ''}
                `}
                        editable={col?.editable ? true : false}
                        headerClassName={numericHeaderClass(isActive, col)}
                        cells={{
                          edit: {
                            text: NoSpinnerNumericEditorWithUOMValidation,
                          },
                          data: (props) =>
                            showThreeColors ? (
                              <RedHighlightCell2
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                allRedCell2={allRedCell2}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ) : (
                              <RedHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }
                  if (col?.type === 'integerOnlyForDays') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={`
        ${col?.isDisabled ? 'k-number-right-disabled' : 'k-number-right'}
        ${col?.isBold ? 'bold-text' : ''}
      `}
                        editable={col?.editable ? true : false}
                        headerClassName={numericHeaderClass(isActive, col)}
                        cells={{
                          edit: {
                            text: IntegerDaysEditor, // ← stable ref, no focus loss
                          },
                          data: (dataProps) =>
                            showThreeColors ? (
                              <RedHighlightCell2
                                {...dataProps}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                allRedCell2={allRedCell2}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ) : (
                              <RedHighlightCell
                                {...dataProps}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }
                  if (col?.field === 'rate') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={
                          col?.title || col?.headerName || 'Rate Reduced (TPH)'
                        }
                        width={setWidth(col?.minWidth || 150)}
                        editable={col?.editable ?? true}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col?.hidden}
                        format={'{0:n2}'}
                        className={`
        ${col?.isDisabled ? 'k-number-right-disabled' : 'k-number-right'}
        ${col?.isBold ? 'bold-text' : ''}
      `}
                        cells={{
                          edit: { text: NoSpinnerNumericEditor },
                          data: (props) => (
                            <SimpleHighlightCell
                              {...props}
                              customModifiedCells={customModifiedCells}
                              highlight={!!permissions?.highlightRate}
                            >
                              {props.dataItem[props.field]}
                            </SimpleHighlightCell>
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        headerClassName={isActive ? 'active-column' : ''}
                      />
                    )
                  }

                  if (col?.crackerValidation) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={`
                  ${col?.isDisabled ? 'k-number-right-disabled' : 'k-number-right'}
                  ${col?.isBold ? 'bold-text' : ''}
                `}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: {
                            text: NoSpinnerNumericEditorCrackerValidation,
                          },
                          data: (props) =>
                            showThreeColors ? (
                              <RedHighlightCell2
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                allRedCell2={allRedCell2}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ) : (
                              <RedHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }

                  if (
                    col?.type === 'number' &&
                    permissions?.showRedCellsForOroductionTarget
                  ) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={`
        ${col?.isDisabled ? 'k-number-right-disabled' : 'k-number-right'}
        ${col?.isBold ? 'bold-text' : ''}
      `}
                        editable={col?.editable ? true : false}
                        headerClassName={numericHeaderClass(isActive, col)}
                        cells={{
                          edit: { text: NoSpinnerNumericEditor },
                          data: (props) => {
                            const productName =
                              props.dataItem?.productName ||
                              props.dataItem?.displayName ||
                              props.dataItem?.materialDisplayName ||
                              ''
                            const isMcuRed = isMcuMaxCapRedCell(
                              productName,
                              props.field,
                            )
                            if (isMcuRed) {
                              return (
                                <td
                                  {...props.tdProps}
                                  title={String(
                                    props.dataItem[props.field] ?? '',
                                  )}
                                  style={{ color: 'red', fontWeight: 'bold' }}
                                >
                                  {props.children}
                                </td>
                              )
                            }

                            return showThreeColors ? (
                              <RedHighlightCell2
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                allRedCell2={allRedCell2}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ) : (
                              <RedHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                disableRedHighlight={disableRedHighlight}
                              />
                            )
                          },
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }

                  // Dedicated block for ON/OFF dropdown rows (e.g. Business Demand UOM: 'ON/OFF')
                  // Enable via permissions.enableOnOffDropdown = true
                  if (
                    col.type === 'number' &&
                    permissions?.enableOnOffDropdown
                  ) {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col.hidden}
                        className={`
                        ${col?.isDisabled ? 'k-number-right-disabled' : 'k-number-right'}
                        ${col?.isBold ? 'bold-text' : ''}
                      `}
                        editable={col?.editable ? true : false}
                        isDisabled={col?.isDisabled}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: {
                            text: OnOffSwitchEditCell,
                          },
                          data: (props) => {
                            // ON/OFF rows: show switch with direct edit mode
                            const uomTypes = ['ON/OFF', 'YES/NO']
                            if (
                              uomTypes.includes(props.dataItem?.UOM) ||
                              permissions?.enableSwitchToggle
                            ) {
                              return (
                                <SwitchEditor
                                  {...props}
                                  directEditMode={true}
                                  onChange={(e) => itemChange(e)}
                                  customModifiedCells={customModifiedCells}
                                  rowId={props.dataItem.id}
                                  setRows={setRows}
                                  editable={
                                    props.dataItem?.[
                                      `${col.field}_editable`
                                    ] === false
                                      ? false
                                      : col?.editable
                                  }
                                  isDisabled={
                                    props.dataItem?.[`${col.field}_isDisabled`]
                                      ? true
                                      : col?.isDisabled
                                  }
                                />
                              )
                            }
                            // Regular rows
                            return showThreeColors ? (
                              <RedHighlightCell2
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                allRedCell2={allRedCell2}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ) : (
                              <RedHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                disableRedHighlight={disableRedHighlight}
                              />
                            )
                          },
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col.format}
                      />
                    )
                  }
                  if (col?.type === 'integerNumberOnly') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={`
        ${col?.isDisabled ? 'k-number-right-disabled' : 'k-number-right'}
        ${col?.isBold ? 'bold-text' : ''}
      `}
                        editable={col?.editable ? true : false}
                        headerClassName={numericHeaderClass(isActive, col)}
                        cells={{
                          edit: { text: NoSpinnerNumericIntegerEditor },
                          data: (props) =>
                            showThreeColors ? (
                              <RedHighlightCell2
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                allRedCell2={allRedCell2}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ) : (
                              <RedHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }
                  if (col?.type === 'groupedColumn') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={`
                          ${col?.isDisabled ? 'k-number-right-disabled' : 'k-number-right'}
                          ${col?.isBold ? 'bold-text' : ''}
                        `}
                        editable={col?.editable ? true : false}
                        headerClassName={numericHeaderClass(isActive, col)}
                        cells={{
                          data: GroupedColumnCell,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }
                  if (col?.type === 'number') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={`
        ${col?.isDisabled ? 'k-number-right-disabled' : 'k-number-right'}
        ${col?.isBold ? 'bold-text' : ''}
      `}
                        editable={col?.editable ? true : false}
                        headerClassName={numericHeaderClass(isActive, col)}
                        cells={{
                          edit: { text: NoSpinnerNumericEditor },
                          data: (props) =>
                            showThreeColors ? (
                              <RedHighlightCell2
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                allRedCell2={allRedCell2}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ) : (
                              <RedHighlightCell
                                {...props}
                                customModifiedCells={customModifiedCells}
                                allRedCell={allRedCell}
                                disableRedHighlight={disableRedHighlight}
                              />
                            ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }

                  if (col?.type === 'switch') {
                    const handleCheckboxChange = (props, value) => {
                      const { dataItem, field } = props
                      const { materialName, id } = dataItem

                      onGlobalCheckboxChange(
                        gridName,
                        id,
                        materialName,
                        field,
                        value,
                        dataItem,
                      )
                    }

                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title='.'
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={true}
                        cells={{
                          data: (props) => {
                            const dataItem = props.dataItem || {}
                            const normType = (dataItem.Particulars || '')
                              .toString()
                              .toLowerCase()

                            if (
                              showCatChemUtilityCheckbox &&
                              !CHECK_TYPES.includes(normType)
                            ) {
                              return <td />
                            }

                            if (
                              showCatChemUtilityCheckbox2 &&
                              !CHECK_TYPES2.includes(normType)
                            ) {
                              return <td />
                            }

                            return (
                              <td style={{ textAlign: 'center' }}>
                                <Checkbox
                                  checked={!!props.dataItem[props.field]}
                                  onChange={(e) => {
                                    const checked =
                                      e?.value ?? e?.target?.checked ?? false
                                    handleCheckboxChange(props, checked)
                                  }}
                                />
                              </td>
                            )
                          },
                          headerCell: BlankHeader,
                        }}
                      />
                    )
                  }

                  if (col?.type === 'switch2') {
                    const handleCheckboxChange = (props, value) => {
                      const { dataItem, field } = props
                      const { materialName, id } = dataItem

                      onGlobalCheckboxChange(
                        gridName,
                        id,
                        materialName,
                        field,
                        value,
                        dataItem,
                      )
                    }

                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title='.'
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={true}
                        cells={{
                          data: (props) => {
                            const dataItem = props.dataItem || {}
                            const normType = (dataItem.Particulars || '')
                              .toString()
                              .toLowerCase()

                            if (
                              showCatChemUtilityCheckbox2 &&
                              CHECK_TYPES2.includes(normType)
                            ) {
                              return <td />
                            }

                            return (
                              <td style={{ textAlign: 'center' }}>
                                <Checkbox
                                  checked={!!props.dataItem[props.field]}
                                  onChange={(e) => {
                                    const checked =
                                      e?.value ?? e?.target?.checked ?? false
                                    handleCheckboxChange(props, checked)
                                  }}
                                />
                              </td>
                            )
                          },
                          headerCell: BlankHeader,
                        }}
                      />
                    )
                  }

                  if (col?.type === 'numberWidth') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        className={
                          col?.isDisabled
                            ? 'k-number-right-disabled'
                            : 'k-number-right'
                        }
                        editable={col?.editable ? true : false}
                        headerClassName={numericHeaderClass(isActive, col)}
                        cells={{
                          edit: { text: NoSpinnerNumericEditor },
                          data: toolTipRenderer,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        filter='numeric'
                        format={col?.format}
                      />
                    )
                  }

                  if (col?.field === 'ConstantValue') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={!!col?.editable}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          edit: { text: NoSpinnerNumericEditor },
                          data: toolTipRenderer,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }

                  if (col?.type === 'checkbox') {
                    return (
                      <GridColumn
                        locked={col.locked || false}
                        key={col?.field}
                        field={col?.field}
                        title={col?.title || col?.headerName}
                        width={setWidth(col?.minWidth || 150)}
                        hidden={col?.hidden}
                        editable={col?.editable ? true : false}
                        headerClassName={isActive ? 'active-column' : ''}
                        cells={{
                          data: (props) => {
                            const dataItem = props.dataItem || {}
                            const val = !!dataItem[props.field]
                            const isDisabled =
                              col?.editable === false || READ_ONLY
                            return (
                              <td style={{ textAlign: 'center' }}>
                                <Checkbox
                                  checked={val}
                                  onChange={(e) => {
                                    const checked =
                                      e?.value ?? e?.target?.checked ?? false
                                    const changeEvent = {
                                      dataItem,
                                      field: props.field,
                                      value: checked,
                                    }
                                    itemChange(changeEvent)
                                  }}
                                  disabled={isDisabled}
                                />
                              </td>
                            )
                          },
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                      />
                    )
                  }

                  return (
                    <GridColumn
                      locked={col.locked || false}
                      key={col?.field}
                      field={col?.field}
                      title={col?.title || col?.headerName}
                      width={setWidth(col?.minWidth || 150)}
                      hidden={col?.hidden}
                      editable={col?.editable ? true : false}
                      headerClassName={isActive ? 'active-column' : ''}
                      cells={{
                        edit: { text: TextCellEditor },
                        data: toolTipRenderer,
                        headerCell: SimpleHeaderWithTooltip,
                      }}
                      columnMenu={ColumnMenuCheckboxFilter}
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
                      headerCell: SimpleHeaderWithTooltip,
                    }}
                  />
                )}
              </Grid>
            </ExcelExport>
          </Tooltip>
        </div>
      </Collapse>

      {gridExpanded && (permissions?.approveBtn || permissions?.nextBtn) && (
        <Box className='action-box'>
          {/* {permissions?.showCreateCasebutton && (
            <Button
              variant='contained'
              onClick={createCase}
              disabled={READ_ONLY ||isCreatingCase || !showCreateCasebutton}
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
              disabled={isButtonDisabled || READ_ONLY}
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
              disabled={isButtonDisabled || READ_ONLY}
              loading={loading} // Use the loading prop to trigger loading state
              loadingposition='start' // Use loadingPosition to control where the spinner appears
            >
              Next
            </Button>
          )}
        </Box>
      )}
      <Notification
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
        autoHide={snackbarData?.autoHide ?? true}
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
            bgcolor: '#fef2f2', // soft red
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
            {permissions?.showNoteWhileDeleting ? (
              <>
                Are you sure you want to delete this row?
                <Box
                  sx={{
                    mt: 1,
                    p: 1,
                    borderRadius: '6px',
                    bgcolor: '#fee2e2',
                    fontSize: '0.7rem',
                    color: '#7f1d1d',
                    fontWeight: 600,
                  }}
                >
                  {deleteNoteOnDeleteDialogeBox}
                </Box>
              </>
            ) : (
              'Are you sure you want to delete this row?'
            )}
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
        open={deleteMultipleConfirms}
        onClose={() => setDeleteMultipleConfirms(false)}
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
            bgcolor: '#fef2f2', // soft red
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
            onClick={() => setDeleteMultipleConfirms(false)}
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
            {'Are you sure you want to delete?'}{' '}
          </Typography>
        </DialogContent>

        {/* Actions */}
        <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
          <Button
            onClick={() => setDeleteMultipleConfirms(false)}
            className='btn-no'
          >
            Cancel
          </Button>

          <Button
            onClick={handleDeleteMultiple}
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
        open={openCalculateDialogeBox}
        onClose={closeCalculateDialogBox}
        aria-labelledby='alert-dialog-title'
        aria-describedby='alert-dialog-description'
        disableScrollLock
        slotProps={{
          backdrop: { disableScrollLock: true },
        }}
      >
        <DialogTitle
          sx={{
            p: 1.5,
            px: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            bgcolor: '#f6f8fa',
            borderBottom: '1px solid #DDDEE1',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box component='img' src={CalculateIcon} className='w16-icon' />
            <Typography
              sx={{
                fontWeight: 800,
                fontSize: '0.8rem',
                color: 'text.primary',
                letterSpacing: '0.4px',
              }}
            >
              Confirm Calculate
            </Typography>
          </Box>

          <IconButton
            size='small'
            onClick={closeCalculateDialogBox}
            sx={{ color: 'text.primary' }}
          >
            <CloseIcon fontSize='small' />
          </IconButton>
        </DialogTitle>

        <DialogContent sx={{ p: 1.5, pt: '12px !important' }}>
          <Typography
            sx={{
              fontSize: '0.75rem',
              color: 'text.primary',
              lineHeight: 1.5,
              fontWeight: 600,
            }}
          >
            Are you sure you want to calculate? This will override the existing
            values.
          </Typography>
        </DialogContent>

        <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
          <Button onClick={closeCalculateDialogBox} className='btn-no'>
            Cancel
          </Button>

          <Button
            onClick={handleCalculateConfirmation}
            variant='contained'
            size='small'
            className='btn-yes'
          >
            Calculate
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
            {permissions?.showNoteWhileSaving ? (
              <>
                Are you sure you want to save these changes?
                <Box
                  sx={{
                    mt: 1,
                    p: 1,
                    borderRadius: '6px',
                    bgcolor: '#f1f5f9',
                    fontSize: '0.7rem',
                    color: '#334155',
                    fontWeight: 600,
                  }}
                >
                  {noteOnSaveDialogeBox}
                </Box>
              </>
            ) : (
              'Are you sure you want to save these changes?'
            )}
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

      <Dialog
        open={openResetDataDialogeBox}
        onClose={closeResetDataDialogeBox}
        aria-labelledby='alert-dialog-title'
        aria-describedby='alert-dialog-description'
        disableScrollLock
      >
        <DialogTitle id='alert-dialog-title'>{'Reset ?'}</DialogTitle>
        <DialogContent>
          <DialogContentText id='alert-dialog-description'>
            Are you sure you want to reset these changes?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button className='btn-no' onClick={closeResetDataDialogeBox}>
            Cancel
          </Button>
          <Button className='btn-yes' onClick={resetConfirmation} autoFocus>
            Reset
          </Button>
        </DialogActions>
      </Dialog>

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
            // Power-user shortcut: Ctrl + Enter to Save
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

export default KendoDataTables
