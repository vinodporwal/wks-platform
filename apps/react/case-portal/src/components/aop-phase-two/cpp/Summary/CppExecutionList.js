import { useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { Box } from '@mui/material'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { SummaryApiService } from '../../services/cpp/summaryApiService'
import { SvgIcon } from '@progress/kendo-react-common'
import { eyeIcon, downloadIcon } from '@progress/kendo-svg-icons'
import { Tooltip } from '@progress/kendo-react-tooltip'
import Config from 'consts/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const CppExecutionList = ({ onViewClick }) => {
  const keycloak = useSession()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const dataGridStore = useSelector((state) => state.dataGridStore)

  const { year, yearChanged } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  // Column definitions
  const columns = [
    {
      field: 'id',
      title: 'ID',
      widthT: 120,
      minWidth: 120,
      type: 'text',
      editable: false,
      hidden: true,
    },
    {
      field: 'financialYearDisplay',
      title: 'Financial Year',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
      hidden: false,
    },
    {
      field: 'executionDateTimeFormatted',
      title: 'Execution Date Time',
      widthT: 180,
      minWidth: 180,
      type: 'text',
      editable: false,
      hidden: false,
    },
    {
      field: 'status',
      title: 'Status',
      widthT: 120,
      minWidth: 120,
      type: 'text',
      editable: false,
    },
    {
      field: 'monthsSucceeded',
      title: 'Months Succeeded',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
    },
    {
      field: 'monthsFailed',
      title: 'Months Failed',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
    },
  ]

  useEffect(() => {
    fetchCppModelLogs()
  }, [yearChanged])

  const data = [
    {
      id: 'ed724e8c-6f7a-4f0a-9a92-42fc401ac2ae',
      financialYear: 2025,
      executionDateTime: 'Jan 26, 2026, 8:47:21 PM',
      status: 'Success',
      totalIterations: 102,
      totalMonthsProcessed: 12,
      totalExecutionTime: '0.00s',
      monthsSucceeded: 12,
      monthsFailed: 0,
      monthsWithWarnings: 0,
    },
  ]

  const fetchCppModelLogs = async () => {
    setLoading(true)
    try {
      const financialYear = AOP_YEAR ? parseInt(AOP_YEAR.split('-')[0]) : null
      const res = await SummaryApiService.getCppModelLogs(
        keycloak,
        financialYear,
      )
      //   const res = data
      if (res?.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }

      const formattedData = res?.map((item, index) => ({
        ...item,
        id: item?.id || index + 1,
      }))
      setRows(formattedData)
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

  // Permissions for view-only grid
  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: false,
    allAction: true,
    showExport: false,
    ExcelName: 'CPP Model Logs',
    showImport: false,
    showTitleNameBusiness: false,
    showTitle: true,
    titleName: 'CPP Model Logs',
    customActionButton: true, // Enable custom action button
  }

  // Custom action cell with view and download icons
  const CustomActionsCell = ({ dataItem }) => {
    return (
      <td style={{ textAlign: 'center', verticalAlign: 'middle' }}>
        <div
          style={{
            display: 'flex',
            gap: '10px',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          <Tooltip anchorElement='target' position='top'>
            <SvgIcon
              icon={eyeIcon}
              themeColor='primary'
              style={{ cursor: 'pointer' }}
              onClick={() => handleViewClick(dataItem)}
              title='View Details'
            />
          </Tooltip>
          <Tooltip anchorElement='target' position='top'>
            <SvgIcon
              icon={downloadIcon}
              themeColor='success'
              style={{ cursor: 'pointer' }}
              onClick={() => handleDownloadAllExcel(dataItem)}
              title='Download All Monthly Reports'
            />
          </Tooltip>
        </div>
      </td>
    )
  }

  const handleViewClick = (dataItem) => {
    if (onViewClick) {
      onViewClick(dataItem)
    } else {
      // Default behavior - you can customize this
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Viewing details for ${dataItem.financialYearDisplay || dataItem.financialYear}`,
        severity: 'info',
      })
    }
  }

  const handleDownloadAllExcel = async (dataItem) => {
    try {
      setLoading(true)

      // Download annual Excel report from parent execution
      const url = `${Config.CaseEngineUrl}/task/cpp-model-logs/month/${dataItem.id}/download-excel`

      const response = await fetch(url, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${keycloak.token}`,
        },
      })

      if (!response.ok) {
        if (response.status === 404) {
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'Annual Excel report not available for this execution',
            severity: 'warning',
          })
          return
        }
        throw new Error('Failed to download annual Excel report')
      }

      // Get filename from Content-Disposition header
      const contentDisposition = response.headers.get('Content-Disposition')
      let filename = `Annual_Balance_Summary_FY${dataItem.financialYear}.xlsx`

      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(
          /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/,
        )
        if (filenameMatch && filenameMatch[1]) {
          filename = filenameMatch[1].replace(/['"]/g, '')
        }
      }

      // Download the file
      const blob = await response.blob()
      const downloadUrl = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = downloadUrl
      a.download = filename
      document.body.appendChild(a)
      a.click()

      // Cleanup
      window.URL.revokeObjectURL(downloadUrl)
      document.body.removeChild(a)

      setSnackbarOpen(true)
      setSnackbarData({
        message: `Annual Excel report downloaded successfully`,
        severity: 'success',
      })
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download annual Excel report',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
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
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        // customHeight={40}
        paginationConfig={{
          threshold: 50,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 20,
        }}
        customActionCell={CustomActionsCell}
        READ_ONLY={true}
      />
    </Box>
  )
}

export default CppExecutionList
