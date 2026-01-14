import React from 'react'
import {
  Box,
  Step,
  StepLabel,
  Stepper,
  Tooltip,
  Typography,
} from '@mui/material'
import { verticalEnums } from 'enums/verticalEnums'
import { drawerWidth } from 'config'
import { useMenuContext } from 'menu/menuProvider'
import { useCallback, useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { useLocation, useNavigate } from 'react-router-dom'

// Toggle between fixed and sticky behavior here
const USE_FIXED = true // set to false to use position: 'sticky'

export default function StepperNav() {
  const location = useLocation()
  const navigate = useNavigate()
  const { plantID, verticalChange } = useSelector(
    (state) => state.dataGridStore,
  )
  const plantName = plantID?.plantName
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || 'meg'

  const dataGridStore = useSelector((state) => state.dataGridStore)

  const {
    yearChanged,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
  } = dataGridStore

  const PLANT_NAME = plantObject?.name?.toLowerCase()
  const SITE_NAME = siteObject?.name?.toLowerCase()
  const [steps, setSteps] = useState([])

  const { items: menuItems } = useMenuContext()
  const { drawerOpen } = useSelector((state) => state.menu)
  const collectItems = useCallback(
    (nodes) =>
      nodes.flatMap((node) => {
        if (node.type === 'item') {
          return [node]
        }
        if (node.children) {
          return collectItems(node.children)
        }
        return []
      }),
    [],
  )

  const buildSteps = useCallback(
    (menuArr) => {
      const planGroup = menuArr
        .flatMap((m) => m.children || [])
        .find((c) => c.id === 'production-norms-plan')

      if (!planGroup?.children) return []
      const allItems = collectItems(planGroup.children)
      return allItems.map((item) => {
        const slug = item.url.split('/').pop()
        return { label: item.title, url: item.url, key: slug }
      })
    },
    [collectItems],
  )

  useEffect(() => {
    const newSteps = buildSteps(menuItems)

    const isPE = lowerVertName === 'pe'
    const shouldFilterSlowdown = lowerVertName === verticalEnums.PP || isPE

    if (shouldFilterSlowdown) {
      const filteredSteps = newSteps.filter(
        (step) => step.key !== 'slowdown-norms',
      )
      setSteps(filteredSteps)
    } else {
      setSteps(newSteps)
    }

    const currentSlug = location.pathname.split('/').pop()
    const found = newSteps.some((s) => s.key === currentSlug)

    if (newSteps.length && !found) {
      navigate(newSteps[0].url, { replace: true })
    }
  }, [
    menuItems,
    lowerVertName,
    plantName,
    buildSteps,
    navigate,
    location.pathname,
  ])

  const currentPath = location.pathname.split('/').pop()
  const activeStep = steps.findIndex((s) => s.key === currentPath)

  // Helper to return first 15 characters then ellipsis
  const getAbbrev = (label) => {
    if (!label) return ''
    const text = label.trim()
    return text.length <= 12 ? text : `${text.slice(0, 12)}…`
  }

  useEffect(() => {
    const el = document.querySelector('.MuiStep-root.Mui-active')
    el?.scrollIntoView({
      behavior: 'smooth',
      inline: 'center',
      block: 'nearest',
    })
  }, [activeStep])

  // shared Stepper element with modern styling
  const StepperElement = (
    <Stepper
      nonLinear
      alternativeLabel
      activeStep={activeStep >= 0 ? activeStep : 0}
      sx={{
        minWidth: 'max-content', // 🔥 IMPORTANT

        '& .MuiStepLabel-label': {
          fontWeight: '500',
          fontSize: '0.8125rem',
          letterSpacing: '0.01em',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        },
        '& .MuiStepLabel-label.Mui-active': {
          fontWeight: '700',
          color: '#0100cb',
          fontSize: '0.875rem',
        },
        '& .MuiStepLabel-alternativeLabel': {
          marginTop: '4px !important',
        },
        '& .MuiStepConnector-alternativeLabel': {
          top: '18px',
        },
        '& .MuiStepConnector-line': {
          borderColor: '#e0e0e0',
          borderTopWidth: '2px',
          transition: 'all 0.3s ease',
        },
        '& .MuiStepConnector-root.Mui-active .MuiStepConnector-line': {
          borderColor: '#bbbbbb',
          background: 'linear-gradient(90deg, #0100cb 0%, #5b59ff 100%)',
          borderTopWidth: '2px',
        },
        '& .MuiStepConnector-root.Mui-completed .MuiStepConnector-line': {
          borderColor: '#bbbbbb',
        },
      }}
    >
      {steps.map((step, index) => {
        const abbrev = getAbbrev(step.label)
        const isActive = activeStep === index

        return (
          <Step
            key={step.key}
            onClick={() => navigate(step.url)}
            sx={{
              cursor: 'pointer',
              transition: 'transform 0.2s ease',
              '&:hover': {
                transform: 'translateY(-2px)',
              },
              '& .MuiStepIcon-root': {
                fontSize: '1.75rem',
                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                filter: 'drop-shadow(0px 2px 4px rgba(0, 0, 0, 0.1))',
              },
              '& .MuiStepIcon-root.Mui-active': {
                color: '#51b17c',
                transform: 'scale(1.15)',
                filter: 'drop-shadow(0px 4px 8px rgba(1, 0, 203, 0.3))',
              },
              '& .MuiStepIcon-root.Mui-completed': {
                color: '#0100cb',
              },
              '& .MuiStepIcon-root:hover': {
                transform: 'scale(1.1)',
                filter: 'drop-shadow(0px 4px 8px rgba(1, 0, 203, 0.25))',
              },
            }}
            aria-label={step.label}
          >
            <StepLabel
              sx={{
                cursor: 'pointer',
                '& .MuiStepLabel-iconContainer': {
                  transition: 'all 0.3s ease',
                },
              }}
            >
              <Tooltip
                title={step.label}
                enterDelay={200}
                arrow
                slotProps={{
                  tooltip: {
                    sx: {
                      bgcolor: 'rgba(65, 63, 63, 0.9)',
                      backdropFilter: 'blur(8px)',
                      fontSize: '0.8125rem',
                      py: 0,
                      px: 2,
                      boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                    },
                  },
                  arrow: {
                    sx: {
                      color: 'rgba(0, 0, 0, 0.9)',
                    },
                  },
                }}
              >
                <Typography
                  variant='caption'
                  sx={{
                    minWidth: 38,
                    maxWidth: 100,
                    lineHeight: 1.2,
                    display: 'inline-block',
                    textAlign: 'center',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    fontWeight: isActive ? '700' : '500',
                    color: isActive ? '#303284' : 'text.secondary',
                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                    '&:hover': {
                      color: '#303284',
                      fontWeight: '600',
                    },
                  }}
                >
                  {step.label}
                </Typography>
              </Tooltip>
            </StepLabel>
          </Step>
        )
      })}
    </Stepper>
  )

  return (
    <>
      {USE_FIXED ? (
        <>
          {/* Fixed to viewport but TRANSPARENT to background - no border, no radius, no bg */}
          <Box
            sx={{
              position: 'fixed',
              top: '55px',
              left: drawerOpen ? `${drawerWidth + 8}px` : '5px',
              right: '5px',
              zIndex: (theme) => (theme.zIndex?.appBar ?? 1100) + 1,
              background: 'transparent',
              backdropFilter: 'none',
              boxShadow: 'none',
              border: 'none',
              borderRadius: 0,
              py: 0,
              px: 0,
              transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
              maxHeight: '80px',
              '&:hover': {
                // keep subtle hover transform but do not add borders/bg
                transform: 'none',
              },
              // remove decorative pseudo element that added a colored line
              '&::before': {
                content: 'none',
              },
            }}
          >
            <Box
              sx={{
                overflowX: 'auto',
                overflowY: 'hidden',
                width: '100%',
                scrollbarWidth: 'thin',
                '&::-webkit-scrollbar': {
                  height: 6,
                },
                '&::-webkit-scrollbar-thumb': {
                  backgroundColor: 'rgba(1,0,203,0.4)',
                  borderRadius: 4,
                },
              }}
            >
              {StepperElement}
            </Box>
          </Box>

          {/* Spacer so fixed element doesn't cover content */}
          <Box
            sx={{ height: (theme) => theme?.mixins?.toolbar?.minHeight ?? 64 }}
          />
        </>
      ) : (
        <Box
          sx={{
            background: 'transparent',
            backdropFilter: 'none',
            boxShadow: 'none',
            border: 'none',
            borderRadius: 0,
            py: 0,
            px: 0,
            transition: 'all 0.3s ease',
          }}
        >
          {StepperElement}
        </Box>
      )}
    </>
  )
}
