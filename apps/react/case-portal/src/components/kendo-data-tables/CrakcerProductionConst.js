import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import { NormalOperationNormsApiService } from 'services/normal-operation-norms-api-service'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import useValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import {
  Backdrop,
  CircularProgress,
} from '../../../node_modules/@mui/material/index'
import KendoDataTables from './index'
import { validateFields } from 'utils/validationUtils'
import { ProductionConstarintsApiService } from 'services/production-constraints-api-service'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
const CrakcerProductionConst = () => {
  const keycloak = useSession()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    sitePlantChange,
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
  } = dataGridStore
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const vertName = verticalChange?.selectedVertical
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const lowerVertName = vertName?.toLowerCase()
  const [tabIndex, setTabIndex] = useState(0)

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

  const [loading1, setLoading1] = useState(false)

  const [productionRowsConstants, setProductionRowsConstants] = useState([])

  const valueFormat = useValueFormatterConsumption()

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [startDate, setStartDate] = useState()
  const [endDate, setEndDate] = useState()
  const [startDateObj, setStartDateObj] = useState([])
  const [endDateObj, setEndDateObj] = useState([])
  const [configurationExecutionDetails, setConfigurationExecutionDetails] =
    useState([])
  const [openConfirmDialog, setOpenConfirmDialog] = useState(false)
  const [gradeId, setGradeId] = React.useState(null)

  const [remarkDialogOpenConstants, setRemarkDialogOpenConstants] =
    useState(false)
  const [currentRemarkConstants, setCurrentRemarkConstants] = useState('')
  const [modifiedCellsConstants, setModifiedCellsConstants] = React.useState({})
  const [open1, setOpen1] = useState(false)
  const [currentRowIdConstants, setCurrentRowIdConstants] = useState(null)
  const [rowsConstants, setRowsConstants] = useState()

  const unsavedChangesRefConstants = React.useRef({
    unsavedRows: {},
    rowsBeforeChange: {},
  })

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      showCalculate: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
    }
  }

  const handleOpenDialog = () => {
    setOpenConfirmDialog(true)
  }
  const handleCloseDialog = () => {
    setOpenConfirmDialog(false)
  }
  const handleConfirmLoad = () => {
    setOpenConfirmDialog(false)
    onLoad()
  }

  useEffect(() => {
    if (!PLANT_ID || !AOP_YEAR) return

    fetchConstantsData()
  }, [PLANT_ID, AOP_YEAR])

  const FORMATE_VALUE = ValueFormatterProduction()

  const colDefsConstants = useMemo(() => {
    const cols = [
      {
        field: 'DisplayName',
        title: 'Particulars',
        editable: false,
        widthT: 150,
        hidden: false,
        minWidth: 120,
      },
      {
        field: 'UOM',
        title: 'UOM',
        editable: false,
        widthT: 80,
        minWidth: 60,
      },
      {
        field: 'ConstantValue',
        title: 'Value',
        editable: true,
        type: 'number',
        widthT: 120,
        format: FORMATE_VALUE,
        minWidth: 100,
      },

      {
        field: 'remarks',
        title: 'Remark',
        editable: false,
        widthT: 140,
        minWidth: 80,
        autoAdjust: false,
        type: 'string',
      },
    ]
    if (SITE_NAME_NO_CASE === 'C2') {
      return cols.map((col) => {
        if (col.field === 'ConstantValue') {
          return {
            ...col,
            type: 'integerNumberOnly',
          }
        }
        return col
      })
    }
    return cols
  }, [FORMATE_VALUE, SITE_NAME_NO_CASE])

  const saveCatalystData = async (newRow) => {
    setLoading1(true)
    try {
      var payload = []

      payload = newRow.map((row) => ({
        apr: row.apr || row.ConstantValue || null,
        may: row.apr || row.ConstantValue || null,
        jun: row.apr || row.ConstantValue || null,
        jul: row.apr || row.ConstantValue || null,
        aug: row.apr || row.ConstantValue || null,
        sep: row.apr || row.ConstantValue || null,
        oct: row.apr || row.ConstantValue || null,
        nov: row.apr || row.ConstantValue || null,
        dec: row.apr || row.ConstantValue || null,
        jan: row.apr || row.ConstantValue || null,
        feb: row.apr || row.ConstantValue || null,
        mar: row.apr || row.ConstantValue || null,
        UOM: '',
        auditYear: AOP_YEAR,
        normParameterFKId: row.normParameterFKId || row.NormParameter_FK_Id,
        remarks: row.remarks,
        id: row.idFromApi || null,
      }))

      const response = await DataService.saveCatalystData(
        PLANT_ID,
        payload,
        keycloak,
        AOP_YEAR,
      )
      if (response) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCellsConstants({})
        setLoading1(false)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Falied!',
          severity: 'error',
        })
      }

      return response
    } catch (error) {
      console.error('Error saving data:', error)
      setLoading1(false)
    } finally {
      fetchConstantsData()
      setLoading1(false)
    }
  }

  const fetchConstantsData = useCallback(async () => {
    setProductionRowsConstants([])
    try {
      const constantsRes =
        await ProductionConstarintsApiService.getProductionConstraints(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          'constant',
        )
      if (constantsRes?.code !== 200) {
        setProductionRowsConstants([])
        return
      }

      const data = constantsRes?.data
      const formattedData = data.map((item, index) => ({
        ...item,
        idFromApi: item.id,
        id: index,
        originalRemark: item.Remarks,
        srNo: index + 1,
        Particulars: item.NormTypeName,
        remarks: item.Remarks,
      }))

      setProductionRowsConstants(formattedData)
    } catch (error) {
      console.error('Error fetching constants data:', error)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const handleRemarkCellClickConstants = (row) => {
    if (READ_ONLY) return
    setCurrentRemarkConstants(row.remarks || '')
    setCurrentRowIdConstants(row.id)
    setRemarkDialogOpenConstants(true)
  }

  const uploadCrackerConstant = async (rawFile) => {
    setLoading1(true)

    try {
      let response

      response = await DataService.saveConfigurationExcelConstants(
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

        setLoading1(false)

        setModifiedCellsConstants({})
        fetchConstantsData()
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

        setLoading1(false)
        fetchConstantsData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })

        setLoading1(false)
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
      setLoading1(false)
    }
  }

  const handleExcelUpload = (rawFile) => {
    uploadCrackerConstant(rawFile)
  }
  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      let response
      response = await DataService.getConfigurationExcelConstants(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        `${EXCEL_EXPORT_TITLE}_Constant`,
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

  const adjustedPermissionsConstants = getAdjustedPermissions(
    {
      showAction: false,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: siteObject?.name?.toLowerCase() === 'c2' ? 'Production Basis' : 'Production Target Constraints',
      saveWithRemark: true,
      saveBtn: true,
      showCalculate: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      makePagable: false,
    },
    isOldYear,
  )

  const saveChanges = React.useCallback(async () => {
    try {
      var data = Object.values(modifiedCellsConstants)

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
      saveCatalystData(data)
    } catch (error) {
      // Handle error if necessary
    }
  }, [modifiedCellsConstants])

  return (
    <div>
      <LoaderBackdrop open={!!loading1} />

      <Box>
        <KendoDataTables
          modifiedCells={modifiedCellsConstants}
          setModifiedCells={setModifiedCellsConstants}
          columns={colDefsConstants}
          setRows={setProductionRowsConstants}
          rows={productionRowsConstants}
          onAddRow={(newRow) => console.log('New Row Added:', newRow)}
          onDeleteRow={(id) => console.log('Row Deleted:', id)}
          onRowUpdate={(updatedRow) => console.log('Row Updated:', updatedRow)}
          paginationOptions={[100, 200, 300]}
          saveChanges={saveChanges}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          open1={open1}
          setOpen1={setOpen1}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          remarkDialogOpen={remarkDialogOpenConstants}
          setRemarkDialogOpen={setRemarkDialogOpenConstants}
          currentRemark={currentRemarkConstants}
          setCurrentRemark={setCurrentRemarkConstants}
          currentRowId={currentRowIdConstants}
          unsavedChangesRef={unsavedChangesRefConstants}
          handleRemarkCellClick={handleRemarkCellClickConstants}
          permissions={adjustedPermissionsConstants}
          {...(SITE_NAME_NO_CASE === 'VMD' && { groupBy: 'Particulars' })}
          plantID={PLANT_ID}
          handleExcelUpload={handleExcelUpload}
          downloadExcelForConfiguration={downloadExcelForConfiguration}
        />
      </Box>
      <Notification
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
      />
    </div>
  )
}

export default CrakcerProductionConst
