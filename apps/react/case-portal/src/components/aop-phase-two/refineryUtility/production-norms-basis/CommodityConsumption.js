import { Box } from '@mui/material'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/refineryUtility/productionNormsApiService'
import { validateFields } from 'utils/validationUtils'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import Notification from 'components/Utilities/Notification'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'

const MONTHS_LIST = [
  'april',
  'may',
  'june',
  'july',
  'august',
  'september',
  'october',
  'november',
  'december',
  'january',
  'february',
  'march',
]

const CommodityConsumption = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year, oldYear, isReleased } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const valueFormat = ValueFormatterPhaseTwo()

  const plantName = (plantObject?.name || '').trim()
  const siteName = (siteObject?.name || '').trim().toUpperCase()

  const isDtaCtPlant =
    siteName === 'DTA' &&
    (plantName.toLowerCase() === 'ct8 (738)' ||
      plantName.toLowerCase() === 'ct9 (739)')

  const isDtaUnitPlant =
    siteName === 'DTA' &&
    (plantName.toLowerCase() === 'new_cpu_unit (722)' ||
      plantName.toLowerCase() === 'new_dm_unit (721)' ||
      plantName.toLowerCase() === 'new_remineralisation_unit (712)' ||
      plantName.toLowerCase() === 'old_cpu_unit (722)' ||
      plantName.toLowerCase() === 'old_dm_unit (721)' ||
      plantName.toLowerCase() === 'old_remineralisation_unit (712)' ||
      plantName.toLowerCase() === 'spent_caustic (palpro)' ||
      plantName.toLowerCase() === 'wao_unit (672)')

  const headerMap = useMemo(() => generateHeaderNames(AOP_YEAR), [AOP_YEAR])

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [isTwoColumnPlant, setIsTwoColumnPlant] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const formatUomValue = useCallback(
    (val) => {
      if (val === null || val === undefined || val === '') return ''
      const num = parseFloat(val)
      if (isNaN(num)) return val

      if (isDtaUnitPlant) {
        return (Math.trunc(num * 100) / 100).toFixed(2)
      }
      return val
    },
    [isDtaUnitPlant],
  )

  useEffect(() => {
    const fetchPlantColumnConfig = async () => {
      if (!PLANT_ID) return
      try {
        const res = await ProductionNormsApiService.checkIsSummerWinterPlant(keycloak, PLANT_ID)
        setIsTwoColumnPlant(Boolean(res?.data?.isSummerWinter ?? res?.data?.isTwoColumn))
      } catch (err) {
        console.error('Error checking plant column config:', err)
        setIsTwoColumnPlant(false)
      }
    }
    if (PLANT_ID) {
      fetchPlantColumnConfig()
    }
  }, [PLANT_ID, keycloak])

  const fetchCommodityData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = isDtaCtPlant
        ? await ProductionNormsApiService.getCommodityChemicalsConsumptionMonthly(
            keycloak,
            PLANT_ID,
            AOP_YEAR,
          )
        : await ProductionNormsApiService.getCommodityConsumptionData(
            keycloak,
            PLANT_ID,
            AOP_YEAR,
          )
      if (response?.code === 200) {
        const formattedData = (response?.data || []).map((item, index) => {
          let val =
            item?.april !== undefined && item?.april !== null
              ? item.april
              : item?.value !== undefined && item?.value !== null
              ? item.value
              : ''
          if (isDtaUnitPlant) {
            val = formatUomValue(val)
          }
          const uomVal = item?.uom || item?.UOM || ''
          const nameVal =
            item?.displayName || item?.name || item?.productName || ''
          const groupName =
            item?.normTypeName ||
            item?.TypeDisplayName ||
            'Commodity Chemical Consumption'

          const monthValues = {}
          MONTHS_LIST.forEach((m) => {
            const rawM =
              item?.[m] !== undefined && item?.[m] !== null ? item[m] : ''
            monthValues[m] = isDtaUnitPlant ? formatUomValue(rawM) : rawM
          })

          return {
            ...item,
            ...monthValues,
            idFromApi: item?.normParameterFKId || item?.id,
            id: index,
            Type: groupName,
            ParticularG: groupName,
            name: item?.name || nameVal,
            displayName: nameVal,
            productName: nameVal,
            uom: uomVal,
            UOM: uomVal,
            april: isDtaCtPlant ? monthValues.april : val,
            value: val,
            remarks: item?.remarks || '',
            originalRemark: item?.remarks || '',
            isEditable: item?.isEditable ?? true,
          }
        })
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching Commodity Chemical Consumption data:', error)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, isDtaCtPlant, isDtaUnitPlant, formatUomValue])

  useEffect(() => {
    setRows([])
    setModifiedCells({})
    fetchCommodityData()
  }, [fetchCommodityData, PLANT_ID, AOP_YEAR, isDtaUnitPlant])

  const colDefs = useMemo(() => {
    if (isDtaCtPlant) {
      return [
        {
          field: 'displayName',
          title: 'Particulars',
          editable: false,
          width: 300,
          minWidth: 250,
          widthT: 300,
          locked: true,
        },
        {
          field: 'uom',
          title: 'UOM',
          editable: false,
          width: 80,
          minWidth: 80,
          widthT: 80,
          locked: true,
        },
        {
          field: 'april',
          title: headerMap[4],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'may',
          title: headerMap[5],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'june',
          title: headerMap[6],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'july',
          title: headerMap[7],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'august',
          title: headerMap[8],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'september',
          title: headerMap[9],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'october',
          title: headerMap[10],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'november',
          title: headerMap[11],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'december',
          title: headerMap[12],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'january',
          title: headerMap[1],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'february',
          title: headerMap[2],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'march',
          title: headerMap[3],
          widthT: 120,
          minWidth: 120,
          type: 'number1',
          editable: true,
          format: valueFormat,
        },
        {
          field: 'remarks',
          title: 'Remark',
          widthT: 150,
          minWidth: 120,
          type: 'textarea',
          editable: true,
        },
      ]
    }

    return [
      {
        field: 'ParticularG',
        title: 'Particulars',
        editable: false,
        width: 300,
        minWidth: 250,
        widthT: 300,
        locked: true,
        hidden: true,
      },
      {
        field: 'displayName',
        title: 'Particulars',
        editable: false,
        width: 300,
        minWidth: 250,
        widthT: 300,
      },
      {
        field: 'uom',
        title: 'UOM',
        editable: false,
        width: 100,
        minWidth: 80,
        widthT: 100,
      },
      {
        field: 'april',
        title: 'Value',
        width: 150,
        minWidth: 120,
        widthT: 150,
        type: 'number',
        format: isDtaUnitPlant ? '{0:0.00}' : valueFormat,
        editable: true,
      },
      {
        field: 'remarks',
        title: 'Remark',
        editable: true,
        width: 300,
        minWidth: 250,
        widthT: 300,
      },
    ]
  }, [isDtaCtPlant, headerMap, valueFormat, isDtaUnitPlant])

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = useCallback(async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) return
    const requiredFields = ['remarks']
    const validationData = modifiedData.map((row) => ({
      ...row,
    }))
    const validationMessage = validateFields(validationData, requiredFields)
    if (validationMessage) {
      setSnackbarData({ message: validationMessage, severity: 'error' })
      setSnackbarOpen(true)
      return
    }

    setLoading(true)
    try {
      let response
      if (isDtaCtPlant) {
        const payload = modifiedData.map((row) => {
          const getMonthVal = (val) =>
            val !== undefined && val !== '' && val !== null && !isNaN(Number(val))
              ? Number(val)
              : null

          return {
            normParameterFKId: row.normParameterFKId || row.idFromApi,
            name: row.name || row.displayName || '',
            displayName: row.displayName || row.name || '',
            uom: row.uom || row.UOM || '',
            normTypeName: row.normTypeName || row.ParticularG || '',
            april: getMonthVal(row.april),
            may: getMonthVal(row.may),
            june: getMonthVal(row.june),
            july: getMonthVal(row.july),
            august: getMonthVal(row.august),
            september: getMonthVal(row.september),
            october: getMonthVal(row.october),
            november: getMonthVal(row.november),
            december: getMonthVal(row.december),
            january: getMonthVal(row.january),
            february: getMonthVal(row.february),
            march: getMonthVal(row.march),
            auditYear: row.auditYear || AOP_YEAR,
            remarks: row.remarks || '',
            displayOrder: row.displayOrder ?? null,
            isEditable: row.isEditable ?? true,
          }
        })

        response =
          await ProductionNormsApiService.saveCommodityChemicalsConsumptionMonthly(
            keycloak,
            PLANT_ID,
            AOP_YEAR,
            payload,
          )
      } else {
        const payload = modifiedData.map((row) => {
          const numericVal =
            row.april !== undefined && row.april !== '' && row.april !== null
              ? Number(row.april)
              : row.value !== undefined && row.value !== '' && row.value !== null
              ? Number(row.value)
              : null

          return {
            normParameterFKId: row.normParameterFKId || row.idFromApi,
            name: row.name || row.displayName || '',
            displayName: row.displayName || row.name || '',
            uom: row.uom || row.UOM || '',
            normTypeName: row.normTypeName || row.ParticularG || '',
            april: numericVal,
            value: numericVal,
            auditYear: row.auditYear || AOP_YEAR,
            remarks: row.remarks || '',
            displayOrder: row.displayOrder ?? null,
            isEditable: row.isEditable ?? true,
          }
        })

        response = await ProductionNormsApiService.saveCommodityConsumptionData(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          payload,
        )
      }

      if (response?.code === 200 || response?.success) {
        setSnackbarData({
          message: response?.message || 'Saved Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchCommodityData()
      } else if (response?.code === 400) {
        setSnackbarData({
          message: response?.message || 'Partial Data Updated',
          severity: 'warning',
        })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchCommodityData()
      } else {
        setSnackbarData({
          message: response?.message || 'Save Failed!',
          severity: 'error',
        })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving Commodity Chemical Consumption data:', error)
      setSnackbarData({
        message: error?.message || 'Error saving data',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, isDtaCtPlant, PLANT_ID, AOP_YEAR, keycloak, fetchCommodityData])

  const handleExcelUpload = async (file) => {
    setLoading(true)
    try {
      const response =
        await ProductionNormsApiService.importCommodityConsumptionExcel(
          file,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      if (response?.code === 200) {
        setSnackbarData({
          message: 'Imported Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
        fetchCommodityData()
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
        link.setAttribute('download', 'Error File - Commodity Chemical Consumption.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchCommodityData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error importing Commodity Chemical Consumption data:', error)
      setSnackbarData({ message: 'Error importing data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    try {
      setSnackbarData({ message: 'Export Started!', severity: 'success' })
      setSnackbarOpen(true)
      const excelName = `${verticalObject?.name}_${siteObject?.name}_${plantObject?.name}_Commodity Chemical Consumption`
      await ProductionNormsApiService.exportCommodityConsumptionExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        excelName,
      )
      setSnackbarData({ message: 'Export Successful!', severity: 'success' })
      setSnackbarOpen(true)
    } catch (error) {
      console.error('Error exporting Commodity Chemical Consumption data:', error)
      setSnackbarData({ message: 'Error exporting data', severity: 'error' })
      setSnackbarOpen(true)
    }
  }

  const adjustedPermissions = useMemo(() => {
    const basePermissions = {
      showAction: true,
      saveWithRemark: true,
      saveBtn: true,
      allAction: true,
      showTitleNameBusiness: true,
      showExport: false,
      showImport: false,
      showCalculate: false,
      showCalculateVisibility: false,
    }

    if (READ_ONLY || IS_OLD_YEAR) {
      return {
        ...basePermissions,
        showAction: false,
        saveWithRemark: false,
        saveBtn: false,
        allAction: false,
      }
    }

    return basePermissions
  }, [READ_ONLY, IS_OLD_YEAR])

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        rows={rows}
        setRows={setRows}
        columns={colDefs}
        permissions={adjustedPermissions}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title='Commodity Chemical Consumption'
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        plantID={PLANT_ID}
        groupBy='ParticularG'
        handleExcelUpload={handleExcelUpload}
        handleExport={downloadExcelForConfiguration}
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

export default CommodityConsumption
