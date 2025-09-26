import { Box, Backdrop } from '@mui/material'
import React, { useEffect, useState, useCallback } from 'react'
import Notification from 'components/Utilities/Notification'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import { DataService } from 'services/DataService'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { Tabs, Tab } from '@mui/material'

const crackerModes = [
  { key: '5F', title: '5F' },
  { key: '4F', title: '4F' },
  { key: '4F+D', title: '4F+D' }
]

const SpyroMain = () => {
  const keycloak = useSession()
  const year = localStorage.getItem('year')
  const plantId = JSON.parse(localStorage.getItem('selectedPlant'))?.id
  const headerMap = generateHeaderNames(year)
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({ message: '', severity: 'info' })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [inputReports, setInputReports] = useState({})
  const [outputReports, setOutputReports] = useState({})

  const handleInputGridRowsChange = (modeKey, newRows) => {
    setInputReports(prev => ({
      ...prev,
      [modeKey]: {
        ...prev[modeKey],
        rows: newRows
      }
    }))
  }

  const handleOutputGridRowsChange = (modeKey, newRows) => {
    setOutputReports(prev => ({
      ...prev,
      [modeKey]: {
        ...prev[modeKey],
        rows: newRows
      }
    }))
  }

  // Only month columns and particulars/uom
  const columnDefs = [
    { field: "particulars", title: "Particulars", editable: false, widthT: 150, hidden: false },
    { field: 'uom', headerName: 'UOM', editable: false, align: 'left', headerAlign: 'left', flex: 1, widthT: 70 },
    { field: 'apr', headerName: headerMap[4], editable: false, align: 'right', headerAlign: 'left', flex: 1, format: '{0:#.#####}', type: 'number' },
    { field: 'may', headerName: headerMap[5], align: 'right', headerAlign: 'left', editable: false, format: '{0:#.#####}', type: 'number' },
    { field: 'jun', headerName: headerMap[6], editable: false, align: 'right', headerAlign: 'left', format: '{0:#.#####}', type: 'number' },
    { field: 'jul', headerName: headerMap[7], editable: false, align: 'right', headerAlign: 'left', format: '{0:#.#####}', type: 'number' },
    { field: 'aug', headerName: headerMap[8], align: 'right', headerAlign: 'left', editable: false, format: '{0:#.#####}', type: 'number' },
    { field: 'sep', headerName: headerMap[9], align: 'right', headerAlign: 'left', editable: false, format: '{0:#.#####}', type: 'number' },
    { field: 'oct', headerName: headerMap[10], align: 'right', headerAlign: 'left', editable: false, format: '{0:#.#####}', type: 'number' },
    { field: 'nov', headerName: headerMap[11], align: 'right', headerAlign: 'left', editable: false, format: '{0:#.#####}', type: 'number' },
    { field: 'dec', headerName: headerMap[12], align: 'right', headerAlign: 'left', editable: false, format: '{0:#.#####}', type: 'number' },
    { field: 'jan', headerName: headerMap[1], align: 'right', headerAlign: 'left', editable: false, format: '{0:#.#####}', type: 'number' },
    { field: 'feb', headerName: headerMap[2], align: 'right', headerAlign: 'left', editable: false, format: '{0:#.#####}', type: 'number' },
    { field: 'mar', headerName: headerMap[3], align: 'right', headerAlign: 'left', editable: false, format: '{0:#.#####}', type: 'number' },
    { field: "normParameterFKID", headerName: "normParameterFKID", filterable: false, hide: true }
  ]

  const visibleColumns = columnDefs.filter(col => !col.hide && !col.hidden)

 
  // For input
useEffect(() => {
  if (year && plantId && keycloak?.token) {
    crackerModes.forEach(({ key }) => {
      DataService.getSpyroInputData(keycloak, key, 'Feed')
        .then(apiResp => {
          let rows = apiResp?.data || []
          rows = rows.map((item, index) => ({
            ...item,
            id: index,
          }))
          setInputReports(prev => ({
            ...prev,
            [key]: { columns: visibleColumns, rows }
          }))
        })
        .catch(() => {
          setInputReports(prev => ({
            ...prev,
            [key]: { columns: visibleColumns, rows: [] }
          }))
        })
    })
  }
}, [year, plantId, keycloak])

// For output
useEffect(() => {
  if (year && plantId && keycloak?.token) {
    crackerModes.forEach(({ key }) => {
      DataService.getSpyroOutputData(keycloak, key, 'Total Products')
        .then(apiResp => {
          let rows = apiResp?.data || []
          rows = rows.map((item, index) => ({
            ...item,
            id: index,
          }))
          setOutputReports(prev => ({
            ...prev,
            [key]: { columns: visibleColumns, rows }
          }))
        })
        .catch(() => {
          setOutputReports(prev => ({
            ...prev,
            [key]: { columns: visibleColumns, rows: [] }
          }))
        })
    })
  }
}, [year, plantId, keycloak])

  const downloadExcelForConfiguration = async (mode, type) => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      let response
      if (type === 'input') {
        response = await DataService.exportSpyroInputExcel(keycloak, mode, year, plantId)
      } else if (type === 'output') {
        response = await DataService.exportSpyroOutputExcel(keycloak, mode, year, plantId)
      }

      if (response?.code === 200) {
        setSnackbarData({
          message: 'Excel download completed successfully!',
          severity: 'success',
        })
      } else {
        setSnackbarData({
          message: 'Failed to download Excel.',
          severity: 'error',
        })
      }
    } catch (error) {
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    }
  }

  return (
  <Box sx={{ width: '100%' }}>
    <Backdrop
      sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
      open={!!loading}
    />

    {/* INPUT SECTION */}
    <Box sx={{ mb: 4 }}>
      <Box sx={{ width: '100%', mb: 2 }}>
      </Box>
      <Box sx={{ mt: 2 }}>
        {crackerModes.map(({ key, title }) => {
          const rpt = inputReports[key] || {}
          return (
            <Box key={key} sx={{ mb: 2 }}>
              <KendoDataTablesReports
                columns={visibleColumns}
                rows={rpt.rows || []}
                title={title}
                permissions={{
                  textAlignment: 'center',
                  showTitle: true,
                  showTitleNameBusiness: key=== '5F', // Only show title for 5F
                  titleName:  key === '5F' ? 'Spyro Input' : '',
                  showExport: true,
                }}
                setRows={newRows => handleInputGridRowsChange(key, newRows)}
                handleExport={() => downloadExcelForConfiguration(key, 'input')}
              />
            </Box>
          )
        })}
      </Box>
    </Box>

    {/* OUTPUT SECTION */}
    <Box>
      <Box sx={{ width: '100%', mb: 2 }}>
      </Box>
      <Box sx={{ mt: 2 }}>
        {crackerModes.map(({ key, title }) => {
          const rpt = outputReports[key] || {}
          return (
            <Box key={key} sx={{ mb: 2 }}>
              <KendoDataTablesReports
                columns={visibleColumns}
                rows={rpt.rows || []}
                title={title}
                permissions={{
                  textAlignment: 'center',
                  showTitle: true,
                 showTitleNameBusiness: key === '5F', 
                 titleName: key === '5F' ? 'Spyro Output' : '', 
                  showExport: true,
                }}
                setRows={newRows => handleOutputGridRowsChange(key, newRows)}
                handleExport={() => downloadExcelForConfiguration(key, 'output')}
              />
            </Box>
          )
        })}
      </Box>
    </Box>

    <Notification
      open={snackbarOpen}
      message={snackbarData.message}
      severity={snackbarData.severity}
      onClose={() => setSnackbarOpen(false)}
    />
  </Box>
)
} 
export default SpyroMain