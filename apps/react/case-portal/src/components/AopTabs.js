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

  if (tabs?.length === 0) {
    return null
  }

  return (
    <Box
      sx={{
        display: 'flex',
        maxWidth: '100%',
        overflowX: 'auto',
        WebkitOverflowScrolling: 'touch',
        p: 0.125,
        borderRadius: '0px',
        borderBottom: '1px solid #E0E0E0',
        marginBottom: '15px',
        scrollbarWidth: 'none',
        '&::-webkit-scrollbar': { display: 'none' },
      }}
    >
      <Tabs
        value={tabIndex}
        onChange={(e, newIndex) => setTabIndex(newIndex)}
        variant='scrollable'
        className="aop-tabs-tabs"
        sx={{
          minHeight: 20,
          width: 'max-content',
          fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
          marginBottom: '-2px',
          '& .MuiTabs-flexContainer': {
            gap: BOX_TABS ? '1px' : '10px',
          },

          '& .MuiTabs-indicator': {
            display: BOX_TABS ? 'none' : 'block',
            height: '3px',
            borderRadius: '2px',
            backgroundColor: '#AE4787',
            marginLeft: '5px'
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
                fontSize: '14px',
                fontWeight: isSelected ? 600 : 400,
                textTransform: 'none',
                fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                borderRadius: 0,

                color: isSelected ? '#303030 !important' : '#606060',
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
                  color: '#303030 !important',
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
