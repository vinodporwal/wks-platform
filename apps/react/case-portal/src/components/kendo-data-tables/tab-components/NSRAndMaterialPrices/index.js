import { useState, useEffect, useCallback } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import getEnhancedAOPColDefs from 'components/data-tables/CommonHeader/kendo_ConfigHeader'
import { getRoleName } from 'services/role-service'
import { validateFields } from 'utils/validationUtils'
import { ProductionNormsApiService } from 'services/production-norms-api-service'
import KendoDataTables from 'components/kendo-data-tables/index'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const NSRAndMaterialPrices = () => {
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
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_NSR_MATERIAL_PRICES`

  const FORMATE_VALUE = ValueFormatterProduction()

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const productionColumns = [
    {
      field: 'id',
      title: 'id',
      filterable: false,
      hidden: true,
    },
    {
      field: 'plantName',
      title: 'Plant Name',
      editable: false,
      widthT: 120,
      hidden: false,
    },
    {
      field: 'plantCode',
      title: 'Plant Code',
      editable: false,
      widthT: 100,
    },
    {
      field: 'materialCode',
      title: 'Material Code',
      editable: false,
      widthT: 100,
    },
    {
      field: 'materialDescription',
      title: 'Mat Desc',
      editable: false,
      widthT: 100,
    },

    {
      field: 'UOM',
      title: 'UOM',
      editable: false,
      widthT: 80,
    },

    {
      field: 'price',
      title: 'Price',
      editable: true,
      type: 'number',
      widthT: 100,
      format: FORMATE_VALUE,
    },
  ]

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    try {
      var data = await ProductionNormsApiService.getNSRAndMaterialPrices(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const formattedData = data?.map((row, index) => ({
        ...row,
        price: Number(row.price) || 0,
        remarks: row.remarks || '',
        originalRemark: row.remarks || '',
        inEdit: false,
        isEditable: false,
      }))
      setRows(formattedData || [])
    } catch (error) {
      console.error('Error fetching IBIN Losses data:', error)
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
    fetchData()
  }, [oldYear, yearChanged, keycloak, PLANT_ID, AOP_YEAR])

  const handleUpdate = async (updatedRows) => {
    setLoading(true)
    try {
      let payload = updatedRows?.map((row) => ({
        id: row.id,
        plantId: row.plantId,
        plantName: row.plantName,
        plantCode: row.plantCode,
        siteName: row.siteName,
        aopYear: row.aopYear,
        materialCode: row.materialCode,
        materialDescription: row.materialDescription,
        grade: row.grade,
        account: row.account,
        mAccount: row.mAccount,
        mContributiontype: row.mContributiontype,
        price: row.price,
        remarks: row.remarks,
      }))

      const response = await ProductionNormsApiService.saveNSRAndMaterialPrices(
        PLANT_ID,
        payload,
        keycloak,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: response?.message || 'Saved Successfully!',
        severity: 'success',
      })

      setModifiedCells({})
      await fetchData()
      return response
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
  }, [modifiedCells])

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
      showAction: true,
      saveWithRemark: true,
      saveBtn: false,
      allAction: true,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      showTitleNameBusiness: true,
      titleName: 'NSR & Material Prices',
    },
    IS_OLD_YEAR,
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setRows}
        columns={productionColumns}
        rows={rows}
        paginationOptions={[100, 200, 300]}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        handleRemarkCellClick={handleRemarkCellClick}
        fetchData={fetchData}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        permissions={adjustedPermissions}
        groupBy={'account'}
      />
    </Box>
  )
}

export default NSRAndMaterialPrices
