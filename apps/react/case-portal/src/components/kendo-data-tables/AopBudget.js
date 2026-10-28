import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Select,
  MenuItem,
  Backdrop,
  Box,
  CircularProgress,
  Typography,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'

import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector, useDispatch } from 'react-redux'
import { setIsReleased } from 'store/reducers/dataGridStore'
import { validateFields } from 'utils/validationUtils'
import { Grid, TextField } from '../../../node_modules/@mui/material/index'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { TextArea } from '../../../node_modules/@progress/kendo-react-inputs/index'
import { AOPMaintenanceApiService } from 'services/aop-maintenance-api-service'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { DataService } from 'services/DataService'
import { shouldShowReleaseButton } from 'utils/releaseButtonUtils'
import { useMenuContext } from 'menu/menuProvider'
export default function AopBudget() {
  const keycloak = useSession()

  // const READ_ONLY = getRoleName(keycloak)

  const [row, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [textAreaRedDesign, setTextAreaRedDesign] = useState(false)
  const [textAreaRedRemark, setTextAreaRedRemark] = useState(false)
  const [openSaveDialog, setOpenSaveDialog] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = React.useState({})
  const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
  const [designRemarks, setDesignRemarks] = useState('')
  const [designBasis, setDesignBasis] = useState('')
  const [
    designBasisAndDesignRemarksEdited,
    setDesignBasisAndDesignRemarksEdited,
  ] = useState(false)

  const [
    designBasisAndDesignRemarksEdited2,
    setDesignBasisAndDesignRemarksEdited2,
  ] = useState(false)

  const dispatch = useDispatch()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const headerMap = generateHeaderNames(AOP_YEAR)
  const thisYear = AOP_YEAR


  const VERTICAL_NAME_U = verticalObject?.name?.toUpperCase()
  const SiteName = siteObject?.name
  const SITE_NAME_U = siteObject?.name?.toUpperCase()
  const PLANT_NAME_U = plantObject?.name?.toUpperCase()
  const EXCEL_NAME = `${VERTICAL_NAME_U}_${SITE_NAME_U}_${AOP_YEAR}_Budget_Maintenance`



  // second grid states
  const [rowsP, setRowsP] = useState([])
  const [remarkDialogOpenP, setRemarkDialogOpenP] = useState(false)
  const [currentRemarkP, setCurrentRemarkP] = useState('')
  const [currentRowIdP, setCurrentRowIdP] = useState(null)
  const [modifiedCellsP, setModifiedCellsP] = React.useState({})
  const [enableSaveAddBtnP, setEnableSaveAddBtnP] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [openReleaseDialogBox, setOpenReleaseDialogBox] = useState(false)
  const [isReleaseDisabled, setIsReleaseDisabled] = useState(true)

  const { items: menuItems } = useMenuContext()
  const showReleaseButton = shouldShowReleaseButton(menuItems)
  const oldYearLabel = useMemo(() => {
    if (!thisYear || !thisYear.includes('-')) return ''
    const [start, end] = thisYear.split('-').map(Number)
    return `${start - 1}-${(end - 1).toString().slice(-2)}`
  }, [thisYear])

  const FORMATE_DECIMAL = ValueFormatterProduction()

  const monthFields = [
    {
      field: 'apr',
      index: 4,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'may',
      index: 5,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'jun',
      index: 6,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'jul',
      index: 7,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'aug',
      index: 8,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'sep',
      index: 9,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'oct',
      index: 10,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'nov',
      index: 11,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'dec',
      index: 12,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'jan',
      index: 1,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'feb',
      index: 2,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
    {
      field: 'mar',
      index: 3,
      editable: true,
      type: 'number',
      format: FORMATE_DECIMAL,
      width: 120,
      minWidth: 100,
    },
  ]

  const monthTotal = [
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

  const columns = [
    { field: 'plantName', title: 'Plant', widthT: 130, minWidth: 130, locked: true },
    { field: 'costName', title: 'Cost', widthT: 130, minWidth: 130, locked: true },
    {
      field: 'budgetType',
      title: 'Budget Type',
      widthT: 80,
      hidden: true,
      minWidth: 100,
      isVisibe: false,
    },
    {
      field: 'percentChange',
      title: '% Change (+/-)',
      widthT: 130,
      editable: true,
      type: 'percentChange',
      minWidth: 130,
    },
    // { field: 'symbol', title: '+VE/-VE', width: 120 },
    ...monthFields.map(
      ({ field, index, editable, type, format, width, minWidth }) => ({
        field,
        title: headerMap[index],
        editable,
        type,
        format,
        width,
        minWidth,
      }),
    ),
    {
      field: 'allMonthsTotal',
      title: 'Total',
      editable: false,

      type: 'number',
      format: FORMATE_DECIMAL,
      minWidth: 100,
    },
    {
      field: 'remark',
      title: 'Remark',
      editable: true,
      widthT: 100,
      minWidth: 100,
    },
  ]

  const formatPercentChange = (value) => {
    if (value == null || value === '') return null // keep null/empty as-is

    const raw = String(value).trim()

    // If it already starts with + or -, leave as-is
    if (/^[+-]/.test(raw)) return raw

    // Convert to number safely
    const num = Number(raw)
    if (isNaN(num)) return raw // not a valid number, leave it alone

    // If number is exactly 0, make it null
    if (num === 0) return null

    // If number > 0, prefix "+"
    return num > 0 ? `+${num}` : `${num}`
  }

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      // Fetch for Consumption Budget
      const resConsumption = await AOPMaintenanceApiService.maintenacegetdata(
        keycloak,
        'ConsumptionBudget',
        PLANT_ID,
        AOP_YEAR,
      )

      const mapped = (resConsumption?.data || []).map((item) => {
        const allMonthsTotal = monthTotal?.reduce((sum, month) => {
          const value = parseFloat(item[month]) || 0
          return sum + value
        }, 0)

        return {
          ...item,
          plantName: item.plantName || '',
          IsEditable: item.isEditable,
          originalRemark: item.remark?.trim() || '',
          percentChange: formatPercentChange(item?.percentChange),
          originalPercentChange: item?.percentChange || null,
          allMonthsTotal,
        }
      })

      setRows(mapped)

      // Fetch for Procurement Budget
      const resProcurement = await AOPMaintenanceApiService.maintenacegetdata(
        keycloak,
        'ProcurementBudget',
        PLANT_ID,
        AOP_YEAR,
      )
      const mappedP = (resProcurement?.data || []).map((item, index) => {
        const allMonthsTotal = monthTotal?.reduce((sum, month) => {
          const value = parseFloat(item[month]) || 0
          return sum + value
        }, 0)

        return {
          ...item,
          plantName: item.plantName || item.plantName || '',
          IsEditable: item.isEditable,
          originalRemark: item.remark?.trim() || '',
          percentChange: item?.percentChange || null,
          originalPercentChange: item?.percentChange || null,
          allMonthsTotal,
        }
      })
      setRowsP(mappedP)
    } catch (err) {
      console.error('fetchData error', err)
      setRows([])
      setRowsP([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, yearChanged, PLANT_ID])

  const resetDataChanges = useCallback(async () => {
    setModifiedCells({})
    setModifiedCellsP({})

    //FETCH DATA WHEN RESET BUTTON CLICKED
    fetchData()

    //FETCH BOTH DESIGN BASIS & DESIGN REMARKS AS WELL
    fetchDesignRemarksAndDesignBasis()
  }, [keycloak, yearChanged, PLANT_ID])

  const fetchDesignRemarksAndDesignBasis = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    setTextAreaRedDesign(false)
    setTextAreaRedRemark(false)

    setDesignRemarks('')
    setDesignBasis('')
    try {
      const resDesignBasis = await AOPMaintenanceApiService.designBasis(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      setDesignBasis(resDesignBasis?.data[0]?.summary)

      const resDesignRemarks = await AOPMaintenanceApiService.designRemarks(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      setDesignRemarks(resDesignRemarks?.data[0]?.summary)
    } catch (err) {
      console.error('fetchData error', err)
      setDesignBasis(null)
      setDesignRemarks(null)
    } finally {
      setLoading(false)
    }
  }, [keycloak, yearChanged, PLANT_ID])

  useEffect(() => {
    fetchData()
    fetchDesignRemarksAndDesignBasis()
  }, [
    fetchData,
    fetchDesignRemarksAndDesignBasis,
    yearChanged,
    plantID,
    keycloak,
  ])
  const handleCalculate = () => { }
  const handleCalculateP = () => { }

  const getIsReleased = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    try {
      const response = await DataService.getReleaseAOPStatus(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

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

  const handleRemarkCellClick = useCallback((row) => {
    if (!row?.IsEditable || READ_ONLY) return
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }, [])

  const handleRemarkCellClickP = useCallback((row) => {
    if (READ_ONLY) return
    setCurrentRemarkP(row.remark || '')
    setCurrentRowIdP(row.id)
    setRemarkDialogOpenP(true)
  }, [])

  const getAdjustedPermissionsP = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,

      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      resetButton: false,
      percentChangeLogic: true,
    }
  }

  const adjustedPermissionsP = getAdjustedPermissionsP(
    {
      saveBtn: false,
      addButton: false,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: 'Procurement Budget',
      adjustedPermissions: true,
      // downloadExcelBtnFromUI: true,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      ExcelName: `${vertName}_${siteObject?.name}_${AOP_YEAR}_Budget_Maintenance`,
      constarins: ['+', '-'],
      resetButton: false,
      percentChangeLogic: true,
    },
    isOldYear,
  )
  const getAdjustedPermissionsC = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,

      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      resetButton: false,
      percentChangeLogic: true,
    }
  }

  const adjustedPermissionsC = getAdjustedPermissionsC(
    {
      allAction: true,
      saveBtn: true,
      showTitleNameBusiness: true,
      titleName: 'Consumption Budget',
      addButton: false,
      adjustedPermissions: true,
      downloadExcelBtnFromUI: false,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      ExcelName: `${vertName}_${siteObject?.name}_${AOP_YEAR}_Budget_Maintenance`,
      constarins: ['+', '-'],
      resetButton: false,
      percentChangeLogic: true,
      showResetButton: true,
      showReleaseBtn: showReleaseButton ? true : false,
    },
    isOldYear,
  )

  function omitFields(obj, fields) {
    const result = { ...obj }
    fields.forEach((field) => {
      delete result[field]
    })
    return result
  }

  const saveSummary = async () => {
    try {
      await AOPMaintenanceApiService.saveDesignRemarks(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        designRemarks,
      )

      await AOPMaintenanceApiService.saveDesignBasis(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        designBasis,
      )

      fetchDesignRemarksAndDesignBasis()
      setDesignBasisAndDesignRemarksEdited(false)
      setDesignBasisAndDesignRemarksEdited2(false)
    } catch (err) {
      // setSnackbarData({ message: 'Save failed!', severity: 'error' })
      // setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const handleSaveAll = async () => {
    setLoading(true)
    try {
      const consumptionData = Object.values(modifiedCells)
      const procurementData = Object.values(modifiedCellsP)

      //VALIDATION REMOVED

      // if (
      //   !designBasisAndDesignRemarksEdited ||
      //   !designBasisAndDesignRemarksEdited2
      // ) {
      //   setSnackbarData({
      //     message: 'Please update Justification & Remarks',
      //     severity: 'error',
      //   })
      //   setSnackbarOpen(true)
      //   setLoading(false)

      //   if (!designBasisAndDesignRemarksEdited) {
      //     setTextAreaRedDesign(true)
      //   } else {
      //     setTextAreaRedDesign(false)
      //   }
      //   if (!designBasisAndDesignRemarksEdited2) {
      //     setTextAreaRedRemark(true)
      //   } else {
      //     setTextAreaRedRemark(false)
      //   }
      //   return
      // }

      setTextAreaRedDesign(false)
      setTextAreaRedRemark(false)

      const requiredFields = ['remark']
      const validationMessageC = validateFields(consumptionData, requiredFields)
      const validationMessageP = validateFields(procurementData, requiredFields)

      if (validationMessageC || validationMessageP) {
        setSnackbarData({
          message: validationMessageC || validationMessageP,
          severity: 'error',
        })
        setSnackbarOpen(true)
        setLoading(false)
        return
      }

      const fieldsToOmit = ['isEditable', 'IsEditable']

      const allRows = [
        ...consumptionData.map((row) => omitFields(row, fieldsToOmit)),
        ...procurementData.map((row) => omitFields(row, fieldsToOmit)),
      ]

      const prefixPlusForNumericPercent = (row) => {
        if (!row || row.percentChange == null) return row
        const raw = String(row.percentChange).trim()
        if (/^[0-9]+(\.[0-9]+)?$/.test(raw)) {
          return { ...row, percentChange: `+${raw}` }
        }
        return row
      }

      const processedRows = allRows.map(prefixPlusForNumericPercent)

      saveSummary()
      // Send as array payload
      await AOPMaintenanceApiService.savemaintenacegetdata(
        processedRows,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({ message: 'Saved successfully!', severity: 'success' })
      setModifiedCells({})
      setModifiedCellsP({})

      fetchData()
    } catch (err) {
      setSnackbarData({ message: 'Save failed!', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }


  const downloadExcelForConfiguration = async () => {
    setLoading(true)
    try {
      await AOPMaintenanceApiService.maintenaceExportdata(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME
      )

      setSnackbarData({ message: 'Export started!', severity: 'success' })
      setSnackbarOpen(true)
    } catch (err) {
      setSnackbarData({ message: 'Export failed!', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }
  const budgetMaintenanceExcelFile = async (rawFile) => {
    setLoading(true)

    try {
      let response

      response = await AOPMaintenanceApiService.maintenaceImportExceldata(
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
        fetchData()
        fetchDesignRemarksAndDesignBasis()
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
        link.setAttribute('download', 'Error File - budgetMaintenance.xlsx')
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
        fetchDesignRemarksAndDesignBasis()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }

      return response
    } catch (error) {
      console.error('Error uploading Budget Maintenance Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = (rawFile) => {
    budgetMaintenanceExcelFile(rawFile)
  }

  const resetRowData1 = async (paramsForDelete) => { }

  const resetRowData2 = async (paramsForDelete) => { }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      {PLANT_NAME?.toLowerCase() === 'eoeg' && (
        <Typography component='div' className='grid-title'>
          <div>Planning Plant : 40N0 </div>
          <div>Maintenance Plant : 40N3</div>
        </Typography>
      )}

      <Typography
        component='div'
        className='grid-title'
        sx={{ marginBottom: '5px' }}
      >
        <Grid container spacing={1}>
          {/* Design Basis Section */}
          <Grid item xs={6}>
            <Grid
              container
              alignItems='center'
              justifyContent='space-between'
            // sx={{ marginBottom: 0.5 }}
            >
              <Grid item>
                <div
                  style={{
                    fontWeight: 600,
                    // marginBottom: 0.5,
                  }}
                >
                  Justification
                  {textAreaRedDesign && <span style={{ color: 'red' }}>*</span>}
                </div>
              </Grid>
            </Grid>

            <TextArea
              disabled={READ_ONLY}
              className={textAreaRedDesign ? 'textarea-error' : ''}
              value={designBasis}
              rows={3}
              onChange={(e) => {
                setDesignBasis(e.target.value)
                setDesignBasisAndDesignRemarksEdited(true)
              }}
            />
          </Grid>

          {/* Remarks Section */}
          <Grid item xs={6}>
            <Grid
              container
              alignItems='center'
              justifyContent='space-between'
            // sx={{ marginBottom: 0.5 }}
            >
              <Grid item>
                <div
                  style={{
                    fontWeight: 600,
                    // marginBottom: 0.5,
                  }}
                >
                  Remarks
                  {textAreaRedRemark && <span style={{ color: 'red' }}>*</span>}
                </div>
              </Grid>
            </Grid>

            <TextArea
              disabled={READ_ONLY}
              className={textAreaRedRemark ? 'textarea-error' : ''}
              value={designRemarks}
              rows={3}
              onChange={(e) => {
                setDesignRemarks(e.target.value)
                setDesignBasisAndDesignRemarksEdited2(true)
              }}
            />
          </Grid>
        </Grid>
      </Typography>

      <KendoDataTables
        title='Consumption Budget'
        titleMain='Monthly Budget'
        modifiedCells={modifiedCells}
        columns={columns}
        rows={row}
        setRows={setRows}
        fetchData={fetchData}
        setModifiedCells={setModifiedCells}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        enableSaveAddBtn={enableSaveAddBtn}
        saveChanges={handleSaveAll}
        handleCalculate={handleCalculate}
        handleRemarkCellClick={handleRemarkCellClick}
        handleExcelUpload={handleExcelUpload}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        permissions={adjustedPermissionsC}
        groupBy='budgetType'
        resetRowData={resetRowData1}
        summaryEdited={designBasisAndDesignRemarksEdited}
        resetDataChanges={resetDataChanges}
        isReleaseDisabled={isReleaseDisabled}
        handleRelease={handleRelease}
      // setEditMode={setEditMode}
      />

      <KendoDataTables
        rows={rowsP}
        setRows={setRowsP}
        title='Procurement Budget'
        modifiedCells={modifiedCellsP}
        columns={columns}
        fetchData={fetchData}
        setModifiedCells={setModifiedCellsP}
        remarkDialogOpen={remarkDialogOpenP}
        setRemarkDialogOpen={setRemarkDialogOpenP}
        currentRemark={currentRemarkP}
        setCurrentRemark={setCurrentRemarkP}
        currentRowId={currentRowIdP}
        setCurrentRowId={setCurrentRowIdP}
        enableSaveAddBtn={enableSaveAddBtnP}
        saveChanges={handleSaveAll}
        handleCalculate={handleCalculateP}
        handleRemarkCellClick={handleRemarkCellClickP}
        permissions={adjustedPermissionsP}
        groupBy='budgetType'
        resetRowData={resetRowData2}
        summaryEdited={designBasisAndDesignRemarksEdited2}
        // setEditMode={setEditMode}
        resetDataChanges={resetDataChanges}
      />
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
      </Dialog>
      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </Box>
  )
}
