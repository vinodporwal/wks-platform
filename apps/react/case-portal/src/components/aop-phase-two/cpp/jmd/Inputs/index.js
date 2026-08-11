import { useEffect, useState } from 'react'
import { Box, Stack } from '@mui/material'
import { useSelector } from 'react-redux'
import AssetCapacity from './AssetCapacity/index'
import ShutdownAndOperational from './ShutdownAndOperational/index'
import ExportAvailability from './ExportAvailability'
import HeatRate from './HeatRate/index'
import Fuel from './Fuel/index'
import AopDesignBasis from './AopDesignBasis'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AopTabs from 'components/AopTabs'
import AssetAvailability from './AssetAvailability/index'
import FuelPriority from './FuelPriority/index'
import ImportPower from './ImportPowerMain/index'
import SRMapping from '../../common/SRMapping/index'
import InputNorms from './InputNorms/index'
import SpinningMargin from '../../common/SpinningMargin/index'
import useConfigurationTabs from 'components/aop-phase-two/common/hooks/useConfigurationTabs'

const InputsJMD = () => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { siteObject, verticalObject } = dataGridStore

  const lowerVertName = verticalObject?.name?.toLowerCase()
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const IS_CPP = lowerVertName === 'cpp'

  // Tab management via custom hook (type = 'Inputs')
  const { tabs, filteredTabs, loading } = useConfigurationTabs('Inputs')

  const [tabIndex, setTabIndex] = useState(0)

  // Reset tabIndex when tabs change.
  // NOTE: depend on `tabs` (stable state from the hook), NOT `filteredTabs`
  // (which is a new array reference every render and would reset tabIndex
  // on every render, breaking tab switching).
  useEffect(() => {
    if (filteredTabs.length > 0) {
      setTabIndex(0)
    } else {
      setTabIndex(null)
    }
  }, [tabs])

  // Get current tab display name
  const currentTabName = filteredTabs[tabIndex]?.name

  // Render tab content based on display name
  const renderTabContent = () => {
    if (!currentTabName) return null

    switch (currentTabName) {
      case 'AOP Design Basis':
        return <AopDesignBasis />
      case 'Purchase Power Input':
        return <ImportPower />
      case 'Shutdown and Operational Hrs.':
        return <ShutdownAndOperational />
      case 'Asset Priority':
        return (
          <Stack sx={{ mt: 2 }}>
            <AssetAvailability />
          </Stack>
        )
      case 'Asset Capacity':
        return <AssetCapacity />
      case 'Spinning Margin':
        return <SpinningMargin />
      case 'Heat Rate':
        return <HeatRate />
      case 'SR Mapping':
        return <SRMapping />
      case 'Norms & Quantity':
        return <InputNorms />
      case 'Fuel Availability':
        return <Fuel />
      case 'Fuel Priority':
        return <FuelPriority />
      case 'Export Availability':
        return <ExportAvailability />
      default:
        return null
    }
  }

  if (!IS_CPP) return null

  return (
    <Box sx={{ p: 0 }}>
      <LoaderBackdrop open={!!loading} />

      {/* Tabs - sticky below StepperNav */}
      {filteredTabs.length > 0 && (
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
            tabs={filteredTabs.map((tab) => tab.name)}
          />
        </Box>
      )}

      {/* Tab Content */}
      <Box>{renderTabContent()}</Box>
    </Box>
  )
}

export default InputsJMD
