import React, { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import { getRoleName } from 'services/role-service'
import { validateFields } from 'utils/validationUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { PCGProductionRangeApiService } from 'components/aop-phase-two/services/pcg/pcgProductionRangeApiService'
import RowBasedKendoTable from 'components/aop-phase-two/common/RowBasedKendoTable/index'

const ConfigurationLimit = () => {
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [open1, setOpen1] = useState(false)
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
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_Production_Range_Limit`

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  // Handle Status checkbox toggle — updates row locally and marks as modified
  const handleStatusToggle = useCallback(
    (rowId) => {
      if (READ_ONLY) return
      setRows((prev) =>
        prev.map((r) => {
          if (r.id !== rowId) return r
          const updated = { ...r, status: !r.status, inEdit: true }
          setModifiedCells((prevCells) => ({
            ...prevCells,
            [rowId]: updated,
          }))
          return updated
        }),
      )
    },
    [READ_ONLY],
  )

  const NormConfigurationColumns = [
    {
      field: 'displayName',
      title: 'Particulars',
      editable: false,
      widthT: 250,
      autoAdjust: false,
      minWidth: 250,
    },
    {
      field: 'uom',
      title: 'UOM',
      editable: false,
      widthT: 80,
      minWidth: 100,
    },
    {
      field: 'productionLimit',
      title: 'Limit',
      editable: false,
      widthT: 100,
      minWidth: 100,
    },
    {
      field: 'apr',
      title: 'Value',
      editable: true,
      widthT: 100,
      type: 'row-based',
      minWidth: 100,
    },
    {
      field: 'status',
      title: 'Status',
      editable: true,
      widthT: 100,
      minWidth: 100,
      type: 'checkbox',
    },
    {
      field: 'remarks',
      title: 'Remark',
      editable: true,
      widthT: 250,
      autoAdjust: false,
      type: 'text',
      minWidth: 200,
    },
    {
      field: 'normParameterFKId',
      title: 'idFromApi',
      filterable: 'false',
      hidden: true,
      isVisible: false,
      minWidth: 100,
    },
  ]

  useEffect(() => {
    setModifiedCells({})
    fetchData()
  }, [oldYear, yearChanged, keycloak, PLANT_ID, AOP_YEAR])

  const adjustedPermissionsManual = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showExport: false,
    downloadExcelBtnFromUI: true,
    ExcelName: `PCG_Production_Norms_Configuration_Range_Limit_${AOP_YEAR}`,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Configuration Range (Limit)',
    showCalculateVisibility: true,
  }

  const handleUpdate = async (updatedRows) => {
    setLoading(true)
    try {
      let payload = updatedRows?.map((row) => {
        const { id, inEdit, particulars, originalRemark, ...rest } = row
        return {
          normParameterFkId: row.normParameterFkId,
          apr: row.apr,
          may: row.may,
          status: row.status ?? false,
          remarks: row.remarks,
          auditYear: row.auditYear,
          uom: row.uom,
          normTypeName: row.normTypeName,
          isEditable: row.isEditable,
          displayName: row.displayName,
          type: row.type ?? '',
        }
      })

      const response = await PCGProductionRangeApiService.postData(
        keycloak,
        payload,
        PLANT_ID,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully!',
        severity: 'success',
      })

      await fetchData()
      return { code: 200 }
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
      const fieldsToCheck = ['attributeValue']
      const validationError = validateRowDataWithRemarks(
        data.filter((item) => item.isEditable == true),
        originalRows,
        fieldsToCheck,
        'displayName',
      )

      if (validationError) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationError,
          severity: 'error',
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
        return
      }
      await handleUpdate(data)
    } catch (error) {
      console.log('Error saving changes:', error)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells])

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setModifiedCells({})

    try {
      setLoading(true)

      // const response = await PCGProductionRangeApiService.getDataForLimit(
      //   keycloak,
      //   PLANT_ID,
      //   AOP_YEAR,
      // )
      const response = {
        data: {
productionRangeLimitList: [
            {
              id: 1,
              displayName: 'Sulphur Production',
              uom: '%',
              productionLimit: '±',
              apr: '5',
              type: 'number',
              normParameterType: 'Fuel (Syngas)',
              status: true,
              remarks: 'Target should be from PIMS Sulphur production output (A) with ±5% deviation.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 2,
              displayName: 'Sulphur Production',
              uom: '%',
              productionLimit: '±',
              apr: '5',
              type: 'number',
              normParameterType: 'HP Steam',
              status: true,
              remarks: 'Target should be from PIMS Sulphur production output (A) with ±5% deviation.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 3,
              displayName: 'Sulphur Production',
              uom: '%',
              productionLimit: '±',
              apr: '5',
              type: 'number',
              normParameterType: 'LP Steam',
              status: true,
              remarks: 'Target should be from PIMS Sulphur production output (A) with ±5% deviation.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 4,
              displayName: 'Sulphur Production',
              uom: '%',
              productionLimit: '±',
              apr: '5',
              type: 'number',
              normParameterType: 'Oxygen',
              status: true,
              remarks: 'Target should be from PIMS Sulphur production output (A) with ±5% deviation.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 5,
              displayName: 'Sulphur Production',
              uom: '%',
              productionLimit: '±',
              apr: '5',
              type: 'number',
              normParameterType: 'Power',
              status: true,
              remarks: 'Target should be from PIMS Sulphur production output (A) with ±5% deviation.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 6,
              displayName: 'Sulphur Production',
              uom: '%',
              productionLimit: '±',
              apr: '5',
              type: 'number',
              normParameterType: 'Return Steam Condensate',
              status: true,
              remarks: 'Target should be from PIMS Sulphur production output (A) with ±5% deviation.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 7,
              displayName: 'Sulphur Production',
              uom: '%',
              productionLimit: '±',
              apr: '5',
              type: 'number',
              normParameterType: 'BFW',
              status: true,
              remarks: 'Target should be from PIMS Sulphur production output (A) with ±5% deviation.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 8,
              displayName: 'J1 Acid Gas Flow',
              uom: 'Nm³/hr',
              productionLimit: '±',
              apr: '500',
              type: 'number',
              normParameterType: 'Fuel (Syngas)',
              status: true,
              remarks: 'Target will be manual input with ±500 nm3/hr',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 9,
              displayName: 'J1 Acid Gas Flow',
              uom: 'Nm³/hr',
              productionLimit: '±',
              apr: '500',
              type: 'number',
              normParameterType: 'HP Steam',
              status: true,
              remarks: 'Target will be manual input with ±500 nm3/hr',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 10,
              displayName: 'J1 Acid Gas Flow',
              uom: 'Nm³/hr',
              productionLimit: '±',
              apr: '500',
              type: 'number',
              normParameterType: 'LP Steam',
              status: true,
              remarks: 'Target will be manual input with ±500 nm3/hr',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 11,
              displayName: 'J1 Acid Gas Flow',
              uom: 'Nm³/hr',
              productionLimit: '±',
              apr: '500',
              type: 'number',
              normParameterType: 'Oxygen',
              status: true,
              remarks: 'Target will be manual input with ±500 nm3/hr',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 12,
              displayName: 'J1 Acid Gas Flow',
              uom: 'Nm³/hr',
              productionLimit: '±',
              apr: '500',
              type: 'number',
              normParameterType: 'Power',
              status: true,
              remarks: 'Target will be manual input with ±500 nm3/hr',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 13,
              displayName: 'J1 Acid Gas Flow',
              uom: 'Nm³/hr',
              productionLimit: '±',
              apr: '500',
              type: 'number',
              normParameterType: 'Return Steam Condensate',
              status: true,
              remarks: 'Target will be manual input with ±500 nm3/hr',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 14,
              displayName: 'J1 Acid Gas Flow',
              uom: 'Nm³/hr',
              productionLimit: '±',
              apr: '500',
              type: 'number',
              normParameterType: 'BFW',
              status: true,
              remarks: 'Target will be manual input with ±500 nm3/hr',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 15,
              displayName: 'Average J3 Acid Gas H2S Concentration',
              uom: '%',
              productionLimit: '±',
              apr: '2',
              type: 'number',
              normParameterType: 'Fuel (Syngas)',
              status: true,
              remarks: 'Target will be manual input with ± 2%.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 16,
              displayName: 'Average J3 Acid Gas H2S Concentration',
              uom: '%',
              productionLimit: '±',
              apr: '2',
              type: 'number',
              normParameterType: 'HP Steam',
              status: true,
              remarks: 'Target will be manual input with ± 2%.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 17,
              displayName: 'Average J3 Acid Gas H2S Concentration',
              uom: '%',
              productionLimit: '±',
              apr: '2',
              type: 'number',
              normParameterType: 'LP Steam',
              status: true,
              remarks: 'Target will be manual input with ± 2%.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 18,
              displayName: 'Average J3 Acid Gas H2S Concentration',
              uom: '%',
              productionLimit: '±',
              apr: '2',
              type: 'number',
              normParameterType: 'Oxygen',
              status: true,
              remarks: 'Target will be manual input with ± 2%.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 19,
              displayName: 'Average J3 Acid Gas H2S Concentration',
              uom: '%',
              productionLimit: '±',
              apr: '2',
              type: 'number',
              normParameterType: 'Power',
              status: true,
              remarks: 'Target will be manual input with ± 2%.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 20,
              displayName: 'Average J3 Acid Gas H2S Concentration',
              uom: '%',
              productionLimit: '±',
              apr: '2',
              type: 'number',
              normParameterType: 'Return Steam Condensate',
              status: true,
              remarks: 'Target will be manual input with ± 2%.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 21,
              displayName: 'Average J3 Acid Gas H2S Concentration',
              uom: '%',
              productionLimit: '±',
              apr: '2',
              type: 'number',
              normParameterType: 'BFW',
              status: true,
              remarks: 'Target will be manual input with ± 2%.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 22,
              displayName: 'Incinerator Temperature',
              uom: '°C',
              productionLimit: '>',
              apr: '740',
              type: 'number',
              normParameterType: 'Fuel (Syngas)',
              status: true,
              remarks: 'Data to be filtered for (G360TIC000157 >740℃ & G361TIC000157>740 ℃)',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 23,
              displayName: 'Incinerator Temperature',
              uom: '°C',
              productionLimit: '>',
              apr: '740',
              type: 'number',
              normParameterType: 'HP Steam',
              status: true,
              remarks: 'Data to be filtered for (G360TIC000157 >740℃ & G361TIC000157>740 ℃)',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 24,
              displayName: 'Incinerator Temperature',
              uom: '°C',
              productionLimit: '>',
              apr: '740',
              type: 'number',
              normParameterType: 'LP Steam',
              status: true,
              remarks: 'Data to be filtered for (G360TIC000157 >740℃ & G361TIC000157>740 ℃)',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 25,
              displayName: 'Incinerator Temperature',
              uom: '°C',
              productionLimit: '>',
              apr: '740',
              type: 'number',
              normParameterType: 'Return Steam Condensate',
              status: true,
              remarks: 'Data to be filtered for (G360TIC000157 >740℃ & G361TIC000157>740 ℃)',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 26,
              displayName: 'Incinerator Temperature',
              uom: '°C',
              productionLimit: '>',
              apr: '740',
              type: 'number',
              normParameterType: 'BFW',
              status: true,
              remarks: 'Data to be filtered for (G360TIC000157 >740℃ & G361TIC000157>740 ℃)',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 27,
              displayName: 'Average O2 Enrichment',
              uom: '%',
              productionLimit: '±',
              apr: '2',
              type: 'number',
              normParameterType: 'Oxygen',
              status: true,
              remarks: 'Target will be manual input with ± 2%.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 28,
              displayName: 'Average O2 Enrichment',
              uom: '%',
              productionLimit: '±',
              apr: '2',
              type: 'number',
              normParameterType: 'Power',
              status: true,
              remarks: 'Target will be manual input with ± 2%.',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 29,
              displayName: 'SWS-1 Sour Water Processing Rate',
              uom: 'm3/hr',
              productionLimit: '>',
              apr: '200',
              type: 'number',
              normParameterType: 'LP Steam',
              status: true,
              remarks: 'Data to be filtered for (G350FIC144004+ G350FIC244004) >200 m3/hr',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 30,
              displayName: 'SWS-1 Sour Water Processing Rate',
              uom: 'm3/hr',
              productionLimit: '>',
              apr: '200',
              type: 'number',
              normParameterType: 'Return Steam Condensate',
              status: true,
              remarks: 'Data to be filtered for (G350FIC144004+ G350FIC244004) >200 m3/hr',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 31,
              displayName: 'SRU-1 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'LLP N2',
              status: true,
              remarks: 'SRU-1 Sulphur production GSR1PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 32,
              displayName: 'SRU-1 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'LP N2',
              status: true,
              remarks: 'SRU-1 Sulphur production GSR1PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 33,
              displayName: 'SRU-1 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'Instrument Air',
              status: true,
              remarks: 'SRU-1 Sulphur production GSR1PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 34,
              displayName: 'SRU-1 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'DM Water',
              status: true,
              remarks: 'SRU-1 Sulphur production GSR1PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 35,
              displayName: 'SRU-1 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'Utility Water',
              status: true,
              remarks: 'SRU-1 Sulphur production GSR1PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 36,
              displayName: 'SRU-1 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'Plant Air',
              status: true,
              remarks: 'SRU-1 Sulphur production GSR1PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 37,
              displayName: 'SRU-1 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'caustic',
              status: true,
              remarks: 'SRU-1 Sulphur production GSR1PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 38,
              displayName: 'SRU-1 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'CHEM Ammonia',
              status: true,
              remarks: 'SRU-1 Sulphur production GSR1PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 39,
              displayName: 'SRU-1 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'Chem Maxtreat 3223 SJ',
              status: true,
              remarks: 'SRU-1 Sulphur production GSR1PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 40,
              displayName: 'SRU-2 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'LLP N2',
              status: true,
              remarks: 'SRU-2 Sulphur production GSR2PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 41,
              displayName: 'SRU-2 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'LP N2',
              status: true,
              remarks: 'SRU-2 Sulphur production GSR2PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 42,
              displayName: 'SRU-2 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'Instrument Air',
              status: true,
              remarks: 'SRU-2 Sulphur production GSR2PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 43,
              displayName: 'SRU-2 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'DM Water',
              status: true,
              remarks: 'SRU-2 Sulphur production GSR2PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 44,
              displayName: 'SRU-2 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'Utility Water',
              status: true,
              remarks: 'SRU-2 Sulphur production GSR2PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 45,
              displayName: 'SRU-2 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'Plant Air',
              status: true,
              remarks: 'SRU-2 Sulphur production GSR2PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 46,
              displayName: 'SRU-2 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'caustic',
              status: true,
              remarks: 'SRU-2 Sulphur production GSR2PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 47,
              displayName: 'SRU-2 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'CHEM Ammonia',
              status: true,
              remarks: 'SRU-2 Sulphur production GSR2PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
            {
              id: 48,
              displayName: 'SRU-2 Sulphur Production',
              uom: 'TPD',
              productionLimit: '>',
              apr: '50',
              type: 'number',
              normParameterType: 'Chem Maxtreat 3223 SJ',
              status: true,
              remarks: 'SRU-2 Sulphur production GSR2PR001 > 50 TPD',
              isEditable: true,
              allowNegative: true,
            },
          ],
        },
      }

      const formattedData = response?.data?.productionRangeLimitList?.map(
        (row, index) => ({
          ...row,
          id: row.id || index,
          particulars: row.displayName,
          originalRemark: row.remarks || '',
          normParameterFKId: row.normParameterFKId,
          auditYear: row.auditYear,
          normTypeName: row.normParameterType,
          isEditable: row.isEditable,
          displayName: row.displayName,
          // type: row.type,
          productionLimit: row.productionLimit,
          status: row.status ?? false,
          allowNegative: row?.allowNegative ?? false,
        }),
      )

      setRows(formattedData || [])
      setOriginalRows(formattedData || [])
    } catch (error) {
      setRows([])
      setOriginalRows([])
      console.error('Error fetching Range Limit data:', error)
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      await PCGProductionRangeApiService.getProductionRangeLimitExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
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
    }
  }

  const handleExcelUpload = (rawFile) => {
    uploadProductionRangeLimit(rawFile)
  }

  const uploadProductionRangeLimit = async (rawFile) => {
    setLoading(true)

    try {
      let response =
        await PCGProductionRangeApiService.productionRangeLimitImport(
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
        link.setAttribute(
          'download',
          'Error File - Production Range Limit.xlsx',
        )
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
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }

      return response
    } catch (error) {
      console.error('Error uploading Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <Box>
        <RowBasedKendoTable
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          setRows={setRows}
          columns={NormConfigurationColumns}
          rows={rows}
          title={
            adjustedPermissionsManual.showTitle
              ? adjustedPermissionsManual.titleName
              : ''
          }
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          apiRef={apiRef}
          open1={open1}
          setOpen1={setOpen1}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          handleRemarkCellClick={handleRemarkCellClick}
          fetchData={fetchData}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          permissions={adjustedPermissionsManual}
          groupBy={['normTypeName']}
          saveChanges={saveChanges}
          handleExport={downloadExcelForConfiguration}
          handleExcelUpload={handleExcelUpload}
          paginationConfig={{
            threshold: 100,
            buttonCount: 5,
            pageSizes: [10, 20, 50, 100],
            defaultPageSize: 100,
          }}
        />
      </Box>
    </Box>
  )
}

export default ConfigurationLimit
