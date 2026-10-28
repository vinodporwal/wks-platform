import React, { useState } from 'react'
import { Box } from '@mui/material'
import AopTabs from 'components/AopTabs'
import TabCrud from './TabCrud'
import TabAccess from './TabAccess'

const TABS = ['Tab Access', 'Tab Configuration']

const TabManagement = () => {
  const [tabIndex, setTabIndex] = useState(0)

  const renderTabContent = () => {
    switch (tabIndex) {
      case 0:
        return <TabAccess />
      case 1:
        return <TabCrud />
      default:
        return null
    }
  }

  return (
    <Box sx={{ p: 2 }}>
      <Box
        sx={{
          position: 'sticky',
          top: -1,
          zIndex: 10,
          backgroundColor: '#ffffff',
          borderBottom: 1,
          borderColor: 'divider',
          mb: 2,
        }}
      >
        <AopTabs tabIndex={tabIndex} setTabIndex={setTabIndex} tabs={TABS} />
      </Box>
      <Box>{renderTabContent()}</Box>
    </Box>
  )
}

export default TabManagement
