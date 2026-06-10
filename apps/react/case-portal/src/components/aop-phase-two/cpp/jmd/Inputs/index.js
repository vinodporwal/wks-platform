import { useEffect, useState, useCallback } from 'react'
import { Box, Stack } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ImportPower from './ImportPower'
import AssetCapacity from './AssetCapacity/index'
import ShutdownAndOperational from './ShutdownAndOperational/index'
import { generateMockData } from './InputUtility'
import ExportAvailability from './ExportAvailability'
import HeatRate from './HeatRate/index'
import FixedNorms from './FixedNorms'
import Fuel from './Fuel/index'
import AopDesignBasis from './AopDesignBasis'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AopTabs from 'components/AopTabs'
import AssetAvailability from './AssetAvailability/index'

const InputsJMD = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear

  // State management
  const [tabObj, setTabObj] = useState([])
  const [tabIndex, setTabIndex] = useState(0)
  const [tabsData, setTabsData] = useState({})
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [loading, setLoading] = useState(false)

  // Initialize tabs
  useEffect(() => {
    const tabs = [
      {
        id: 'aop-design-basis',
        name: 'aopDesignBasis',
        displayName: 'AOP Design Basis',
        displaySequence: 0,
      },
      {
        id: 'purchase-power',
        name: 'purchasePowerInput',
        displayName: 'Purchase Power Input',
        displaySequence: 1,
      },
      {
        id: 'shutdown-operational',
        name: 'shutdownOperationalHrs',
        displayName: 'Shutdown and Operational Hrs.',
        displaySequence: 2,
      },
      {
        id: 'asset-priority',
        name: 'assetPriority',
        displayName: 'Asset Priority',
        displaySequence: 3,
      },
      {
        id: 'asset-capacity',
        name: 'assetCapacity',
        displayName: 'Asset Capacity',
        displaySequence: 4,
      },
      {
        id: 'heat-rate',
        name: 'heatRate',
        displayName: 'Heat Rate',
        displaySequence: 5,
      },
      {
        id: 'fixed-norms',
        name: 'Norms',
        displayName: 'Norms',
        displaySequence: 6,
      },
      {
        id: 'fuel-availability',
        name: 'fuelAvailability',
        displayName: 'Fuel Availability',
        displaySequence: 7,
      },
      // { id: 'export-availability',name:'exportAvailability', displayName: 'Export Availability', displaySequence: 6 },
    ]
    setTabObj(tabs)
  }, [])

  // Get current tab
  const currentTab = tabObj[tabIndex] || {}

  // Store data for any tab dynamically
  const setRowsForTab = useCallback((tabId, data) => {
    setTabsData((prev) => ({
      ...prev,
      [tabId]: data,
    }))
  }, [])

  // Fetch data for current tab
  const fetchTabData = useCallback(
    async (tabId) => {
      if (!tabId) return
      try {
        setLoading(true)
        let transformedData = []

        // Mock data for demonstration - replace with actual API call
        const mockData = generateMockData(tabId)
        transformedData = mockData.map((item, index) => ({
          id: item.id || `row_${index}`,
          ...item,
        }))

        setRowsForTab(tabId, transformedData)
      } catch (err) {
        setSnackbarData({
          message: `Failed to load data. Please try again.`,
          severity: 'error',
        })
        setSnackbarOpen(true)
        setRowsForTab(tabId, [])
      } finally {
        setLoading(false)
      }
    },
    [setRowsForTab],
  )

  // Load data when tab changes
  useEffect(() => {
    if (currentTab.id) {
      fetchTabData(currentTab.id)
    }
  }, [tabIndex, currentTab.id, fetchTabData])

  // Render tab content based on tab ID
  const renderTabContent = () => {
    switch (currentTab.id) {
      case 'aop-design-basis':
        return <AopDesignBasis />
      case 'purchase-power':
        return <ImportPower />
      case 'asset-priority':
        return (
          <Stack sx={{ mt: 2 }}>
            <AssetAvailability />
          </Stack>
        )
      case 'asset-capacity':
        return <AssetCapacity />
      case 'shutdown-operational':
        return <ShutdownAndOperational />
      case 'export-availability':
        return <ExportAvailability />
      case 'heat-rate':
        return <HeatRate />
      case 'fixed-norms':
        return <FixedNorms />
      case 'fuel-availability':
        return <Fuel />
      default:
        return null
    }
  }

  return (
    <Box sx={{ p: 0 }}>
      <LoaderBackdrop open={!!loading} />

      {/* Tabs - sticky below StepperNav */}
      <Box
        sx={{
          position: 'sticky',
          top: -1,
          zIndex: 10,
          backgroundColor: '#ffffff',
          borderBottom: 1,
          borderColor: 'divider',
          mb: 1,
        }}
      >
        <AopTabs
          tabIndex={tabIndex}
          setTabIndex={setTabIndex}
          tabs={tabObj?.map((tab) => tab.displayName || tab.name) || []}
        />
      </Box>

      {/* Tab Content */}
      <Box>{renderTabContent()}</Box>
    </Box>
  )
}

export default InputsJMD
