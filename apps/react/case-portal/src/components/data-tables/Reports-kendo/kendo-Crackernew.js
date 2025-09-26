import { Box, Backdrop } from '@mui/material'
import React, { useEffect, useState, useCallback } from 'react'
import Notification from 'components/Utilities/Notification'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import { DataService } from 'services/DataService'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { Tabs, Tab } from '@mui/material'
//import { format } from '../../../../node_modules/@progress/kendo-intl/dist/npm/format'
import { format } from '@progress/kendo-intl'
const crackerModes = [
  { key: '5F', title: '5F' },
  { key: '4F', title: '4F' },
  { key: '4F+D', title: '4F+D' }
]

const  SpyroData = () => {
  const keycloak = useSession()
  const year = localStorage.getItem('year')
  const plantId = JSON.parse(localStorage.getItem('selectedPlant'))?.id
  const headerMap = generateHeaderNames(year)
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({ message: '', severity: 'info' })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [inputReports, setInputReports] = useState({})
  const [outputReports, setOutputReports] = useState({})
  
  // Separate state for input and output tabs
  const [inputTabIndex, setInputTabIndex] = useState(0)
  const [outputTabIndex, setOutputTabIndex] = useState(0)
  
  const [availableTabs, setAvailableTabs] = useState([])
  const [outputTabObjects, setOutputTabObjects] = useState([])

  const inputTabs = [
    'Feed',
    'Composition',
    'Optimizing',
    'Hydrogenation',
    'Recovery',
    'Furnace',
    'Constant'
  ]

  const outputTabs = [
    'Total Feed',
    'Total Products',
    'Miscellaneous Parameters',
    'Yield'
  ]

  // Get current selected tabs
  const currentInputTab = inputTabs[inputTabIndex]
  const currentOutputTab = outputTabObjects[outputTabIndex]?.name || (outputTabObjects.length > 0 ? outputTabObjects[0].name : 'Yield')

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

  // Define your columns here (use headerMap for month columns)
  const columnDefs = [
    {
      field: "particulars",
      title: "Particulars",
      editable: false,
      widthT: 150,
      hidden: false
    },
    {
      field: 'uom',
      headerName: 'UOM',
      editable: false,
      align: 'left',
      headerAlign: 'left',
      flex: 1,
      widthT: 70,
    },
    {
      field: 'apr',
      headerName: headerMap[4],
      editable: false,
      align: 'right',
      headerAlign: 'left',
      flex: 1,
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'may',
      headerName: headerMap[5],
      align: 'right',
      headerAlign: 'left',
      editable: false,
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'jun',
      headerName: headerMap[6],
      editable: false,
      align: 'right',
      headerAlign: 'left',
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'jul',
      headerName: headerMap[7],
      editable: false,
      align: 'right',
      headerAlign: 'left',
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'aug',
      headerName: headerMap[8],
      align: 'right',
      headerAlign: 'left',
      editable: false,
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'sep',
      headerName: headerMap[9],
      align: 'right',
      headerAlign: 'left',
      editable: false,
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'oct',
      headerName: headerMap[10],
      align: 'right',
      headerAlign: 'left',
      editable: false,
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'nov',
      headerName: headerMap[11],
      align: 'right',
      headerAlign: 'left',
      editable: false,
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'dec',
      headerName: headerMap[12],
      align: 'right',
      headerAlign: 'left',
      editable: false,
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'jan',
      headerName: headerMap[1],
      align: 'right',
      headerAlign: 'left',
      editable: false,
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'feb',
      headerName: headerMap[2],
      align: 'right',
      headerAlign: 'left',
      editable: false,
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: 'mar',
      headerName: headerMap[3],
      align: 'right',
      headerAlign: 'left',
      editable: false,
      format: '{0:#.##}',
      type: 'number',
    },
    {
      field: "normParameterFKID",
      headerName: "normParameterFKID",
      filterable: false,
      hide: true
    }
  ]

  const yieldColumnDefs = [
    { field: "particulars", title: "Particulars", editable: false, hidden: false },
    { field: "5F_C2C3", title: "5F-C2C3", editable: false, type: "number", format: '{0:#.##}' },
    { field: "5F_Propane", title: "5F-Propane", editable: false, type: "number", format: '{0:#.##}' },
    { field: "5F_Ethane", title: "5F-Ethane", editable: false, type: "number", format: '{0:#.##}' },
    { field: "4F_C2C3", title: "4F-C2C3", editable: false, type: "number", format: '{0:#.##}' },
    { field: "4F_Propane", title: "4F-Propane", editable: false, type: "number", format: '{0:#.##}' },
    { field: "4F_Ethane", title: "4F-Ethane", editable: false, type: "number", format: '{0:#.##}' },
    { field: "4FD_C2C3", title: "4FD-C2C3", editable: false, type: "number", format: '{0:#.##}' },
    { field: "4FD_Propane", title: "4FD-Propane", editable: false, type: "number", format: '{0:#.##}' },
    { field: "4FD_Ethane", title: "4FD-Ethane", editable: false, type: "number", format: '{0:#.##}' },
    { field: "NormParameterFKID", title: "NormParameterFKID", filterable: false, hidden: true },
    { field: "id", hidden: true }
  ]

  const constantColumnDefs = [
    { field: "DisplayName", title: "Particulars", editable: false, widthT: 220, hidden: false },
    { field: "UOM", title: "UOM", editable: false, widthT: 100 },
    { field: "ConstantValue", title: "Value", editable: true, type: "number" },
    { field: "remarks", title: "Remark", editable: false, widthT: 220, type: "string" },
    { field: "normParameterFKID", title: "normParameterFKID", filterable: false, hidden: true }
  ]

  const visibleColumns = columnDefs.filter(col => !col.hide && !col.hidden)

  const fetchCrackerRowsYield = useCallback(
    async () => {
      setLoading(true)
      try {
        const apiResp = await DataService.getSpyroOutputDataYield(keycloak, plantId, year)
        let transformedData = []
        if (apiResp && Array.isArray(apiResp.data)) {
          const rowMap = {}
          apiResp.data.forEach((item, i) => {
            const comp = item.displayName
            const col = `${item.operation}_${item.type}`
            if (!rowMap[comp]) {
              rowMap[comp] = {
                id: `row_${i}`,
                particulars: comp,
                uom: item.uom,
                remarks: item.remarks,
                normParameterFKID: item.normParameterId,
                '5F_C2C3': null,
                '5F_Propane': null,
                '5F_Ethane': null,
                '4F_C2C3': null,
                '4F_Propane': null,
                '4F_Ethane': null,
                '4FD_C2C3': null,
                '4FD_Propane': null,
                '4FD_Ethane': null,
              }
            }
            rowMap[comp][col] = item.attributeValue
          })
          transformedData = Object.values(rowMap)
        }
        setOutputReports(prev => ({ ...prev, Yield: { columns: visibleColumns, rows: transformedData } }))
      } catch (err) {
        setOutputReports(prev => ({ ...prev, Yield: { columns: visibleColumns, rows: [] } }))
        setSnackbarData({
          message: `Failed to load Yield data. Please try again.`,
          severity: 'error',
        })
        setSnackbarOpen(true)
      } finally {
        setLoading(false)
      }
    },
    [keycloak, plantId, year, visibleColumns]
  )

  // Load data for input tabs
  const loadInputData = async (tabName) => {
    setLoading(true)
    const out = {}
    await Promise.all(
      crackerModes.map(async ({ key }) => {
        try {
          const apiResp = await DataService.getSpyroInputData(keycloak, key, tabName)
          let rows = apiResp?.data || []
          rows = rows.map((item, index) => ({
            ...item,
            id: index,
          }))
          out[key] = { columns: visibleColumns, rows }
        } catch (err) {
          out[key] = { columns: visibleColumns, rows: [] }
        }
      })
    )
    setInputReports(prev => ({ ...prev, ...out }))
    setLoading(false)
  }

  // Load data for output tabs
  const loadOutputData = async (tabName) => {
    if (tabName === 'Yield') {
      fetchCrackerRowsYield()
      return
    }
    
    setLoading(true)
    const out = {}
    
    // Debug: Log the tab name being requested
    console.log('Loading output data for tab:', tabName)
    
    await Promise.all(
      crackerModes.map(async ({ key }) => {
        try {
          const apiResp = await DataService.getSpyroOutputData(keycloak, key, tabName)
          console.log(`API Response for ${key} - ${tabName}:`, apiResp)
          
          let rows = apiResp?.data || []
          rows = rows.map((item, index) => ({
            ...item,
            id: index,
          }))
          out[key] = { columns: visibleColumns, rows }
        } catch (err) {
          console.error(`Error loading data for ${key} - ${tabName}:`, err)
          out[key] = { columns: visibleColumns, rows: [] }
        }
      })
    )
    
    console.log('Setting output reports:', out)
    setOutputReports(prev => ({ ...prev, ...out }))
    setLoading(false)
  }

  useEffect(() => {
    const fetchAvailableTabs = async () => {
      try {
        const resp = await DataService.getConfigurationAvailableTabs(keycloak)
        if (
          resp?.code === 200 &&
          Array.isArray(resp.data?.configurationTypeList)
        ) {
          setAvailableTabs(resp.data.configurationTypeList)
        } else {
          setAvailableTabs(
            inputTabs.map((t) => ({
              id: t,
              displayName: t.charAt(0).toUpperCase() + t.slice(1),
            }))
          )
        }
      } catch (err) {
        setAvailableTabs(
          inputTabs.map((t) => ({
            id: t,
            displayName: t.charAt(0).toUpperCase() + t.slice(1),
          }))
        )
      }
    }
    fetchAvailableTabs()
  }, [keycloak])

  useEffect(() => {
    const fetchOutputTabs = async () => {
      try {
        const resp = await DataService.getConfigurationAvailableTabs(keycloak)
        if (
          resp?.code === 200 &&
          Array.isArray(resp.data?.configurationTypeList)
        ) {
          // Get all output tabs from backend
          const backendOutputTabs = resp.data.configurationTypeList
            .filter(tab => outputTabs.includes(tab.name) || tab.name?.toLowerCase() === 'yield')
            .filter((tab, idx, arr) =>
              arr.findIndex(t => t.name === tab.name) === idx
            )
          
          // Add Yield if not present in backend response
          const hasYield = backendOutputTabs.some(tab => tab.name?.toLowerCase() === 'yield')
          if (!hasYield) {
            backendOutputTabs.push({ id: 'yield', name: 'Yield', displayName: 'Yield' })
          }
          
          setOutputTabObjects(backendOutputTabs)
        } else {
          setOutputTabObjects([
            { id: 'yield', name: 'Yield', displayName: 'Yield' }
          ])
        }
      } catch (err) {
        setOutputTabObjects([
          { id: 'yield', name: 'Yield', displayName: 'Yield' }
        ])
      }
    }
    fetchOutputTabs()
  }, [keycloak])

  // Load input data when input tab changes
  useEffect(() => {
    if (year && plantId && keycloak?.token && currentInputTab) {
      loadInputData(currentInputTab)
    }
  }, [year, plantId, keycloak, inputTabIndex])

  // Load output data when output tab changes
  useEffect(() => {
    if (year && plantId && keycloak?.token && currentOutputTab && outputTabObjects.length > 0) {
      loadOutputData(currentOutputTab)
    }
  }, [year, plantId, keycloak, outputTabIndex, outputTabObjects])

  const downloadExcelForConfiguration = async (mode, type) => {
  setSnackbarOpen(true)
  setSnackbarData({
    message: 'Excel download started!',
    severity: 'success',
  })

  const year = localStorage.getItem('year')
  const plantId = JSON.parse(localStorage.getItem('selectedPlant'))?.id

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
    console.error('Error downloading Excel:', error)
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
        {/* Input Tabs */}
        <Box sx={{ overflowX: 'auto', width: '100%' }}>
          <Tabs
            value={inputTabIndex}
            onChange={(e, newIndex) => setInputTabIndex(newIndex)}
            textColor="primary"
            indicatorColor="primary"
            sx={{
              borderBottom: '0px solid #ccc',
              '.MuiTabs-indicator': { display: 'none' },
            }}
          >
            {inputTabs.map((tabId, index) => {
              const info = availableTabs.find(
                (t) => t.name?.toLowerCase() === tabId.toLowerCase() || t.id?.toLowerCase() === tabId.toLowerCase()
              )
              const label = info?.displayName || tabId
              return (
                <Tab
                  key={tabId}
                  label={label}
                  sx={{
                    border: '1px solid #ADD8E6',
                    borderBottom: '1px solid #ADD8E6',
                    padding: '9px',
                    minHeight: '10px',
                  }}
                />
              )
            })}
          </Tabs>
        </Box>

        {/* Input Grids */}
        <Box sx={{ mt: 2 }}>
          {currentInputTab === 'Constant' ? (
            <KendoDataTablesReports
              columns={constantColumnDefs.filter(col => !col.hidden)}
              rows={inputReports.Constant?.rows || []}
              title="Constant"
              permissions={{
                textAlignment: 'center',
                showTitle: true,
              }}
              setRows={newRows => handleInputGridRowsChange('Constant', newRows)}
            />
          ) : (
            crackerModes.map(({ key, title }) => {
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
                      showExport: true,
                    }}
                    setRows={newRows => handleInputGridRowsChange(key, newRows)}
                    handleExport={() => downloadExcelForConfiguration(key, 'input')}
                  />
                </Box>
              )
            })
          )}
        </Box>
      </Box>

      {/* OUTPUT SECTION */}
      <Box>
        {/* Output Tabs */}
        <Box sx={{ overflowX: 'auto', width: '100%' }}>
          <Tabs
            value={outputTabIndex}
            onChange={(e, newIndex) => setOutputTabIndex(newIndex)}
            textColor="primary"
            indicatorColor="primary"
            sx={{
              borderBottom: '0px solid #ccc',
              '.MuiTabs-indicator': { display: 'none' },
            }}
          >
            {outputTabObjects.map((tab, idx) => (
              <Tab
                key={tab.id}
                label={tab.displayName}
                sx={{
                  border: '1px solid #ADD8E6',
                  borderBottom: '1px solid #ADD8E6',
                  padding: '9px',
                  minHeight: '10px',
                }}
              />
            ))}
          </Tabs>
        </Box>

        {/* Output Grids */}
        <Box sx={{ mt: 2 }}>
          {currentOutputTab === 'Yield' ? (
            <KendoDataTablesReports
              columns={yieldColumnDefs.filter(col => !col.hidden)}
              rows={outputReports.Yield?.rows || []}
              title="Yield"
              permissions={{
                textAlignment: 'center',
                showTitle: true,
                showExport: false,
              }}
              setRows={() => {}}
            />
          ) : (
            crackerModes.map(({ key, title }) => {
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
                      showExport: true,
                    }}
                    setRows={newRows => handleOutputGridRowsChange(key, newRows)}
                    handleExport={() => downloadExcelForConfiguration(key, 'output')}
                  />
                </Box>
              )
            })
          )}
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
export default SpyroData