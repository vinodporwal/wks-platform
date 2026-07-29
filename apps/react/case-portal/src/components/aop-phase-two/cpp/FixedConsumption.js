import { useEffect, useState, useMemo } from 'react'
import {
  Box,
  Backdrop,
  CircularProgress,
  Tooltip,
  IconButton,
} from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { UtilityPlantApiServiceV2 as JMDUtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/jmd/utilityPlantApiServiceV2'
import ValueFormatterPhaseTwo from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { validateRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import AdvanceKendoTable from '../common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import FixedConsumptionJMD from './jmd/FixedConsumptionJMD'
import FixedConsumptionDMD from './dmd/FixedConsumptionDMD'
import DeleteDialog from 'components/aop-phase-two/common/AdvanceKendoTable/components/DeleteDialog'
import AddFixedConsumptionDialog from 'components/aop-phase-two/cpp/common/AddFixedConsumptionDialog/index'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const FixedConsumption = () => {
  const keycloak = useSession()
  // State management
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
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
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Fixed_Consumption')

  const PLANT_ID_LIST = useMemo(() => (PLANT_ID ? [PLANT_ID] : []), [PLANT_ID])

  const lowerVertName = verticalObject?.name?.toLowerCase()
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const IS_CPP = lowerVertName === 'cpp'

  const headerMap = generateHeaderNames(AOP_YEAR)
  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const valueFormat = ValueFormatterPhaseTwo()

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const [addRowDialogOpen, setAddRowDialogOpen] = useState(false)
  const [editRowData, setEditRowData] = useState(null)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [rowToDelete, setRowToDelete] = useState(null)

  // Edit/Delete action cell
  const ActionCell = ({ dataItem, tdProps }) => {
    return (
      <td
        {...tdProps}
        style={{
          ...tdProps?.style,
          textAlign: 'center',
          verticalAlign: 'middle',
        }}
      >
        <Box sx={{ display: 'flex', justifyContent: 'center' }}>
          <Tooltip title='Delete Row'>
            <IconButton
              size='medium'
              color='error'
              onClick={() => {
                setRowToDelete(dataItem)
                setDeleteDialogOpen(true)
              }}
            >
              <DeleteOutlineIcon fontSize='medium' />
            </IconButton>
          </Tooltip>
        </Box>
      </td>
    )
  }

  // Column definitions
  const columns = [
    { field: 'id', title: 'ID', hidden: true },
    {
      field: 'plant',
      title: 'Plant',
      widthT: 150,
      type: 'text',
      editable: false,
      hidden: false,
    },
    {
      field: 'plantId',
      title: 'Plant ID',
      widthT: 120,
      type: 'text',
      editable: false,
      hidden: false,
    },
    {
      field: 'costCenter',
      title: 'Cost Center',
      widthT: 150,
      type: 'text',
      editable: false,
      hidden: false,
    },
    {
      field: 'costCenterId',
      title: 'Cost Center ID',
      widthT: 170,
      type: 'text',
      editable: false,
      hidden: false,
    },
    {
      field: 'cppUtility',
      title: 'CPP Utilities',
      widthT: 150,
      type: 'text',
      editable: false,
    },
    {
      field: 'cppUtilityId',
      title: 'CPP Utility IDs',
      widthT: 150,
      type: 'text',
      editable: false,
    },
    {
      field: 'cppPlant',
      title: 'CPP Plant',
      widthT: 150,
      type: 'text',
      editable: false,
    },
    {
      field: 'cppPlantId',
      title: 'CPP Plant ID',
      widthT: 150,
      type: 'text',
      editable: false,
    },
    { field: 'uom', title: 'UOM', widthT: 100, type: 'text', editable: false },
    {
      field: 'april',
      title: headerMap[4], // will be 'Apr-25' if AOP_YEAR is 2025-26
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'may',
      title: headerMap[5],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'june',
      title: headerMap[6],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'july',
      title: headerMap[7],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'aug',
      title: headerMap[8],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'sep',
      title: headerMap[9],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'oct',
      title: headerMap[10],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'nov',
      title: headerMap[11],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'dec',
      title: headerMap[12],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'jan',
      title: headerMap[1],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'feb',
      title: headerMap[2],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'mar',
      title: headerMap[3],
      editable: true,
      widthT: 100,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'total',
      title: 'Total',
      editable: false,
      widthT: 130,
      align: 'left',
      headerAlign: 'left',
      type: 'number1',
      format: valueFormat,
    },
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
    },
    {
      field: 'customActions',
      title: 'Action',
      type: 'customAction',
      minWidth: 100,
      className: 'k-text-center',
      cell: ActionCell,
      // locked: true,
      // lockPosition: 'right',
    },
  ]

  useEffect(() => {
    if (PLANT_ID_LIST?.length && AOP_YEAR && lowerSiteName == 'nmd') {
      fetchFixedConsumptionData()
      setModifiedCells({})
    }
  }, [PLANT_ID_LIST, AOP_YEAR])

  const fetchFixedConsumptionData = async () => {
    setLoading(true)
    try {
      const res = await JMDUtilityPlantApiServiceV2.getFixedConsumptionData(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      if (res?.data?.length === 0 || !res?.data) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }

      const formattedData = res.data.map((item, index) => ({
        ...item,
        total: Object.keys(item)
          .filter((key) =>
            [
              'april',
              'may',
              'june',
              'july',
              'aug',
              'sept',
              'oct',
              'nov',
              'dec',
              'jan',
              'feb',
              'mar',
            ].includes(key),
          )
          .reduce((sum, key) => sum + (parseFloat(item[key]) || 0), 0),
        remarks: item.remarks || '',
        id: item.id || index + 1,
      }))

      setRows(formattedData)
      setOriginalRows(formattedData)
    } catch (error) {
      console.error('Error fetching fixed consumption data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

  // Delete handler
  const handleConfirmDelete = async () => {
    if (!rowToDelete) return

    setDeleteDialogOpen(false)
    setLoading(true)

    try {
      if (rowToDelete.id) {
        await JMDUtilityPlantApiServiceV2.deleteFixedConsumptionRow(
          keycloak,
          rowToDelete.id,
        )
      }
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Row deleted successfully!',
        severity: 'success',
      })
      fetchFixedConsumptionData()
    } catch (error) {
      console.error('Error deleting fixed consumption row:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to delete row. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
      setRowToDelete(null)
    }
  }

  // Permissions (adjust as needed)
  const permissions = {
    showAction: true,
    addButton: true,
    addBtnName: 'Add Row',
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showExport: true,
    ExcelName: `Fixed Consumption - ${AOP_YEAR}`,
    showImport: true,
    showTitleNameBusiness: true,
    showTitle: true,
  }

  // Dummy save handler
  const saveChanges = async () => {
    setLoading(true)

    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length == 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    var rawData = Object.values(modifiedCells)
    const data = rawData.filter((row) => row.inEdit)
    if (data.length == 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    // Custom validation: If any row data is updated, remarks must be filled and different from original
    const fieldsToCheck = [
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
      'mar',
    ]
    const validationError = validateRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'plant',
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

    console.log('modifiedData', modifiedData)
    // const payload = JSON.stringify(modifiedData)
    const payload = modifiedData

    try {
      // Transform modifiedCells into the format expected by the API

      // Call the API to save changes
      const response =
        await JMDUtilityPlantApiServiceV2.saveFixedConsumptionData(
          keycloak,
          PLANT_ID_LIST,
          payload,
          AOP_YEAR,
        )
      console.log('response', response)
      // Update the local state with the saved data
      // setRows(updatedRows)
      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} rows changes!`,
        severity: 'success',
      })
      // Refresh data after import
      await fetchFixedConsumptionData()
    } catch (error) {
      console.error('Error saving plant requirement data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = async (file) => {
    if (!file) return

    setLoading(true)
    try {
      const response =
        await JMDUtilityPlantApiServiceV2.saveFixedConsumptionExcel(
          file,
          keycloak,
          PLANT_ID_LIST,
          AOP_YEAR,
        )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Excel file imported successfully!',
          severity: 'success',
        })
        // Refresh data after import
        await fetchFixedConsumptionData()
      } else if (response?.code === 400 && response?.data) {
        // Handle error response with Excel file download
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
          link.download = `Fixed_Consumption_Errors_${new Date().getTime()}.xlsx`
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          window.URL.revokeObjectURL(url)

          setSnackbarOpen(true)
          setSnackbarData({
            message:
              response?.message ||
              'Import failed with errors. Please check the downloaded file.',
            severity: 'error',
          })
          // Refresh data after import
          await fetchFixedConsumptionData()
        } catch (downloadError) {
          console.error('Error downloading error file:', downloadError)
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'Import failed but could not download error file.',
            severity: 'error',
          })
        }
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Failed to import Excel file.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error uploading Excel file:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Failed to import Excel file: ${error.message}`,
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })

    try {
      await JMDUtilityPlantApiServiceV2.exportFixedConsumptionExcel(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Fixed Consumption data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  // Handle remark cell click
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const renderBySite = () => {
    switch (lowerSiteName) {
      case 'jmd':
        return <FixedConsumptionJMD />
      case 'hmd':
      case 'dmd':
        return <FixedConsumptionDMD />
      // case 'hmd':
      //   return <FixedConsumptionHMD />
      case 'nmd':
      default:
        return (
          <>
            <AdvanceKendoTable
              columns={columns}
              rows={rows}
              setRows={setRows}
              modifiedCells={modifiedCells}
              setModifiedCells={setModifiedCells}
              // title='Fixed Consumption'
              title={screenTitle?.title}
              permissions={permissions}
              handleRemarkCellClick={handleRemarkCellClick}
              remarkDialogOpen={remarkDialogOpen}
              setRemarkDialogOpen={setRemarkDialogOpen}
              currentRemark={currentRemark}
              setCurrentRemark={setCurrentRemark}
              currentRowId={currentRowId}
              setCurrentRowId={() => {}}
              saveChanges={saveChanges}
              handleExcelUpload={handleExcelUpload}
              handleExport={handleExport}
              snackbarData={snackbarData}
              snackbarOpen={snackbarOpen}
              setSnackbarOpen={setSnackbarOpen}
              setSnackbarData={setSnackbarData}
              customHeight={80}
              groupBy='plant'
              // groupBy={['plant', 'plantId']}
              customAddRow={() => {
                setEditRowData(null)
                setAddRowDialogOpen(true)
              }}
            />

            <AddFixedConsumptionDialog
              open={addRowDialogOpen}
              onClose={() => {
                setAddRowDialogOpen(false)
                setEditRowData(null)
              }}
              onSuccess={() => fetchFixedConsumptionData()}
              editRowData={editRowData}
            />

            <DeleteDialog
              openDeleteDialogeBox={deleteDialogOpen}
              setOpenDeleteDialogeBox={setDeleteDialogOpen}
              deleteTheRecord={handleConfirmDelete}
              message='Are you sure you want to delete this row?'
              confirmButtonText='Delete'
            />
          </>
        )
    }
  }

  if (!IS_CPP) return null

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      {/* <KendoDataTables */}
      {renderBySite()}
    </Box>
  )
}

export default FixedConsumption
