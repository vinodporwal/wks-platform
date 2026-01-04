import { Tab, Tabs, Box } from '@mui/material'
import PropTypes from 'prop-types'
import { useEffect, useRef } from 'react'

const AopTabs = ({ tabIndex, setTabIndex, tabs }) => {
  const tabRefs = useRef([])

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
        p: 0.25,
        borderRadius: '8px',
        bgcolor: '#f8fafc',
        border: '1px solid rgba(0,0,0,0.05)',

        scrollbarWidth: 'none',
        '&::-webkit-scrollbar': { display: 'none' },
      }}
    >
      <Tabs
        value={tabIndex}
        onChange={(e, newIndex) => setTabIndex(newIndex)}
        variant='scrollable'
        sx={{
          minHeight: 20,
          width: 'max-content',
          '& .MuiTabs-flexContainer': {
            gap: '4px',
          },
          '& .MuiTabs-indicator': {
            display: 'none',
          },
        }}
      >
        {tabs.map((tab, index) => {
          const isSelected = tabIndex === index

          return (
            <Tab
              key={tab}
              ref={(el) => (tabRefs.current[index] = el)}
              label={tab}
              disableRipple
              sx={{
                minHeight: 20,
                px: '8px',
                fontSize: '0.65rem',
                fontWeight: 600,
                textTransform: 'none',
                borderRadius: '6px',

                color: isSelected ? '#0f172a' : '#475569',
                bgcolor: isSelected ? '#ffffff' : 'transparent',

                border: '1px solid',
                borderColor: isSelected
                  ? 'rgba(15,23,42,0.25)'
                  : 'rgba(15,23,42,0.12)',

                boxShadow: isSelected ? '0 1px 3px rgba(0,0,0,0.12)' : 'none',

                transition: 'background 140ms ease, box-shadow 140ms ease',

                '&:hover': {
                  bgcolor: '#ffffff',
                },
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
