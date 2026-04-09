import React, { useState, useEffect, useCallback } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import KendoDataTables from '../../index'
import { DataService } from 'services/DataService'
import { getRoleName } from 'services/role-service'
import { RawMaterialNormsBasisApiService } from 'services/raw-material-norms-basis-api-service'
import { validateFields } from 'utils/validationUtils'
import { ProductionRangeApiService } from 'services/production-range-api-service copy'
import { PtaConfigurationApiService } from 'services/pta-configuration-api-service'
import getEnhancedColDefsC2C3R from 'components/data-tables/CommonHeader/Kendo_ProductionAopHeaderC2C3R'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { ShutdownRateApiService } from 'services/shutdown-rate-api-service'
import getEnhancedColDefsShutdownRate from 'components/data-tables/CommonHeader/Kendo_ShutdownRateHeader'

const ShutdownRateGrid = ({ summary, summaryEdited, setSummaryEdited }) => {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [open1, setOpen1] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const apiRef = useGridApiRef()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantObject,
    year,
    oldYear,
    yearChanged,
    verticalObject,
    siteObject,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const keycloak = useSession()

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const isOldYear = false

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}_Shutdown_Rate`

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const valueFormat = ValueFormatterProduction()
  const headerMap = generateHeaderNames(AOP_YEAR)

  const NormConfigurationColumns = getEnhancedColDefsShutdownRate({
    headerMap,
    valueFormat,
  })

  useEffect(() => {
    setModifiedCells({})
    fetchData()
  }, [oldYear, yearChanged, keycloak, PLANT_ID, AOP_YEAR])

  const saveSummary = async (summary) => {
    try {
      const response = await DataService.saveSummaryAOPConsumptionNorm(
        PLANT_ID,
        AOP_YEAR,
        summary,
        keycloak,
      )

      if (response?.code == 200) {
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
      } else {
        setSnackbarData({
          message: 'Saved Failed!',
          severity: 'error',
        })
      }
      return response
    } catch (error) {
      console.error('Error saving Summary!', error)
    } finally {
      setLoading(false)
    }
  }

  const handleCalculate = async () => {
    setRows([])
    setLoading(true)
    try {
      const data =
        await RawMaterialNormsBasisApiService.handleCalculateNormsConfiguration(
          PLANT_ID,
          AOP_YEAR,
          keycloak,
        )

      if (data || data === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchData() // Refresh general data after calculation
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Refresh Failed!',
          severity: 'error',
        })
      }
      return data
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
      console.error('Error!', error)
    } finally {
      setLoading(false)
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
      saveBtn: true,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissionsManual = getAdjustedPermissions(
    {
      showAction: true,
      saveWithRemark: true,
      saveBtn: true,
      allAction: true,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      downloadExcelBtnFromUI: true,
      showCalculate: false,
      showCalculateVisibility: true,
      ExcelName: `${EXCEL_EXPORT_TITLE}`,
      showTitleNameBusiness: true,
      titleName: 'Shutdown Rate',
    },
    IS_OLD_YEAR,
  )

  const handleUpdate = async (updatedRows) => {
    setLoading(true)
    try {
      let payload = updatedRows?.map((row) => ({
        jan: 0,
        feb: 0,
        mar: 0,
        apr: row.MajorShutdown ?? 0,
        may: row.OneDayShutdown ?? 0,
        jun: 0,
        jul: 0,
        aug: 0,
        sep: 0,
        oct: 0,
        nov: 0,
        dec: 0,
        UOM: row.UOM || row.uom,
        auditYear: row.auditYear,
        normParameterFKId: row.NormParameter_FK_Id,
        remarks: row.remarks ?? '',
        id: null,
      }))

      await ProductionRangeApiService.postData(
        keycloak,
        payload,
        PLANT_ID,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully!',
        severity: 'success',
      })

      await fetchData()
      return { code: 200 }
    } catch (error) {
      console.error('Error updating data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Data Save failed!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const saveChanges = useCallback(async () => {
    setLoading(true)
    try {
      if (Object.keys(modifiedCells).length === 0) {
        if (summaryEdited) {
          await saveSummary(summary)
          setModifiedCells({})
          setSummaryEdited(false)
        }
        return
      }

      const rawData = Object.values(modifiedCells)
      const data = rawData.filter((row) => row.inEdit)

      if (data.length === 0) {
        setLoading(false)
        return
      }
      const requiredFields = ['remarks']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        return
      }
      await handleUpdate(data)
    } catch (error) {
      console.log('Error saving changes:', error)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, summaryEdited, summary])

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    try {
      setLoading(true)
      const response = await ShutdownRateApiService.getData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const formattedData = response?.data?.data?.map((row, index) => ({
        ...row,
        id: row.id || index,
        originalRemark: row.remarks || '',
        normType: row.normType || 'Rate',
        OneDayShutdown: Number(row.OneDayShutdown) || 0,
        MajorShutdown: Number(row.MajorShutdown) || 0,
      }))

      setRows(formattedData || [])
    } catch (error) {
      console.error('Error fetching data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching data',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = (rawFile) => {
    uploadCrackerConstant(rawFile)
  }

  const uploadCrackerConstant = async (rawFile) => {
    setLoading(true)

    try {
      let response

      response = await PtaConfigurationApiService.importExcel(
        rawFile,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })

        setLoading(false)

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
        link.setAttribute('download', 'Error File - Constants.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })

        setLoading(false)
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })

        setLoading(false)
      }

      return response
    } catch (error) {
      console.error('Error uploading xcel:', error)
      setSnackbarOpen(true)

      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      let response
      response = await PtaConfigurationApiService.exportExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        `${EXCEL_EXPORT_TITLE}`,
      )
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    } finally {
      setSnackbarOpen(true)
    }
  }

  return (
    <Box>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>
      <Box>
        <KendoDataTables
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          setRows={setRows}
          columns={NormConfigurationColumns}
          rows={rows}
          paginationOptions={[100, 200, 300]}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          apiRef={apiRef}
          open1={open1}
          setOpen1={setOpen1}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          handleRemarkCellClick={handleRemarkCellClick}
          handleCalculate={handleCalculate}
          fetchData={fetchData}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          permissions={adjustedPermissionsManual}
          summaryEdited={summaryEdited}
          groupBy={'normType'}
          saveChanges={saveChanges}
          plantID={PLANT_ID}
          handleExcelUpload={handleExcelUpload}
          downloadExcelForConfiguration={downloadExcelForConfiguration}
        />
      </Box>
    </Box>
  )
}

export default ShutdownRateGrid
