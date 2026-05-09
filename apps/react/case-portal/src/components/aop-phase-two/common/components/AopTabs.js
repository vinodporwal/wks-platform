import { Tab, Tabs, Box } from '@mui/material'
import PropTypes from 'prop-types'
import { useEffect, useRef } from 'react'

const AopTabs = ({ tabIndex, setTabIndex, tabs }) => {
  const tabRefs = useRef([])
  const BOX_TABS = false // ?? change to false when needed

  // Ensure tabs is a valid array
  const validTabs = Array.isArray(tabs) ? tabs : []

  useEffect(() => {
    const activeTab = tabRefs.current[tabIndex]
    if (activeTab) {
      activeTab.scrollIntoView({
        behavior: 'smooth',
        inline: 'center',
        block: 'nearest',
      })
    }
  }, [tabIndex])

  return (
    <Box
      sx={{
        display: 'flex',
        maxWidth: '100%',
        overflowX: 'auto',
        WebkitOverflowScrolling: 'touch',
        p: 0.125,
        borderRadius: '0px',
        // bgcolor: '#f8fafc',
        // border: '1px solid rgba(0,0,0,0.05)',
        // borderTop: '1px solid #bbc0c6',
        scrollbarWidth: 'none',
        '&::-webkit-scrollbar': { display: 'none' },
      }}
    >
      <Tabs
        sx={{
          borderBottom: '0px solid #ccc',
          '.MuiTabs-indicator': { display: 'none' },
          margin: '0px 0px 0px 0px',
          minHeight: '28px',
        }}
        textColor='primary'
        indicatorColor='primary'
        value={tabIndex}
        onChange={(e, newIndex) => {
          if (newIndex >= 0 && newIndex < validTabs.length) {
            setTabIndex(newIndex)
          }
        }}
      >
        {validTabs.map((tab, index) => {
          const isSelected = tabIndex === index

          return (
            <Tab
              key={`tab-${index}-${tab}`}
              ref={(el) => (tabRefs.current[index] = el)}
              label={tab}
              sx={{
                border: '1px solid #ADD8E6',
                borderBottom: '1px solid #ADD8E6',
                fontSize: '0.75rem',
                padding: '9px',
                minHeight: '12px',
              }}
            />
          )
        })}
      </Tabs>
    </Box>
  )
}

AopTabs.propTypes = {
  tabIndex: PropTypes.number.isRequired,
  setTabIndex: PropTypes.func.isRequired,
  tabs: PropTypes.array.isRequired,
}

export default AopTabs
