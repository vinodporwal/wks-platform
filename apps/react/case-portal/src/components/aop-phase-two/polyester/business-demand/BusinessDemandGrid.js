import React, { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useDispatch, useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { setIsBlocked } from 'store/reducers/dataGridStore'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { validateFields } from 'utils/validationUtils'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { BusinessDemandApiService } from '../../services/polyester/businessDemandApiService'

const BusinessDemandGrid = () => {
  const dispatch = useDispatch()
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_NAME = siteObject?.name
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = screenTitle?.title
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const valueFormat = ValueFormatterProduction()
  const headerMap = generateHeaderNames(AOP_YEAR)

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const monthsConfig = [
    { field: 'april', key: 4, title: 'April' },
    { field: 'may', key: 5, title: 'May' },
    { field: 'june', key: 6, title: 'June' },
    { field: 'july', key: 7, title: 'July' },
    { field: 'aug', key: 8, title: 'August' },
    { field: 'sep', key: 9, title: 'September' },
    { field: 'oct', key: 10, title: 'October' },
    { field: 'nov', key: 11, title: 'November' },
    { field: 'dec', key: 12, title: 'December' },
    { field: 'jan', key: 1, title: 'January' },
    { field: 'feb', key: 2, title: 'February' },
    { field: 'march', key: 3, title: 'March' },
  ]

  const columns = [
    {
      field: 'Particulars',
      title: 'Type',
      editable: false,
      hidden: true,
      minWidth: 100,
    },
    {
      field: 'displayName',
      title: 'Particulars',
      editable: false,
      minWidth: 200,
    },
    ...monthsConfig.map((m) => ({
      field: m.field,
      title: headerMap[m.key] || m.title,
      editable: true,
      type: 'numberNonGrey',
      format: valueFormat,
      minWidth: 110,
    })),
    {
      field: 'remark',
      title: 'Remark',
      type: 'textarea',
      editable: true,
      minWidth: 160,
    },
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await BusinessDemandApiService.getBusinessDemand(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response && Array.isArray(response)) {
        const formattedData = response.map((item, index) => ({
          ...item,
          idFromApi: item.id,
          id: index,
          originalRemark: item.remark,
          inEdit: false,
          Particulars: item.normParameterTypeDisplayName,
        }))
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching business demand data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const saveChanges = useCallback(async () => {
    try {
      const data = Object.values(modifiedCells)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        return
      }

      // Production Sum validation
      const productionRows = (rows || []).filter(
        (row) => row.Particulars?.toLowerCase() === 'production',
      )

      if (productionRows.length > 0) {
        const SCALE = 10000
        const expected = 100 * SCALE
        const TOLERANCE = 1

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

        const months = monthsConfig.map((m) => m.field)
        const failures = []

        for (const month of months) {
          const sumInt = productionRows.reduce((acc, row) => {
            const modifiedRow = modifiedCells[row.id]
            const val =
              modifiedRow && modifiedRow[month] !== undefined
                ? modifiedRow[month]
                : row[month]
            return acc + toPreciseInt(val)
          }, 0)

          if (Math.abs(sumInt - expected) > TOLERANCE) {
            failures.push({ month, sumInt })
          }
        }

        if (failures.length > 0) {
          const parts = failures.map((f) => {
            const prettyMonth =
              f.month.charAt(0).toUpperCase() + f.month.slice(1)
            const prettySum = formatFromIntRobust(f.sumInt)
            return `${prettyMonth} - ${prettySum}`
          })

          setSnackbarOpen(true)
          setSnackbarData({
            message: `The production Sum should be exactly 100 - Current values (${parts.join(', ')})`,
            severity: 'error',
          })
          return
        }
      }

      const requiredFields = ['normParameterId', 'remark']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        return
      }

      const payloadData = data.map((row) => ({
        april: row.april ?? null,
        may: row.may ?? null,
        june: row.june ?? null,
        july: row.july ?? null,
        aug: row.aug ?? null,
        sep: row.sep ?? null,
        oct: row.oct ?? null,
        nov: row.nov ?? null,
        dec: row.dec ?? null,
        jan: row.jan ?? null,
        feb: row.feb ?? null,
        march: row.march ?? null,
        remark: row.remark || null,
        avgTph: row.avgTph || null,
        year: AOP_YEAR,
        plantId: PLANT_ID,
        siteFKId: siteObject?.id,
        verticalFKId: VERTICAL_ID,
        normParameterId: row.normParameterId,
        id: row.idFromApi || null,
        inEdit: row.inEdit || false,
      }))

      setLoading(true)
      const res = await BusinessDemandApiService.saveBusinessDemand(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payloadData,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully!',
        severity: 'success',
      })
      setModifiedCells({})
      dispatch(setIsBlocked(false))
      fetchData()
    } catch (error) {
      console.error('Error saving business demand data:', error)
    } finally {
      setLoading(false)
    }
  }, [
    modifiedCells,
    rows,
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    fetchData,
    dispatch,
    siteObject,
    VERTICAL_ID,
  ])

  const deleteRowData = useCallback(
    async (paramsForDelete) => {
      try {
        const { idFromApi, id } = paramsForDelete.row
        const deleteId = id

        if (!idFromApi) {
          setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
          return
        }

        setLoading(true)
        await BusinessDemandApiService.deleteBusinessDemand(keycloak, idFromApi)
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        fetchData()
      } catch (error) {
        console.error('Error deleting record:', error)
      } finally {
        setLoading(false)
      }
    },
    [keycloak, fetchData],
  )

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })
    try {
      const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME || 'PE'}_${SITE_NAME || 'NMD'}_${PLANT_NAME || ''}_Business_Demand`
      await BusinessDemandApiService.exportBusinessDemand(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
        SCREEN_NAME,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel downloaded successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting business demand excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    }
  }

  const handleExcelUpload = async (rawFile) => {
    setLoading(true)
    try {
      const response = await BusinessDemandApiService.importBusinessDemand(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        rawFile,
      )
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = Array.from(byteCharacters, (char) =>
          char.charCodeAt(0),
        )
        const byteArray = new Uint8Array(byteNumbers)
        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'Error File - Business Demand.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error importing business demand excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = useCallback(
    (row) => {
      if (READ_ONLY) return
      setCurrentRemark(row.remark || '')
      setCurrentRowId(row.id)
      setRemarkDialogOpen(true)
    },
    [READ_ONLY],
  )

  const permissions = {
    showAction: false,
    addButton: false,
    deleteButton: false,
    editButton: false,
    showUnit: false,
    saveBtn: true,
    showCalculate: false,
    allAction: true,
    showDropdown: false,
    showExport: true,
    showImport: true,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: `${SCREEN_NAME}`,
    isTotalFooterActive: true,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title={SCREEN_NAME}
        loading={loading}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        permissions={permissions}
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        handleExport={handleExport}
        handleExcelUpload={handleExcelUpload}
        deleteRowData={deleteRowData}
        groupBy='Particulars'
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        snackbarData={snackbarData}
        setSnackbarData={setSnackbarData}
      />
    </Box>
  )
}

export default BusinessDemandGrid
