import React, { useState, useEffect, useCallback } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
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
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import KendoDataTables from './index'
import { ReportDataService } from 'services/ReportDataService'

const ProductionNormsReport = () => {
  const [rows, setRows] = useState([])
  const [columns, setColumns] = useState([])
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

  const valueFormat = ValueFormatterProduction()

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const isOldYear = false

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
  const IS_VCM_HMDVMD =VERTICAL_NAME_NO_CASE === 'VCM' && (SITE_NAME_NO_CASE=== 'HMD' || SITE_NAME_NO_CASE=== 'VMD')
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}_Season_Month`

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  useEffect(() => {
    setModifiedCells({})
    fetchData()
  }, [oldYear, yearChanged, keycloak, PLANT_ID, AOP_YEAR])

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
      allAction: true,
      downloadExcelBtnFromUI: false,
      ExcelName: `${EXCEL_EXPORT_TITLE}`,
      showTitleNameBusiness: true,
      titleName: IS_VCM_HMDVMD? `Main Products - Production for the budget year FY-${AOP_YEAR}` : 'Main Products - Production for the budget year',
    },
    IS_OLD_YEAR,
  )

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    try {
      setLoading(true)
      const response = await ReportDataService.getMonthlyProductionReportData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const formattedData = response?.data?.data?.map((row, index) => ({
        ...row,
        id: row.id || index,
        isEditable: false,
      }))

      setRows(formattedData || [])
      const filterColumns = response?.data?.columns
        ?.filter((col) => !['Plant_FK_Id', 'AOPYear'].includes(col.field))
        .map((col) => ({
          ...col,
          format: valueFormat,
          minWidth: 120,
        }))

      setColumns(filterColumns)
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

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <Box>
        <KendoDataTables
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          setRows={setRows}
          columns={columns}
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
          fetchData={fetchData}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          permissions={adjustedPermissionsManual}
          plantID={PLANT_ID}
        />
      </Box>
    </Box>
  )
}

export default ProductionNormsReport
