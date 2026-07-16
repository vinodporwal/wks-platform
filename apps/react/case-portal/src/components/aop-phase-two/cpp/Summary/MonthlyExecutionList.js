import { useEffect, useState } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { SummaryApiService } from '../../services/cpp/summaryApiService'
import { SvgIcon } from '@progress/kendo-react-common'
import { eyeIcon, fileExcelIcon } from '@progress/kendo-svg-icons'
import { Tooltip } from '@progress/kendo-react-tooltip'
import Config from '../../../../config'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const MonthlyExecutionList = ({ executionId, onViewClick, onBack }) => {
  const keycloak = useSession()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

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
    },
    {
      field: 'month',
      title: 'Month',
      widthT: 100,
      minWidth: 100,
      type: 'text',
      editable: false,
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
      field: 'iterations',
      title: 'Iterations',
      widthT: 120,
      minWidth: 120,
      type: 'text',
      editable: false,
    },
    {
      field: 'convergenceStatus',
      title: 'Convergence Status',
      widthT: 150,
      minWidth: 150,
      type: 'text',
      editable: false,
    },
  ]

  useEffect(() => {
    if (executionId) {
      fetchMonthlyExecutionDetails()
    }
  }, [executionId])

  const fetchMonthlyExecutionDetails = async () => {
    setLoading(true)
    try {
      const res = await SummaryApiService.getMonthlyExecutionDetails(
        keycloak,
        executionId,
      )
      // const res = data
      if (res?.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }

      // Sort data in financial year order (April to March)
      // Months 4-12 come first, then months 1-3
      const sortedData = res?.sort((a, b) => {
        const monthA = a.month >= 4 ? a.month : a.month + 12
        const monthB = b.month >= 4 ? b.month : b.month + 12
        return monthA - monthB
      })

      const formattedData = sortedData?.map((item, index) => ({
        ...item,
        id: item?.id || index + 1,
      }))
      setRows(formattedData)
    } catch (error) {
      console.error('Error fetching monthly execution details:', error)
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
    ExcelName: 'Monthly Execution Details',
    showImport: false,
    showTitleNameBusiness: false,
    showTitle: true,
    titleName: 'Monthly Execution Details',
    customActionButton: true,
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
              icon={fileExcelIcon}
              themeColor='success'
              style={{ cursor: 'pointer' }}
              onClick={() => handleDownloadExcel(dataItem)}
              title='Download Excel Report'
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
      console.log('View clicked for:', dataItem)
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Viewing details for Month ${dataItem.month}`,
        severity: 'info',
      })
    }
  }

  const handleDownloadExcel = async (dataItem) => {
    try {
      setLoading(true)

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
            message: 'Excel report not available for this month',
            severity: 'warning',
          })
          return
        }
        throw new Error('Failed to download Excel report')
      }

      // Get filename from Content-Disposition header
      const contentDisposition = response.headers.get('Content-Disposition')
      let filename = `balance_summary_${dataItem.monthName || dataItem.month}_${dataItem.financialYear}.xlsx`

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
        message: `Excel report downloaded successfully`,
        severity: 'success',
      })
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel report',
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

export default MonthlyExecutionList
