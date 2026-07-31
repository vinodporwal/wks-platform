import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Box, Tabs, Tab, Tooltip } from '@mui/material'
import { useLocation, useNavigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useMenuContext } from 'menu/menuProvider'
import { verticalEnums } from 'enums/verticalEnums'
import { drawerWidth, miniDrawerWidth } from 'config'

const USE_FIXED = false

export default function StepperNav() {
  const location = useLocation()
  const navigate = useNavigate()

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

    const found = filteredSteps.some(
      (s) =>
        location.pathname === s.url ||
        location.pathname.startsWith(s.url + '/'),
    )

    if (filteredSteps.length && !found) {
      navigate(filteredSteps[0].url, { replace: true })
    }
  }, [menuItems, lowerVertName, buildSteps, navigate, location.pathname])

  // -------------------------
  // Active Step
  // -------------------------
  const activeStep = useMemo(() => {
    // Prefer exact match first, then longest prefix match to avoid
    // partial slug collisions (e.g. /configuration vs /configuration-other-cost)
    const exactIdx = steps.findIndex((s) => location.pathname === s.url)
    if (exactIdx !== -1) return exactIdx

    // Fallback: find the step whose url is the longest prefix of the pathname
    let bestIdx = -1
    let bestLen = 0
    steps.forEach((s, i) => {
      if (location.pathname.startsWith(s.url + '/') && s.url.length > bestLen) {
        bestIdx = i
        bestLen = s.url.length
      }
    })
    return bestIdx
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
            label={<span className='stepper-nav-tab-label'>{step.label}</span>}
            className='stepper-nav-tab'
          />
        </Tooltip>
      ))}
    </Tabs>
  )

  // -------------------------
  // Render
  // -------------------------
  return (
    <Box className='stepper-nav-container'>
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
