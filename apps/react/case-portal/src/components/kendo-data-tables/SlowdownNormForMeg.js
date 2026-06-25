import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { SlowdownNormForMegServices } from 'services/SlowdownNormForMegServices'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { ConsumptionNormsApiService } from 'services/consumption-norms-api-service'
import { shouldLockColumn } from 'utils/columnLockUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
const SlowdownNormForMeg = () => {
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
  const PLANT_NAME = plantObject?.name
  const SITE_ID = siteObject?.id
  const SITE_NAME = siteObject?.name
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const vertName = verticalChange?.selectedVertical

  const lowerVertName = vertName?.toLowerCase()

  const SCREEN_NAME = screenTitle?.title

  const [isLoading, setIsLoading] = useState(false)
  const [loading, setLoading] = useState(false)
  const [notification, setNotification] = useState({
    open: false,
    message: '',
    severity: 'info',
  })

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const [tableRows, setTableRows] = useState([])
  const [columnDefinitions, setColumnDefinitions] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [calculationResults, setCalculationResults] = useState([])
  const valueFormat = ValueFormatterConsumption()
  const showNotification = useCallback((message, severity = 'info') => {
    setNotification({
      open: true,
      message,
      severity,
    })
  }, [])

  const closeNotification = useCallback(() => {
    setNotification((prev) => ({ ...prev, open: false }))
  }, [])

  const saveSlowdownConfiguration = useCallback(
    async (payload) => {
      setIsLoading(true)
      try {
        const response =
          await SlowdownNormForMegServices.updateSlowdownNormsForMeg({
            keycloak,
            PLANT_ID,
            year: AOP_YEAR,
            payload,
          })

        if (response?.code === 200) {
          showNotification('Saved Successfully!', 'success')
          setModifiedCells({})
          await fetchSlowdownNormsColumns()
        } else {
          showNotification('Data Save Failed!', 'error')
        }

        return response
      } catch (error) {
        console.error('Error saving data:', error)
        showNotification('Data Save Failed!', 'error')
        throw error
      } finally {
        setIsLoading(false)
      }
    },
    [keycloak, PLANT_ID, AOP_YEAR, showNotification],
  )

  const handleSaveChanges = useCallback(async () => {
    try {
      const modifiedData = Object.values(modifiedCells)

      if (modifiedData.length === 0) {
        showNotification('No Records to Save!', 'info')
        return
      }

      const sanitizedData = modifiedData.map((item) => ({
        ...item,
        normParameterFKId: item.NormParameter_FK_Id,
        NormParameter_FK_Id: undefined,
        inEdit: undefined,
        particulars: undefined,
        id: undefined,
        aopYear: undefined,
        normParameterDisplayName: undefined,
        plantId: undefined,
        DisplayName: undefined,
        NormTypeName: undefined,
        srNo: undefined,
        isEditable: undefined,
        IsEditable: undefined,
        Particulars: undefined,
        uom: undefined,
        UOM: undefined,
      }))

      await saveSlowdownConfiguration(sanitizedData)
    } catch (error) {
      console.error('Error in handleSaveChanges:', error)
    }
  }, [modifiedCells, saveSlowdownConfiguration, showNotification])

  const handleCalculateData = useCallback(async () => {
    setIsLoading(true)
    try {
      const response =
        await SlowdownNormForMegServices.getSlowdownNormsCalculateForMeg({
          keycloak,
          PLANT_ID,
          year: AOP_YEAR,
        })

      if (response) {
        showNotification('Data refreshed successfully!', 'success')
        await fetchSlowdownNormsColumns()

        await fetchSlowdownNormsData()
      } else {
        showNotification('Data Refresh Failed!', 'error')
      }

      return response
    } catch (error) {
      console.error('Error refreshing data:', error)
      showNotification('Data Refresh Failed!', 'error')
    } finally {
      setIsLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, showNotification])

  const fetchSlowdownNormsData = useCallback(async () => {
    setIsLoading(true)
    try {
      const { data } =
        await SlowdownNormForMegServices.getSlowdownNormsDataForMeg({
          keycloak,
          PLANT_ID,
          year: AOP_YEAR,
        })

      const formattedRows =
        data?.resultList?.map((item, index) => {
          const parsedItem = Object.entries(item).reduce(
            (acc, [key, value]) => {
              if (
                typeof value === 'string' &&
                !isNaN(value) &&
                value.trim() !== ''
              ) {
                const parsedValue = parseFloat(value)
                acc[key] = isNaN(parsedValue) ? value : parsedValue
              } else {
                acc[key] = value
              }
              return acc
            },
            {},
          )

          return {
            ...parsedItem,
            id: index,
            particulars: item.DisplayName,
            Particulars: item?.NormTypeName,
            isEditable: item?.IsEditable,
          }
        }) || []
      setTableRows(formattedRows)
      setCalculationResults(data?.aopCalculation || [])
    } catch (error) {
      console.error('Error fetching slowdown norms data:', error)
      setTableRows([])
      setCalculationResults([])
    } finally {
      setIsLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const fetchSlowdownNormsColumns = useCallback(async () => {
    setIsLoading(true)
    setColumnDefinitions([])

    try {
      const response =
        await SlowdownNormForMegServices.getSlowdownNormsColumnsForMeg({
          keycloak,
          PLANT_ID,
          year: AOP_YEAR,
        })

      const hiddenColumns = [
        'srNo',
        'NormTypeName',
        'DisplayName',
        'NormParameter_FK_Id',
        'normParameterDisplayName',
        'aopYear',
        'plantId',
        'IsEditable',
      ]

      if (response?.code === 200 && Array.isArray(response.data)) {
        const dynamicColumns = response.data.map((column) => {
          const col = {
            field: column.field,
            title: column.title,
            minWidth: column.field.toLowerCase() === 'uom' ? 90 : 200,

            editable:
              column.field === 'particulars' ||
              column.field.toLowerCase() === 'uom'
                ? false
                : true,

            hidden: hiddenColumns.includes(column.field),
            ...(column.field !== 'particulars' &&
              column.field.toLowerCase() !== 'uom' && {
                format: valueFormat,
                type: 'negativeNumber',
              }),
          }
          if (shouldLockColumn(col)) {
            col.locked = true
          }
          return col
        })

        setColumnDefinitions(dynamicColumns)
        await fetchSlowdownNormsData()
      } else {
        setColumnDefinitions([])
        setTableRows([])
      }
    } catch (error) {
      console.error('Error fetching column definitions:', error)
      setColumnDefinitions([])
      setTableRows([])
    } finally {
      setIsLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, fetchSlowdownNormsData])

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })
    const ExcelName = `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}_${AOP_YEAR}_${SCREEN_NAME}`
    try {
      await ConsumptionNormsApiService.slowdownconsumptionExportMEG(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        ExcelName,
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

  const uploadSlowdownConsumptionData = async (rawFile) => {
    setLoading(true)

    try {
      let response
      response = await ConsumptionNormsApiService.ExcelSlowdownConsumptionMEG(
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
        fetchSlowdownNormsColumns()
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
        link.setAttribute('download', 'Error File - Slowdown Consumption.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchSlowdownNormsColumns()
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

  const handleExcelUpload = (rawFile) => {
    uploadSlowdownConsumptionData(rawFile)
  }

  useEffect(() => {
    if (keycloak && PLANT_ID && AOP_YEAR) {
      fetchSlowdownNormsColumns()
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, fetchSlowdownNormsColumns])

  const tablePermissions = useMemo(() => {
    const isCurrentYear = isOldYear !== 1
    const hasCalculationResults = calculationResults.length > 0

    return {
      saveBtn: isCurrentYear,
      showCalculate: isCurrentYear,
      allAction: isCurrentYear,
      showCalculateVisibility: hasCalculationResults,
      downloadExcelBtnFromUI: false,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      ExcelName: `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}_${AOP_YEAR}_${SCREEN_NAME}`,
      showTitleNameBusiness: true,
      titleName: `${SCREEN_NAME}`,
    }
  }, [isOldYear, tableRows.length, calculationResults.length])

  return (
    <div>
      <LoaderBackdrop open={!!loading} />
      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setTableRows}
        columns={columnDefinitions}
        rows={tableRows}
        saveChanges={handleSaveChanges}
        handleCalculate={handleCalculateData}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarData={setSnackbarData}
        setSnackbarOpen={setSnackbarOpen}
        fetchData={fetchSlowdownNormsColumns}
        permissions={tablePermissions}
        groupBy='Particulars'
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        handleExcelUpload={handleExcelUpload}
      />
    </div>
  )
}

export default SlowdownNormForMeg
