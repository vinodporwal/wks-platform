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
        value={tabIndex}
        onChange={(e, newIndex) => setTabIndex(newIndex)}
        variant='scrollable'
        sx={{
          minHeight: 20,
          width: 'max-content',
          fontFamily:
            "'Segoe UI', system-ui, -apple-system, 'Open Sans', Arial, sans-serif", // ? here

          '& .MuiTabs-flexContainer': {
            gap: BOX_TABS ? '1px' : '1px',
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
                px: '4px',
                fontSize: '0.80rem',
                fontWeight: 600,
                textTransform: 'none',
                fontFamily:
                  "'Segoe UI', system-ui, -apple-system, 'Open Sans', Arial, sans-serif",
                borderRadius: 0,

                color: isSelected ? '#0f172a' : '#475569',
                bgcolor: 'transparent',
                border: 'none',

                boxShadow: 'none !important', // ? force remove shadow

                '&:hover': {
                  bgcolor: 'transparent',
                  color: '#0f172a',
                  boxShadow: 'none !important',
                },

                '&:focus': {
                  outline: 'none',
                  boxShadow: 'none !important',
                },

                '&.Mui-focusVisible': {
                  outline: 'none',
                  boxShadow: 'none !important',
                },

                '&:active': {
                  boxShadow: 'none !important',
                  bgcolor: 'transparent',
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
