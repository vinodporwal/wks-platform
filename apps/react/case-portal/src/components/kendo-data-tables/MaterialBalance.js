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
const MaterialBalance = ({ permissions }) => {
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
  const IS_CRACKER_HMD = lowerVertName === 'cracker' && SITE_NAME === 'HMD'
  const IS_CRACKER_C2 = lowerVertName === 'cracker' && SITE_NAME === 'C2'
  const IS_CHEMICAL_HMD = lowerVertName === 'chemical' && SITE_NAME === 'HMD'
  const IS_CHEMICAL_VMD_BUTADIENE =
    lowerVertName === 'chemical' &&
    SITE_NAME === 'VMD' &&
    PLANT_NAME === 'BUTADIENE'

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

  const fetchMatbalData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      // Using 'matbal' as the type for Material Balance constraints
      const response = await DataService.getMaterialBalanceData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200) {
        const formattedData = (response?.data?.data || []).map(
          (item, index) => ({
            ...item,
            idFromApi: item?.id,
            id: index,
            Type: item?.Type,
            Remarks: item?.Remarks || '',
            originalRemark: item?.Remarks || '',
            isEditable: item?.IsEditable,
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
    fetchMatbalData()
  }, [fetchMatbalData])

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
        field: monthShort,
        title: fieldName,
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
    if (IS_CHEMICAL_HMD) {
      return [
        {
          field: 'Particulars',
          title: 'Particulars',
          editable: false,
          widthT: 100,
          minWidth: 300,
        },
        {
          field: 'UOM',
          title: 'UOM',
          editable: false,
          widthT: 80,
          minWidth: 80,
        },
        ...monthCols,
        {
          field: 'Remarks',
          title: 'Remark',
          editable: false,
          widthT: 100,
          minWidth: 100,
        },
      ]
    }

    return [
      {
        field: 'Particulars',
        title: 'Particulars',
        editable: false,
        widthT: 100,
        minWidth: 300,
      },
      { field: 'UOM', title: 'UOM', editable: false, widthT: 80, minWidth: 80 },
      ...monthCols,
    ]
  }, [monthCols, IS_CHEMICAL_HMD])

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.Remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = useCallback(async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) return
    const requiredFields = ['Remarks']
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
        apr: row.Apr || row.ConstantValue || null,
        may: row.May || null,
        jun: row.Jun || null,
        jul: row.Jul || null,
        aug: row.Aug || null,
        sep: row.Sep || null,
        oct: row.Oct || null,
        nov: row.Nov || null,
        dec: row.Dec || null,
        jan: row.Jan || null,
        feb: row.Feb || null,
        mar: row.Mar || null,
        UOM: '',
        auditYear: AOP_YEAR,
        normParameterFKId: row.NormParameterId,
        remarks: row.Remarks,
        id: row.idFromApi || null,
      }))

      const response = await DataService.saveMaterialBalanceData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payload,
      )
      if (response) {
        setSnackbarData({ message: 'Saved Successfully!', severity: 'success' })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchMatbalData()
      } else {
        setSnackbarData({ message: 'Save Failed!', severity: 'error' })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving Matbal data:', error)
      setSnackbarData({ message: 'Error saving data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, PLANT_ID, AOP_YEAR, keycloak, fetchMatbalData])

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
        fetchMatbalData()
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
        fetchMatbalData()
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

  const handleCalculate = useCallback(async () => {
    setLoading(true)
    try {
      const response = await DataService.calculateMaterialBalanceData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data calculated successfully!',
          severity: 'success',
        })

        fetchMatbalData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Error calculating data!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error calculating spyro input data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to calculate data!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

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
      saveBtn: IS_CHEMICAL_HMD ? true : false,
      allAction: true,

      showTitleNameBusiness: true,
      titleName: 'Material Balance',
      //LATER WE NEED TO ADD EXPORT IMPORT
      downloadExcelBtn: IS_CHEMICAL_HMD ? true : false,
      uploadExcelBtn: IS_CHEMICAL_HMD ? true : false,
      showCalculate:
        IS_CRACKER_HMD || IS_CRACKER_C2 || IS_CHEMICAL_VMD_BUTADIENE,
      showCalculateVisibility: true,
    },
    isOldYear,
  )

  const adjustedPermissionsReadyOnly = getAdjustedPermissions(
    {
      hideRemarkForNonEditableRows: true,
      NON_EDITABLE_GRID: true,
    },
    isOldYear,
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      {(IS_CRACKER_HMD || IS_CRACKER_C2) && (
        <ModeSelection permissions={adjustedPermissionsReadyOnly} />
      )}

      <KendoDataTables
        rows={rows}
        setRows={setRows}
        columns={colDefs}
        permissions={adjustedPermissions}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        // groupBy='Type'
        title='Material Balance'
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        handleExcelUpload={handleExcelUpload}
        handleCalculate={handleCalculate}
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

export default MaterialBalance
