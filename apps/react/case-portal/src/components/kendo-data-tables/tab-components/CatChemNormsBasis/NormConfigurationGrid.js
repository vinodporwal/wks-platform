import React, { useState, useEffect, useCallback } from 'react'
import { Box, Typography, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import KendoDataTables from '../../index'
import { DataService } from 'services/DataService'
import { getRoleName } from 'services/role-service'
import { RawMaterialNormsBasisApiService } from 'services/raw-material-norms-basis-api-service'
import { validateFields } from 'utils/validationUtils'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
const NormsConfigurationGrid = ({
  summary,
  summaryEdited,
  setSummaryEdited,
}) => {
  const [rows, setRows] = useState([])
  const [NormsRows, setNormsRows] = useState([])
  const [NormsRows2, setNormsRows2] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [modifiedNormsCells, setModifiedNormsCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [open1, setOpen1] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [calculationObject, setCalculationObject] = useState([])
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

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_Cat_Chem`

  const FORMATE_VALUE = '{0:0.000}'
  const headerMap = generateHeaderNames(AOP_YEAR)

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const NormConfigurationColumns = [
    {
      field: 'displayName', // matches API
      title: 'Particulars',
      editable: false,
      widthT: 300,
      autoAdjust: false,
      minWidth: 100,
    },
    {
      field: 'uom',
      title: 'UOM',
      editable: false,
      widthT: 80,
      minWidth: 100,
    },
    {
      field: 'apr',
      title: 'Value',
      editable: false,
      widthT: 100,
      type: 'number',
      minWidth: 100,
    },

    {
      field: 'normParameterFKId',
      title: 'idFromApi',
      filterable: 'false',
      hidden: true,
      minWidth: 100,
      isVisible: false,
    },
  ]

  const fetchNormsConfigurationManualData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setModifiedCells({})

    try {
      setLoading(true)

      const response =
        await RawMaterialNormsBasisApiService.getNormsConfigurationData(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          'Manual',
        )

      const formattedData = response?.data?.normConfigurationList?.map(
        // ✅ Fixed key
        (row, index) => ({
          ...row,
          id: row.normParameterFkId || index,
          particulars: row.displayName,
          uom: row.uom,
          value: parseFloat(row.apr) || 0,
          remarks: row.remarks,
          originalRemark: row.remarks || '',
          isEditable: false,
          isdisable: true,
        }),
      )
      setCalculationObject(response?.data?.aopCalculation)

      setNormsRows(formattedData || [])
    } catch (error) {
      console.error('Error fetching Norms Configuration data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching data',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }
  const fetchNormsConfigurationCalculatedData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setModifiedCells({})

    try {
      setLoading(true)

      const response =
        await RawMaterialNormsBasisApiService.getNormsConfigurationData(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          'Calculated',
        )

      const formattedData = response?.data?.normConfigurationList?.map(
        // ✅ Fixed key
        (row, index) => ({
          ...row,
          id: row.normParameterFkId || index,
          particulars: row.displayName,
          uom: row.uom,
          value: parseFloat(row.apr) || 0,
          remarks: row.remarks,
          originalRemark: row.remarks || '',
          isEditable: false,
          isdisable: true,
        }),
      )
      setNormsRows2(formattedData || [])
    } catch (error) {
      console.error('Error fetching Norms Configuration data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching data',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    setModifiedCells({})
    fetchNormsConfigurationManualData()
    // fetchNormsConfigurationCalculatedData()
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
      var data =
        await RawMaterialNormsBasisApiService.handleCalculateNormsConfiguration(
          PLANT_ID,
          AOP_YEAR,
          keycloak,
        )

      if (data == 0 || data) {
        // dispatch(setIsBlocked(true))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchNormsConfigurationManualData()
        // fetchNormsConfigurationCalculatedData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Refresh Falied!',
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

  const adjustedPermissionsManual = getAdjustedPermissions(
    {
      showAction: true,
      saveWithRemark: true,
      saveBtn: false,
      allAction: true,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      showTitleNameBusiness: true,
      showCalculate: true,
      //Object.keys(calculationObject || {}).length > 0 ? true : false,
      titleName: 'Norms Configuration - Calculated',
      showCalculateVisibility: true,
    },
    IS_OLD_YEAR,
  )
  const adjustedPermissionsCalculated = getAdjustedPermissions(
    {
      showAction: true,
      saveWithRemark: true,
      saveBtn: false,
      allAction: true,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      showTitleNameBusiness: true,
      titleName: 'Norms Configuration - Calculated',
      showCalculate: false,
      showCalculateVisibility: false,
    },
    IS_OLD_YEAR,
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <Box>
        <KendoDataTables
          modifiedCells={modifiedNormsCells}
          setModifiedCells={setModifiedNormsCells}
          setRows={setNormsRows}
          columns={NormConfigurationColumns}
          rows={NormsRows}
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
          fetchData={fetchNormsConfigurationManualData}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          permissions={adjustedPermissionsManual}
          summaryEdited={summaryEdited}
          groupBy={'normTypeName'}
        />
        {/* <KendoDataTables
          modifiedCells={modifiedNormsCells}
          setModifiedCells={setModifiedNormsCells}
          setRows={setNormsRows2}
          columns={NormConfigurationColumns}
          rows={NormsRows2}
          paginationOptions={[100, 200, 300]}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          apiRef={apiRef}
          open1={open1}
          setOpen1={setOpen1}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          handleRemarkCellClick={handleRemarkCellClick}
          fetchData={fetchNormsConfigurationCalculatedData}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          permissions={adjustedPermissionsCalculated}
          summaryEdited={summaryEdited}
          groupBy={'normTypeName'}
        /> */}
      </Box>
    </Box>
  )
}

export default NormsConfigurationGrid
