import CircularProgress from '@mui/material/CircularProgress'
import { useGridApiRef } from '@mui/x-data-grid'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import React, { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { setIsReleased } from 'store/reducers/dataGridStore'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from './index'
import { ConfigurationOtherCostApiService } from 'services/configuration-other-cost-api-service'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { shouldShowReleaseButton } from 'utils/releaseButtonUtils'
import {
  Backdrop,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material'
import { DataService } from 'services/DataService'
import { useMenuContext } from 'menu/menuProvider'
const ConfigurationOtherCost = ({ permissions }) => {
  const [modifiedCells, setModifiedCells] = React.useState({})
  const dispatch = useDispatch()
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
  const SCREEN_NAME = screenTitle?.title || 'Other Cost'
  const headerMap = generateHeaderNames(AOP_YEAR)
  const keycloak = useSession()
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}`
  const [openReleaseDialogBox, setOpenReleaseDialogBox] = useState(false)
  const [isReleaseDisabled, setIsReleaseDisabled] = useState(true)
  const { items: menuItems } = useMenuContext()
  const showReleaseButton = shouldShowReleaseButton(menuItems)
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
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'may',
      title: headerMap[5],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'jun',
      title: headerMap[6],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'jul',
      title: headerMap[7],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'aug',
      title: headerMap[8],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'sep',
      title: headerMap[9],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'oct',
      title: headerMap[10],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'nov',
      title: headerMap[11],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'dec',
      title: headerMap[12],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'jan',
      title: headerMap[1],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'feb',
      title: headerMap[2],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
    },
    {
      field: 'mar',
      title: headerMap[3],
      editable: true,
      width: 100,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 90,
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
      locked: true,
    },
    {
      field: 'UOM',
      title: 'UOM',
      widthT: 80,
      editable: false,
      locked: true,
    },
    ...monthColumns,
    {
      field: 'remarks',
      title: 'Remark',
      widthT: 90,
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

  const handleExcelUpload = (rawFile) => {
    saveExcelFile(rawFile)
  }
  const saveExcelFile = async (rawFile) => {
    setLoading(true)
    try {
      let response =
        await ConfigurationOtherCostApiService.saveConfigurationOtherCostExcelData(
          rawFile,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData(gradeId)
        await getNormTransactions()
      } else if (response?.code === 400 && response?.data) {
        // Partial save, error file download
        const byteCharacters = atob(response.data)
        const byteNumbers = new Array(byteCharacters.length)
        for (let i = 0; i < byteCharacters.length; i++) {
          byteNumbers[i] = byteCharacters.charCodeAt(i)
        }
        const byteArray = new Uint8Array(byteNumbers)
        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'Error File Other Cost Norms.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        await fetchData(gradeId)
        await getNormTransactions()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Save Failed!',
          severity: 'error',
        })
      }

      return response
    } catch (error) {
      console.error('Error saving data:', error)
      setLoading(false)
    } finally {
      // fetchData()
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    setLoading(true)
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {

      const resp = await ConfigurationOtherCostApiService.getConfigurationOtherCostExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
        SCREEN_NAME,
      )


      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error!', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    } finally {
      // optional cleanup or logging
      setLoading(false)
    }
  }

  const handleCalculate = async () => {
    setLoading(true)
    try {
      var data =
        await ConfigurationOtherCostApiService.handleCalculateConfigurationOtherCost(
          PLANT_ID,
          AOP_YEAR,
          keycloak,
        )
      if (data == 0 || data) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchData()
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

  const getIsReleased = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    try {
      const response = await DataService.getReleaseAOPStatus(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      // If response has data, disable the button (already released)
      // If no data, enable the button (not yet released)
      if (response?.data && Object.keys(response.data).length > 0) {
        setIsReleaseDisabled(true)
      } else {
        setIsReleaseDisabled(false)
      }
    } catch (error) {
      console.error('Error fetching release status:', error)
    }
  }
  useEffect(() => {
    getIsReleased()
  }, [keycloak, AOP_YEAR, PLANT_ID])

  const handleRelease = () => {
    setOpenReleaseDialogBox(true)
  }

  const closeReleaseDialogBox = () => {
    setOpenReleaseDialogBox(false)
  }

  const submitConfirmation = async () => {
    setOpenReleaseDialogBox(false)
    setLoading(true)
    try {
      const response = await DataService.releaseAOPReport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Released Successfully!',
        severity: 'success',
      })
      setIsReleaseDisabled(true)
      let isReleased = 1
      dispatch(setIsReleased({ isReleased }))
    } catch (error) {
      console.error('Error releasing report:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Release Failed!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }


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
      downloadExcelBtn: true,
      downloadExcelBtnFromUI: false,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: `${SCREEN_NAME}`,
      uploadExcelBtn: true,
      ExcelName: `${EXCEL_EXPORT_TITLE}_${SCREEN_NAME}`,
      showCalculate: true,
      showCalculateVisibility: calculationObject.length > 0,
      showReleaseBtn: showReleaseButton ? true : false,
    },
    isOldYear,
  )

  return (
    <div>
      <LoaderBackdrop open={!!loading} />
      <Dialog
        open={openReleaseDialogBox}
        onClose={closeReleaseDialogBox}
        disableScrollLock
        PaperProps={{
          sx: {
            borderRadius: '20px',
            p: 2,
            width: 400,
            backdropFilter: 'blur(8px)',
            boxShadow: '0 10px 30px rgba(0,0,0,0.08)',
          },
        }}
      >
        <DialogTitle
          sx={{
            fontWeight: 700,
            fontSize: '1.2rem',

            pb: 0.5,
          }}
        >
          Confirm Release
        </DialogTitle>

        <DialogContent sx={{ pt: 1 }}>
          <DialogContentText
            sx={{
              fontSize: '0.9rem',
              color: '#4b5563',
              lineHeight: 1.5,
            }}
          >
            Please confirm that <b style={{ color: '#16a34a' }}>Production</b>
            , <b style={{ color: '#16a34a' }}>Norms</b>, and{' '}
            <b style={{ color: '#16a34a' }}>Reports</b> are verified before
            releasing for review.
          </DialogContentText>
        </DialogContent>

        <DialogActions sx={{ px: 2, pb: 1.5, gap: 1 }}>
          <Button
            onClick={closeReleaseDialogBox}
            variant='text'
            sx={{
              textTransform: 'none',
              fontWeight: 600,
              color: '#6b7280',
              '&:hover': { background: 'rgba(0,0,0,0.04)' },
            }}
          >
            Cancel
          </Button>

          <Button
            onClick={submitConfirmation}
            variant='contained'
            className='btn-save'
            sx={{
              textTransform: 'none',
              px: 2.5,
            }}
          >
            Confirm
          </Button>
        </DialogActions>
      </Dialog>{' '}
      {
        <Box>
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
            handleExcelUpload={handleExcelUpload}
            downloadExcelForConfiguration={downloadExcelForConfiguration}
            handleCalculate={handleCalculate}
            isReleaseDisabled={isReleaseDisabled}
            handleRelease={handleRelease}
            permissions={adjustedPermissions}
            plantID={plantID}
          />
        </Box>
      }
    </div>
  )
}

export default ConfigurationOtherCost
