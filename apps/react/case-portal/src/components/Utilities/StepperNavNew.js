import { Box, Step, StepLabel, Stepper } from '@mui/material'
import { verticalEnums } from 'enums/verticalEnums'
import { useMenuContext } from 'menu/menuProvider'
import { useCallback, useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { useLocation, useNavigate } from 'react-router-dom'

export default function StepperNavNew() {
  const location = useLocation()
  const navigate = useNavigate()
  const { plantID, verticalChange } = useSelector(
    (state) => state.dataGridStore,
  )
  const plantName = plantID?.plantName
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || 'meg'

  const [steps, setSteps] = useState([])

  const { items: menuItems } = useMenuContext()
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
        //   if (!planGroup?.children) return []
        //   return planGroup.children.map((item) => {
        //     const slug = item.url.split('/').pop()
        //     return { label: item.title, url: item.url, key: slug }
      }),
    [],
  )
  // }
  const buildSteps = useCallback(
    (menuArr) => {
      const planGroup = menuArr
        .flatMap((m) => m.children || [])
        .find((c) => c.id === 'production-norms-plan')

      if (!planGroup?.children) return []

      // 2. Prepare an array to collect �step items�

      // 2.a If the child itself is a leaf item (has a valid `url`), include it
      const allItems = collectItems(planGroup.children)
      // 2.b Otherwise, if it�s a collapse and has children, include each grandchild

      // 3. Map each collected item into the shape { label, url, key }
      return allItems.map((item) => {
        const slug = item.url.split('/').pop()
        return { label: item.title, url: item.url, key: slug }
      })
    },
    [collectItems],
  )

  useEffect(() => {
    const newSteps = buildSteps(menuItems)
    const shouldFilterSlowdown =
      lowerVertName === verticalEnums.PE && plantName === 'LDPE'

    if (shouldFilterSlowdown) {
      const filteredSteps = newSteps.filter(
        (step) => step.key !== 'slowdown-norms',
      )
      setSteps(filteredSteps)
    } else {
      setSteps(newSteps)
    }
    // Derive current path slug
    const currentSlug = location.pathname.split('/').pop()
    const found = newSteps.some((s) => s.key === currentSlug)

    // If current path isn't one of our steps, redirect to the first
    if (newSteps.length && !found) {
      navigate(newSteps[0].url, { replace: true })
    }
  }, [menuItems, lowerVertName, plantName])

  const currentPath = location.pathname.split('/').pop()
  const activeStep = steps.findIndex((s) => s.key === currentPath)

  // -- Render Stepper ----------------------tested
  return (
    <>
      <Box
        sx={{
          width: '100%',
          overflowX: 'auto',
          '&::-webkit-scrollbar': {
            height: '6px',
          },
          '&::-webkit-scrollbar-track': {
            background: '#f1f1f1',
            borderRadius: '3px',
          },
          '&::-webkit-scrollbar-thumb': {
            background: '#c1c1c1',
            borderRadius: '3px',
          },
        }}
      >
        <Stepper
          nonLinear
          alternativeLabel
          activeStep={activeStep >= 0 ? activeStep : 0}
          sx={{
            minWidth: 'max-content',
            pb: 1, // Add some padding bottom for scrollbar
            '& .MuiStepLabel-label': {
              fontWeight: 'normal',
            },
            '& .MuiStepLabel-label.Mui-active': {
              fontWeight: 'bold',
              color: '#000',
            },
            '& .MuiStepLabel-alternativeLabel': {
              marginTop: '3px !important',
            },
            // Force text wrapping in alternative label mode
            '& .MuiStepLabel-labelContainer': {
              width: '120px !important',
              maxWidth: '120px !important',
            },
            '& .MuiStepLabel-alternativeLabel .MuiStepLabel-label': {
              whiteSpace: 'normal !important',
              wordWrap: 'break-word !important',
              textAlign: 'center !important',
              lineHeight: '1.3 !important',
              // width: '150px !important',
              // maxWidth: '150px !important',
            },
          }}
        >
          {steps.map((step) => (
            <Step
              key={step.key}
              onClick={() => navigate(step.url)}
              sx={{
                cursor: 'pointer',
                flexShrink: 0, // Prevent step from shrinking
                width: '150px', // Fixed width for each step
                minWidth: '150px', // Ensure minimum width
                '& .MuiStepIcon-root.Mui-active': {
                  color: '#0100cb',
                },
              }}
            >
              <StepLabel sx={{ cursor: 'pointer' }}>{step.label}</StepLabel>
            </Step>
          ))}
        </Stepper>
      </Box>
    </>
  )
}
