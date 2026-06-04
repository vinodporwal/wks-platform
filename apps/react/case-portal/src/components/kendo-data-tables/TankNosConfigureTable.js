import { Box } from '@mui/material'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import KendoDataTables from './index'
import { DataService } from 'services/DataService'
import { validateFields } from 'utils/validationUtils'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import Notification from 'components/Utilities/Notification'
import ModeSelection from './ModeSelection'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
const TankNosConfigureTable = ({ permissions }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year, oldYear, isReleased } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const lowerVertName = verticalObject?.name?.toLowerCase()
  const SITE_NAME = siteObject?.name?.toUpperCase()
  const PLANT_NAME = plantObject?.name?.toUpperCase()

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

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await DataService.getTankNosData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200) {
        const formattedData = (response?.data || []).map(
          (item, index) => ({
            ...item,
            idFromApi: item?.id,
            id: index,
            remarks: item?.remarks || '',
            originalRemark: item?.remarks || '',
            isEditable: item?.isEditable,
            apr: item.apr === true ? 1 : 0,
            may: item.may === true ? 1 : 0,
            jun: item.jun === true ? 1 : 0,
            jul: item.jul === true ? 1 : 0,
            aug: item.aug === true ? 1 : 0,
            sep: item.sep === true ? 1 : 0,
            oct: item.oct === true ? 1 : 0,
            nov: item.nov === true ? 1 : 0,
            dec: item.dec === true ? 1 : 0,
            jan: item.jan === true ? 1 : 0,
            feb: item.feb === true ? 1 : 0,
            mar: item.mar === true ? 1 : 0,
          }),
        )
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching Matbal data:', error)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const [startYearSuffix, endYearSuffix] = useMemo(() => {
    if (!AOP_YEAR) return ['', '']
    const parts = String(AOP_YEAR).split('-')
    const start = parts[0]?.slice(-2) || ''
    const end = (parts[1]?.length === 2 ? parts[1] : parts[1]?.slice(-2)) || ''
    return [start, end]
  }, [AOP_YEAR])

  // const startYearSuffix = '25'
  // const endYearSuffix = '26'

  const monthCols = useMemo(() => {
    return Array.from({ length: 12 }, (_, i) => {
      const monthIndex = (i + 4) % 12 || 12
      const date = new Date(2000, monthIndex - 1)
      const monthShort = date.toLocaleString('en-US', { month: 'short' })
      const yearSuffix = monthIndex >= 4 ? startYearSuffix : endYearSuffix
      const fieldName = `${monthShort}-${yearSuffix}`

      return {
        minWidth: 100,
        field: monthShort.toLowerCase(),
        title: monthShort,
        width: 120,
        type: 'number',
        format: '{0:#.###}',
        editable: true,
        monthNumber: monthIndex,
        originalMonthShort: monthShort,
        originalMonthLong: date
          .toLocaleString('en-US', { month: 'long' })
          .toLowerCase(),
      }
    })
  }, [startYearSuffix, endYearSuffix])

  const colDefs = useMemo(() => {
    return [
      {
        field: 'displayName',
        title: 'Tank No',
        editable: false,
        widthT: 100,
        minWidth: 300,
      },
      {
        field: 'volume',
        title: 'Vol(M3)',
        editable: true,
        widthT: 100,
        minWidth: 100,
        type: 'integerNumberOnly',
      },
      ...monthCols,
      {
        field: 'remarks',
        title: 'Remarks',
        editable: true,
        widthT: 150,
        minWidth: 150,
      },
    ]
  }, [monthCols])

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = useCallback(async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) return
    const requiredFields = ['remarks']
    const validationData = modifiedData.map((row) => ({
      ...row,
      // originalRemark: '',
    }))
    const validationMessage = validateFields(validationData, requiredFields)
    if (validationMessage) {
      setSnackbarData({ message: validationMessage, severity: 'error' })
      setSnackbarOpen(true)
      return
    }

    setLoading(true)
    try {
      const payload = modifiedData.map((row) => ({
        ...row,
        apr: row.apr === 1 ? true : false,
        may: row.may === 1 ? true : false,
        jun: row.jun === 1 ? true : false,
        jul: row.jul === 1 ? true : false,
        aug: row.aug === 1 ? true : false,
        sep: row.sep === 1 ? true : false,
        oct: row.oct === 1 ? true : false,
        nov: row.nov === 1 ? true : false,
        dec: row.dec === 1 ? true : false,
        jan: row.jan === 1 ? true : false,
        feb: row.feb === 1 ? true : false,
        mar: row.mar === 1 ? true : false,
        // UOM: '',
        // auditYear: AOP_YEAR,
        // normParameterFKId: row.NormParameterId,
        remarks: row.remarks,
        id: row.idFromApi || null,
      }))
      const response = await DataService.saveTankNosData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payload,
      )
      if (response?.code === 200) {
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarData({ message: 'Save Failed!', severity: 'error' })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving data:', error)
      setSnackbarData({ message: 'Error saving data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, PLANT_ID, AOP_YEAR, keycloak, fetchData])

  const handleExcelUpload = async (file) => {
    setLoading(true)
    try {
      const response = await DataService.saveMaterialBalanceExcel(
        file,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200) {
        setSnackbarData({
          message: 'Imported Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
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
        link.setAttribute('download', 'Error File - Material Balance.xlsx')
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
      console.error('Error importing Matbal data:', error)
      setSnackbarData({ message: 'Error importing data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    try {
      const excelName = `${verticalObject?.name}_${siteObject?.name}_${plantObject?.name}_Material_Balance`
      await DataService.materialBalanceExport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        excelName,
      )
      setSnackbarData({ message: 'Export Started!', severity: 'success' })
      setSnackbarOpen(true)
    } catch (error) {
      console.error('Error exporting Matbal data:', error)
      setSnackbarData({ message: 'Error exporting data', severity: 'error' })
      setSnackbarOpen(true)
    }
  }


  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: permissions?.showAction ?? true,
      saveWithRemark: permissions?.saveWithRemark ?? true,
      saveBtn: true,
      allAction: true,

      showTitleNameBusiness: true,
      enableOnOffDropdown: true,
      enableSwitchToggle: true,
      titleName: 'Tank Nos',
    },
    isOldYear,
  )


  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <KendoDataTables
        rows={rows}
        setRows={setRows}
        columns={colDefs}
        permissions={adjustedPermissions}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        // groupBy='Type'
        title='Tank Nos Configure'
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        handleExcelUpload={handleExcelUpload}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        plantID={PLANT_ID}
      />

      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </Box>
  )
}

export default TankNosConfigureTable
