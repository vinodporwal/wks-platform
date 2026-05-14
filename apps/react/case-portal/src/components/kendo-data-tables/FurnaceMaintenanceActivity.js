import { useSession } from 'SessionStoreContext'
import Notification from 'components/Utilities/Notification'
import React, { useEffect, useState } from 'react'
import { DataService } from 'services/DataService'
import KendoDataTables from 'components/kendo-data-tables/index'
import { validateFields } from 'utils/validationUtils'
import moment from 'moment'
import { DropDownList } from '@progress/kendo-react-dropdowns'
import { DatePicker } from '@progress/kendo-react-dateinputs'
import { useSelector } from 'react-redux'
import { getRoleName } from 'services/role-service'
const FurnaceMaintenanceActivity = ({ permissions }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const [rows, setRows] = useState()
  const [runLengthColumns, setRunLengthColumns] = useState([])
  const [MaintActivityDropdownData, setMaintActivityDropdownData] = useState([])
  const [uniqueFurnaceNames, setUniqueFurnaceNames] = useState([])

  const [loading, setLoading] = useState(false)
  const keycloak = useSession()

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.Remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const fetchMaintActivityDropdownData = async () => {
    try {
      const maintenanceActivityOptions = [
        { value: 'TLE', name: 'TLE' },
        { value: 'Plan Shutdown', name: 'Plan Shutdown' },
      ]
      setMaintActivityDropdownData(maintenanceActivityOptions)
    } catch (e) {
      console.error('Error loading dropdown data:', e)
    }
  }

  const fetchData = async () => {
    setLoading(true)
    try {
      const data2 = await DataService.getFurnaceMaintenanceActivity(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const toDateObject = (value) =>
        value ? moment(value, 'MMM D, YYYY').toDate() : null

      if (data2?.code === 200) {
        const dateColumns =
          data2?.data?.columns
            ?.filter((col) => col.type === 'date')
            ?.map((col) => col.field) || []

        const processedData = data2.data?.data.map((item, index) => {
          let Duration = null
          if (item.StartDate && item.EndDate) {
            const start = moment(item.StartDate)
            const end = moment(item.EndDate)
            const days = end.diff(start, 'days')
            Duration = days >= 0 ? `${days}` : 'Invalid'
          }
          const converted = {}
          dateColumns.forEach((field) => {
            // FIX: only convert non-empty strings
            if (
              item[field] &&
              typeof item[field] === 'string' &&
              item[field].trim() !== ''
            ) {
              const parsed = moment(
                item[field],
                ['YYYY-MM-DD', 'MMM D, YYYY', moment.ISO_8601],
                true,
              )
              item[field] = parsed.isValid() ? parsed.toDate() : null
            } else {
              item[field] = null // FIX: set null instead of leaving empty string
            }
          })
          return {
            ...item,
            ...converted,
            Id: item.Id,
            id: index,
            Duration,
            DisplayName: item.DisplayName || item.displayName || item.Name,
            originalRemark: item?.Remarks?.trim(),
            Remarks: item?.Remarks?.trim(),
            isEditable: true,
          }
        })

        // Extract unique furnace names from data
        const uniqueNames = [
          ...new Set(
            data2.data?.data?.map((item) => item.Name).filter(Boolean),
          ),
        ]
        setUniqueFurnaceNames(uniqueNames)

        const mappedCols = (data2?.data?.columns || [])
          .filter((col) => !['Id', 'isEditable', 'Remarks'].includes(col.field))
          .map((col) => {
            let columnConfig = {
              ...col,
              editable: [
                'MaintActivity',
                'StartDate',
                'EndDate',
                'Name',
              ].includes(col.field),
            }

            // Set type for specific columns
            if (col.field === 'MaintActivity') {
              columnConfig.type = 'dynamicDropdownshared'
            } else if (col.field === 'Name') {
              columnConfig.type = 'dynamicDropdownshared'
              columnConfig.dropdownOptions = uniqueNames.map((name) => ({
                value: name,
                name: name,
              }))
            }

            return columnConfig
          })

        const remarksCol = (data2?.data?.columns || [])
          .filter((col) => col.field === 'Remarks')
          .map((col) => ({
            ...col,
            editable: true,
          }))

        setRunLengthColumns([
          ...mappedCols,
          // ? Duration before Remarks
          {
            field: 'Duration',
            title: 'Duration',
            type: 'text',
            align: 'right',
            editable: false,
            width: 120,
            hidden: false,
          },
          ...remarksCol,
        ])

        setRows(processedData)
      } else {
        setRows([])
      }
    } catch (e) {
      // ? fixed, no extra brace
      console.error('Error loading previous year:', e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchMaintActivityDropdownData()
  }, [])

  useEffect(() => {
    fetchData()
  }, [keycloak, AOP_YEAR, PLANT_ID])

  const toLocalDateString = (value) => {
    if (!value) return null
    return moment(value).format('YYYY-MM-DD')
  }
  const saveCrackerRunLength = async (newRow) => {
    setLoading(true)
    try {
      //const referenceRows = getRows('IBR Plan')[2]

      // build payload like you already do
      if (newRow.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        setLoading(false)
        return
      }

      // ? Remarks validation
      const requiredFields = ['Remarks']
      const validationMessage = validateFields(newRow, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        setLoading(false)
        return
      }

      // ? Duplicate Name validation - check against all rows in the grid
      const duplicateNamesList = []

      // Second pass: count all names from all rows (including newRow)
      const allRows = rows || []
      const nameCount = {}

      for (const record of allRows) {
        const name = record.Name?.trim()
        if (name) {
          nameCount[name] = (nameCount[name] || 0) + 1
        }
      }

      // Third pass: mark records in newRow that have duplicates in the entire grid
      for (const record of newRow) {
        const name = record.Name?.trim()
        if (name && nameCount[name] > 1) {
          record.isError = true
          if (!duplicateNamesList.includes(name)) {
            duplicateNamesList.push(name)
          }
        }
      }

      if (duplicateNamesList.length > 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Duplicate furnace name(s) found: ${duplicateNamesList.join(', ')}. Each furnace name must be unique. Available: ${uniqueFurnaceNames.join(', ')}`,
          severity: 'error',
        })
        setLoading(false)
        return
      }

      // ? 1. StartDate and EndDate mandatory validation
      const dateRequiredRows = new Set()
      for (const record of newRow) {
        const startMissing = !record.StartDate
        const endMissing = !record.EndDate
        if (startMissing || endMissing) {
          record.isError = true
          dateRequiredRows.add(record.id)
        }
      }
      if (dateRequiredRows.size > 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Start Date and End Date are required for all records.',
          severity: 'error',
        })
        setLoading(false)
        return
      }

      // ? 2. StartDate must be before EndDate validation
      for (const record of newRow) {
        const start = new Date(record.StartDate).getTime()
        const end = new Date(record.EndDate).getTime()
        if (start >= end) {
          record.isError = true
          setSnackbarOpen(true)
          setSnackbarData({
            message: `Start date must be before end date for "${record.DisplayName || 'this record'}".`,
            severity: 'error',
          })
          setLoading(false)
          return
        }
      }

      // ? 3. Dates must be within AOP Year range validation
      const yearStr = AOP_YEAR
      let startLimit, endLimit
      if (yearStr) {
        const [startYear, endYear] = yearStr
          .split('-')
          .map((y) => parseInt(y.trim(), 10))
        if (!isNaN(startYear) && !isNaN(endYear)) {
          startLimit = new Date(`${startYear}-04-01T00:00:00`)
          endLimit = new Date(`20${endYear}-03-31T23:59:59`)
        }
      }

      const formatDateDDMMYYYY = (date) => {
        if (!(date instanceof Date) || isNaN(date)) return ''
        const d = date.getDate().toString().padStart(2, '0')
        const m = (date.getMonth() + 1).toString().padStart(2, '0')
        const y = date.getFullYear()
        return `${d}/${m}/${y}`
      }

      for (const record of newRow) {
        const startDate =
          record.StartDate instanceof Date
            ? record.StartDate
            : new Date(record.StartDate)
        const endDate =
          record.EndDate instanceof Date
            ? record.EndDate
            : new Date(record.EndDate)

        if (
          startLimit &&
          endLimit &&
          (isNaN(startDate) ||
            isNaN(endDate) ||
            startDate < startLimit ||
            startDate > endLimit ||
            endDate < startLimit ||
            endDate > endLimit)
        ) {
          record.isError = true
          setSnackbarOpen(true)
          setSnackbarData({
            message: `Dates must be between ${formatDateDDMMYYYY(startLimit)} and ${formatDateDDMMYYYY(endLimit)} for the selected year.`,
            severity: 'error',
          })
          setLoading(false)
          return
        }
      }

      const apiFields = runLengthColumns
        .map((col) => col.field)
        .filter((field) => field !== 'Duration')

      const payload = newRow.map((row, idx) => {
        const obj = {
          Id: row.Id,
          Plant_FK_Id: PLANT_ID,
          AOPYear: AOP_YEAR,
        }
        apiFields.forEach((field) => {
          let value = row[field]
          const colDef = runLengthColumns.find((col) => col.field === field)
          if (colDef?.type === 'date') {
            obj[field] = toLocalDateString(value)
          } else {
            obj[field] = value ?? null
          }
        })
        return obj
      })

      const response = await DataService.postIbr(
        PLANT_ID,
        payload,
        keycloak,
        AOP_YEAR,
      )

      if (response?.code == 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Data Save Failed!', severity: 'error' })
      }
      return response
    } catch (error) {
      console.error('Error saving data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error saving data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const saveChangesRunLength = React.useCallback(async () => {
    try {
      saveCrackerRunLength(Object.values(modifiedCells))
    } catch (error) {
      console.log('Error saving changes:', error)
    }
  }, [modifiedCells])

  return (
    <React.Fragment>
      <KendoDataTables
        modifiedCells={modifiedCells}
        rows={rows}
        setRows={setRows}
        columns={runLengthColumns}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        saveChanges={saveChangesRunLength}
        handleRemarkCellClick={handleRemarkCellClick}
        fetchData={fetchData}
        setModifiedCells={setModifiedCells}
        permissions={{
          ...permissions,
          remarksEditable: true,
          saveBtn: true,
          saveBtnForRemark: true,
          addButton: false,
          allAction: true,
          deleteButton: false,
          dynamicDropdownOptions: {
            MaintActivity: MaintActivityDropdownData,
          },
        }}
      />
      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </React.Fragment>
  )
}
export default FurnaceMaintenanceActivity
