import { Tab, Tabs, Box } from '@mui/material'
import PropTypes from 'prop-types'
import { useEffect, useRef } from 'react'

const AopTabs = ({ tabIndex, setTabIndex, tabs }) => {
  const tabRefs = useRef([])
  const BOX_TABS = false // ?? change to false when needed

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
            gap: BOX_TABS ? '4px' : '12px',
          },

          '& .MuiTabs-indicator': {
            display: BOX_TABS ? 'none' : 'block',
            height: '2px',
            borderRadius: '2px',
            backgroundColor: '#1258b3',
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
                px: BOX_TABS ? '8px' : '4px',
                fontSize: '0.65rem',
                fontWeight: 600,
                textTransform: 'none',
                borderRadius: BOX_TABS ? '6px' : 0,

                color: isSelected ? '#0f172a' : '#475569',

                // ? BOX TABS MODE
                bgcolor: BOX_TABS && isSelected ? '#ffffff' : 'transparent',
                border: BOX_TABS ? '1px solid' : 'none',
                borderColor:
                  BOX_TABS && isSelected
                    ? 'rgba(15,23,42,0.25)'
                    : 'rgba(15,23,42,0.12)',

                boxShadow:
                  BOX_TABS && isSelected
                    ? '0 1px 3px rgba(0,0,0,0.12)'
                    : 'none',

                // ? FLAT TABS MODE
                '&:hover': {
                  bgcolor: BOX_TABS ? '#ffffff' : 'transparent',
                  color: '#0f172a',
                },

                transition: 'all 160ms ease',
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
