import HelpIcon from '@mui/icons-material/Help'
import InfoIcon from '@mui/icons-material/Info'
import { Tooltip as MuiTooltip } from '@mui/material'
import '@progress/kendo-font-icons/dist/index.css'
import { Grid, GridColumn } from '@progress/kendo-react-grid'
import { Tooltip } from '@progress/kendo-react-tooltip'
import '@progress/kendo-theme-default/dist/all.css'
import { getColumnMenuCheckboxFilter } from 'components/data-tables/Reports-kendo/ColumnMenu1'
import { DateColumnMenu } from 'components/Utilities/DateColumnMenu'
import Notification from 'components/Utilities/Notification'
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import PropaneDropdown from './Utilities-Kendo/PropaneDropdown'
import RestartAltIcon from '@mui/icons-material/RestartAlt'
import { useSelector } from 'react-redux'
import YearDropdownEditor from './Utilities-Kendo/YearDropdownEditor'
import CloseIcon from '@mui/icons-material/Close'
import SDDaysDropdownEditorWrapper from './Utilities-Kendo/SdDaysDropdownEditor'

import ModeEditIcon from '@mui/icons-material/ModeEdit'
import { styled } from '@mui/material/styles'

import AddIcon from '@mui/icons-material/Add'
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd'
import DownloadIcon from '@mui/icons-material/Download'
import UploadIcon from '@mui/icons-material/Upload'
import CalculateIcon from '@mui/icons-material/Calculate'
import SaveIcon from '@mui/icons-material/Save'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import CategoryDropdownEditor from './Utilities-Kendo/CategoryDropdown'
import LineDropdownEditor from './Utilities-Kendo/LineDropdownEditor'

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
import DynamicDropdown from './Utilities-Kendo/DynamicDropdown'

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
import ShutdownRateDropdown from './Utilities-Kendo/ShutdownRateDropdown'
import MonthDropdownPEPP1 from './Utilities-Kendo/MonthDropdownPEPP1'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import { keyframes } from '@mui/material/styles'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'
import Collapse from '@mui/material/Collapse'

// Subtle pulse for the info icon on load
const softPulse = keyframes`
  0% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(1); opacity: 0.6; }
`

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

const ADJUST_PADDING = 4

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
  snackbarData = { message: '', severity: 'info' },
  snackbarOpen = false,
  setRemarkDialogOpen = () => {},
  currentRemark = '',
  setCurrentRemark = () => {},
  currentRowId = null,
  NormParameterIdCell = () => {},
  setModifiedCells = () => {},
  remarkDialogOpen = false,
  handleDeleteSelected = () => {},
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
}) => {
  const _export = useRef(null)

  const _grid = React.useRef(undefined)
  const minGridWidth = useRef(0)
  const grid = React.useRef(null)
  const gridRef = useRef(null)
  const [gridExpanded, setGridExpanded] = useState(true)
  const [openDeleteDialogeBox, setOpenDeleteDialogeBox] = useState(false)
  const [openResetDialogeBox, setOpenResetDialogeBox] = useState(false)
  const [isButtonDisabled, setIsButtonDisabled] = useState(false)
  const showDeleteAll = permissions?.deleteAllBtn && selectedUsers.length > 1
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
  const [issRowEdited, setIsRowEdited] = useState(false)
  const [isDateFilterActive, setIsDateFilterActive] = useState([])
  const ColumnMenuCheckboxFilter = getColumnMenuCheckboxFilter(rows)
  const ColumnMenuCheckboxFilterDate = getColumnMenuDateFilter(rows)
  const [customModifiedCells, setCustomModifiedCells] = useState({})
  const [applyMinWidth, setApplyMinWidth] = useState(false)
  const [gridCurrent, setGridCurrent] = useState(0)
  const dataGridStore = useSelector((state) => state.dataGridStore)

  const ADJUST_PADDING = 4
  const COLUMN_MIN = 4

  const keycloak = useSession()

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

  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const lowerSiteName = SiteName?.toLowerCase()
  const isPEPP = ['pe', 'pp'].includes(lowerVertName)
  const IS_VCM_VERTICAL = ['vcm'].includes(lowerVertName)

  const toggleGrid = () => {
    setGridExpanded((prev) => !prev)
  }

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
    if (isShutdown || isSlowdown) color = 'rgb(240, 235, 235)'

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

  const fileInputRef = useRef(null)

  const extractAllColumns = useCallback((cols) => {
    const allCols = []

    const traverse = (columns) => {
      columns?.forEach((col) => {
        if (!col || Object.keys(col).length === 0) return

        if (col.children && Array.isArray(col.children)) {
          traverse(col.children)
        } else if (col.field && col.hidden !== true) {
          allCols.push(col)
        }
      })
    }

    traverse(cols)
    return allCols
  }, [])

  React.useEffect(() => {
    grid.current = document.querySelector('.k-grid')
    window.addEventListener('resize', handleResize)
    columns.map((item) =>
      item.minWidth !== undefined
        ? (minGridWidth.current += item.minWidth)
        : minGridWidth.current,
    )
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

  const setWidth = (minWidth) => {
    if (minWidth === undefined) {
      minWidth = 0
    }
    let width = applyMinWidth
      ? minWidth
      : minWidth + (gridCurrent - minGridWidth.current) / columns.length
    if (width >= COLUMN_MIN) {
      width -= ADJUST_PADDING
    }
    return width
  }

  const handleEditChange = useCallback((e) => {
    setEdit(e.edit)
  }, [])

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
      setIsRowEdited(true)

      const { dataItem, field } = e
      let { value } = e
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

      // months list in the order you provided
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
                const newEnd = recalcEndDate(base.maintStartDateTime, value)
                if (newEnd) base.maintEndDateTime = newEnd.toISOString()
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
    },
    [
      setRows,
      setModifiedCells,
      setCustomModifiedCells,
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
      }

      return updatedRows
    })

    setRemarkDialogOpen(false)
  }

  console.log('columns', columns)
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
      ...Object.fromEntries(columns?.map((col) => [col.field, ''])),
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
    const lineObj = props.allLines?.find((l) => l.id === dataItem[field])
    const displayLabel = lineObj ? lineObj.displayName : ''
    return (
      <td
        {...tdProps}
        style={{
          color: highlight && isEdited ? 'orange' : undefined,
          fontWeight: highlight && isEdited ? 'bold' : undefined,
        }}
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
    const method = dataItem.Method

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
      <td
        {...tdProps}
        title={value}
        style={{
          color,
          //  fontWeight: method ? 'bold' : 'normal',
          ...tdProps.style,
        }}
      >
        {children}
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
    return (
      <td
        {...tdProps}
        title={value}
        style={{
          color: highlight && isEdited ? 'orange' : undefined,
          fontWeight:
            (highlight && isEdited) || isBoldFromCells ? 'bold' : undefined,
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

    const month = field
    const normId = dataItem.materialFkId || dataItem.NormParameter_FK_Id

    const isRedFromAllRedCell = allRedCell?.some(
      (cell) =>
        cell.month === month &&
        cell.NormParameter_FK_Id?.toLowerCase() === normId?.toLowerCase(),
    )

    const shouldHighlight = isEdited || isRedFromAllRedCell

    return (
      <td
        {...tdProps}
        title={value}
        style={{
          color: shouldHighlight ? 'orange' : undefined,
          fontWeight: shouldHighlight || isBoldFromCells ? 'bold' : undefined,
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
        style={{
          color: highlightColor,
          fontWeight: highlightColor || isBoldFromCells ? 'bold' : undefined,
          // backgroundColor: highlightColorFullCell ? 'lightGrey' : undefined,
        }}
      >
        {children}
      </td>
    )
  }

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

    return (
      <td
        {...props.tdProps}
        title={value}
        style={{
          color: isRed ? 'orange' : undefined,
          fontWeight: isRed ? 'bold' : undefined,
        }}
      >
        {props.children}
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
          fontFamily:
            "'Segoe UI', system-ui, -apple-system, 'Open Sans', Arial, sans-serif",
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
        style={{
          color: highlight && isEdited ? 'orange' : undefined,
          fontWeight: highlight && isEdited ? 'bold' : undefined,
        }}
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
        style={{
          color: isRed ? 'orange' : undefined,
        }}
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
    <div style={{ position: 'relative' }}>
      {loading && (
        <div className='k-loading-mask'>
          <span className='k-loading-text'>Loading...</span>
          <div className='k-loading-image' />
          <div className='k-loading-color' />
        </div>
      )}

      {permissions?.showReportTitleMain && (
        <Typography component='div' className='grid-title'>
          {permissions?.titleNameMain}
        </Typography>
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
            <Box>
              {permissions?.showNote && (
                <Typography component='div' className='text-note'>
                  {note}
                </Typography>
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
                      fontSize: '0.85rem',
                      fontWeight: 800,
                      color: '#1e293b', // Slate 800
                      letterSpacing: '0.2px',
                      position: 'relative',
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
                          },
                        },
                      },
                    }}
                  >
                    <IconButton
                      size='small'
                      sx={{
                        padding: '2px',
                        color: '#94a3b8', // Slate 400 (Quiet until hovered)
                        transition: 'all 0.2s ease-in-out',
                        animation: `${softPulse} 3s ease-in-out infinite`,
                        '&:hover': {
                          backgroundColor: 'rgba(1, 0, 203, 0.08)',
                          color: '#0100cb', // Turns brand blue on hover
                          animation: 'none', // Stop pulse on interaction
                          transform: 'rotate(10deg)',
                        },
                      }}
                    >
                      <InfoOutlinedIcon sx={{ fontSize: '1rem' }} />
                    </IconButton>
                  </MuiTooltip>
                </Box>
              )}

              {/* CASE 1: Permission TRUE → Full Header UI */}
              {permissions?.showTitleNameBusiness ? (
                <Typography
                  component='div'
                  sx={{
                    fontSize: '0.85rem',
                    fontWeight: 850,
                    color: '#2d3748',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.5,
                    mb: permissions?.marginBottom ? '12px' : '4px',
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
                      borderRadius: '10px',
                      backgroundColor: '#eef2ff',
                      color: '#1e293b',
                      ml: 1,
                      cursor: 'pointer',
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

                  {/* ITEMS BADGE */}
                  <Box
                    sx={{
                      ml: 1,
                      px: 1.5,
                      py: 0.5,
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      borderRadius: '6px',
                      backgroundColor: '#f3ece7',
                      color: '#92400e',
                    }}
                  >
                    {rows?.length || 0} {rows?.length === 1 ? 'Item' : 'Items'}
                  </Box>

                  {/* EDITABLE BADGE
                  <Box
                    sx={{
                      ml: 1,
                      px: 1.5,
                      py: 0.5,
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      borderRadius: '6px',
                      backgroundColor: '#dbeafe',
                      color: '#1e40af',
                    }}
                  >
                    Editable
                  </Box> */}
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
                    borderRadius: '10px',
                    backgroundColor: '#eef2ff',
                    color: '#1e293b',
                    ml: 1,
                    cursor: 'pointer',
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

              {permissions?.titleNameExtra && (
                <Typography
                  component='div'
                  className='grid-title-extra'
                  sx={{
                    fontSize: '0.60rem', // little smaller
                    fontWeight: 800,
                    color: '#336063', // very light grey
                    fontStyle: 'italic',
                    lineHeight: 1.4,
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
                  className='dropdown-select'
                  variant='outlined'
                  label={permissions?.dropdownLabel || 'Select'}
                  sx={{
                    display:
                      permissions?.IS_PE_C2_HIDE !== false ? 'block' : 'none',
                  }}
                  InputLabelProps={{
                    shrink: true,
                    sx: {
                      fontWeight: 'bold',
                    },
                  }}
                  SelectProps={{
                    MenuProps: {
                      disableScrollLock: true,
                    },
                  }}
                >
                  <MenuItem value='' disabled>
                    {permissions?.dropdownLabel || 'Select'}
                  </MenuItem>

                  {grades?.map((unit) => (
                    <MenuItem key={unit.gradeId} value={unit.gradeId}>
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
                  className='dropdown-select'
                  variant='outlined'
                  label='Select'
                  InputLabelProps={{
                    shrink: true,
                    sx: {
                      fontWeight: 'bold',
                    },
                  }}
                  SelectProps={{
                    MenuProps: {
                      disableScrollLock: true,
                    },
                  }}
                >
                  <MenuItem value='' disabled>
                    Select
                  </MenuItem>

                  {permissions?.packagingYears?.map((year) => (
                    <MenuItem key={year} value={year}>
                      {year}
                    </MenuItem>
                  ))}
                </TextField>
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
              {permissions?.addButton && (
                <Button
                  variant='contained'
                  className='btn-add'
                  startIcon={<AddIcon />}
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
                  startIcon={<DownloadIcon fontSize='small' />}
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
                  startIcon={<DownloadIcon fontSize='small' />}
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
                    variant='contained'
                    onClick={triggerFileUpload}
                    startIcon={<UploadIcon sx={{ fontSize: 16 }} />}
                    disabled={
                      isButtonDisabled || READ_ONLY || rows?.length === 0
                    }
                    className='btn-import'
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
                  startIcon={<SaveIcon sx={{ fontSize: 16 }} />}
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
                  startIcon={<CalculateIcon sx={{ fontSize: 16 }} />}
                  disabled={
                    READ_ONLY ||
                    (rows?.length === 0
                      ? false
                      : isButtonDisabled ||
                        !permissions?.showCalculateVisibility)
                  }
                  className='btn-calculate'
                >
                  Calculate
                </Button>
              )}

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
                  startIcon={<RestartAltIcon />}
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

              {permissions?.showUnit && (
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
                          color: 'text.secondary',
                          fontWeight: 700,
                          fontSize: '0.6rem',
                          textTransform: 'uppercase',
                          letterSpacing: '0.4px',
                          lineHeight: 1,
                        }}
                      >
                        Unit:
                      </Typography>
                    ),
                  }}
                  sx={{
                    minWidth: 120,
                    '& .MuiOutlinedInput-root': {
                      height: '30px',
                      backgroundColor: 'rgba(255, 255, 255, 0.85)',
                      borderRadius: '7px',
                      fontSize: '0.7rem',
                      fontWeight: 600,
                      '& fieldset': {
                        borderColor: 'rgba(0, 0, 0, 0.08)',
                      },
                      '&:hover fieldset': {
                        borderColor: '#0100cb',
                      },
                      '&.Mui-focused fieldset': {
                        borderColor: '#0100cb',
                        borderWidth: '1.2px',
                      },
                    },
                    '& .MuiSelect-select': {
                      display: 'flex',
                      alignItems: 'center',
                      padding: '2px 6px !important',
                    },
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
                            fontSize: '0.7rem',
                            fontWeight: 500,
                            minHeight: '26px',
                            margin: '1px 4px',
                            borderRadius: '5px',
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
                  <MenuItem value='' disabled sx={{ fontSize: '0.65rem' }}>
                    <em>Select UOM</em>
                  </MenuItem>

                  {/* Render the correct unit options dynamically */}
                  {permissions?.units?.map((unit) => (
                    <MenuItem key={unit} value={unit}>
                      {unit}
                    </MenuItem>
                  ))}
                </TextField>
              )}

              {permissions?.showModes && (
                <TextField
                  select
                  value={selectMode ?? ''}
                  onChange={(e) => setSelectMode(e.target.value)}
                  className='dropdown-select'
                  variant='outlined'
                  label='Select Mode'
                  SelectProps={{
                    MenuProps: {
                      disableScrollLock: true,
                    },
                  }}
                >
                  <MenuItem value='' disabled>
                    Select Mode
                  </MenuItem>

                  {permissions.modes.map((m) => (
                    <MenuItem key={m.name} value={m.name}>
                      {m.displayName}
                    </MenuItem>
                  ))}
                </TextField>
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
              >
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
                    (col.field === 'maintStartDateTime' ||
                      col.field === 'maintEndDateTime')
                  ) {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
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
                        hidden={col.hidden}
                        filter='date'
                        columnMenu={ColumnMenuCheckboxFilterDate}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        headerClassName={
                          isDateFilterActive.includes(col.field)
                            ? 'active-column'
                            : ''
                        }
                      />
                    )
                  }

                  if (dateFields.includes(col.field)) {
                    if (
                      screenType === 'ElastomerSlowdown' &&
                      lowerVertName === 'elastomer'
                    ) {
                      return (
                        <GridColumn
                          key={col.field}
                          field={col.field}
                          title={col.title || col.headerName}
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
                            ].includes(col.field)
                              ? '{0:dd-MM-yyyy}'
                              : '{0:dd-MM-yyyy hh:mm a}'
                          }
                          editor='date'
                          hidden={col.hidden}
                          // columnMenu={DateColumnMenu}
                          filter='date'
                          columnMenu={ColumnMenuCheckboxFilterDate}
                          width={setWidth(col?.widthT || col?.width || 100, col.field)}
                          minWidth={setWidth(100, col.field)}
                          headerClassName={
                            isDateFilterActive.includes(col.field)
                              ? 'active-column'
                              : ''
                          }
                        />
                      )
                    }
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        cells={{
                          edit: {
                            date: [
                              'fromDate',
                              'toDate',
                              'periodTo',
                              'periodFrom',
                              'toDateReport',
                              'fromDateReport',
                            ].includes(col.field)
                              ? DateOnlyPicker
                              : DateTimePickerEditor,
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
                          ].includes(col.field)
                            ? '{0:dd-MM-yyyy}'
                            : '{0:dd-MM-yyyy hh:mm a}'
                        }
                        editor='date'
                        hidden={col.hidden}
                        // columnMenu={DateColumnMenu}
                        filter='date'
                        columnMenu={ColumnMenuCheckboxFilterDate}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        headerClassName={
                          isDateFilterActive.includes(col.field)
                            ? 'active-column'
                            : ''
                        }
                      />
                    )
                  }

                  if (dateFields1.includes(col.field)) {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
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
                            ].includes(col.field)
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
                          ].includes(col.field)
                            ? '{0:dd-MM-yyyy}'
                            : '{0:dd-MM-yyyy}'
                        }
                        editor='date'
                        hidden={col.hidden}
                        filter='date'
                        // columnMenu={DateColumnMenu}
                        columnMenu={ColumnMenuCheckboxFilterDate}
                      />
                    )
                  }
                  if (
                    lowerVertName === 'vcm' &&
                    monthFields.includes(col.field) &&
                    permissions?.highlightShutdownConsumption
                  ) {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                        format={col.format}
                      />
                    )
                  }
                  if (col?.field === 'symbol') {
                    return (
                      <GridColumn
                        key='symbol'
                        field='symbol'
                        width={setWidth(col?.widthT || col?.width || 80, col.field)}
                        minWidth={setWidth(80, col.field)}
                        title={col.title}
                        editable={col.editable || true}
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
                        key='limit'
                        field='limit'
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        title={col.title}
                        editable={col.editable || true}
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
                        key='discriptionDrpdwn'
                        field='discriptionDrpdwn'
                        title={col.title || col.headerName || 'Particulars'}
                        editable={col.editable || true}
                        hidden={col.hidden}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
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
                        key='discription'
                        field='discription'
                        title={col.title || col.headerName || 'Particulars'}
                        editable={col.editable || true}
                        hidden={col.hidden}
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
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                      />
                    )
                  }

                  if (col?.field === 'productName1') {
                    return (
                      <GridColumn
                        key='productName1'
                        field='productName1'
                        title={col.title || col.headerName || 'Particulars'}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        editable={col.editable || true}
                        hidden={col.hidden}
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
                        key='month'
                        field='month'
                        title={col.title || col.headerName || 'month'}
                        editable={col.editable || true}
                        hidden={col.hidden}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
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

                  if (['discription', 'Name'].includes(col?.field)) {
                    return (
                      <GridColumn
                        key={col?.field}
                        field={col?.field}
                        title={col.title || col.headerName || 'Description'}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        editable={true}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col.hidden}
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

                  if (col.type === 'descLimit') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 150, col.field)}
                        minWidth={setWidth(150, col.field)}
                        hidden={col.hidden}
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
                        key='UOM'
                        field='UOM'
                        title={col.title || col.headerName || 'UOM'}
                        // width={setWidth(110)}
                        // width={setWidth(columnCount <= 10 ? 50 : 100)}
                        width={setWidth(
                          columnCount <= 6 ? 80 : columnCount <= 10 ? 80 : 100,
                          col.field
                        )}
                        minWidth={setWidth(col?.widthT || col?.width || 80, col.field)}
                        editable={false}
                        columnMenu={ColumnMenuCheckboxFilter}
                        headerClassName={isActive ? 'active-column' : ''}
                        hidden={col.hidden}
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
                        key='ReceipeName'
                        field='ReceipeName'
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        editable={false}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col.hidden}
                        cells={{
                          data: toolTipRenderer,
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                      />
                    )
                  }
                  if (col.type === 'Receipe') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                        format={col.format}
                      />
                    )
                  }

                  if (col.field === 'sapMaterialCode' && col.useMethodColors) {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                  // ...existing code...
                  if (col.type === 'monthDropdownPEPP') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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

                  if (col.type === 'monthDropdown') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                  if (col.type === 'Categorydropdown') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
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
                  if (col.type === 'yeardropdown') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                  if (col.type === 'typesdDropdown') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                  if (col.type === 'lineDropdown') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        editable={col.editable}
                        cells={{
                          edit: { text: LineDropdownEditorWrapper },
                          data: (props) => (
                            <LineDisplayCell
                              {...props}
                              allLines={allLines}
                              customModifiedCells={customModifiedCells}
                              highlightField={col.field}
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
                        key='DisplayName'
                        field={col?.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        editable={false}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col.hidden}
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
                      col.field,
                    )
                  ) {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        // editor={true}
                        // editable={{ mode: 'popup' }}
                        cells={{
                          data: (cellProps, allRedCell) => (
                            <RemarkCell
                              {...cellProps}
                              allRedCell={allRedCell}
                              onRemarkClick={handleRemarkCellClick}
                            />
                          ),
                          headerCell: SimpleHeaderWithTooltip,
                        }}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col.hidden}
                        headerClassName={isActive ? 'active-column' : ''}
                        width={setWidth(col?.widthT || col?.width || 120, col.field)}
                        minWidth={setWidth(120, col.field)}
                      />
                    )
                  }
                  if (col.field === 'durationInHrs') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        editable={true}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col.hidden}
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

                  if (col.field === 'rpfDownTime') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        editable={true}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col.hidden}
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

                  if (col.hideFilter && col.hideSort) {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        hidden={col.hidden}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
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
                        format={col.format}
                        sortable={false}
                      />
                    )
                  }

                  if (col.type === 'propaneDropdown') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                  if (col.type === 'dynamicDropdown') {
                    const dropdownOptions =
                      permissions?.dynamicDropdownOptions || []
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
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

                  if (col.type === 'percentChange') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                        format={col.format}
                      />
                    )
                  }

                  if (col.type === 'negativeNumber') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                        format={col.format}
                      />
                    )
                  }

                  if (col.type === 'numberWithUOMValidation') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                        format={col.format}
                      />
                    )
                  }
                  if (col.field === 'rate') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={
                          col.title || col.headerName || 'Rate Reduced (TPH)'
                        }
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        editable={true}
                        columnMenu={ColumnMenuCheckboxFilter}
                        hidden={col.hidden}
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

                  if (col.crackerValidation) {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                        format={col.format}
                      />
                    )
                  }

                  if (
                    col.type === 'number' &&
                    permissions?.showRedCellsForOroductionTarget
                  ) {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                        format={col.format}
                      />
                    )
                  }

                  if (col.type === 'number') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                        format={col.format}
                      />
                    )
                  }

                  if (col.type === 'switch') {
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
                        key={col.field}
                        field={col.field}
                        title='.'
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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

                  if (col.type === 'switch2') {
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
                        key={col.field}
                        field={col.field}
                        title='.'
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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

                  if (col.type === 'numberWidth') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(100, col.field)}
                        hidden={col.hidden}
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
                        format={col.format}
                      />
                    )
                  }

                  if (col.field === 'ConstantValue') {
                    return (
                      <GridColumn
                        key={col.field}
                        field={col.field}
                        title={col.title || col.headerName}
                        width={setWidth(col?.widthT || col?.width || 100, col.field)}
                        minWidth={setWidth(80, col.field)}
                        hidden={col.hidden}
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

                  return (
                    <GridColumn
                      key={col.field}
                      field={col.field}
                      title={col.title || col.headerName}
                      minWidth={setWidth(
                        columnCount <= 6 ? 80 : columnCount <= 10 ? 100 : 120,
                        col.field
                      )}
                      width={setWidth(col?.widthT || col?.width || 100, col.field)}
                      hidden={col.hidden}
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
                    width={100}
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

      {/* {(permissions?.allActionOfBottomBtns ?? true) && ( */}
      <Box
        sx={{
          marginTop: 2,
          display: 'flex',
          gap: 2,
        }}
      >
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
        {showDeleteAll && (
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
        )}
      </Box>
      {/* )} */}
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

        <DialogActions sx={{ p: 1.5, pt: 0, gap: 1 }}>
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
