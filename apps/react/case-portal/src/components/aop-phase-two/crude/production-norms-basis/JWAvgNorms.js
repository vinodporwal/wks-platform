import React, { useEffect, useState } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { JWAvgNormsApiService } from 'components/aop-phase-two/services/common/jwAvgNormsApiService'
import { validateRowDataWithoutRemarks, validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import { getRoleName } from 'services/role-service'

const JWAvgNorms = () => {
  const keycloak = useSession()

  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, oldYear, verticalObject, siteObject } = dataGridStore
  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name || ''
  const VERTICAL_NAME = verticalObject?.name || ''
  const SITE_NAME = siteObject?.name || ''
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR)
  const EXCEL_NAME = `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}_${AOP_YEAR}_Job_Work_Avg_Norms`

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const MergedGroupCell = (props) => {
    const { dataItem, field, tdProps } = props
    if (!dataItem) return <td {...tdProps} />

    const getGroupId = (item) => {
      if (!item) return null
      const id =
        item.groupFkId ||
        item.GroupFkId ||
        item.groupName ||
        item.GroupName ||
        item.groupDisplayName ||
        item.GroupDisplayName
      return id && String(id).trim() !== '' ? String(id).trim() : null
    }

    const rawValue = dataItem[field]
    const displayValue =
      rawValue !== null && rawValue !== undefined ? String(rawValue) : ''

    const rowIndex =
      props.dataIndex !== undefined && props.dataIndex !== null
        ? props.dataIndex
        : rows.findIndex(
            (r) =>
              (r.id || r.materialId) === (dataItem.id || dataItem.materialId),
          )

    const currentGroupId = getGroupId(dataItem)

    // If no group, render as normal individual cell
    if (!currentGroupId || rowIndex === -1) {
      return (
        <td
          {...tdProps}
          style={{
            ...tdProps?.style,
            textAlign: field === 'materialGroup' ? 'center' : 'left',
            verticalAlign: 'middle',
          }}
          title={displayValue}
        >
          {displayValue}
        </td>
      )
    }

    // Check if previous row belongs to the same group
    const prevRow = rowIndex > 0 ? rows[rowIndex - 1] : null
    const prevGroupId = getGroupId(prevRow)

    if (prevGroupId === currentGroupId) {
      // Continuation row in the merged group -> hide cell
      return (
        <td
          {...tdProps}
          style={{
            ...tdProps?.style,
            display: 'none',
          }}
        />
      )
    }

    // First row of the group -> calculate consecutive rowSpan
    let spanCount = 1
    while (
      rowIndex + spanCount < rows.length &&
      getGroupId(rows[rowIndex + spanCount]) === currentGroupId
    ) {
      spanCount++
    }

    return (
      <td
        {...tdProps}
        rowSpan={spanCount}
        style={{
          ...tdProps?.style,
          verticalAlign: 'middle',
          textAlign: field === 'materialGroup' ? 'center' : 'left',
          backgroundColor: '#FFFFFF',
          fontWeight:
            field === 'materialGroup' && displayValue === 'YES'
              ? 600
              : 'normal',
        }}
        title={displayValue}
      >
        {displayValue}
      </td>
    )
  }

  const columns = [
    {
      field: 'plantName',
      title: 'Unit',
      widthT: 140,
      minWidth: 100,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'sapMatCode',
      title: 'SAP MAT Code',
      widthT: 180,
      minWidth: 140,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'materialDisplayName',
      title: 'Cat-Chem Material Description',
      widthT: 340,
      minWidth: 260,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'uom',
      title: 'UOM',
      widthT: 100,
      minWidth: 80,
      type: 'text',
      editable: false,
      hidden: true,
    },
    {
      field: 'value',
      title: 'JW Avg Norms',
      editable: !READ_ONLY,
      widthT: 180,
      minWidth: 140,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: '{0:0.0000}',
    },
    {
      field: 'materialGroup',
      title: 'Material Group',
      widthT: 180,
      minWidth: 140,
      type: 'customAction',
      editable: false,
      cell: MergedGroupCell,
    },
    {
      field: 'groupDisplayName',
      title: 'Group Name',
      widthT: 180,
      minWidth: 140,
      type: 'customAction',
      editable: false,
      cell: MergedGroupCell,
    },
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 250,
      minWidth: 180,
      type: 'textarea',
      editable: !READ_ONLY,
      hidden: true,
    },
  ]

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchJWAvgNormsData()
    }
  }, [PLANT_ID, AOP_YEAR])

  const fetchJWAvgNormsData = async () => {
    setLoading(true)
    try {
      const response =
        await JWAvgNormsApiService.getJobWorkAvgNormsData(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      const res = response?.data || []

      if (!res || res.length === 0) {
        setRows([])
        setOriginalRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No records found', severity: 'info' })
        return
      }

      const formattedData = res.map((item, index) => {
        const hasGroup =
          item.groupFkId ||
          item.GroupFkId ||
          (item.groupDisplayName && item.groupDisplayName.trim() !== '') ||
          (item.GroupDisplayName && item.GroupDisplayName.trim() !== '')

        return {
          ...item,
          id: item.materialId || item.UUID || item.id || `row_${index + 1}`,
          plantName: item.plantName || item.PlantName || '',
          sapMatCode: item.sapMatCode || item.SapMatCode || '',
          materialDisplayName:
            item.materialDisplayName ||
            item.MaterialDisplayName ||
            item.materialName ||
            '',
          groupFkId: item.groupFkId || item.GroupFkId || null,
          groupName: item.groupName || item.GroupName || '',
          groupDisplayName:
            item.groupDisplayName ||
            item.GroupDisplayName ||
            item.groupName ||
            item.GroupName ||
            '',
          materialGroup: hasGroup ? 'YES' : 'NO',
          uom: item.uom || item.UOM || '',
          value:
            item.value !== null && item.value !== undefined ? item.value : null,
          remarks: item.remarks || item.Remarks || '',
        }
      })

      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      console.error('Error fetching JW Avg Norms data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching JW Avg Norms data',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: !READ_ONLY,
    saveBtn: !READ_ONLY,
    allAction: true,
    showExport: true,
    ExcelName: EXCEL_NAME,
    showImport: !READ_ONLY,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Job Work Avg Norms',
  }

  const saveChanges = async () => {
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      return
    }

    const data = modifiedData.filter((row) => row.inEdit)
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      return
    }

    // Value validation: ensure updated values are not empty/invalid
    const fieldsToCheck = ['value']
    const validationError = validateRowDataWithoutRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'materialDisplayName',
    )

    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validationError,
        severity: 'error',
      })
      return
    }

    setLoading(true)

    const payload = data.map((row) => ({
      materialId: row.materialId || row.id,
      aopYear: AOP_YEAR,
      value:
        row.value !== undefined && row.value !== null && row.value !== ''
          ? parseFloat(row.value)
          : null,
      remarks: row.remarks || '',
    }))

    try {
      const response =
        await JWAvgNormsApiService.saveJobWorkAvgNormsData(
          keycloak,
          payload,
        )

      if (response?.code === 200 || response?.success) {
        setModifiedCells({})
        setSnackbarOpen(true)
        setSnackbarData({
          message:
            response?.message ||
            `Successfully saved ${payload.length} records!`,
          severity: 'success',
        })
        await fetchJWAvgNormsData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to save changes.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error saving JW Avg Norms data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExport = async () => {
    try {
      const fileName = `${EXCEL_NAME}.xlsx`
      await JWAvgNormsApiService.exportJobWorkAvgNormsExcel(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        fileName,
      )
    } catch (error) {
      console.error('Error exporting JW Avg Norms Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to export Excel file.',
        severity: 'error',
      })
    }
  }

  const handleExcelUpload = async (file) => {
    if (!file) return

    setLoading(true)
    try {
      const response = await JWAvgNormsApiService.importJobWorkAvgNormsExcel(
        file,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200 || response?.success) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        await fetchJWAvgNormsData()
      } else if (response?.code === 400 && response?.data) {
        try {
          const base64Data = response.data
          const binaryString = window.atob(base64Data)
          const bytes = new Uint8Array(binaryString.length)
          for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i)
          }
          const blob = new Blob([bytes], {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          })
          const url = window.URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = url
          link.download = `Job_Work_Avg_Norms_Errors_${Date.now()}.xlsx`
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          window.URL.revokeObjectURL(url)

          setSnackbarOpen(true)
          setSnackbarData({
            message:
              response?.message ||
              'Validation errors found. Error file downloaded.',
            severity: 'error',
          })
        } catch (downloadErr) {
          console.error('Error downloading error Excel file:', downloadErr)
          setSnackbarOpen(true)
          setSnackbarData({
            message: response?.message || 'Failed to import Excel file.',
            severity: 'error',
          })
        }
        await fetchJWAvgNormsData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to import Excel file.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error importing Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to import Excel file. Please check file format.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        saveChanges={saveChanges}
        handleExport={handleExport}
        handleExcelUpload={handleExcelUpload}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />
    </Box>
  )
}

export default JWAvgNorms
