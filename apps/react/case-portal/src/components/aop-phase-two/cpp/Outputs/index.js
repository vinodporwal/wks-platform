import { useEffect, useState, useCallback } from 'react'
import { Box } from '@mui/material'
import AopTabs from 'components/AopTabs'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'

import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import HeatRate from './heat-rate'

const Outputs = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear

  const lowerVertName = verticalObject?.name?.toLowerCase()
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const IS_CPP = lowerVertName === 'cpp'

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
        id: 'heat-rate',
        name: 'heatRate',
        displayName: 'Heat Rate',
        displaySequence: 0,
      },
    ]
    setTabObj(tabs)
  }, [])

  // Get current tab
  const currentTab = tabObj[tabIndex] || {}

  // Render tab content based on tab ID
  const renderTabContent = () => {
    switch (currentTab.id) {
      case 'heat-rate':
        return <HeatRate />
      default:
        return null
    }
  }

  const renderBySite = () => {
    switch (lowerSiteName) {
      case 'jmd':
      case 'vmd':
      case 'hmd':
      case 'dmd':
      default:
        return (
          <>
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
          </>
        )
    }
  }

  if (!IS_CPP) return null

  return (
    <Box sx={{ p: 0 }}>
      <LoaderBackdrop open={!!loading} />
      {renderBySite()}
    </Box>
  )
}

export default Outputs
