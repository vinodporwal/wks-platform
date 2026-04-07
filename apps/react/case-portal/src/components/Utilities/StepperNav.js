import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Box, Tabs, Tab, Tooltip } from '@mui/material'
import { useLocation, useNavigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useMenuContext } from 'menu/menuProvider'
import { verticalEnums } from 'enums/verticalEnums'
import { drawerWidth, miniDrawerWidth } from 'config'

const USE_FIXED = true

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
      const planGroup = menuArr
        .flatMap((m) => m.children || [])
        .find((c) => c.id === 'production-norms-plan')

      if (!planGroup?.children) return []

      const allItems = collectItems(planGroup.children)

      return allItems.map((item) => ({
        label: item.title,
        url: item.url,
        key: item.id,
        icon: item.icon,
      }))
    },
    [collectItems],
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
    <Box
      sx={{
        backgroundColor: '#f7f8fa',
        borderBottom: '1px solid #e2e8f0',
      }}
    >
      <Tabs
        value={activeStep >= 0 ? activeStep : 0}
        onChange={(e, newValue) => {
          navigate(steps[newValue].url)
        }}
        variant='scrollable'
        scrollButtons='auto' // ✅ FIXED
        allowScrollButtonsMobile
        sx={{
          minHeight: 40,

          // ✅ FORCE SINGLE LINE (NO WRAP BUG)
          '& .MuiTabs-flexContainer': {
            flexWrap: 'nowrap',
          },

          '& .MuiTabs-indicator': {
            top: 0,
            height: 2,
            backgroundColor: '#2563eb',
          },

          '& .MuiTab-root': {
            minHeight: 40,
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
              label={
                <span
                  style={{
                    display: 'inline-block',
                    maxWidth: 120,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {step.label}
                </span>
              }
              sx={{
                textTransform: 'none',
                fontWeight: 500,
                fontSize: '0.85rem',
                color: '#475569',
                px: 1.5,
                minHeight: 40,
                gap: 0.5,
                transition: 'all 0.2s ease',

                '&:hover': {
                  backgroundColor: '#eef2ff',
                },

                '&.Mui-selected': {
                  color: '#2563eb',
                  fontWeight: 600,
                },
              }}
            />
          </Tooltip>
        ))}
      </Tabs>
    </Box>
  )

  // -------------------------
  // Render
  // -------------------------
  return (
    <>
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
    </>
  )
}
