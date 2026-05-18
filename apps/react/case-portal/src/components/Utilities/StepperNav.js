import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Box, Tabs, Tab, Tooltip, useTheme } from '@mui/material'
import { useLocation, useNavigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useMenuContext } from 'menu/menuProvider'
import { verticalEnums } from 'enums/verticalEnums'
import { drawerWidth, miniDrawerWidth } from 'config'

const USE_FIXED = false

export default function StepperNav() {
  const location = useLocation()
  const navigate = useNavigate()
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'

  const { items: menuItems } = useMenuContext()
  const { drawerOpen } = useSelector((state) => state.menu)
  const { verticalChange } = useSelector((state) => state.dataGridStore)

  const lowerVertName = verticalChange?.selectedVertical?.toLowerCase() || 'meg'

  const [steps, setSteps] = useState([])

  // -------------------------
  // Collect nested menu items
  // -------------------------
  const collectItems = useCallback((nodes) => {
    return nodes.flatMap((node) => {
      if (node.type === 'item') return [node]
      if (node.children) return collectItems(node.children)
      return []
    })
  }, [])

  // -------------------------
  // Build Steps from Menu
  // -------------------------
  const buildSteps = useCallback(
    (menuArr) => {
      // const planGroup = menuArr
      //   .flatMap((m) => m.children || [])
      //   .find((c) => c.id === 'production-norms-plan')
      //********************************* */
      const menuGroupIds = ['production-norms-plan', 'tcs', 'utilityPlant']
      const currentPathname = location.pathname

      // Find which menu group matches the current pathname
      const matchedGroupId = menuGroupIds.find((id) =>
        currentPathname.includes(id),
      )
      const planGroup = menuArr
        .flatMap((m) => m.children || [])
        .find((c) => c.id === matchedGroupId)
      //********************************* */

      if (!planGroup?.children) return []

      const allItems = collectItems(planGroup.children)

      return allItems.map((item) => ({
        label: item.title,
        url: item.url,
        key: item.id,
        icon: item.icon,
      }))
    },
    [collectItems, location.pathname],
  )

  // -------------------------
  // Initialize + Filter
  // -------------------------
  useEffect(() => {
    const newSteps = buildSteps(menuItems)

    const isPE = lowerVertName === 'pe'
    const shouldFilterSlowdown = lowerVertName === verticalEnums.PP || isPE

    const filteredSteps = shouldFilterSlowdown
      ? newSteps.filter((step) => step.key !== 'slowdown-norms')
      : newSteps

    setSteps(filteredSteps)

    const currentSlug = location.pathname.split('/').pop()
    const found = filteredSteps.some((s) => s.url.includes(currentSlug))

    if (filteredSteps.length && !found) {
      navigate(filteredSteps[0].url, { replace: true })
    }
  }, [menuItems, lowerVertName, buildSteps, navigate, location.pathname])

  // -------------------------
  // Active Step
  // -------------------------
  const activeStep = useMemo(() => {
    return steps.findIndex((s) => location.pathname.includes(s.url))
  }, [steps, location.pathname])

  // -------------------------
  // Tabs UI
  // -------------------------
  const TabsElement = (
    <Tabs
      value={activeStep >= 0 ? activeStep : 0}
      onChange={(e, newValue) => {
        navigate(steps[newValue].url)
      }}
      variant='scrollable'
      scrollButtons='auto' // ✅ FIXED
      allowScrollButtonsMobile
      className='stepper-nav-tabs'
      sx={{
        '& .MuiTabs-indicator': {
          top: 0,
          height: 4,
          backgroundColor: '#ae4787',
        },
        '& .MuiTabs-scrollButtons': {
          color: isDark ? '#F0F0F0' : 'inherit',
        },
      }}
    >
      {steps.map((step) => (
        <Tooltip key={step.key} title={step.label} arrow>
          <Tab
            icon={
              step.icon
                ? React.isValidElement(step.icon)
                  ? step.icon
                  : React.createElement(step.icon, {
                      fontSize: 'small',
                    })
                : null
            }
            iconPosition='start'
            label={<span className='stepper-nav-tab-label' style={{ color: 'inherit' }}>{step.label}</span>}
            className='stepper-nav-tab'
            sx={{
              textTransform: 'none !important',
              paddingLeft: '12px !important',
              paddingRight: '12px !important',
              minHeight: '40px !important',
              gap: '4px !important',
              transition: 'all 0.2s ease !important',
              color: isDark ? '#F0F0F0 !important' : '#303030 !important',
              '&:hover': {
                backgroundColor: isDark ? 'rgba(255, 255, 255, 0.05) !important' : '#eef2ff !important',
              },
              '&.Mui-selected': {
                backgroundColor: isDark ? 'rgba(255, 255, 255, 0.08) !important' : '#f6f7f8 !important',
                color: isDark ? '#F0F0F0 !important' : '#303030 !important',
                '& .stepper-nav-tab-label': {
                  color: isDark ? '#F0F0F0 !important' : '#303030 !important',
                  fontWeight: 600,
                },
              },
            }}
          />
        </Tooltip>
      ))}
    </Tabs>
  )

  // -------------------------
  // Render
  // -------------------------
  return (
    <Box
      className='stepper-nav-container'
      sx={{
        backgroundColor: isDark ? '#1C2236' : '#ffffff',
        borderBottom: isDark ? '1px solid rgba(255, 255, 255, 0.1)' : '1px solid #e2e8f0',
        transition: 'all 0.3s ease',
      }}
    >
      {USE_FIXED ? (
        <>
          <Box
            sx={{
              position: 'fixed',
              left: drawerOpen
                ? `${drawerWidth + 8}px`
                : `${miniDrawerWidth + 10}px`,
              right: '8px',

              // ✅ CRITICAL FIXES
              width: `calc(100% - ${
                drawerOpen ? drawerWidth + 16 : miniDrawerWidth + 18
              }px)`,
              overflow: 'hidden',

              zIndex: (theme) => (theme.zIndex?.appBar ?? 1100) + 1,
              transition: 'all 0.3s ease',
            }}
          >
            {TabsElement}
          </Box>

          <Box
            sx={{
              height: (theme) => theme?.mixins?.toolbar?.minHeight ?? 56,
            }}
          />
        </>
      ) : (
        TabsElement
      )}
    </Box>
  )
}
