import { Box } from '@mui/material'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import Config from 'consts/index'
import { ProductionNormsApiService } from 'components/aop-phase-two/services/refineryUtility/productionNormsApiService'
import { validateFields } from 'utils/validationUtils'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import Notification from 'components/Utilities/Notification'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'

const ResinConsumption = () => {
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

  const siteName = (siteObject?.name || '').trim().toUpperCase()
  const plantName = (plantObject?.name || '').trim()

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

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
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

  const fetchResinData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await ProductionNormsApiService.getResinConsumptionData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200 || response?.status === 200) {
        const list = Array.isArray(response?.data)
          ? response.data
          : Array.isArray(response)
          ? response
          : []
        const formattedData = list.map((item, index) => {
          let val = item?.value
          if (val !== undefined && val !== null && val !== '') {
            val = isDtaUnitPlant && !isNaN(parseFloat(val))
              ? (Math.trunc(parseFloat(val) * 100) / 100).toFixed(2)
              : Number(val)
          } else {
            val = item?.value ?? ''
          }

          return {
            ...item,
            id: item?.id ?? index + 1,
            idFromApi: item?.normParameterId || item?.normParameterFKId || null,
            normParameterId:
              item?.normParameterId || item?.normParameterFKId || null,
            displayName:
              item?.displayName || item?.DisplayName || item?.name || '',
            name: item?.name || item?.displayName || '',
            value: val,
            remarks: item?.remarks || '',
            originalRemark: item?.remarks || '',
            aopYear: item?.aopYear || AOP_YEAR,
            plantId: item?.plantId || PLANT_ID,
            isEditable: item?.isEditable ?? !READ_ONLY,
          }
        })
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching Resin Consumption data:', error)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, READ_ONLY, isDtaUnitPlant])

  useEffect(() => {
    setRows([])
    setModifiedCells({})
    fetchResinData()
  }, [fetchResinData, PLANT_ID, AOP_YEAR, isDtaUnitPlant])

  const colDefs = useMemo(
    () => [
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
        width: 60,
        minWidth: 60,
        widthT: 60,
      },
      {
        field: 'value',
        title: 'Value',
        width: 150,
        minWidth: 120,
        widthT: 150,
        type: 'number',
        format: isDtaUnitPlant ? '{0:0.00}' : valueFormat,
        editable: !READ_ONLY,
      },
      {
        field: 'remarks',
        title: 'Remark',
        editable: !READ_ONLY,
        width: 300,
        minWidth: 250,
        widthT: 300,
      },
    ],
    [valueFormat, READ_ONLY, isDtaUnitPlant],
  )

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY || IS_OLD_YEAR) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = useCallback(async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarData({ message: 'No changes to save.', severity: 'info' })
      setSnackbarOpen(true)
      return
    }

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
      const payload = modifiedData.map((row) => {
        const numericVal =
          row.value !== undefined && row.value !== '' && row.value !== null
            ? Number(row.value)
            : null

        return {
          normParameterId: row.normParameterId || row.idFromApi || null,
          displayName: row.displayName || '',
          value: numericVal,
          remarks: row.remarks || '',
          aopYear: row.aopYear || AOP_YEAR,
          plantId: row.plantId || PLANT_ID,
        }
      })

      let response
      if (typeof ProductionNormsApiService.saveResinConsumption === 'function') {
        try {
          response = await ProductionNormsApiService.saveResinConsumption(
            keycloak,
            AOP_YEAR,
            payload,
            PLANT_ID,
          )
        } catch (apiErr) {
          if (apiErr instanceof ReferenceError) {
            try {
              response = await ProductionNormsApiService.saveResinConsumption(
                keycloak,
                PLANT_ID,
                AOP_YEAR,
                payload,
              )
            } catch (secondErr) {
              const url = `${Config.CaseEngineUrl}/task/resin-consumptions?plantFKId=${PLANT_ID}&year=${AOP_YEAR}`
              const res = await fetch(url, {
                method: 'POST',
                headers: {
                  Accept: 'application/json',
                  'Content-Type': 'application/json',
                  Authorization: `Bearer ${keycloak?.token}`,
                },
                body: JSON.stringify(payload),
              })
              response = await res.json()
            }
          } else {
            throw apiErr
          }
        }
      } else {
        const url = `${Config.CaseEngineUrl}/task/resin-consumptions?plantFKId=${PLANT_ID}&year=${AOP_YEAR}`
        const res = await fetch(url, {
          method: 'POST',
          headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
            Authorization: `Bearer ${keycloak?.token}`,
          },
          body: JSON.stringify(payload),
        })
        response = await res.json()
      }

      if (
        response?.code === 200 ||
        response?.status === 200 ||
        response?.success
      ) {
        setSnackbarData({
          message: response?.message || 'Saved Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchResinData()
      } else if (response?.code === 400) {
        setSnackbarData({
          message: response?.message || 'Partial Data Updated',
          severity: 'warning',
        })
        setSnackbarOpen(true)
        setModifiedCells({})
        fetchResinData()
      } else {
        setSnackbarData({
          message: response?.message || 'Save Failed!',
          severity: 'error',
        })
        setSnackbarOpen(true)
      }
    } catch (error) {
      console.error('Error saving Resin Consumption data:', error)
      setSnackbarData({
        message: error?.message || 'Error saving data',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, PLANT_ID, AOP_YEAR, keycloak, fetchResinData])

  const handleExcelUpload = async (file) => {
    setLoading(true)
    try {
      if (
        typeof ProductionNormsApiService.importVendorConsumptionExcel !==
        'function'
      ) {
        setSnackbarData({
          message: 'Excel import is not configured for Resin Consumption.',
          severity: 'warning',
        })
        setSnackbarOpen(true)
        return
      }

      const response =
        await ProductionNormsApiService.importVendorConsumptionExcel(
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
        fetchResinData()
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
        link.setAttribute('download', 'Error File - Resin Consumption.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchResinData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error importing Resin Consumption data:', error)
      setSnackbarData({ message: 'Error importing data', severity: 'error' })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    try {
      if (
        typeof ProductionNormsApiService.exportVendorConsumptionExcel !==
        'function'
      ) {
        setSnackbarData({
          message: 'Excel export is not configured for Resin Consumption.',
          severity: 'warning',
        })
        setSnackbarOpen(true)
        return
      }

      setSnackbarData({ message: 'Export Started!', severity: 'success' })
      setSnackbarOpen(true)
      const excelName = `${verticalObject?.name}_${siteObject?.name}_${plantObject?.name}_Resin Consumption`
      await ProductionNormsApiService.exportVendorConsumptionExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        excelName,
      )
      setSnackbarData({ message: 'Export Successful!', severity: 'success' })
      setSnackbarOpen(true)
    } catch (error) {
      console.error('Error exporting Resin Consumption data:', error)
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
        title='Resin Consumption'
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        plantID={PLANT_ID}
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

export default ResinConsumption


