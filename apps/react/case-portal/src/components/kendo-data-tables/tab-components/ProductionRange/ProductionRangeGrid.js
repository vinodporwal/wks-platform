import React, { useState, useEffect, useCallback } from 'react'
import { Box, Typography, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import KendoDataTables from '../../index'
import { DataService } from 'services/DataService'
import { getRoleName } from 'services/role-service'
import { RawMaterialNormsBasisApiService } from 'services/raw-material-norms-basis-api-service'
import { validateFields } from 'utils/validationUtils'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { ProductionRangeApiService } from 'services/production-range-api-service copy'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
const ProductionRange = ({ summary, summaryEdited, setSummaryEdited }) => {
  const [rows, setRows] = useState([])
  const [NormsRows, setNormsRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [modifiedNormsCells, setModifiedNormsCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [open1, setOpen1] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [calculationObject, setCalculationObject] = useState([])
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
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_Cat_Chem`

  const FORMATE_VALUE = '{0:0.000}'
  const headerMap = generateHeaderNames(AOP_YEAR)

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const NormConfigurationColumns = [
    {
      field: 'displayName', // matches API
      title: 'Particulars',
      editable: false,
      widthT: 100,
    },
    {
      field: 'uom',
      title: 'UOM',
      editable: false,
      widthT: 55,
    },
    {
      field: 'apr',
      title: 'Min',
      editable: true,
      widthT: 100,
      type: 'number',
    },
    {
      field: 'may',
      title: 'Max',
      editable: true,
      widthT: 100,
      type: 'number',
    },

    {
      field: 'remarks',
      title: 'Remark',
      editable: false,
      widthT: 105,
      type: 'string',
    },
    {
      field: 'normParameterFKId',
      title: 'idFromApi',
      filterable: 'false',
      hidden: true,
    },
  ]

  useEffect(() => {
    setModifiedCells({})
    fetchData()
  }, [oldYear, yearChanged, keycloak, PLANT_ID, AOP_YEAR])

  const saveSummary = async (summary) => {
    try {
      const response = await DataService.saveSummaryAOPConsumptionNorm(
        PLANT_ID,
        AOP_YEAR,
        summary,
        keycloak,
      )

      if (response?.code == 200) {
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
      } else {
        setSnackbarData({
          message: 'Saved Failed!',
          severity: 'error',
        })
      }
      return response
    } catch (error) {
      console.error('Error saving Summary!', error)
    } finally {
      setLoading(false)
    }
  }
  const handleCalculate = async () => {
    setRows([])
    setLoading(true)
    try {
      var data =
        await RawMaterialNormsBasisApiService.handleCalculateNormsConfiguration(
          PLANT_ID,
          AOP_YEAR,
          keycloak,
        )

      if (data == 0 || data) {
        // dispatch(setIsBlocked(true))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchNormsConfigurationManualData()
        fetchNormsConfigurationCalculatedData()
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
    }
  }

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissionsManual = getAdjustedPermissions(
    {
      showAction: true,
      saveWithRemark: true,
      saveBtn: true,
      allAction: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      showTitleNameBusiness: true,
      showCalculate: false,
      //Object.keys(calculationObject || {}).length > 0 ? true : false,
      titleName: 'Production Range',
      showCalculateVisibility: true,
    },
    IS_OLD_YEAR,
  )
  const adjustedPermissionsCalculated = getAdjustedPermissions(
    {
      showAction: true,
      saveWithRemark: true,
      saveBtn: false,
      allAction: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      showTitleNameBusiness: true,
      titleName: 'Norms Configuration - Calculated',
      showCalculate: false,
      showCalculateVisibility: false,
    },
    IS_OLD_YEAR,
  )

  const handleUpdate = async (updatedRows) => {
    setLoading(true)
    try {
      let payload = updatedRows?.map((row) => {
        const { id, inEdit, particulars, originalRemark, ...rest } = row

        return {
          normParameterFKId: row.normParameterFkId,
          apr: row.apr,
          may: row.may,
          remarks: row.remarks,
          auditYear: row.auditYear,
          uom: row.uom,
          normTypeName: row.normTypeName,
          isEditable: row.isEditable,
          displayName: row.displayName,
          type: row.type ?? '',
        }
      })

      // console.log('payload', payload)
      const response = await ProductionRangeApiService.postData(
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
        if (summaryEdited) {
          await saveSummary(summary)
          setModifiedCells({})
          setSummaryEdited(false)
        }
        return
      }

      const rawData = Object.values(modifiedCells)
      const data = rawData.filter((row) => row.inEdit)

      if (data.length === 0) {
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
  }, [modifiedCells, summaryEdited, summary])

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setModifiedCells({})

    try {
      setLoading(true)

      const response = await ProductionRangeApiService.getData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const formattedData = response?.data?.productionRangeList?.map(
        (row, index) => ({
          ...row,
          id: row.id || index,
          particulars: row.displayName,

          originalRemark: row.remarks || '',
          normParameterFKId: row.normParameterFKId,
          auditYear: row.auditYear,
          normTypeName: row.normTypeName,
          isEditable: row.isEditable,
          displayName: row.displayName,
          type: row.type,
        }),
      )

      setRows(formattedData || [])
    } catch (error) {
      console.error('Error fetching Cat Chem data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching data',
        severity: 'error',
      })
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
      await ProductionRangeApiService.getProductionRangeExcel(
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
    uploadProductionRange(rawFile)
  }
  const uploadProductionRange = async (rawFile) => {
    setLoading(true)

    try {
      let response = await ProductionRangeApiService.productionRangeImport(
        rawFile,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      console.log('Upload response:', response)

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
        link.setAttribute('download', 'Error File - Production Range.xlsx')
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
        <KendoDataTables
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          setRows={setRows}
          columns={NormConfigurationColumns}
          rows={rows}
          paginationOptions={[100, 200, 300]}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          apiRef={apiRef}
          open1={open1}
          setOpen1={setOpen1}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          handleRemarkCellClick={handleRemarkCellClick}
          handleCalculate={handleCalculate}
          fetchData={fetchData}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          permissions={adjustedPermissionsManual}
          summaryEdited={summaryEdited}
          groupBy={'normTypeName'}
          saveChanges={saveChanges}
          downloadExcelForConfiguration={() => downloadExcelForConfiguration()}
          handleExcelUpload={handleExcelUpload}
        />
      </Box>
    </Box>
  )
}

export default ProductionRange
