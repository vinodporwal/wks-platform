import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { useGridApiRef } from '@mui/x-data-grid'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import getEnhancedColDefsByProducts from 'components/data-tables/CommonHeader/Kendo_ProductionAopHeaderByProducts'
import React, { useEffect, useMemo, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'

import { ProductionNormsApiService } from 'services/production-norms-api-service'
import { setIsBlocked } from 'store/reducers/dataGridStore'
import { validateFields } from 'utils/validationUtils'
import getEnhancedColDefs from '../data-tables/CommonHeader/Kendo_ProductionAopHeader'
import KendoDataTables from './index'
import ProductionNormsCracker from './ProductionNormsCracker'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import AopTabs from 'components/AopTabs'
import { Box } from '@mui/material'
import { DataService } from 'services/DataService'
const ProductionNormsElastomerJmd = ({ permissions }) => {
  // State for tabs
  const [tabIndex, setTabIndex] = useState(0)
  const [tabs, setTabs] = useState([])
  const [lineDetails, setLineDetails] = useState([])
  const [modifiedCells, setModifiedCells] = React.useState({})
  const [calculationObject, setCalculationObject] = useState([])
  const keycloak = useSession()
  const [editResetKey, setEditResetKey] = useState(0)

  const apiRef = useGridApiRef()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const [_plantID, set_PlantID] = useState('')

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
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id

  const PLANT_NAME_UPPERCASE = plantObject?.name
  const SITE_NAME_UPPERCASE = siteObject?.name
  const VERTICAL_NAME_UPPERCASE = verticalObject?.name

  const AOP_YEAR = year?.selectedYear

  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const SITE_NAME = siteObject?.name?.toLowerCase()
  const VERTICAL_NAME = verticalObject?.name?.toLowerCase()
  const IS_PP_DTA = false
  const IS_PP_SEZ = false
  const IS_PP_HMD = false

  // const IS_PP_DTA = lowerVertName === 'pp' && SITE_NAME === 'dta'
  // const IS_PP_SEZ = lowerVertName === 'pp' && SITE_NAME === 'sez'

  const plantName = plantObject?.name?.toLowerCase()
  const SITE_NAME_LOWERCASE = siteObject?.name?.toLowerCase()
  const IS_VCM = verticalObject?.name?.toLowerCase() == 'vcm'
  const IS_AROMATIC_SEZ =
    lowerVertName === 'aromatics' && SITE_NAME_LOWERCASE === 'sez'
  const IS_PVC_VMD = lowerVertName === 'pvc' && SITE_NAME_LOWERCASE === 'vmd'
  const IS_PVC_DMD = lowerVertName === 'pvc' && SITE_NAME_LOWERCASE === 'dmd'
  const IS_AROMATIC_DTA_PLATFORMER =
    lowerVertName === 'aromatics' &&
    SITE_NAME_LOWERCASE === 'dta' &&
    plantName === 'plat'
  const IS_AROMATIC_HMD =
    lowerVertName === 'aromatics' && SITE_NAME_LOWERCASE === 'hmd'
  const IS_CHEMICAL = lowerVertName === 'chemical'
  const IS_ELASTOMER_JMD =
    lowerVertName === 'elastomer' && SITE_NAME_LOWERCASE === 'jmd'
  const [loading, setLoading] = useState(false)
  const [calculatebtnClicked, setCalculatebtnClicked] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const headerMap = generateHeaderNames(AOP_YEAR)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [selectedUnit, setSelectedUnit] = useState('')
  const [rows, setRows] = useState([])
  const [rowsByProducts, setRowsByProducts] = useState([])
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [rowsGradeWise, setRowsGradeWise] = useState([])
  const [rowsIIR, setRowsIIR] = useState([])
  const unsavedChangesRef = React.useRef({
    unsavedRows: {},
    rowsBeforeChange: {},
  })
  const dispatch = useDispatch()

  const saveChanges = React.useCallback(async () => {
    console.log('saveChanges called')
    try {
      var editedData = Object.values(modifiedCells)
      updateProductNormData(editedData)
    } catch (error) {
      console.error('Error in saveChanges:', error)
    }
  }, [selectedUnit, calculatebtnClicked, modifiedCells])

  const updateProductNormData = async (newRow) => {
    setLoading(true)

    try {
      let plantId = PLANT_ID
      const isKiloTon = selectedUnit != ('MT' || 'MT/Month')

      const productNormData = newRow.map((row) => ({
        aopType: row.aopType || 'production',
        aopCaseId: row.aopCaseId || 'production',
        aopStatus: row.aopStatus || 'production',
        aopYear: AOP_YEAR,
        plantFKId: plantId,
        materialFKId: row.normParametersFKId,
        siteFKId: SITE_ID,
        verticalFKId: VERTICAL_ID,
        april:
          row.april === 0
            ? 0
            : isKiloTon && row.april
              ? row.april * 1000
              : row.april || null,
        may:
          row.may === 0
            ? 0
            : isKiloTon && row.may
              ? row.may * 1000
              : row.may || null,
        june:
          row.june === 0
            ? 0
            : isKiloTon && row.june
              ? row.june * 1000
              : row.june || null,
        july:
          row.july === 0
            ? 0
            : isKiloTon && row.july
              ? row.july * 1000
              : row.july || null,
        aug:
          row.aug === 0
            ? 0
            : isKiloTon && row.aug
              ? row.aug * 1000
              : row.aug || null,
        sep:
          row.sep === 0
            ? 0
            : isKiloTon && row.sep
              ? row.sep * 1000
              : row.sep || null,
        oct:
          row.oct === 0
            ? 0
            : isKiloTon && row.oct
              ? row.oct * 1000
              : row.oct || null,
        nov:
          row.nov === 0
            ? 0
            : isKiloTon && row.nov
              ? row.nov * 1000
              : row.nov || null,
        dec:
          row.dec === 0
            ? 0
            : isKiloTon && row.dec
              ? row.dec * 1000
              : row.dec || null,
        jan:
          row.jan === 0
            ? 0
            : isKiloTon && row.jan
              ? row.jan * 1000
              : row.jan || null,
        feb:
          row.feb === 0
            ? 0
            : isKiloTon && row.feb
              ? row.feb * 1000
              : row.feb || null,
        march:
          row.march === 0
            ? 0
            : isKiloTon && row.march
              ? row.march * 1000
              : row.march || null,

        // avgTPH: findAvg('1', row) || null,
        avgTPH: findSum('1', row) || null,
        aopRemarks: row.aopRemarks,
        id: row.idFromApi || null,
      }))

      const response = await ProductionNormsApiService.updateProductNormData(
        productNormData,
        keycloak,
      )

      if (response) {
        dispatch(setIsBlocked(false))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully !',
          severity: 'success',
        })

        setCalculatebtnClicked(false)
        setLoading(false)
        setModifiedCells({})

        unsavedChangesRef.current = {
          unsavedRows: {},
          rowsBeforeChange: {},
        }

        setCalculatebtnClicked(false)
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Falied!',
          severity: 'error',
        })
        setLoading(false)

        setCalculatebtnClicked(false)
      }
    } catch (error) {
      console.error('Error Saving Production AOP:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error Saving Production AOP!',
        severity: 'error',
      })
    } finally {
      setLoading(false)

      setCalculatebtnClicked(false)
    }
  }

  const handleCalculate = async () => {
    // dispatch(setIsBlocked(true))
    setCalculatebtnClicked(true)
    setLoading(true)
    try {
      const data = await ProductionNormsApiService.handleCalculate(
        PLANT_ID,
        AOP_YEAR,
        keycloak,
      )
      if (data?.code == 200) {
        fetchData()

        if (lowerVertName === 'meg') {
          fetchDataByProducts()
        }
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        setLoading(false)
        return
      }
      return res
    } catch (error) {
      console.error('Error saving refresh data:', error)
      setLoading(false)
    }
  }
  const fetchDataAnnualproduction = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      var res = await ProductionNormsApiService.getIIRAnnualData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = res?.data.map((item, index) => ({
          id: item.id || null,
          product: item.product,
          value: item.value,
          isEditable: false,
          Particulars: item.type,
        }))
        setRowsIIR(mapped)
      } else {
        setRowsIIR([])
      }
    } catch (err) {
      console.error('fetchData error', err)
      setRowsIIR([])
    } finally {
      setLoading(false)
    }
  }

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      setRows([])
      setLoading(true)
      const selectedLine = lineDetails[tabIndex]
      const lineId = selectedLine?.id
      let response = ''
      if (IS_PP_DTA || IS_PP_SEZ || IS_PVC_DMD || IS_PP_HMD) {
        response = await ProductionNormsApiService.getAOPDataLineWise(
          keycloak,
          'Production',
          PLANT_ID,
          AOP_YEAR,
          lineId,
        )
      } else {
        response = await ProductionNormsApiService.getAOPData(
          keycloak,
          'Production',
          PLANT_ID,
          AOP_YEAR,
        )
      }
      setCalculationObject(response?.data?.aopCalculation)
      if (response?.code != 200) {
        setRows([])
        setLoading(false)
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Error fetching data. Please try again.',
          severity: 'error',
        })
        return
      }

      let dataSet = response?.data?.aopDTOList
      // if (lowerVertName === 'cracker') {
      //   dataSet = rowDataForCracker
      // }

      var data = dataSet
        ?.map((product) => ({
          ...product,
          normParametersFKId: product.materialFKId,
          originalRemark: product.aopRemarks,
          isEditable:
            lowerVertName === 'elastomer' && SITE_NAME_LOWERCASE === 'jmd'
              ? true
              : false,
          april: product?.april,
          may: product?.may,
          june: product?.june,
          july: product?.july,
          aug: product?.aug,
          sep: product?.sep,
          oct: product?.oct,
          nov: product?.nov,
          dec: product?.dec,
          jan: product?.jan,
          feb: product?.feb,
          march: product?.march,
          Particulars: product.normParameterDisplayName,
          ...(product.materialFKId !== undefined
            ? { materialFKId: undefined }
            : {}),
        }))
        .map(({ materialFKId, ...rest }) => rest)

      let formattedData = []

      if (lowerVertName !== 'cracker') {
        formattedData = data.map((item, index) => {
          const isKiloTon = selectedUnit == 'KT'
          const transformedItem = {
            ...item,
            idFromApi: item.id,
            uom: selectedUnit ? selectedUnit : 'MT',
            normParametersFKId: item?.normParametersFKId?.toLowerCase(),
            id: index,
            ...(isKiloTon && {
              jan: item.jan ? item.jan / 1000 : item.jan,
              feb: item.feb ? item.feb / 1000 : item.feb,
              march: item.march ? item.march / 1000 : item.march,
              april: item.april ? item.april / 1000 : item.april,
              may: item.may ? item.may / 1000 : item.may,
              june: item.june ? item.june / 1000 : item.june,
              july: item.july ? item.july / 1000 : item.july,
              aug: item.aug ? item.aug / 1000 : item.aug,
              sep: item.sep ? item.sep / 1000 : item.sep,
              oct: item.oct ? item.oct / 1000 : item.oct,
              nov: item.nov ? item.nov / 1000 : item.nov,
              dec: item.dec ? item.dec / 1000 : item.dec,
            }),
          }
          const total = [
            transformedItem.april,
            transformedItem.may,
            transformedItem.june,
            transformedItem.july,
            transformedItem.aug,
            transformedItem.sep,
            transformedItem.oct,
            transformedItem.nov,
            transformedItem.dec,
            transformedItem.jan,
            transformedItem.feb,
            transformedItem.march,
          ].reduce((sum, val) => sum + (parseFloat(val) || 0), 0)
          return {
            ...transformedItem,
            averageTPH: total,
            _displayNameLower: String(
              transformedItem.displayName || '',
            ).toLowerCase(),
          }
        })
      }

      const fiscalYear = AOP_YEAR
      const startYear = parseInt(fiscalYear.split('-')[0], 10)
      const nextYear = startYear + 1

      const isLeap = (year) => new Date(year, 1, 29).getDate() === 29

      if (lowerVertName === 'cracker') {
        formattedData = data.map((item, index) => {
          const TPH = selectedUnit == 'TPH'
          const transformedItem = {
            ...item,
            idFromApi: item.id,
            uom: selectedUnit ? selectedUnit : 'MT/Month',
            normParametersFKId: item?.normParametersFKId?.toLowerCase(),
            id: index,
            ...(TPH && {
              jan: item.jan ? item.jan / 24 / 31 : item.jan,
              feb: item.feb
                ? item.feb / 24 / (isLeap(nextYear) ? 29 : 28)
                : item.feb,
              march: item.march ? item.march / 24 / 31 : item.march,
              april: item.april ? item.april / 24 / 30 : item.april,
              may: item.may ? item.may / 24 / 31 : item.may,
              june: item.june ? item.june / 24 / 30 : item.june,
              july: item.july ? item.july / 24 / 31 : item.july,
              aug: item.aug ? item.aug / 24 / 31 : item.aug,
              sep: item.sep ? item.sep / 24 / 30 : item.sep,
              oct: item.oct ? item.oct / 24 / 31 : item.oct,
              nov: item.nov ? item.nov / 24 / 30 : item.nov,
              dec: item.dec ? item.dec / 24 / 31 : item.dec,
            }),
          }
          const total = [
            transformedItem.april,
            transformedItem.may,
            transformedItem.june,
            transformedItem.july,
            transformedItem.aug,
            transformedItem.sep,
            transformedItem.oct,
            transformedItem.nov,
            transformedItem.dec,
            transformedItem.jan,
            transformedItem.feb,
            transformedItem.march,
          ].reduce((sum, val) => sum + (parseFloat(val) || 0), 0)
          return {
            ...transformedItem,
            averageTPH: total,
            _displayNameLower: String(
              transformedItem.displayName || '',
            ).toLowerCase(),
          }
        })
      }

      const monthFields = [
        'april',
        'may',
        'june',
        'july',
        'aug',
        'sep',
        'oct',
        'nov',
        'dec',
        'jan',
        'feb',
        'march',
      ]

      const mapTrainNumberToLabel = (val) => {
        const TOL = 0.0001
        if (val === null || val === undefined || val === '') return val

        const parsed = parseFloat(String(val).trim())
        if (Number.isNaN(parsed)) return val

        const candidates = [parsed]
        if (Math.abs(parsed) < 0.1) candidates.push(parsed * 1000)

        for (const num of candidates) {
          const rounded = Math.round(num)
          if (Math.abs(num - rounded) <= TOL) {
            if (rounded === 1) return 'Single'
            if (rounded === 2) return 'Two'
            if (rounded === 3) return 'Three'
          }
        }

        return val
      }

      if (
        lowerVertName === 'aromatics' &&
        Array.isArray(formattedData) &&
        formattedData.length
      ) {
        const trainIndex = formattedData.findIndex(
          (r) =>
            String(r._displayNameLower || r.displayName || '').toLowerCase() ===
            'train',
        )
        if (trainIndex !== -1) {
          monthFields.forEach((m) => {
            const original = formattedData[trainIndex][m]
            if (
              original !== undefined &&
              original !== null &&
              original !== ''
            ) {
              formattedData[trainIndex][m] = mapTrainNumberToLabel(original)
            }
          })
          formattedData[trainIndex].averageTPH =
            formattedData[trainIndex].averageTPH || ''
        }
      }

      const totalsRow = {
        id: formattedData.length,
        displayName: 'Total',
        isEditable: false,
        ...monthFields.reduce((acc, field) => {
          acc[field] = formattedData.reduce(
            (sum, row) => sum + (parseFloat(row[field]) || 0),
            0,
          )
          return acc
        }, {}),
      }

      totalsRow.averageTPH = monthFields.reduce(
        (sum, field) => sum + (parseFloat(totalsRow[field]) || 0),
        0,
      )

      const trainRow = formattedData.find(
        (r) =>
          String(r._displayNameLower || r.displayName || '').toLowerCase() ===
          'train',
      )

      if (lowerVertName === 'aromatics' && trainRow) {
        trainRow.averageTPH = '-'
      }

      let finalData = []

      if (formattedData.length > 0) {
        if (
          lowerVertName !== 'meg' &&
          lowerVertName !== 'cracker' &&
          lowerVertName !== 'elastomer' &&
          lowerVertName !== 'vcm' &&
          lowerVertName !== 'pta' &&
          lowerVertName !== 'chemical' &&
          !IS_AROMATIC_SEZ &&
          !IS_AROMATIC_HMD &&
          !IS_AROMATIC_DTA_PLATFORMER
        ) {
          finalData = [...formattedData, totalsRow]
        } else {
          finalData = [...formattedData]
        }
      } else {
        finalData = []
      }

      setRows(finalData)
      setLoading(false)
    } catch (error) {
      console.error('Error fetching data:', error)
    } finally {
      setLoading(false)
    }
  }

  const fetchDataByProducts = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      setRowsByProducts([])
      setLoading(true)

      const response = await ProductionNormsApiService.getAOPData(
        keycloak,
        'ByProducts',
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code != 200) {
        setRowsByProducts([])
        setLoading(false)
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Error fetching data. Please try again.',
          severity: 'error',
        })
        return
      }

      var data = response?.data?.aopDTOList
        ?.map((product) => ({
          ...product,
          normParametersFKId: product.materialFKId,
          originalRemark: product.aopRemarks,
          isEditable: false,
          Particulars: product.normParameterDisplayName,

          ...(product.materialFKId !== undefined
            ? { materialFKId: undefined }
            : {}),
        }))
        .map(({ materialFKId, ...rest }) => rest)

      const formattedData = data.map((item, index) => {
        const transformedItem = {
          ...item,
          idFromApi: item.id,
          normParametersFKId: item?.normParametersFKId?.toLowerCase(),
          id: index,
        }

        const total = [
          transformedItem.april,
          transformedItem.may,
          transformedItem.june,
          transformedItem.july,
          transformedItem.aug,
          transformedItem.sep,
          transformedItem.oct,
          transformedItem.nov,
          transformedItem.dec,
          transformedItem.jan,
          transformedItem.feb,
          transformedItem.march,
        ].reduce((sum, val) => sum + (parseFloat(val) || 0), 0)

        return {
          ...transformedItem,
          averageTPH: total,
        }
      })

      const finalData = [...formattedData]

      if (lowerVertName == 'meg') {
        setRowsByProducts(finalData)
      }

      setLoading(false)
    } catch (error) {
      console.error('Error fetching Production AOP data:', error)
    } finally {
      setLoading(false)
    }
  }

  const findSum = (value, row) => {
    const months = [
      'april',
      'may',
      'june',
      'july',
      'aug',
      'sep',
      'oct',
      'nov',
      'dec',
      'jan',
      'feb',
      'march',
    ]

    const values = months.map((month) => Number(row[month]) || 0)
    const sum = values.reduce((acc, val) => acc + val, 0)

    const total = sum.toFixed(2)
    return total === '0.00' ? null : total
  }

  const initialRender = React.useRef(true)

  useEffect(() => {
    const fetchDataWrapper = async () => {
      // Only fetch data if this is not the initial render or if dependencies have changed
      if (!initialRender.current || PLANT_ID) {
        await fetchData()
        if (lowerVertName === 'meg') {
          await fetchDataByProducts()
        }
      } else {
        initialRender.current = false
      }
    }
    if (
      (IS_PP_DTA || IS_PP_SEZ || IS_PVC_DMD || IS_PP_HMD) &&
      lineDetails?.length === 0
    ) {
      return
    }
    fetchDataWrapper()
  }, [PLANT_ID, yearChanged, keycloak, selectedUnit, tabIndex, lineDetails])

  // Fetch line details when component mounts or plantID/year changes
  const fetchLineDetails = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    try {
      const response = await DataService.getLineDetails(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code != 200) {
        setTabs([])
        return
      }
      if (response && Array.isArray(response?.data)) {
        setLineDetails(response.data)
        // Update tabs based on the response
        const lineTabs = response?.data.map((line) => line.displayName)
        setTabs(lineTabs)
      }
    } catch (err) {
      console.error('Error fetching line details:', err)
      // Fallback to default tabs if API fails
      setTabs([])
    }
  }

  useEffect(() => {
    if (IS_PP_DTA || IS_PP_SEZ || IS_PVC_DMD || IS_PP_HMD) {
      fetchLineDetails()
    } else {
      setLineDetails([])
    }
  }, [PLANT_ID, keycloak, yearChanged])

  const valueFormat_ = ValueFormatterProduction()
  const valueFormat = IS_VCM ? '{0:0.000}' : valueFormat_

  const productionColumns = getEnhancedColDefs({
    headerMap,
    valueFormat,
  })
  const columnIIR = [
    {
      field: 'id',
      title: 'ID',
      editable: false,
      hidden: true,
    },

    {
      field: 'product',
      title: 'Product',
      widthT: 200,
      editable: false,
    },
    {
      field: 'value',
      title: 'Values',
      editable: false,
      type: 'number',
      widthT: 200,
    },
  ]

  const productionColumnsByProducts = getEnhancedColDefsByProducts({
    headerMap,
    valueFormat,
  })

  const handleUnitChange = (unit) => {
    setSelectedUnit(unit)
  }
  const isCellEditable = (params) => params.row.id !== 'total'
  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchDataAnnualproduction()
    }
  }, [PLANT_ID, AOP_YEAR])

  // const downloadExcelForConfiguration = async () => {
  //     setSnackbarOpen(true)
  //     setSnackbarData({
  //       message: 'Excel download started!',
  //       severity: 'success',
  //     })

  //     try {
  //       let response
  //       if ( lowerVertName === 'pta') {
  //         response = await ProductionNormsApiService.MonthwiseProductionExport(
  //           keycloak,
  //           PLANT_ID,
  //           AOP_YEAR,
  //           'Production',
  //         )
  //       }
  //     } catch (error) {
  //       console.error('Error downloading Excel:', error)
  //       setSnackbarData({
  //         message: 'Failed to download Excel.',
  //         severity: 'error',
  //       })
  //     } finally {
  //       setSnackbarOpen(true)
  //     }
  //   }
  useEffect(() => {
    console.log('modifiedCells:', modifiedCells)
  }, [modifiedCells])
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
      showCalculate: false,
      isOldYear: isOldYear,
      showNote: true,
      downloadExcelBtn: false,
    }
  }

  const adjustedPermissions = useMemo(
    () =>
      getAdjustedPermissions(
        {
          showAction: permissions?.showAction ?? false,
          addButton: permissions?.addButton ?? false,
          deleteButton: permissions?.deleteButton ?? false,
          editButton: permissions?.editButton ?? false,
          showUnit:
            lowerVertName === 'vcm' ||
            lowerVertName === 'pta' ||
            lowerVertName === 'cracker' ||
            lowerVertName === 'chemical'
              ? true
              : permissions?.showUnit ?? true,
          saveWithRemark: permissions?.saveWithRemark ?? true,
          showCalculate: permissions?.showCalculate ?? true,
          allAction: permissions?.allAction ?? true,
          showNote: true,

          showTitleNameBusiness: true,
          titleName: permissions?.title
            ? permissions?.title
            : 'Month wise Production plan',

          showCalculateVisibility:
            calculationObject && Object.keys(calculationObject).length > 0
              ? permissions?.showCalculate ?? true
              : false,
          saveBtn: IS_ELASTOMER_JMD ? true : permissions?.saveBtn ?? false,
          units:
            lowerVertName === 'cracker' ? ['MT/Month', 'TPH'] : ['MT', 'KT'],
          customHeight: permissions?.customHeight,
          downloadExcelBtnFromUI:
            lowerVertName === 'vcm' ||
            lowerVertName === 'pta' ||
            lowerVertName === 'cracker' ||
            lowerVertName === 'chemical'
              ? true
              : !permissions?.hideExportBtn,
          // downloadExcelBtn: lowerVertName === 'pta'
          // ? true
          // : false,
          ExcelName: `${VERTICAL_NAME_UPPERCASE}_${SITE_NAME_UPPERCASE}_${PLANT_NAME_UPPERCASE}_Month wise Production plan`,
          unitForExcelToadd:
            lowerVertName === 'cracker'
              ? selectedUnit || 'MT/Month'
              : lowerVertName === 'vcm' ||
                  lowerVertName === 'pta' ||
                  lowerVertName === 'chemical'
                ? selectedUnit || 'MT'
                : null,
        },
        isOldYear,
      ),
    [permissions, calculationObject, lowerVertName, selectedUnit, isOldYear],
  )

  const adjustedPermissionsByProducts = getAdjustedPermissions(
    {
      showAction: permissions?.showAction ?? false,
      addButton: permissions?.addButton ?? false,
      deleteButton: permissions?.deleteButton ?? false,
      editButton: permissions?.editButton ?? false,
      showUnit: permissions?.showUnit ?? false,
      saveWithRemark: permissions?.saveWithRemark ?? false,
      showCalculate: permissions?.showCalculate ?? false,
      allAction: permissions?.allAction ?? true,
      showCalculateVisibility:
        calculationObject && Object.keys(calculationObject).length > 0
          ? permissions?.showCalculate ?? true
          : false,
      saveBtn: permissions?.saveBtn ?? false,
      units: lowerVertName == 'cracker' ? ['MT/Month', 'TPH'] : ['MT', 'KT'],
      downloadExcelBtnFromUI:
        lowerVertName === 'vcm' ? false : !permissions?.hideExportBtn,
      ExcelName: `${VERTICAL_NAME_UPPERCASE}_${SITE_NAME_UPPERCASE}_${PLANT_NAME_UPPERCASE}_Month wise Production plan (By Products)`,

      customHeight: permissions?.customHeight,
    },
    isOldYear,
  )
  const adjustedPermissionsIIR = getAdjustedPermissions(
    {
      allAction: true,
      saveBtn: false,
      showTitleNameBusiness: true,
      titleName: 'Grade Wise Annual production',
      ExcelName: `${lowerVertName}_Shutdown_Slowdown_Plan_${AOP_YEAR}`,
      downloadExcelBtnFromUI: true,
      //addButton: true,
      //deleteButton: true,
    },
    isOldYear,
  )
  return (
    <div>
      {/* LINE1-LINE6 Tabs - Only for PP VERTICAL | DTA SITE */}
      {(IS_PP_DTA || IS_PP_SEZ || IS_PVC_DMD || IS_PP_HMD) && (
        <Box display='flex' alignItems='center' sx={{ mb: 1, mt: 1 }}>
          <AopTabs tabIndex={tabIndex} setTabIndex={setTabIndex} tabs={tabs} />
        </Box>
      )}
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>
      <KendoDataTables
        columns={columnIIR}
        rows={rowsIIR}
        setRows={setRowsIIR}
        fetchData={fetchDataAnnualproduction}
        title='IIR Annual production'
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        //saveChanges={saveChanges}
        // deleteRowData={deleteRowData}
        permissions={adjustedPermissionsIIR}
        groupBy='Particulars'
      />
      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        columns={productionColumns}
        rows={rows}
        setRows={setRows}
        title={'Production AOP'}
        isCellEditable={isCellEditable}
        onAddRow={(newRow) => console.log('New Row Added:', newRow)}
        onDeleteRow={(id) => console.log('Row Deleted:', id)}
        onRowUpdate={(updatedRow) => console.log('Row Updated:', updatedRow)}
        paginationOptions={[100, 200, 300]}
        updateProductNormData={updateProductNormData}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        handleCalculate={handleCalculate}
        apiRef={apiRef}
        fetchData={fetchData}
        handleUnitChange={handleUnitChange}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        unsavedChangesRef={unsavedChangesRef}
        permissions={adjustedPermissions}
        selectedUOM={'UOM'}
        resetEditSignal={editResetKey}
        setEditResetKey={setEditResetKey}
        // downloadExcelForConfiguration={downloadExcelForConfiguration}
        note={
          !permissions?.hideNoteText &&
          lowerVertName !== 'cracker' &&
          lowerVertName !== 'elastomer' &&
          lowerVertName !== 'aromatics' &&
          lowerVertName !== 'vcm' &&
          lowerVertName !== 'pe' &&
          lowerVertName !== 'pp' &&
          lowerVertName !== 'pta' &&
          lowerVertName !== 'chemical' &&
          lowerVertName !== 'pet' &&
          !IS_PVC_VMD
            ? '* MT per Annum'
            : ''
        }
      />

      {lowerVertName === 'meg' && !permissions?.hideNoteText && (
        <KendoDataTables
          columns={productionColumnsByProducts}
          rows={rowsByProducts}
          setRows={setRowsByProducts}
          title={'By Products'}
          fetchData={fetchDataByProducts}
          permissions={adjustedPermissionsByProducts}
          resetEditSignal={editResetKey}
          setEditResetKey={setEditResetKey}
        />
      )}
    </div>
  )
}

export default ProductionNormsElastomerJmd
