import { useEffect, useState, useMemo, useCallback } from 'react'
import { Box, Stack, Tooltip, IconButton } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { validateNestedRowDataWithRemarks } from 'components/aop-phase-two/common/commonUtilityFunctions'
import NestedKendoTable from 'components/aop-phase-two/common/NestedKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'
import EditIcon from '@mui/icons-material/Edit'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import DeleteDialog from 'components/aop-phase-two/common/AdvanceKendoTable/components/DeleteDialog'
import {
  transformApiResponseToGridFormat,
  transformGridFormatToApiFormat,
} from './utils'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import AddAssetDialog from './components/AddAssetDialog'

const STGGrid = ({ hoursRows = [] }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, screenTitle, jmdSelectedPlants } = dataGridStore
  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Steam_Operational_HRS')
  const PLANT_ID_LIST = useMemo(
    () => jmdSelectedPlants?.map((plant) => plant.id) || [],
    [jmdSelectedPlants],
  )

  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = ValueFormatterProduction()

  const [rows, setRows] = useState([])
  const [originalRows, setOriginalRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [addAssetDialogOpen, setAddAssetDialogOpen] = useState(false)
  const [editAssetRowData, setEditAssetRowData] = useState(null)
  const [deleteAssetRowData, setDeleteAssetRowData] = useState(null)
  const [deleteAssetDialogOpen, setDeleteAssetDialogOpen] = useState(false)

  const handleEditAsset = (dataItem) => {
    setEditAssetRowData(dataItem)
    setAddAssetDialogOpen(true)
  }

  const handleDeleteAsset = (dataItem) => {
    setDeleteAssetRowData(dataItem)
    setDeleteAssetDialogOpen(true)
  }

  const AssetActionCell = ({ dataItem, tdProps }) => {
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
          <Tooltip title='Edit Asset'>
            <IconButton size='small' onClick={() => handleEditAsset(dataItem)}>
              <EditIcon fontSize='small' />
            </IconButton>
          </Tooltip>
          <Tooltip title='Delete Asset'>
            <IconButton
              size='small'
              color='error'
              onClick={() => handleDeleteAsset(dataItem)}
            >
              <DeleteOutlineIcon fontSize='small' />
            </IconButton>
          </Tooltip>
        </Box>
      </td>
    )
  }

  const MONTH_TO_INDEX = {
    april: 4,
    may: 5,
    june: 6,
    july: 7,
    aug: 8,
    sep: 9,
    oct: 10,
    nov: 11,
    dec: 12,
    jan: 1,
    feb: 2,
    march: 3,
  }
  const MONTH_FIELDS = [
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
    'march',
  ]

  const MONTH_COLUMNS = MONTH_FIELDS.map((mon) => ({
    title: headerMap[MONTH_TO_INDEX[mon]],
    children: [
      {
        field: `${mon}.shutdownHrs`,
        title: 'Shutdown Hrs',
        widthT: 150,
        minWidth: 150,
        editable: true,
        type: 'wholeNumber',
        format: valueFormat,
      },
      {
        field: `${mon}.netOperationHrs`,
        title: 'Operational Hrs',
        widthT: 150,
        minWidth: 150,
        editable: false,
        type: 'wholeNumber',
        format: valueFormat,
      },
    ],
  }))

  const nestedColumns = [
    {
      field: 'assetName',
      title: 'Asset Name',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'assetType',
      title: 'Asset Type',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
      locked: false,
      hidden: true,
    },
    {
      field: 'utilityDistributed.name',
      title: 'Utility Distributed',
      widthT: 180,
      minWidth: 180,
      type: 'text',
      editable: false,
    },
    {
      field: 'utilityDistributed.sapCode',
      title: 'Distributed SAP Code',
      widthT: 200,
      minWidth: 200,
      type: 'text',
      editable: false,
    },
    {
      field: 'utilityGenerated.name',
      title: 'Utility Generated',
      widthT: 180,
      minWidth: 180,
      type: 'text',
      editable: false,
    },
    {
      field: 'utilityGenerated.sapCode',
      title: 'Generated SAP Code',
      widthT: 200,
      minWidth: 200,
      type: 'text',
      editable: false,
    },
    ...MONTH_COLUMNS,
    {
      field: 'remarks',
      title: 'Remarks',
      width: 250,
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
      cell: AssetActionCell,
    },
  ]

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await InputApiService.getOperationHoursData(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )

      if (!res || res?.data?.SteamOperationalHours?.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        setLoading(false)
        return
      }
      const steamResponse = res?.data?.SteamOperationalHours
      const transformedData = transformApiResponseToGridFormat(
        steamResponse,
        hoursRows,
      )
      const rowsWithIds = transformedData?.map((row, index) => ({
        ...row,
        id: row.id || index + 1,
      }))

      setRows(rowsWithIds)
      setOriginalRows(rowsWithIds)
    } catch (error) {
      console.error('Error fetching power grid data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR, hoursRows])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchData()
        setModifiedCells({})
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR, fetchData],
  )

  const permissions = {
    showAction: true,
    addButton: false,
    addBtnName: 'Add Asset',
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showImport: true,
    showExport: true,
    ExcelName: EXCEL_NAME,
    showTitleNameBusiness: true,
    showTitle: false,
  }

  const saveChanges = async () => {
    setLoading(true)
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      setLoading(false)
      return
    }

    const data = modifiedData.filter((row) => row.inEdit)
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
      setLoading(false)
      return
    }

    const fieldsToCheck = [
      'april.shutdownHrs',
      'may.shutdownHrs',
      'june.shutdownHrs',
      'july.shutdownHrs',
      'aug.shutdownHrs',
      'sep.shutdownHrs',
      'oct.shutdownHrs',
      'nov.shutdownHrs',
      'dec.shutdownHrs',
      'jan.shutdownHrs',
      'feb.shutdownHrs',
      'march.shutdownHrs',
    ]
    const validationError = validateNestedRowDataWithRemarks(
      data,
      originalRows,
      fieldsToCheck,
      'assetName',
    )
    if (validationError) {
      setSnackbarOpen(true)
      setSnackbarData({ message: validationError, severity: 'error' })
      setLoading(false)
      return
    }

    const gridFormatData = modifiedData.map(({ inEdit, ...rest }) => rest)
    const apiFormatData = transformGridFormatToApiFormat(gridFormatData)
    try {
      await InputApiService.saveOperationHours(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        {
          steamResponse: apiFormatData,
        },
      )
      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      fetchData()
    } catch (error) {
      console.error('Error saving STG grid data:', error)
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
      const response = await InputApiService.saveSteamResponseExcel(
        file,
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Excel file imported successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteArray = new Uint8Array(
          Array.from(byteCharacters, (c) => c.charCodeAt(0)),
        )
        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute(
          'download',
          'Error File - STG Shutdown and Operational.xlsx',
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
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Upload Failed!', severity: 'error' })
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
    setSnackbarData({ message: 'Excel download started!', severity: 'info' })
    try {
      await InputApiService.exportSteamResponseExcel(
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
      console.error('Error exporting STG response data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const handleAddAsset = () => {
    setEditAssetRowData(null)
    setAddAssetDialogOpen(true)
  }

  const confirmDeleteAsset = async () => {
    if (!deleteAssetRowData) return
    setLoading(true)
    try {
      const assetId = deleteAssetRowData.assetFkId || deleteAssetRowData.id
      await InputApiService.deleteAsset(keycloak, assetId)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Asset deleted successfully!',
        severity: 'success',
      })
      fetchData()
    } catch (error) {
      console.error('Error deleting asset:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to delete asset. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
      setDeleteAssetDialogOpen(false)
      setDeleteAssetRowData(null)
    }
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <Stack>
        <NestedKendoTable
          columns={nestedColumns}
          rows={rows}
          setRows={setRows}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
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
          groupBy={['plantName', 'assetType']}
          hoursRows={hoursRows}
          customAddRow={handleAddAsset}
        />
      </Stack>

      <AddAssetDialog
        open={addAssetDialogOpen}
        onClose={() => {
          setAddAssetDialogOpen(false)
          setEditAssetRowData(null)
        }}
        onSuccess={() => {
          setAddAssetDialogOpen(false)
          setEditAssetRowData(null)
          fetchData()
        }}
        editRowData={editAssetRowData}
        assetCategory='Steam'
      />

      <DeleteDialog
        openDeleteDialogeBox={deleteAssetDialogOpen}
        setOpenDeleteDialogeBox={setDeleteAssetDialogOpen}
        deleteTheRecord={confirmDeleteAsset}
        confirmButtonText={'Delete'}
      />
    </Box>
  )
}

export default STGGrid
