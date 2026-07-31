import React, { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import { getRoleName } from 'services/role-service'
import { validateFields } from 'utils/validationUtils'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { ProductionNormsApiService } from '../../services/pcg/productionNormsApiService'

const TargetGasifierOperation = () => {
  const [rows, setRows] = useState([])
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
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_Production_Norms_Basis_Target_Gasifier_Operation_${AOP_YEAR}`

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
      widthT: 250,
      autoAdjust: false,
      type: 'text',
      editable: false,
      minWidth: 250,
    },
    {
      field: 'uom',
      title: 'UOM',
      type: 'text',
      editable: false,
      widthT: 80,
      minWidth: 100,
    },
    {
      field: 'apr',
      title: 'Min',
      editable: true,
      widthT: 100,
      type: 'number',
      minWidth: 100,
    },
    {
      field: 'may',
      title: 'Max',
      editable: true,
      widthT: 100,
      type: 'number',
      minWidth: 100,
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
    showExport: true,
    ExcelName: `Production_Norms_Target_Gasifier_Operation_${AOP_YEAR}`,
    showImport: true,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Target Gasifier Operation',
    showCalculate: false,
    calculateDisabled: true,
  }

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
      const response =
        await ProductionNormsApiService.saveTargetGasifierOperationData(
          keycloak,
          AOP_YEAR,
          payload,
          PLANT_ID,
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

      const response =
        await ProductionNormsApiService.getTargetGasifierOperationData(
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
      setRows([])
      console.error('Error fetching Cat Chem data:', error)
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
      await ProductionNormsApiService.exportTargetGasifierOperationExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        `${EXCEL_EXPORT_TITLE}.xlsx`,
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
      let response =
        await ProductionNormsApiService.importTargetGasifierOperationExcel(
          rawFile,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      // console.log('Upload response:', response)

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
        <AdvanceKendoTable
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          setRows={setRows}
          columns={NormConfigurationColumns}
          rows={rows}
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
          saveChanges={saveChanges}
          handleExcelUpload={handleExcelUpload}
          title={
            adjustedPermissionsManual.showTitle
              ? adjustedPermissionsManual.titleName
              : ''
          }
          handleExport={downloadExcelForConfiguration}
          // customHeight={60}
          // groupBy={['normTypeName']}
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

export default TargetGasifierOperation
