import { useState, useEffect, useCallback } from 'react'
import { Box, Stack } from '../../../../../node_modules/@mui/material/index'
import TabSection from 'components/aop-phase-two/common/utilities/Tabs'
import ConfigurationAccordian from '../../common/components/ConfigurationAccordian'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import TabAccessApiService from 'components/aop-phase-two/services/common/tabAccessApiService'
import Notification from 'components/aop-phase-two/common/utilities/Notification'
import Configuration from './Configuration'
import ProductionRange from './ProductionRange'
import ReportManualEntry from './ReportManualEntry/index'

const ProductionNormsBasis = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { oldYear, plantObject, siteObject, verticalObject, year } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR)
  const isOldYear = false

  const [tabIndex, setTabIndex] = useState(0)
  const [loading, setLoading] = useState(false)
  const [tabs, setTabs] = useState([])
  const [availableTabs, setAvailableTabs] = useState([])
  const [startDate, setStartDate] = useState(null)
  const [endDate, setEndDate] = useState(null)
  const [normCalculationLoading, setNormCalculationLoading] = useState(false)
  const [refreshData, setRefreshData] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
    autoHide: true,
  })

  const getConfigurationTabsMatrix = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR || !SITE_ID || !VERTICAL_ID) return
    setLoading(true)
    try {
      const response = await TabAccessApiService.getConfigurationTabsMatrix(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        SITE_ID,
        VERTICAL_ID,
      )
      if (response?.code === 200) {
        const parsedData = JSON.parse(response?.data)
        setTabs(parsedData)
      } else {
        setTabs([])
      }
    } catch (error) {
      console.error('Error fetching configuration tabs matrix:', error)
      setTabs([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, SITE_ID, VERTICAL_ID])

  const getConfigurationAvailableTabs = useCallback(async () => {
    setLoading(true)
    try {
      const response =
        await TabAccessApiService.getConfigurationAvailableTabs(keycloak)
      if (response?.code === 200) {
        setAvailableTabs(response?.data?.configurationTypeList)
      } else {
        setAvailableTabs([])
      }
    } catch (error) {
      console.error('Error fetching configuration available tabs:', error)
      setAvailableTabs([])
    } finally {
      setLoading(false)
    }
  }, [keycloak])

  useEffect(() => {
    if (!PLANT_ID || !AOP_YEAR) return
    setTabIndex(0)
    getConfigurationTabsMatrix()
    getConfigurationAvailableTabs()
  }, [
    PLANT_ID,
    AOP_YEAR,
    getConfigurationTabsMatrix,
    getConfigurationAvailableTabs,
  ])

  // Callback to receive dates from ConfigurationAccordian
  const handleDatesChange = (start, end) => {
    setStartDate(start)
    setEndDate(end)
  }

  // Helper function to format date for API
  const formatDateForAPI = (date) => {
    if (!date) return null
    const d = new Date(date)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  const [start, end] = AOP_YEAR ? AOP_YEAR.split('-').map(Number) : [0, 0]
  const prevYearFormatted = `${start - 1}-${(start - 1 + 1).toString().slice(-2)}`

  // Helper function to get tab display name by matching the UUID from tabs array
  const getTabName = (tabId) => {
    if (!tabId || !availableTabs.length) return null
    const tab = availableTabs.find(
      (t) => t.id.toLowerCase() === tabId.toLowerCase(),
    )
    return tab ? tab.displayName : null
  }

  // Dynamic tab list from API
  const filteredTabs = tabs
    .map((tabId) => {
      const tabInfo = availableTabs.find(
        (tab) => tab.id.toLowerCase() === tabId.toLowerCase(),
      )

      if (!tabInfo) return null

      const name = tabInfo.displayName

      return {
        id: tabId,
        name,
      }
    })
    .filter(Boolean)

  const renderTab = () => {
    if (!filteredTabs.length || !availableTabs.length) {
      return null
    }

    const currentTabId = filteredTabs[tabIndex]?.id
    if (!currentTabId) return null

    const tabData = getTabName(currentTabId)
    const currentTabName = typeof tabData === 'object' ? tabData.name : tabData

    switch (currentTabName) {
      case 'Configuration':
        return (
          <Configuration
            startDate={startDate}
            endDate={endDate}
            refreshData={refreshData}
          />
        )
      case 'Manual Entry':
        return (
          <ReportManualEntry
            startDate={startDate}
            endDate={endDate}
            refreshData={refreshData}
          />
        )
      case 'ProductionRange':
        return <ProductionRange />
      default:
        return null
    }
  }

  console.log('filteredTabs', filteredTabs)
  return (
    <div>
      <Stack sx={{ mt: 1, mb: 1 }}>
        <ConfigurationAccordian
          PLANT_ID={PLANT_ID}
          AOP_YEAR={AOP_YEAR}
          isOldYear={isOldYear}
          isSummaryRequired={false}
          onDatesChange={handleDatesChange}
          normCalculationLoading={normCalculationLoading}
        />
      </Stack>

      {tabs.length > 0 && availableTabs.length > 0 && (
        <Stack
          direction='row'
          justifyContent='space-between'
          alignItems='center'
        >
          <TabSection
            tabIndex={tabIndex}
            setTabIndex={setTabIndex}
            tabs={filteredTabs.map((tab) => tab.name)}
          />
        </Stack>
      )}

      {/* Tab Content */}
      <Box sx={{ mt: 2 }}>{renderTab()}</Box>
      {/* Notification */}
      <Notification
        open={snackbarOpen}
        onClose={() => setSnackbarOpen(false)}
        message={snackbarData.message}
        severity={snackbarData.severity}
        autoHide={snackbarData.autoHide}
      />
    </div>
  )
}

export default ProductionNormsBasis
