import React, { useEffect, useState, useCallback } from 'react'
import KendoDataTables from './index'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { ReliabilityPerformancePlantWiseFunctionalApiService } from 'services/reliability-performance-plant-wise-functional-api-service'
import { validateFields } from 'utils/validationUtils'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

export default function RelPerfPlantWise() {
  const keycloak = useSession()

  const [loading, setLoading] = useState(false)
  const FORMATE_DECIMAL = ValueFormatterProduction()

  const [
    snackbarOpenReliabilityPerformance,
    setSnackbarOpenReliabilityPerformance,
  ] = useState(false)
  const [OpenReliabilityPerformance, setOpenReliabilityPerformance] =
    useState(false)

  const [
    snackbarDataReliabilityPerformance,
    setSnackbarDataReliabilityPerformance,
  ] = useState({
    message: '',
    severity: 'info',
  })

  const dataGridStore = useSelector((state) => state.dataGridStore)

  const {
    yearChanged,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
    isReleased,
  } = dataGridStore

  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const [startYear, endYear] = AOP_YEAR ? AOP_YEAR.split('-') : ['', '']

  const VERTICAL_NAME = verticalObject?.name?.toUpperCase()
  const SITE_NAME = siteObject?.name?.toUpperCase()
  const PLANT_NAME = plantObject?.name?.toUpperCase()

  const [reliabilityRows, setReliabilityRows] = useState([])
  const [modifiedReliabilityCells, setModifiedReliabilityCells] = useState({})
  const [remarkDialogOpenReliability, setRemarkDialogOpenReliability] =
    useState(false)
  const [currentRemarkReliability, setCurrentRemarkReliability] = useState('')
  const [currentRowIdReliability, setCurrentRowIdReliability] = useState(null)

  const reliabilityPerformanceColumns = [
    {
      field: 'rowNo',
      title: 'S.No.',
      widthT: 70,
      editable: false,
      type: 'number',
      minWidth: 70,
    },
    { field: 'parameter', title: 'Parameter', editable: false, minWidth: 100 },
    { field: 'uom', title: 'UOM', editable: false, widthT: 70, minWidth: 100 },
    {
      field: 'bestAchieved',
      title: 'Best Achieved',
      editable: true,
      type: 'numberWithUOMValidation',
      format: FORMATE_DECIMAL,
      minWidth: 100,
    },
    {
      field: 'aop',
      title: `FY${startYear ? startYear.slice(-2) : ''} AOP`,
      editable: true,
      type: 'numberWithUOMValidation',
      format: FORMATE_DECIMAL,
      minWidth: 100,
    },
    {
      field: 'actuals',
      title: `FY${startYear ? startYear.slice(-2) : ''} Actual`,
      editable: true,
      type: 'numberWithUOMValidation',
      format: FORMATE_DECIMAL,
      minWidth: 100,
    },
    {
      field: 'plann',
      title: `FY${endYear || ''} Plan`,
      editable: true,
      type: 'numberWithUOMValidation',
      format: FORMATE_DECIMAL,
      minWidth: 100,
    },
    { field: 'limit', title: 'Limit', editable: true, minWidth: 100 },
    {
      field: 'rationale',
      title: 'Rationale / Reasons for Changes',
      editable: true,
      minWidth: 100,
    },
    {
      field: 'remarks',
      title: 'Remarks',
      editable: true,
      minWidth: 100,
    },
  ]

  const gridPermissions = {
    saveBtn: true,
    allAction: true,
    showTitleNameBusiness: true,
    adjustedPermissions: true,
  }

  const handleRemarkCellClickReliabilityPerformance = (dataItem) => {
    if (READ_ONLY) return
    setCurrentRemarkReliability(dataItem.remarks || '')
    setCurrentRowIdReliability(dataItem.id)
    setRemarkDialogOpenReliability(true)
  }

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !SITE_ID || !VERTICAL_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      var data =
        await ReliabilityPerformancePlantWiseFunctionalApiService.getReliabilityPerformancePlantWise(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          'Reliability Performance',
        )

      const processedData1 = data.data.map((item, index) => ({
        ...item,
        id: item?.id || index,
        idFromAPI: item?.id,
        rowNo: index + 1,
        actuals: item.actual,
        originalRemark: item?.remarks || '',
        isEditable: true,
        currentPlanEditable:
          item?.isEditable === true || item?.isEditable === 'true',
      }))

      setReliabilityRows(processedData1)
      setLoading(false)
    } catch (error) {
      console.error('Error fetching Reliability Performance data:', error)
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, SITE_ID, VERTICAL_ID, AOP_YEAR])

  useEffect(() => {
    fetchData()
  }, [PLANT_ID, oldYear, yearChanged, keycloak, fetchData])

  const saveReliabilityPerformance = async (newRows) => {
    try {
      const payloadData = newRows.map((row) => ({
        actual: row?.actuals,
        aop: row?.aop,
        bestAchieved: row?.bestAchieved,
        id: row?.idFromAPI || null,
        limit: row?.limit,
        plann: row?.plann,
        rationale: row?.rationale,
        remarks: row?.remarks,
        reportType: row?.reportType,
        masterId: row?.masterId,
        aopYear: row?.aopYear,
        plantId: row?.plantId,
        isEditable: row?.isEditable,
      }))

      const response =
        await ReliabilityPerformancePlantWiseFunctionalApiService.saveReliabilityPerformancePlantWise(
          payloadData,
          keycloak,
        )

      setSnackbarOpenReliabilityPerformance(true)
      setSnackbarDataReliabilityPerformance({
        message: 'Saved Successfully!',
        severity: 'success',
      })
      setModifiedReliabilityCells({})

      fetchData()
      return response
    } catch (error) {
      console.error('Error in saving data!', error)
    }
  }

  const saveChangesReliabilityPerformance = useCallback(async () => {
    setLoading(true)

    try {
      if (Object.keys(modifiedReliabilityCells).length === 0) {
        setSnackbarOpenReliabilityPerformance(true)
        setSnackbarDataReliabilityPerformance({
          message: 'No Records to Save!',
          severity: 'info',
        })
        setLoading(false)
        return
      }

      var rawData = Object.values(modifiedReliabilityCells)
      const data = rawData.filter((row) => row.inEdit)
      if (data.length === 0) {
        setSnackbarOpenReliabilityPerformance(true)
        setSnackbarDataReliabilityPerformance({
          message: 'No Records to Save!',
          severity: 'info',
        })
        setLoading(false)
        return
      }

      const requiredFields = ['remarks']
      const validationMessage = validateFields(data, requiredFields)

      if (validationMessage) {
        setSnackbarOpenReliabilityPerformance(true)
        setSnackbarDataReliabilityPerformance({
          message: validationMessage,
          severity: 'error',
        })
        setLoading(false)
        return
      }
      await saveReliabilityPerformance(data)
    } catch (error) {
      console.log('Error saving changes:', error)
    }
  }, [modifiedReliabilityCells, fetchData])

  const exportReliabilityExcel = async (
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    excelName,
  ) => {
    setSnackbarOpenReliabilityPerformance(true)
    setSnackbarDataReliabilityPerformance({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      await ReliabilityPerformancePlantWiseFunctionalApiService.exportReliabilityExcelPlantWise(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        excelName,
      )

      setSnackbarOpenReliabilityPerformance(true)
      setSnackbarDataReliabilityPerformance({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarOpenReliabilityPerformance(true)

      setSnackbarDataReliabilityPerformance({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    }
  }

  const saveReliabilityPerformanceExcelFile = async (rawFile) => {
    setLoading(true)
    try {
      let response

      response =
        await ReliabilityPerformancePlantWiseFunctionalApiService.importReliabilityPerformanceExcelPlantWise(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          rawFile,
        )

      if (response?.code === 200) {
        setSnackbarOpenReliabilityPerformance(true)
        setSnackbarDataReliabilityPerformance({
          message: 'Uploaded Successfully!',
          severity: 'success',
        })

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
          'Error File - Reliability Performance.xlsx',
        )
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpenReliabilityPerformance(true)
        setSnackbarDataReliabilityPerformance({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchData()
      } else {
        // setSnackbarOpen(true)
      }

      return response
    } catch (error) {
      console.error('Error uploading Reliability Performance Excel:', error)
    } finally {
      setLoading(false)
      fetchData()
    }
  }

  const handleExcelUpload = (rawFile) => {
    saveReliabilityPerformanceExcelFile(rawFile)
  }
  const handleLoad = async () => {
    setLoading(true)
    try {
      const data =
        await ReliabilityPerformancePlantWiseFunctionalApiService.handleLoadReliabilityPlantwise(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      if (data || data == 0) {
        setSnackbarOpenReliabilityPerformance(true)
        setSnackbarDataReliabilityPerformance({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        await fetchData()
      } else {
        setSnackbarOpenReliabilityPerformance(true)
        setSnackbarDataReliabilityPerformance({
          message: 'Data Refresh Falied!',
          severity: 'error',
        })
      }

      return data
    } catch (error) {
      setSnackbarOpenReliabilityPerformance(true)
      setSnackbarDataReliabilityPerformance({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
      console.error('Error!', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <LoaderBackdrop open={!!loading} />

      {/* Reliability Performance Grid */}
      <KendoDataTables
        rows={reliabilityRows}
        setRows={setReliabilityRows}
        title='Reliability Performance (Plant)'
        modifiedCells={modifiedReliabilityCells}
        setModifiedCells={setModifiedReliabilityCells}
        remarkDialogOpen={remarkDialogOpenReliability}
        setRemarkDialogOpen={setRemarkDialogOpenReliability}
        currentRemark={currentRemarkReliability}
        setCurrentRemark={setCurrentRemarkReliability}
        currentRowId={currentRowIdReliability}
        setCurrentRowId={setCurrentRowIdReliability}
        snackbarData={snackbarDataReliabilityPerformance}
        snackbarOpen={snackbarOpenReliabilityPerformance}
        setSnackbarOpen={setSnackbarOpenReliabilityPerformance}
        setSnackbarData={setSnackbarDataReliabilityPerformance}
        setOpenReliabilityPerformance={setOpenReliabilityPerformance}
        handleRemarkCellClick={handleRemarkCellClickReliabilityPerformance}
        OpenReliabilityPerformance={OpenReliabilityPerformance}
        permissions={{
          ...gridPermissions,
          titleName: 'Reliability Performance (Plant)',
          ExcelName: `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}_${AOP_YEAR}_Reliability_Performance`,
          downloadExcelBtn: true,
          uploadExcelBtn: true,
          showLoadBtn: true,
        }}
        columns={reliabilityPerformanceColumns}
        saveChanges={saveChangesReliabilityPerformance}
        handleLoad={handleLoad}
        downloadExcelForConfiguration={() =>
          exportReliabilityExcel(
            keycloak,
            PLANT_ID,
            AOP_YEAR,
            'Reliability_Performance',
          )
        }
        handleExcelUpload={handleExcelUpload}
      />
    </>
  )
}
