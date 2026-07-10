import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { useGridApiRef } from '@mui/x-data-grid'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import React, { useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from './index'
import { ConfigurationOtherCostApiService } from 'services/configuration-other-cost-api-service'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const ConfigurationOtherCost = ({ permissions }) => {
  const [modifiedCells, setModifiedCells] = React.useState({})
  const [loading, setLoading] = useState(false)
  const menu = useSelector((state) => state.dataGridStore)
  const { yearChanged, oldYear, plantID } = menu
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const [open1, setOpen1] = useState(false)
  const apiRef = useGridApiRef()
  const [rows, setRows] = useState([])
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [calculationObject, setCalculationObject] = useState([])
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    screenTitle,
    plantObject,
    siteObject,
    verticalObject,
    year,
  } = dataGridStore
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const PLANT_NAME = plantObject?.name
  const SITE_NAME = siteObject?.name
  const VERTICAL_NAME = verticalObject?.name
  const SCREEN_NAME = screenTitle?.title || 'Configuration Other Cost'
  const headerMap = generateHeaderNames(AOP_YEAR)
  const keycloak = useSession()
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

  const saveChanges = React.useCallback(async () => {
    try {
      setLoading(true)
      var data = Object.values(modifiedCells)

      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
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
        setLoading(false)
        return
      }

      await saveOtherCostData(data)
    } catch (error) {
      setLoading(false)
    }
  }, [apiRef, modifiedCells])

  const isCellEditable = (params) => {
    return params.row.isEditable
  }
  const valueFormat = ValueFormatterConsumption()

  const monthColumns = [
    {
      field: 'apr',
      title: headerMap[4],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'may',
      title: headerMap[5],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'jun',
      title: headerMap[6],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'jul',
      title: headerMap[7],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'aug',
      title: headerMap[8],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'sep',
      title: headerMap[9],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'oct',
      title: headerMap[10],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'nov',
      title: headerMap[11],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'dec',
      title: headerMap[12],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'jan',
      title: headerMap[1],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'feb',
      title: headerMap[2],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
    {
      field: 'mar',
      title: headerMap[3],
      editable: true,
      width: 120,
      align: 'right',
      format: valueFormat,
      type: 'number',
    },
  ]

  const colDefs = [
    {
      field: 'normParameterFKId',
      title: 'Particulars',
      widthT: 160,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 165,
    },
    {
      field: 'UOM',
      title: 'UOM',
      widthT: 80,
      editable: false,
    },
    ...monthColumns,
    {
      field: 'remarks',
      title: 'Remark',
      widthT: 100,
      editable: true,
    },
  ]

  const handleRemarkCellClick = (row) => {
    if (!row?.isEditable || READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveOtherCostData = async (rowsToSave) => {
    setLoading(true)
    try {
      const payload = rowsToSave.map((row) => ({
        apr: row.apr || null,
        may: row.may || null,
        jun: row.jun || null,
        jul: row.jul || null,
        aug: row.aug || null,
        sep: row.sep || null,
        oct: row.oct || null,
        nov: row.nov || null,
        dec: row.dec || null,
        jan: row.jan || null,
        feb: row.feb || null,
        mar: row.mar || null,
        UOM: row.UOM,
        remarks: row.remarks,
        auditYear: AOP_YEAR,
        normParameterFKId: row.normParameterFKId,
        id: null,
      }))

      if (payload.length > 0) {
        const response = await ConfigurationOtherCostApiService.saveConfigurationOtherCostData(
          PLANT_ID,
          payload,
          keycloak,
          AOP_YEAR,
        )
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Saved Successfully!`,
          severity: 'success',
        })
        setModifiedCells({})
        return response
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Data not saved!`,
          severity: 'error',
        })
      }
    } catch (error) {
      console.error(`Error saving Data`, error)
    } finally {
      fetchData()
      setLoading(false)
    }
  }

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    try {
      setLoading(true)
      setRows([])
      const response = await ConfigurationOtherCostApiService.getConfigurationOtherCostData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code !== 200) {
        setRows([])
        setLoading(false)
        return
      }

      setCalculationObject(response?.data?.aopCalculation || [])

      const formattedData = response?.data?.configurationDTOList?.map((item, index) => ({
        ...item,
        idFromApi: item.normParameterFKId,
        id: index,
        remarks: item?.remarks?.trim() || null,
        originalRemark: item?.remarks?.trim(),
        materialFkId: item?.normParameterFKId?.toLowerCase(),
        Particulars: item.productName || 'Particulars',
        isEditable: item.isEditable,
      })) || []

      setRows(formattedData)
    } catch (error) {
      console.error('Error fetching data:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [yearChanged, PLANT_ID, AOP_YEAR])

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear !== 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      addButton: false,
      deleteButton: false,
      saveBtn: true,
      customHeight: permissions?.customHeight,
      showAction: false,
      editButton: false,
      saveWithRemark: true,
      showCheckbox: false,
      marginBottom: true,
      allAction: true,
      downloadExcelBtn: false,
      downloadExcelBtnFromUI: true,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: `${SCREEN_NAME}`,
      uploadExcelBtn: false,
      ExcelName: `${EXCEL_EXPORT_TITLE}_${SCREEN_NAME}`,
    },
    isOldYear,
  )

  return (
    <div>
      <LoaderBackdrop open={!!loading} />
      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        isCellEditable={isCellEditable}
        title={SCREEN_NAME}
        columns={colDefs}
        setRows={setRows}
        rows={rows}
        onAddRow={(newRow) => console.log('New Row Added:', newRow)}
        onDeleteRow={(id) => console.log('Row Deleted:', id)}
        onRowUpdate={(updatedRow) => console.log('Row Updated:', updatedRow)}
        paginationOptions={[100, 200, 300]}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        open1={open1}
        setOpen1={setOpen1}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        handleRemarkCellClick={handleRemarkCellClick}
        permissions={adjustedPermissions}
        plantID={plantID}
      />
    </div>
  )
}

export default ConfigurationOtherCost
