import { useGridApiRef } from '@mui/x-data-grid'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import React, { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { setIsReleased } from 'store/reducers/dataGridStore'
import { validateRowDataWithRemarks } from '../../common/commonUtilityFunctions'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import ValueFormatterPhaseTwo from '../../common/ValueFormatterPhaseTwo'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { shouldShowReleaseButton } from 'utils/releaseButtonUtils'
import { Box } from '@mui/material'
import { DataService } from 'services/DataService'
import { useMenuContext } from 'menu/menuProvider'
import { ConfigurationOtherCostApiService } from 'components/aop-phase-two/services/polyester/configuration-other-cost-api-service'
import ReleaseDialog from '../../common/components/ReleaseDialog'
import { generateExcelNameWithoutExt } from '../../common/utilities/excelNameUtil'
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
  const [originalRows, setOriginalRows] = useState([])
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
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = screenTitle?.title || 'Other Cost'
  const headerMap = generateHeaderNames(AOP_YEAR)
  const keycloak = useSession()
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const EXCEL_EXPORT_TITLE = generateExcelNameWithoutExt(dataGridStore, SCREEN_NAME)
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

      const fieldsToCheck = [
        'apr',
        'may',
        'jun',
        'jul',
        'aug',
        'sep',
        'oct',
        'nov',
        'dec',
        'jan',
        'feb',
        'mar',
      ]
      const validationMessage = validateRowDataWithRemarks(
        data,
        originalRows,
        fieldsToCheck,
        'Particulars',
        'remarks',
      )
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
  }, [apiRef, modifiedCells, originalRows])

  const isCellEditable = (params) => {
    return params.row.isEditable
  }
  const valueFormat = ValueFormatterPhaseTwo()

  const monthColumns = [
    {
      field: 'apr',
      title: headerMap[4],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'may',
      title: headerMap[5],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'jun',
      title: headerMap[6],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'jul',
      title: headerMap[7],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'aug',
      title: headerMap[8],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'sep',
      title: headerMap[9],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'oct',
      title: headerMap[10],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'nov',
      title: headerMap[11],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'dec',
      title: headerMap[12],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'jan',
      title: headerMap[1],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'feb',
      title: headerMap[2],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
    {
      field: 'mar',
      title: headerMap[3],
      editable: true,
      align: 'right',
      format: valueFormat,
      type: 'number',
      minWidth: 120,
    },
  ]

  const colDefs = [
    {
      field: 'normParameterFKId',
      title: 'Particulars',
      minWidth: 20,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'productName',
      title: 'Particulars',
      minWidth: 200,
      locked: true,
    },
    {
      field: 'UOM',
      title: 'UOM',
      minWidth: 100,
      editable: false,
      locked: true,
    },
    ...monthColumns,
    {
      field: 'remarks',
      title: 'Remark',
      minWidth: 160,
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
      setOriginalRows([])
      const response = await ConfigurationOtherCostApiService.getConfigurationOtherCostData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code !== 200) {
        setRows([])
        setOriginalRows([])
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
      setOriginalRows(formattedData)
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
        await fetchData()
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
        await fetchData()
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
    } finally{
      setLoading(false)
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
      showExport: false,
      showImport: false,
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
      showExport: true,
      showImport: true,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: `${SCREEN_NAME}`,
      uploadExcelBtn: true,
      ExcelName: `${EXCEL_EXPORT_TITLE}`,
      showCalculate: false,
      calculateDisabled: false,
      showReleaseBtn: showReleaseButton ? true : false,
    },
    isOldYear,
  )

  return (
    <div>
      <LoaderBackdrop open={!!loading} />
      <ReleaseDialog
        openReleaseDialogBox={openReleaseDialogBox}
        closeReleaseDialogBox={closeReleaseDialogBox}
        submitConfirmation={submitConfirmation}
      />
      <Box>
        <AdvanceKendoTable
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          title={SCREEN_NAME}
          columns={colDefs}
          setRows={setRows}
          rows={rows}
          saveChanges={saveChanges}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          setCurrentRowId={setCurrentRowId}
          handleRemarkCellClick={handleRemarkCellClick}
          handleExcelUpload={handleExcelUpload}
          handleExport={downloadExcelForConfiguration}
          handleCalculate={handleCalculate}
          isReleaseDisabled={isReleaseDisabled}
          handleRelease={handleRelease}
          permissions={adjustedPermissions}
        />
      </Box>
    </div>
  )
}

export default ConfigurationOtherCost
