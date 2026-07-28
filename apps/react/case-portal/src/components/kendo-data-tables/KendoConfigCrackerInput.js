import { Backdrop, Box, CircularProgress, Tab, Tabs } from '@mui/material'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import getEnhancedAOPColDefs from 'components/data-tables/CommonHeader/kendo_ConfigHeader'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from './index'
import { OptimizerDataApiService } from 'services/optimizer-api-service'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { getRoleName } from 'services/role-service'
import AopTabs from 'components/AopTabs'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import StartAndEndPicker from './Utilities-Kendo/StartAndEndPicker'
import NaphthaLimsDataSet from './NaphthaLimsDataSet'
import NaphthaHMDComponent from './NaphthaHMDComponent'
import ModeSelection from './ModeSelection'
import SpyroInputMinMax from './SpyroInputMinMax'

const CrackerConfig = () => {
  const keycloak = useSession()
  // const READ_ONLY = getRoleName(keycloak)
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    oldYear,
    plantID,
    yearChanged,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id

  const VERTICAL_ID = verticalObject?.id
  const PLANT_NAME = plantObject?.name?.toUpperCase()
  const SITE_NAME = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME = verticalObject?.name?.toUpperCase()
  const AOP_YEAR = year?.selectedYear
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const [carbonFilterDataNaphtha, setCarbonFilterDataNaphtha] = useState([])
  const [naphthaLoadedFlag, setNaphthaLoadedFlag] = useState(false)
  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const headerMap = useMemo(
    () => generateHeaderNames(AOP_YEAR),
    [AOP_YEAR, PLANT_ID],
  )

  const rawTabsStatic = [
    'Feed',
    'Optimizing',
    'Composition',
    'Hydrogenation',
    'Recovery',
    'Furnace',
    'Constant',
    'Naphtha',
    'External Streams',
    'OptimizerPrices',
  ]
  const [tabs, setTabs] = useState(rawTabsStatic)
  const [availableTabs, setAvailableTabs] = useState([])
  const [modes, setModes] = useState([])
  const [tabIndex, setTabIndex] = useState(0)

  const [feedRows, setFeedRows] = useState([])
  const [compositionRows, setCompositionRows] = useState([])
  const [hydrogenationRows, setHydrogenationRows] = useState([])
  const [recoveryRows, setRecoveryRows] = useState([])
  const [optimizing, setOptimizing] = useState([])
  const [furnace, setFurnance] = useState([])

  const IS_CRACKER_HMD = lowerVertName === 'cracker' && SITE_NAME === 'HMD'
  const IS_CRACKER_C2 = lowerVertName === 'cracker' && SITE_NAME === 'C2'

  // const allModes = ['5F', '4F', '4F+D']

  const [selectMode, setSelectMode] = useState('')
  const [constantsRows, setConstantsRows] = useState([])
  const [naphthaRows, setNaphthaRows] = useState([])
  const [naphthaDateRange, setNaphthaDateRange] = useState({
    startDate: null,
    endDate: null,
  })
  const [exsternalSteamRows, setExsternalSteamRows] = useState([])
  const [optimizerPricesRows, setOptimizerPricesRows] = useState([])

  const currentTabDisplay = useMemo(() => {
    const idLower = tabs[tabIndex]?.toLowerCase() || ''
    const info = availableTabs.find((t) => t.id.toLowerCase() === idLower)
    return info ? info.name : tabs[tabIndex] || 'Feed'
  }, [tabs, tabIndex, availableTabs])

  useEffect(() => {
    if (Array.isArray(modes) && modes.length > 0) {
      setSelectMode((prev) => {
        const stillExists = modes.some((m) => m.name === prev)
        return stillExists ? prev : modes[0].name
      })
    } else {
      setSelectMode('')
    }
  }, [modes])

  const FORMATE_VALUE = ValueFormatterProduction()

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      showModes: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      allAction: false,
      uploadExcelBtn: false,
      downloadExcelBtn: false,
    }
  }
  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      showModes:
        !IS_CRACKER_HMD &&
        !IS_CRACKER_C2 &&
        lowerVertName === 'cracker' &&
        currentTabDisplay !== 'Naphtha' &&
        currentTabDisplay !== 'External Streams',
      saveWithRemark: true,
      saveBtn: true,
      allAction: lowerVertName === 'cracker',
      showCalculate: IS_CRACKER_HMD || IS_CRACKER_C2,
      showCalculateVisibility: true,
      modes: modes,
      uploadExcelBtn:
        currentTabDisplay == 'Constant' ||
        currentTabDisplay == 'External Streams'
          ? false
          : true,
      downloadExcelBtn:
        currentTabDisplay == 'Constant' ||
        currentTabDisplay == 'External Streams'
          ? false
          : true,
      hideRemarkForNonEditableRows: true,
      makePagable: currentTabDisplay !== 'Composition',
    },
    isOldYear,
  )

  const adjustedPermissionsReadyOnly = getAdjustedPermissions(
    {
      hideRemarkForNonEditableRows: true,
      NON_EDITABLE_GRID: true,
    },
    isOldYear,
  )
  const adjustedPermissionsType = getAdjustedPermissions(
    {
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      saveBtn: false,
      allAction: lowerVertName === 'cracker',
      showCalculate: false,
      showCalculateVisibility: true,
    },
    isOldYear,
  )

  const productionColumns = useMemo(() => {
    const configType =
      currentTabDisplay === 'Composition'
        ? lowerSiteName === 'c2'
          ? 'cracker_composition_c2'
          : 'cracker_composition'
        : currentTabDisplay === 'Constant'
          ? 'cracker_constants'
          : currentTabDisplay === 'Yield'
            ? 'cracker_yield'
            : currentTabDisplay === 'Naphtha'
              ? 'Naphtha'
              : currentTabDisplay === 'External Streams'
                ? 'External_Streams'
                : (currentTabDisplay === 'Recovery' ||
                    currentTabDisplay === 'Hydrogenation') &&
                  lowerSiteName === 'c2'
                  ? 'cracker_c2_recovery'
                  : lowerSiteName === 'c2'
                    ? 'cracker_c2'
                    : 'cracker'

    return getEnhancedAOPColDefs({
      headerMap,
      handleRemarkCellClick,
      configType,
      FORMATE_VALUE,
    })
  }, [headerMap, currentTabDisplay, lowerSiteName])

  const fetchTabsMatrix = useCallback(async () => {
    try {
      const resp = await DataService.getConfigurationTabsMatrix(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        SITE_ID,
        VERTICAL_ID,
      )
      let tabsFromApi = []
      if (typeof resp.data === 'string') {
        try {
          tabsFromApi = JSON.parse(resp.data)
        } catch (e) {
          console.error('Failed parsing tabs JSON', e)
        }
      } else if (Array.isArray(resp.data)) {
        tabsFromApi = resp.data
      }
      if (Array.isArray(tabsFromApi) && tabsFromApi.length) {
        setTabs(tabsFromApi)
      } else {
        setTabs(rawTabsStatic)
      }
    } catch (err) {
      console.error('Error fetching cracker tabs matrix:', err)
      setTabs(rawTabsStatic)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, SITE_ID, VERTICAL_ID])

  const fetchAvailableTabs = useCallback(async () => {
    try {
      const resp = await DataService.getConfigurationAvailableTabs(keycloak)
      if (
        resp?.code === 200 &&
        Array.isArray(resp.data?.configurationTypeList)
      ) {
        setAvailableTabs(resp.data.configurationTypeList)
      } else {
        setAvailableTabs(
          rawTabsStatic.map((t) => ({
            id: t,
            displayName: t.charAt(0).toUpperCase() + t.slice(1),
          })),
        )
      }
    } catch (err) {
      console.error('Error fetching available tabs:', err)
      setAvailableTabs(
        rawTabsStatic.map((t) => ({
          id: t,
          displayName: t.charAt(0).toUpperCase() + t.slice(1),
        })),
      )
    }
  }, [keycloak])

  const fetchModes = useCallback(async () => {
    try {
      const resp = await OptimizerDataApiService.fetchModes(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        '1',
      )

      if (resp?.code === 200 && Array.isArray(resp.data)) {
        setModes(resp.data)
        setSelectMode(resp.data[0]?.name ?? '')
      } else {
        setModes([])
        setSelectMode('')
      }
    } catch (err) {
      console.error('Error fetching data:', err)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    fetchModes()
    fetchTabsMatrix()
    fetchAvailableTabs()
    setTabIndex(0)
  }, [
    keycloak,
    fetchTabsMatrix,
    fetchAvailableTabs,
    fetchModes,
    AOP_YEAR,
    PLANT_ID,
  ])

  const getRows = useCallback(
    (tabId) => {
      switch (tabId) {
        case 'Feed':
          return feedRows
        case 'Composition':
          return compositionRows
        case 'Hydrogenation':
          return hydrogenationRows
        case 'Recovery':
          return recoveryRows
        case 'Optimizing':
          return optimizing
        case 'Furnace':
          return furnace
        case 'Constant':
          return constantsRows
        case 'Naphtha':
          return naphthaRows
        case 'External Streams':
          return exsternalSteamRows
        case 'OptimizerPrices':
          return optimizerPricesRows
        default:
          return []
      }
    },
    [
      feedRows,
      compositionRows,
      hydrogenationRows,
      recoveryRows,
      furnace,
      optimizing,
      constantsRows,
      naphthaRows,
      exsternalSteamRows,
      optimizerPricesRows,
    ],
  )

  const setRowsForTab = useCallback((tabId, data) => {
    switch (tabId) {
      case 'Feed':
        setFeedRows(data)
        break
      case 'Composition':
        setCompositionRows(data)
        break
      case 'Hydrogenation':
        setHydrogenationRows(data)
        break
      case 'Recovery':
        setRecoveryRows(data)
        break
      case 'Furnace':
        setFurnance(data)
        break
      case 'Optimizing':
        setOptimizing(data)

        break

      case 'Constant':
        setConstantsRows(data)
        break

      case 'Naphtha':
        setNaphthaRows(data)
        break
      case 'External Streams':
        setExsternalSteamRows(data)
        break
      case 'OptimizerPrices':
        setOptimizerPricesRows(data)
        break

      default:
        console.warn('No state for tab:', tabId)
    }
  }, [])

  const fetchCrackerRows = useCallback(
    async (currentTabDisplay, mode, startDate = null, endDate = null) => {
      if (!currentTabDisplay) return
      try {
        setLoading(true)
        let transformedData = []
        let transformedData1 = []
        let transformedData12 = []
        var spyroVM1 = []
        if (IS_CRACKER_HMD || IS_CRACKER_C2) {
          mode = currentTabDisplay
        }
        if (currentTabDisplay == 'Constant') {
          spyroVM1 = await DataService.getSpyroInputData(
            keycloak,
            mode,
            currentTabDisplay,
            PLANT_ID,
            AOP_YEAR,
          )

          if (spyroVM1?.data && Array.isArray(spyroVM1.data)) {
            transformedData1 = spyroVM1.data.map((item, index) => ({
              id: item.NormParameterFKID || `row_${index}`,
              particulars: item.Particulars,
              uom: item.UOM,
              remarks: item.remarks ?? item.Remarks ?? '',
              originalRemark: item.remarks ?? item.Remarks ?? '',
              ParticularsType: item.NormParameterTypeName,

              april:
                item.Apr && item.Apr.trim() !== '' ? Number(item.Apr) : null,
              NormParameterFKID: item.NormParameterFKID,
              ...item,
            }))
          }
          setRowsForTab(currentTabDisplay, transformedData1)
          return
        }
        if (currentTabDisplay == 'Naphtha') {
          const formatDate = (date) => {
            if (!date) return ''
            const year = date.getFullYear()
            const month = String(date.getMonth() + 1).padStart(2, '0')
            const day = String(date.getDate()).padStart(2, '0')
            return `${year}-${month}-${day}`
          }

          const getDefaultStartDate = () => {
            const date = new Date()
            date.setFullYear(date.getFullYear() - 2)
            return formatDate(date)
          }

          const getDefaultEndDate = () => {
            return formatDate(new Date())
          }

          spyroVM1 = await DataService.getNaphthaData(
            keycloak,
            currentTabDisplay,
            PLANT_ID,
            AOP_YEAR,
          )
          const naphthaRows = (spyroVM1.data.Data || []).map((item, idx) => ({
            id: idx + 1,
            type: item.type,
            limsTagName: item.limsTagName,
            uom: item.uom,
            jmdNaphtha: item.jmdNaphtha,
            pmdNaphtha: item.pmdNaphtha,
            ioclNaphtha: item.ioclNaphtha,
            gailNaphtha: item.gailNaphtha,
            bpclNaphtha: item.bpclNaphtha,
            ongcNaphtha: item.ongcNaphtha,
            otherNaphtha: item.otherNaphtha,
            naphthaBlendCompositionForOptimizerInput:
              item.naphthaBlendCompositionForOptimizerInput,
            normParameterFKID: item.normParameterFKID || '',
            // Include all NaphthaId fields!
            jmdNaphthaId: item.jmdNaphthaId,
            pmdNaphthaId: item.pmdNaphthaId,
            ioclNaphthaId: item.ioclNaphthaId,
            gailNaphthaId: item.gailNaphthaId,
            bpclNaphthaId: item.bpclNaphthaId,
            ongcNaphthaId: item.ongcNaphthaId,
            otherNaphthaId: item.otherNaphthaId,
            bcoiNaphthaId: item.bcoiNaphthaId,
          }))

          setRowsForTab(currentTabDisplay, naphthaRows)
          return
        }
        if (currentTabDisplay == 'External Streams') {
          spyroVM1 = await DataService.getExsternalSteamData(
            keycloak,
            currentTabDisplay,
            VERTICAL_ID,
            SITE_ID,
            PLANT_ID,
            AOP_YEAR,
          )

          const dataList =
            spyroVM1?.data?.externalStreamDataList || spyroVM1?.data || []

          if (Array.isArray(dataList)) {
            transformedData12 = dataList.map((item, index) => ({
              id: item.normParameterId || `row_${index}`,
              particulars: item.particulars,
              uom: item.uom,
              remarks: item.remarks ?? '',
              originalRemark: item.remarks ?? '',
              ParticularsType: item.normParameterTypeDisplayName,
              april: item.apr ?? null,
              may: item.may ?? null,
              june: item.jun ?? null,
              july: item.jul ?? null,
              august: item.aug ?? null,
              september: item.sep ?? null,
              october: item.oct ?? null,
              november: item.nov ?? null,
              december: item.dec ?? null,
              NormParameterFKID: item.normParameterId,
              ...item,
            }))
          }
          setRowsForTab(currentTabDisplay, transformedData12)
          return
        }

        const spyroVM = await DataService.getSpyroInputData(
          keycloak,
          mode,
          currentTabDisplay,
          PLANT_ID,
          AOP_YEAR,
        )
        setTimeout(() => {
          if (spyroVM?.data && Array.isArray(spyroVM.data)) {
            transformedData = spyroVM.data.map((item, index) => ({
              id: item.NormParameterFKID || `row_${index}`,
              particulars: item.Particulars,
              uom: item.UOM,
              remarks: item.remarks ?? item.Remarks ?? '',
              originalRemark: item.remarks ?? item.Remarks ?? '',
              ParticularsType: item.normParameterTypeName,
              NormParameterFKID: item.NormParameterFKID,
              ...item,
            }))
          }
          setRowsForTab(currentTabDisplay, transformedData)
        }, 500)
      } catch (err) {
        // console.warn(`Failed to load ${tabId} data:`, err)
        // setSnackbarData({
        //   message: `Failed to load ${currentTabDisplay} data. Please try again.`,
        //   severity: 'error',
        // })
        // setSnackbarOpen(true)
        setRowsForTab(currentTabDisplay, [])
      } finally {
        setLoading(false)
      }
    },
    [keycloak, setRowsForTab, currentTabDisplay, AOP_YEAR, PLANT_ID],
  )

  useEffect(() => {
    if (keycloak && PLANT_ID && AOP_YEAR && currentTabDisplay) {
      if (currentTabDisplay !== 'Naphtha' && !selectMode) {
        console.log('Skipping fetchCrackerRows until selectMode is set')
        return
      }

      fetchCrackerRows(currentTabDisplay, selectMode)
    } else {
      console.warn('Missing data for fetchCrackerRows:', {
        hasKeycloak: !!keycloak,
        hasPlantId: !!PLANT_ID,
        currentTabDisplay,
      })
    }
  }, [
    tabIndex,
    selectMode,
    PLANT_ID,
    tabs,
    fetchCrackerRows,
    keycloak,
    currentTabDisplay,
    AOP_YEAR,
  ])

  const [modifiedCells, setModifiedCells] = useState({})
  const saveChanges = useCallback(async () => {
    try {
      let data = []
      if (currentTabDisplay === 'Naphtha') {
        const editedRows = Object.values(modifiedCells).filter(
          (row) => row.inEdit,
        )
        // Get first two records
        const firstTwoRows = naphthaRows.slice(0, 2)
        // Merge and deduplicate by id
        const merged = [...firstTwoRows, ...editedRows].reduce((acc, row) => {
          acc[row.id] = row
          return acc
        }, {})
        data = Object.values(merged)
      } else {
        if (Object.keys(modifiedCells).length === 0) {
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'No Records to Save!',
            severity: 'info',
          })
          setLoading(false)
          return
        }
        const rawData = Object.values(modifiedCells)
        data = rawData.filter((row) => row.inEdit)
        if (data.length === 0) {
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'No Records to Save!',
            severity: 'info',
          })
          setLoading(false)
          return
        }
      }
      console.log('data', data)
      const naphthaFields = []
      const validationMessage = validateFields(
        data,
        currentTabDisplay == 'Naphtha'
          ? naphthaFields
          : ['particulars', 'remarks'],
      )
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({ message: validationMessage, severity: 'error' })
        setLoading(false)
        return
      }
      await saveSpyroData(data)
    } catch (error) {
      console.error('Error saving changes:', error)
    }
  }, [modifiedCells])

  const saveSpyroData = async (newRows) => {
    setLoading(true)
    try {
      let SpyroInputData
      if (currentTabDisplay == 'Naphtha') {
        SpyroInputData = newRows.map(({ id, inEdit, ...rest }) => rest)
      } else if (currentTabDisplay == 'External Streams') {
        SpyroInputData = newRows.map((row) => ({
          normParameterId: row.normParameterId ?? row.NormParameterFKID ?? null,
          particulars: row.particulars ?? null,
          uom: row.uom ?? null,
          remarks: row.remarks ?? null,
          jan: row.jan ?? null,
          feb: row.feb ?? null,
          mar: row.mar ?? null,
          apr: row.apr ?? null,
          may: row.may ?? null,
          jun: row.jun ?? null,
          jul: row.jul ?? null,
          aug: row.aug ?? null,
          sep: row.sep ?? null,
          oct: row.oct ?? null,
          nov: row.nov ?? null,
          dec: row.dec ?? null,
          verticalId: row.verticalId ?? VERTICAL_ID,
          plantId: row.plantId ?? PLANT_ID,
          normParameterTypeFkId: row.normParameterTypeFkId ?? null,
        }))
      } else {
        SpyroInputData = newRows.map((row) => ({
          normParameterFKID: row.normParameterFKID ?? null,
          Remarks: row.remarks ?? row.Remarks ?? null,
          remarks: row.remarks ?? row.Remarks ?? null,
          jan: row.jan || null,
          feb: row.feb || null,
          mar: row.mar || null,
          apr: row.apr || null,
          may: row.may || null,
          jun: row.jun || null,
          jul: row.jul || null,
          aug: row.aug || null,
          sep: row.sep || null,
          oct: row.oct || null,
          nov: row.nov || null,
          dec: row.dec || null,
          id: null,
        }))
      }
      let response
      if (currentTabDisplay == 'Naphtha') {
        response = await DataService.saveNaphthaData(
          SpyroInputData,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      } else if (currentTabDisplay == 'External Streams') {
        response = await DataService.saveExternalStreamData(
          SpyroInputData,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          SITE_ID,
          VERTICAL_ID,
        )
      } else {
        response = await DataService.saveSpyroInput(
          SpyroInputData,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      }
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        setNaphthaLoadedFlag(false)
        const tabId = tabs[tabIndex]
        if (tabId) fetchCrackerRows(currentTabDisplay, selectMode)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Error saving Optimizer data!',
          severity: 'error',
        })
      }
      return response
    } catch (error) {
      console.error('Error saving Optimizer Input data!', error)
    } finally {
      setLoading(false)
    }
  }

  const handleCalculate = useCallback(async () => {
    setLoading(true)
    try {
      let mode = selectMode || currentTabDisplay || 'Feed'
      const type = currentTabDisplay || 'Feed'
      if (IS_CRACKER_HMD || IS_CRACKER_C2) {
        mode = currentTabDisplay
      }
      const response = await DataService.calculateSpyroInputData(
        keycloak,
        mode,
        type,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data calculated successfully!',
          severity: 'success',
        })

        fetchCrackerRows(currentTabDisplay, selectMode)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Error calculating data!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error calculating spyro input data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to calculate data!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [
    keycloak,
    selectMode,
    currentTabDisplay,
    PLANT_ID,
    AOP_YEAR,
    IS_CRACKER_HMD,
    IS_CRACKER_C2,
  ])

  const saveSpyroInputExcelFile = async (rawFile) => {
    setLoading(true)
    try {
      let mode = selectMode || ''
      let response
      if (IS_CRACKER_HMD || IS_CRACKER_C2) {
        mode = currentTabDisplay
      }
      if (currentTabDisplay == 'Naphtha') {
        response = await DataService.importNaphthaExcel(
          rawFile,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      } else {
        response = await DataService.importSpyroInputExcel(
          rawFile,
          keycloak,
          mode,
          PLANT_ID,
          AOP_YEAR,
        )
      }

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })

        fetchCrackerRows(currentTabDisplay, selectMode)
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
        link.setAttribute('download', 'Error File - Optimizer Input.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
      } else {
        // setSnackbarOpen(true)
      }

      return response
    } catch (error) {
      console.error('Error uploading Optimizer Input Excel:', error)
    } finally {
      setLoading(false)
      fetchCrackerRows(currentTabDisplay, selectMode)
    }
  }
  const handleExcelUpload = (rawFile) => {
    saveSpyroInputExcelFile(rawFile)
  }

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    let mode = selectMode
    if (IS_CRACKER_HMD || IS_CRACKER_C2) {
      mode = currentTabDisplay
    }
    const EXCEL_NAME =
      currentTabDisplay == 'Naphtha'
        ? `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}_Optimizer_Input_${AOP_YEAR}`
        : `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}_${mode}_Optimizer_Input_${AOP_YEAR}`

    const formatDate = (date) => {
      if (!date) return ''
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }

    const getDefaultStartDate = () => {
      const date = new Date()
      date.setFullYear(date.getFullYear() - 2)
      return formatDate(date)
    }

    const getDefaultEndDate = () => {
      return formatDate(new Date())
    }

    try {
      let response
      if (currentTabDisplay == 'Naphtha') {
        response = await DataService.exportNaphthaExcel(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          EXCEL_NAME,
        )
      } else {
        response = await DataService.exportSpyroInputExcel(
          keycloak,
          mode,
          PLANT_ID,
          AOP_YEAR,
          EXCEL_NAME,
        )
      }

      if (response?.code === 200) {
        setSnackbarOpen(true)

        setSnackbarData({
          message: 'Excel download completed successfully!',
          severity: 'success',
        })
      } else {
        setSnackbarOpen(true)

        setSnackbarData({
          message: 'Failed to download Excel.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarOpen(true)

      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    } finally {
      setSnackbarOpen(false)
    }
  }
  const handleLoadNaphthaData = async (startDate, endDate) => {
    try {
      setLoading(true)
      const resp = await DataService.loadNaphthaData(
        keycloak,
        currentTabDisplay,
        PLANT_ID,
        AOP_YEAR,
        startDate,
        endDate,
      )
      // Parse and set rows for Naphtha
      const naphthaRows = (resp.data.Data || []).map((item, idx) => ({
        id: idx + 1,
        type: item.type,
        limsTagName: item.limsTagName,
        uom: item.uom,
        jmdNaphtha: item.jmdNaphtha,
        pmdNaphtha: item.pmdNaphtha,
        ioclNaphtha: item.ioclNaphtha,
        gailNaphtha: item.gailNaphtha,
        bpclNaphtha: item.bpclNaphtha,
        ongcNaphtha: item.ongcNaphtha,
        otherNaphtha: item.otherNaphtha,
        naphthaBlendCompositionForOptimizerInput:
          item.naphthaBlendCompositionForOptimizerInput,
        normParameterFKID: item.normParameterFKID || '',
        jmdNaphthaId: item.jmdNaphthaId,
        pmdNaphthaId: item.pmdNaphthaId,
        ioclNaphthaId: item.ioclNaphthaId,
        gailNaphthaId: item.gailNaphthaId,
        bpclNaphthaId: item.bpclNaphthaId,
        ongcNaphthaId: item.ongcNaphthaId,
        otherNaphthaId: item.otherNaphthaId,
        bcoiNaphthaId: item.bcoiNaphthaId,
      }))

      setNaphthaRows(naphthaRows)
      setModifiedCells({})
      setNaphthaLoadedFlag(true)
      setSnackbarData({ message: 'Naphtha data loaded!', severity: 'success' })
      setSnackbarOpen(true)
    } catch (error) {
      // setSnackbarData({
      //   message: 'Failed to load Naphtha data.',
      //   severity: 'error',
      // })
      // setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    if (currentTabDisplay === 'Naphtha') {
      // Reset FIRST ? this clears the picker immediately
      setNaphthaDateRange({ startDate: null, endDate: null })
      setNaphthaLoadedFlag(false)

      if (PLANT_ID && AOP_YEAR) {
        DataService.getNaphthatabDate(
          keycloak,
          currentTabDisplay,
          PLANT_ID,
          AOP_YEAR,
        )
          .then((resp) => {
            const startDate = resp?.data?.startDate
            const endDate = resp?.data?.endDate
            if (startDate && endDate) {
              setNaphthaDateRange({ startDate, endDate }) // Only set if API returns dates
            }
            // If null/undefined ? stays null ? picker shows empty
          })
          .catch(() => {
            setNaphthaDateRange({ startDate: null, endDate: null })
          })
      }
    }
  }, [currentTabDisplay, PLANT_ID, AOP_YEAR, keycloak])

  const resolvedTabs = tabs.map((tabId) => {
    const info = availableTabs.find(
      (t) => t.id.toLowerCase() === tabId.toLowerCase(),
    )
    return info?.displayName || tabId
  })
  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <Box sx={{ overflowX: 'auto', width: '100%' }}>
        <AopTabs
          tabIndex={tabIndex}
          setTabIndex={(newIndex) => {
            if (newIndex >= 0 && newIndex < resolvedTabs.length) {
              setTabIndex(newIndex)
            }
          }}
          tabs={resolvedTabs}
        />
      </Box>
      {IS_CRACKER_HMD && (
        <ModeSelection permissions={adjustedPermissionsReadyOnly} />
      )}
      <Box>
        {(() => {
          const rows = getRows(currentTabDisplay)
          const setRowsForCurrent = useCallback(
            (newRows) => setRowsForTab(currentTabDisplay, newRows),
            [currentTabDisplay],
          )
          switch (currentTabDisplay) {
            case 'Feed':
            case 'Hydrogenation':
            case 'Recovery':
            case 'Optimizing':
            case 'OptimizerPrices':
            case 'Constant':
              return (
                <Box key={currentTabDisplay}>
                  <KendoDataTables
                    rows={rows}
                    setRows={setRowsForCurrent}
                    fetchData={() =>
                      fetchCrackerRows(currentTabDisplay, selectMode)
                    }
                    configType='cracker'
                    handleRemarkCellClick={handleRemarkCellClick}
                    columns={productionColumns}
                    remarkDialogOpen={remarkDialogOpen}
                    setRemarkDialogOpen={setRemarkDialogOpen}
                    currentRemark={currentRemark}
                    setCurrentRemark={setCurrentRemark}
                    currentRowId={currentRowId}
                    permissions={{
                      ...adjustedPermissions,
                      makePagable: false,
                    }}
                    handleCalculate={handleCalculate}
                    selectMode={selectMode}
                    setSelectMode={setSelectMode}
                    saveChanges={saveChanges}
                    snackbarData={snackbarData}
                    snackbarOpen={snackbarOpen}
                    setSnackbarOpen={setSnackbarOpen}
                    setSnackbarData={setSnackbarData}
                    modifiedCells={modifiedCells}
                    setModifiedCells={setModifiedCells}
                    handleExcelUpload={handleExcelUpload}
                    downloadExcelForConfiguration={
                      downloadExcelForConfiguration
                    }
                    groupBy={currentTabDisplay == 'Naphtha' ? 'type' : ''}
                  />
                </Box>
              )
            case 'Furnace':
              return (
                <Box key={currentTabDisplay}>
                  {IS_CRACKER_C2 && (
                    <Box sx={{ mt: 1, mb: 3 }}>
                      <SpyroInputMinMax />
                    </Box>
                  )}
                  <KendoDataTables
                    rows={rows}
                    setRows={setRowsForCurrent}
                    fetchData={() =>
                      fetchCrackerRows(currentTabDisplay, selectMode)
                    }
                    configType='cracker'
                    handleRemarkCellClick={handleRemarkCellClick}
                    columns={productionColumns}
                    remarkDialogOpen={remarkDialogOpen}
                    setRemarkDialogOpen={setRemarkDialogOpen}
                    currentRemark={currentRemark}
                    setCurrentRemark={setCurrentRemark}
                    currentRowId={currentRowId}
                    permissions={{
                      ...adjustedPermissions,
                      makePagable: false,
                    }}
                    handleCalculate={handleCalculate}
                    selectMode={selectMode}
                    setSelectMode={setSelectMode}
                    saveChanges={saveChanges}
                    snackbarData={snackbarData}
                    snackbarOpen={snackbarOpen}
                    setSnackbarOpen={setSnackbarOpen}
                    setSnackbarData={setSnackbarData}
                    modifiedCells={modifiedCells}
                    setModifiedCells={setModifiedCells}
                    handleExcelUpload={handleExcelUpload}
                    downloadExcelForConfiguration={
                      downloadExcelForConfiguration
                    }
                    groupBy={currentTabDisplay == 'Naphtha' ? 'type' : ''}
                  />
                </Box>
              )
            case 'Naphtha':
              if (IS_CRACKER_HMD || IS_CRACKER_C2) {
                return (
                  <Box key={currentTabDisplay}>
                    <NaphthaHMDComponent />
                  </Box>
                )
              }
              return (
                <Box key={currentTabDisplay}>
                  {/* Carbon Number Distribution Grid with Date Filter */}
                  <Box sx={{ mb: 2 }}>
                    <StartAndEndPicker
                      dateFormat='YYYY-MM-DD'
                      startDate={naphthaDateRange.startDate}
                      endDate={naphthaDateRange.endDate}
                      onLoad={({ startDate, endDate }) => {
                        setNaphthaDateRange({ startDate, endDate })
                        handleLoadNaphthaData(startDate, endDate)
                      }}
                    />

                    <KendoDataTables
                      rows={rows}
                      setRows={setRowsForCurrent}
                      fetchData={() =>
                        fetchCrackerRows(currentTabDisplay, selectMode)
                      }
                      configType='Naphtha'
                      handleRemarkCellClick={handleRemarkCellClick}
                      columns={productionColumns}
                      remarkDialogOpen={remarkDialogOpen}
                      setRemarkDialogOpen={setRemarkDialogOpen}
                      currentRemark={currentRemark}
                      setCurrentRemark={setCurrentRemark}
                      currentRowId={currentRowId}
                      permissions={{
                        ...adjustedPermissions,
                        naphthaLoadedFlag: naphthaLoadedFlag,
                      }}
                      selectMode={selectMode}
                      setSelectMode={setSelectMode}
                      saveChanges={saveChanges}
                      snackbarData={snackbarData}
                      snackbarOpen={snackbarOpen}
                      setSnackbarOpen={setSnackbarOpen}
                      setSnackbarData={setSnackbarData}
                      modifiedCells={modifiedCells}
                      setModifiedCells={setModifiedCells}
                      handleExcelUpload={handleExcelUpload}
                      downloadExcelForConfiguration={
                        downloadExcelForConfiguration
                      }
                      groupBy='type'
                    />
                  </Box>

                  <NaphthaLimsDataSet />
                </Box>
              )
            case 'External Streams':
              return (
                <Box key={currentTabDisplay}>
                  <KendoDataTables
                    rows={rows}
                    setRows={setRowsForCurrent}
                    fetchData={() =>
                      fetchCrackerRows(currentTabDisplay, selectMode)
                    }
                    configType='External_Streams'
                    groupBy='ParticularsType'
                    handleRemarkCellClick={handleRemarkCellClick}
                    columns={productionColumns}
                    remarkDialogOpen={remarkDialogOpen}
                    setRemarkDialogOpen={setRemarkDialogOpen}
                    currentRemark={currentRemark}
                    setCurrentRemark={setCurrentRemark}
                    currentRowId={currentRowId}
                    permissions={adjustedPermissions}
                    selectMode={selectMode}
                    setSelectMode={setSelectMode}
                    saveChanges={saveChanges}
                    snackbarData={snackbarData}
                    snackbarOpen={snackbarOpen}
                    setSnackbarOpen={setSnackbarOpen}
                    setSnackbarData={setSnackbarData}
                    modifiedCells={modifiedCells}
                    setModifiedCells={setModifiedCells}
                    handleExcelUpload={handleExcelUpload}
                    downloadExcelForConfiguration={
                      downloadExcelForConfiguration
                    }
                  />
                </Box>
              )

            case 'Composition':
              return (
                <Box key={currentTabDisplay}>
                  <KendoDataTables
                    rows={rows}
                    setRows={setRowsForCurrent}
                    fetchData={() =>
                      fetchCrackerRows(currentTabDisplay, selectMode)
                    }
                    configType='cracker_composition'
                    groupBy='ParticularsType'
                    handleRemarkCellClick={handleRemarkCellClick}
                    columns={productionColumns}
                    remarkDialogOpen={remarkDialogOpen}
                    setRemarkDialogOpen={setRemarkDialogOpen}
                    currentRemark={currentRemark}
                    setCurrentRemark={setCurrentRemark}
                    handleCalculate={handleCalculate}
                    currentRowId={currentRowId}
                    permissions={adjustedPermissions}
                    selectMode={selectMode}
                    setSelectMode={setSelectMode}
                    saveChanges={saveChanges}
                    snackbarData={snackbarData}
                    snackbarOpen={snackbarOpen}
                    setSnackbarOpen={setSnackbarOpen}
                    setSnackbarData={setSnackbarData}
                    modifiedCells={modifiedCells}
                    setModifiedCells={setModifiedCells}
                    handleExcelUpload={handleExcelUpload}
                    downloadExcelForConfiguration={
                      downloadExcelForConfiguration
                    }
                  />
                </Box>
              )

            default:
              return null
          }
        })()}
      </Box>
    </Box>
  )
}

export default CrackerConfig
