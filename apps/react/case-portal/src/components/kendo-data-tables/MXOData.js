import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import { DataService } from 'services/DataService'
import { SiteReportDataService } from 'services/SiteReportDataService'
import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { validateFields } from 'utils/validationUtils'
import { ProductionNormsApiService } from 'services/production-norms-api-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const getAvailableHoursInMonth = (month, year) => {
  if (!month) return 0
  let parsedYear = 2026 // default fallback
  if (year) {
    const match = year.match(/\d{4}/)
    if (match) parsedYear = parseInt(match[0], 10)
  }
  
  const monthLower = month.toLowerCase()
  if (
    monthLower.startsWith('apr') ||
    monthLower.startsWith('jun') ||
    monthLower.startsWith('sep') ||
    monthLower.startsWith('nov')
  ) {
    return 30 * 24 // 720
  }
  if (monthLower.startsWith('feb')) {
    // February belongs to the next calendar year of the financial year (parsedYear + 1)
    const calendarYear = parsedYear + 1
    const isLeap =
      (calendarYear % 4 === 0 && calendarYear % 100 !== 0) ||
      calendarYear % 400 === 0
    return isLeap ? 29 * 24 : 28 * 24
  }
  return 31 * 24 // 744 (Jan, Mar, May, Jul, Aug, Oct, Dec)
}

const getStaticMXOValues = (year) => {
  const months = [
    'Apr',
    'May',
    'June',
    'July',
    'Aug',
    'Sep',
    'Oct',
    'Nov',
    'Dec',
    'Jan',
    'Feb',
    'Mar',
  ]
  return months.map((month, index) => {
    return {
      id: index + 1,
      months: month,
      mode: 'From Planning team',
      mxoGeneration: 10.5, // dummy value corresponding to MXO Gen from Table 1
      downtimeHrs: 10,     // dummy manual entry
      mxoGenerationTpm: 0, // will calculate
      onstreamHrs: 0,      // will calculate
      maxMxoReprocessingRate: 5.0, // dummy manual entry
      Particulars: 'MXO Values',
    }
  })
}

const getStaticMXOCalculation = () => {
  const months = [
    'Apr',
    'May',
    'June',
    'July',
    'Aug',
    'Sep',
    'Oct',
    'Nov',
    'Dec',
    'Jan',
    'Feb',
    'Mar',
  ]
  return months.map((month, index) => {
    return {
      id: index + 1,
      months: month,
      mxoOpeningStockInMt: month === 'Apr' ? 100.0 : 0, // Apr manual entry, others calculated
      mxoGenerationTpm: 0,   // calculated
      mxoReprocessingTpm: 0, // calculated
      mxoClosingStockInMt: 0, // calculated
    }
  })
}

const recalculateAllMXOData = (mxoValues, mxoCalculation, year) => {
  // 1. Recalculate Table 1 (MXO Values)
  const updatedValues = mxoValues.map((row) => {
    const month = row.months || row.month
    const availableHrs = getAvailableHoursInMonth(month, year)
    const downtime = parseFloat(row.downtimeHrs) || 0
    const onstream = Math.max(0, availableHrs - downtime)
    const mxoGenTph = parseFloat(row.mxoGeneration) || 0
    const mxoGenTpm = mxoGenTph * onstream

    return {
      ...row,
      onstreamHrs: onstream.toFixed(2),
      mxoGenerationTpm: parseFloat(mxoGenTpm.toFixed(2)),
    }
  })

  // 2. Recalculate Table 2 (MXO Calculation)
  let lastClosingStock = 0
  const updatedCalculation = mxoCalculation.map((row) => {
    const month = row.months || row.month
    // Find corresponding row in Table 1
    const valRow = updatedValues.find((v) => (v.months || v.month) === month) || {}

    // Opening stock: April is manual; subsequent months are previous closing stock
    let openingStock = 0
    if (month && month.toLowerCase().startsWith('apr')) {
      openingStock = parseFloat(row.mxoOpeningStockInMt) || 0
    } else {
      openingStock = lastClosingStock
    }

    // MXO Generation (TPM) is from Table 1
    const mxoGenTpm = valRow.mxoGenerationTpm || 0

    // MXO Reprocessing (TPM) = Max MXO Reprocessing rate in tph * Onstream in hrs
    const maxReprocessRate = parseFloat(valRow.maxMxoReprocessingRate) || 0
    const onstreamHrs = parseFloat(valRow.onstreamHrs) || 0
    const mxoReprocessTpm = maxReprocessRate * onstreamHrs

    // Closing stock = Opening stock + Generation - Reprocessing
    const closingStock = Math.max(0, openingStock + mxoGenTpm - mxoReprocessTpm)
    lastClosingStock = closingStock

    return {
      ...row,
      mxoOpeningStockInMt: parseFloat(openingStock.toFixed(2)),
      mxoGenerationTpm: parseFloat(mxoGenTpm.toFixed(2)),
      mxoReprocessingTpm: parseFloat(mxoReprocessTpm.toFixed(2)),
      mxoClosingStockInMt: parseFloat(closingStock.toFixed(2)),
    }
  })

  return { updatedValues, updatedCalculation }
}

export default function MXODataComponent() {
  const keycloak = useSession()
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
    screenTitle,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id

  const SCREEN_NAME = screenTitle?.title
  const AOP_YEAR = year?.selectedYear
  const thisYear = AOP_YEAR
  const [rows, setRows] = useState(() => {
    const staticValues = getStaticMXOValues('2026-27')
    const staticCalc = getStaticMXOCalculation()
    const { updatedValues } = recalculateAllMXOData(staticValues, staticCalc, '2026-27')
    return updatedValues
  })
  const [rows1, setRows1] = useState(() => {
    const staticValues = getStaticMXOValues('2026-27')
    const staticCalc = getStaticMXOCalculation()
    const { updatedCalculation } = recalculateAllMXOData(staticValues, staticCalc, '2026-27')
    return updatedCalculation
  })
  const [loading, setLoading] = useState(false)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})
  const [modifiedCells1, setModifiedCells1] = useState({})
  const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const lowerSiteName = siteObject?.name.toLowerCase()
  const lowerPlantName = plantObject?.name.toLowerCase()

  const headerMap = generateHeaderNames(AOP_YEAR)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [calculationObject, setCalculationObject] = useState([])
  const unsavedChangesRef = useRef({ unsavedRows: {}, rowsBeforeChange: {} })

  const MXOValuesColumns = [
    {
      field: 'id',
      title: 'ID',
      widthT: 50,
      editable: false,
      hidden: true,
    },
    {
      field: 'months',
      title: 'Month',
      editable: false,
      widthT: 200,
    },
    {
      field: 'mode',
      title: 'Mode',
      type: 'string',
      editable: false,
      widthT: 120,
    },
    {
      field: 'mxoGeneration',
      title: 'MXO Generation in TPH',
      editable: false,
      type: 'string',
      widthT: 120,
    },

    {
      field: 'downtimeHrs',
      title: 'Downtime in hrs',
      editable: true,
      type: 'number',
      widthT: 120,
    },
    {
      field: 'mxoGenerationTpm',
      title: 'MXO Generation (TPM)',
      editable: false,
      type: 'number',
      widthT: 120,
    },
    {
      field: 'onstreamHrs',
      title: 'Onstream in hrs',
      editable: false,
      type: 'string',
      widthT: 120,
    },
    {
      field: 'maxMxoReprocessingRate',
      title: 'Max MXO Reprocessing rate in tph',
      editable: true,
      type: 'number',
      widthT: 120,
    },
  ]
  const MXOCalculationColumns = [
    {
      field: 'id',
      title: 'ID',
      hidden: true,
      widthT: 50,
      editable: false,
    },
    {
      field: 'months',
      title: 'Month',
      editable: false,
      widthT: 200,
    },
    {
      field: 'mxoOpeningStockInMt',
      title: 'MXO opening stock in MT',
      editable: true,
      widthT: 200,
    },
    {
      field: 'mxoGenerationTpm',
      title: 'MXO generation (TPM)',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'mxoReprocessingTpm',
      title: 'MXO Reprocessing (TPM)',
      editable: false,
      width: 120,
      type: 'number',
    },
    {
      field: 'mxoClosingStockInMt',
      title: 'MXO Closing stock in MT',
      editable: false,
      width: 120,
      type: 'number',
    }
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const res = await ProductionNormsApiService.getNaphthaHMDData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200 && res?.data?.Data?.length > 0) {
        const mapped = res?.data?.Data?.map((item, index) => ({
          id: item.id || index,
        }))
        setRows(mapped)
      } else {
        const staticValues = getStaticMXOValues(AOP_YEAR)
        const staticCalc = getStaticMXOCalculation()
        const { updatedValues } = recalculateAllMXOData(staticValues, staticCalc, AOP_YEAR)
        setRows(updatedValues)
      }
    } catch (err) {
      console.error('fetchData error', err)
      const staticValues = getStaticMXOValues(AOP_YEAR)
      const staticCalc = getStaticMXOCalculation()
      const { updatedValues } = recalculateAllMXOData(staticValues, staticCalc, AOP_YEAR)
      setRows(updatedValues)
    } finally {
      setLoading(false)
    }
  }, [keycloak, yearChanged, plantID, AOP_YEAR])

  const fetchCalculationData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const res = await ProductionNormsApiService.getLimsData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      setCalculationObject(res?.data?.aopCalculation)
      if (res?.code === 200 && res?.data?.Data?.length > 0) {
        const mapped = res?.data?.Data?.map((item, index) => ({
          id: item.id || index,
        }))
        setRows1(mapped)
      } else {
        const staticValues = getStaticMXOValues(AOP_YEAR)
        const staticCalc = getStaticMXOCalculation()
        const { updatedCalculation } = recalculateAllMXOData(staticValues, staticCalc, AOP_YEAR)
        setRows1(updatedCalculation)
      }
    } catch (err) {
      console.error('fetchCalculationData error', err)
      const staticValues = getStaticMXOValues(AOP_YEAR)
      const staticCalc = getStaticMXOCalculation()
      const { updatedCalculation } = recalculateAllMXOData(staticValues, staticCalc, AOP_YEAR)
      setRows1(updatedCalculation)
    } finally {
      setLoading(false)
    }
  }, [keycloak, yearChanged, plantID, AOP_YEAR])

  useEffect(() => {
    // fetchData()
    // fetchCalculationData
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

  useEffect(() => {
    if (AOP_YEAR) {
      const staticValues = getStaticMXOValues(AOP_YEAR)
      const staticCalc = getStaticMXOCalculation()
      const { updatedValues, updatedCalculation } = recalculateAllMXOData(staticValues, staticCalc, AOP_YEAR)
      setRows(updatedValues)
      setRows1(updatedCalculation)
    }
  }, [AOP_YEAR])

  const saveChanges = React.useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        return
      }

      const requiredFields = ['remarks']

      //   const validationMessage = validateFields(data, requiredFields)
      //   if (validationMessage) {
      //     setSnackbarOpen(true)
      //     setSnackbarData({
      //       message: validationMessage,
      //       severity: 'error',
      //     })
      //     setLoading(false)
      //     return
      //   }

      const payload = data.map((item) => ({
        //id: item.id || null,
        section: item.section,
        name: item.name,
        max: item.max,
        min: item.min,
        months: item.months,
        maxId: item.maxId,
        minId: item.minId,
        monthsId: item.monthsId,
      }))

      // 3. Save to API
      const response = await ProductionNormsApiService.saveNaphthaHMDData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payload,
      )

      // 4. Handle API response
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
        fetchLimsData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Save failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchData, fetchLimsData])
  const saveChangesLimsData = React.useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells1)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        return
      }

      const payload = data.map((item) => ({
        id: item.id || null,
        name: item.name,
        displayName: item.displayName,
        uom: item.uom,
        jmd: item.jmd,
        hpn: item.hpn,
        heavy: item.heavy,
        others: item.others,
        blend: item.blend,
        blendIp21: item.blendIp21,
        jmdId: item.jmdId,
        hpnId: item.hpnId,
        heavyId: item.heavyId,
        othersId: item.othersId,
        blendId: item.blendId,
        blendIp21Id: item.blendIp21Id,
      }))

      // 3. Save to API
      const response = await ProductionNormsApiService.saveLimsData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        payload,
      )

      // 4. Handle API response
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells1({})
        fetchLimsData()
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Save failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells1, keycloak, PLANT_ID, AOP_YEAR, fetchLimsData, fetchData])

  const handleMXOValuesChange = useCallback((updater) => {
    setRows((prevRows) => {
      const nextRows = typeof updater === 'function' ? updater(prevRows) : updater
      const { updatedValues, updatedCalculation } = recalculateAllMXOData(nextRows, rows1, AOP_YEAR)
      setRows1(updatedCalculation)
      return updatedValues
    })
  }, [rows1, AOP_YEAR])

  const handleMXOCalculationChange = useCallback((updater) => {
    setRows1((prevRows1) => {
      const nextRows1 = typeof updater === 'function' ? updater(prevRows1) : updater
      const { updatedValues, updatedCalculation } = recalculateAllMXOData(rows, nextRows1, AOP_YEAR)
      setRows(updatedValues)
      return updatedCalculation
    })
  }, [rows, AOP_YEAR])

  const handleCalculate = async () => {
    try {
      setLoading(true)
      const res = await ProductionNormsApiService.calculateLIMSData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchLimsData()
        fetchData()
        return
      }
    } catch (error) {
      console.warn('API calculate failed, performing local calculation fallback', error)
    } finally {
      setLoading(false)
    }

    // Local calculation fallback
    const { updatedValues, updatedCalculation } = recalculateAllMXOData(rows, rows1, AOP_YEAR)
    setRows(updatedValues)
    setRows1(updatedCalculation)
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Local calculation completed!',
      severity: 'success',
    })
  }

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
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      allAction: true,
      saveBtn: true,
      showTitleNameBusiness: true,
      titleName: 'MXO Values',
      adjustedPermissions: true,
      uploadExcelBtn: true,
      downloadExcelBtn: true,
      ExcelName: `${lowerVertName}_${lowerSiteName}_${lowerPlantName}_MXO Values`,
      showCalculate: true,
      showCalculateVisibility: true,
      //addButton: true,
      //deleteButton: true,
    },
    isOldYear,
  )
  const adjustedPermissions1 = getAdjustedPermissions(
    {
      allAction: true,
      saveBtn: true,
      showTitleNameBusiness: true,
      titleName: 'MXO Calculation',
      adjustedPermissions: true,
      downloadExcelBtnFromUI: true,
      ExcelName: `${lowerVertName}_${lowerSiteName}_${lowerPlantName}_MXO Calculation`,
      showCalculate: true,
      showCalculateVisibility: true,
    },
    isOldYear,
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      
      <KendoDataTables
        columns={MXOValuesColumns}
        rows={rows}
        setRows={handleMXOValuesChange}
        title='MXO Values'
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        enableSaveAddBtn={enableSaveAddBtn}
        saveChanges={saveChanges}
        //handleRemarkCellClick={handleRemarkCellClick}
        //deleteRowData={deleteRowData}
        permissions={adjustedPermissions}
      />

      <KendoDataTables
        columns={MXOCalculationColumns}
        rows={rows1}
        setRows={handleMXOCalculationChange}
        title='MXO Calculation'
        modifiedCells={modifiedCells1}
        setModifiedCells={setModifiedCells1}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={currentRowId}
        enableSaveAddBtn={enableSaveAddBtn}
        saveChanges={saveChangesLimsData}
        //handleRemarkCellClick={handleRemarkCellClick}
        permissions={adjustedPermissions1}
        handleCalculate={handleCalculate}
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
