// UtilityDetails.jsx
import React, { useState, useEffect } from 'react'
import { Box, Typography, Stack, Button, IconButton } from '@mui/material'
import { useDispatch, useSelector } from 'react-redux'
import { setScreenTitle } from 'store/reducers/dataGridStore'
import { useLocation } from 'react-router-dom'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import ReportProblemIcon from '@mui/icons-material/ReportProblem'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import SparklesIcon from '@mui/icons-material/AutoAwesome'

// const TOTAL_STEPS = 11 // fixed as requested

export default function UtilityDetails({ navigation }) {
  const [expanded, setExpanded] = useState(false) // default expanded
  const dispatch = useDispatch()
  const location = useLocation()
  const currentTitle = useSelector((s) => s.dataGridStore.screenTitle?.title)

  const [currentStep, setCurrentStep] = useState(null)
  const [totalSteps, setTotalSteps] = useState(0)

  useEffect(() => {
    if (!navigation?.items) {
      setCurrentStep(null)
      setTotalSteps(0)
      return
    }

    // collect item nodes across a group
    const collectItems = (menu, out = []) => {
      if (!menu?.children) return out
      for (const c of menu.children) {
        if (c.type === 'collapse') collectItems(c, out)
        else if (c.type === 'item') out.push(c)
      }
      return out
    }

    let matchedIndex = -1

    for (const g of navigation.items || []) {
      if (g.type !== 'group') continue
      // check branch contains path
      const items = collectItems(g, [])

      const allItems = collectItems(g, [])

      // 2. Filter to only include the specific path prefix
      const productionItems = allItems.filter((it) =>
        it.url?.startsWith('/production-norms-plan/'),
      )

      matchedIndex = items.findIndex((it) => it.url === location.pathname)
      if (matchedIndex !== -1) {
        // set current step (1-based)
        setCurrentStep(matchedIndex + 1)
        setTotalSteps(productionItems.length)

        // dispatch title if changed
        const matchedTitle = items[matchedIndex]?.title
        if (matchedTitle && matchedTitle !== currentTitle) {
          dispatch(setScreenTitle({ title: matchedTitle }))
        }
        break
      }
    }

    if (matchedIndex === -1) {
      setCurrentStep(null)
      setTotalSteps(0)
    }
  }, [navigation, location.pathname, currentTitle, dispatch])

  const toggleExpanded = () => setExpanded((s) => !s)

  // content strings (static)
  const peakTitle = 'Peak Performance'
  const peakBody =
    'EOE showed highest values in June, September, December, February, and March with 12.34 TPH and is performing good'
  const needTitle = 'Needs Attention'
  const needBody =
    'MEG shows significant fluctuations throughout the year, ranging from 4.32 to 12.34 TPH, which required attention'

  return (
    <Box sx={{ width: '100%', pl: 1, pr: 1, m: 0 }}>
      {/* header (compact) */}
      <Stack
        direction='row'
        alignItems='center'
        justifyContent='space-between'
        sx={{ p: 0, m: 0 }}
      >
        <Stack
          direction='row'
          alignItems='center'
          spacing={0}
          sx={{
            p: 0,
            m: 0,
            height: '28px', // keeps row tight like screenshot
          }}
        >
          <Typography
            sx={{
              fontWeight: 600, // lighter than 700
              fontSize: '1.05rem', // slightly larger for balance
              color: '#2b3a47', // softer than #213243
              lineHeight: 1,
              letterSpacing: '0.2px',
              p: 0,
              m: 0,
            }}
          >
            {currentTitle}
          </Typography>

          <Box
            sx={{
              ml: 1,
              px: '8px',
              py: '3px',
              borderRadius: '8px',
              background: '#f1e3d7', // slightly softer beige
              color: '#8b4513', // deeper brown tone
              fontWeight: 600,
              fontSize: '0.78rem',
              lineHeight: 1,
              display: 'flex',
              alignItems: 'center',
              height: '22px', // controls pill height
            }}
          >
            {currentStep
              ? `${currentStep} of ${totalSteps} Steps`
              : `— of ${totalSteps} Steps`}
          </Box>
        </Stack>

        {/* HIDE AS OF NOW  */}
        {/* HIDE AS OF NOW */}
        {false && (
          <Stack
            direction='row'
            spacing={0.5}
            alignItems='center'
            sx={{ p: 0, m: 0 }}
          >
            <IconButton
              onClick={toggleExpanded}
              size='small'
              sx={{
                border: '1px solid rgba(0,0,0,0.06)',
                borderRadius: '6px',
                p: 0.4,
                width: 34,
                height: 34,
              }}
            >
              {expanded ? (
                <ExpandLessIcon fontSize='small' />
              ) : (
                <ExpandMoreIcon fontSize='small' />
              )}
            </IconButton>

            <IconButton
              aria-label='tiny-action'
              sx={{
                width: 34,
                height: 34,
                borderRadius: '6px',
                border: '1px solid rgba(98,68,255,0.12)',
                background: 'transparent',
                color: '#6c5ce7',
                p: 0.4,
              }}
            >
              <SparklesIcon fontSize='small' />
            </IconButton>

            <Button
              variant='contained'
              size='small'
              onClick={() => {}}
              sx={{
                textTransform: 'none',
                fontWeight: 700,
                borderRadius: '8px',
                px: 1,
                py: 0.4,
                minWidth: 0,
                background: 'linear-gradient(90deg, #3b82f6 0%, #7c3aed 100%)',
                boxShadow: 'none',
                fontSize: '0.8rem',
              }}
            >
              Mark as Complete
            </Button>
          </Stack>
        )}
      </Stack>

      {/* cards (compact) */}
      {expanded && (
        <Box
          sx={{
            /* on small screens show horizontal scroll with thin custom scrollbar,
               on md+ revert to grid 2-column layout (keeps everything else unchanged) */
            display: { xs: 'flex', md: 'grid' },
            flexDirection: 'row',
            overflowX: { xs: 'auto', md: 'visible' },
            gap: { xs: '8px', md: '8px' },
            gridTemplateColumns: { md: '1fr 1fr' },
            mt: 0.5,

            /* custom thin scrollbar styling (webkit + firefox) */
            '&::-webkit-scrollbar': {
              height: '8px',
            },
            '&::-webkit-scrollbar-track': {
              background: 'transparent',
              borderRadius: '8px',
            },
            '&::-webkit-scrollbar-thumb': {
              background: '#cbd5e1', // light bluish-gray thumb
              borderRadius: '8px',
            },
            /* firefox */
            scrollbarWidth: 'thin',
            scrollbarColor: '#cbd5e1 transparent',
            /* ensure children don't shrink on flex layout */
            '& > .utility-card': {
              flex: { xs: '0 0 360px', md: 'unset' },
            },
          }}
        >
          {/* Peak Performance */}
          <Box
            className='utility-card'
            sx={{
              p: '1px', // tiny outer border to show gradient effect
              borderRadius: 2,
              background:
                'linear-gradient(90deg, rgba(59,130,246,0.12), rgba(124,58,237,0.12))',
              m: 0,
              /* add blue border as requested (visible even with gradient) */
              border: '1px solid rgba(59,130,246,0.6)',
            }}
          >
            <Box
              sx={{
                background:
                  'linear-gradient(180deg, rgba(250,245,247,0.8), rgba(255,250,253,0.8))',
                borderRadius: 2,
                px: 2, // compact inside padding
                py: 2,
                minHeight: 72,
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
              }}
            >
              <Stack
                direction='row'
                spacing={0.5}
                alignItems='center'
                sx={{ p: 0, m: 0 }}
              >
                <Box
                  sx={{
                    width: 28,
                    height: 28,
                    borderRadius: 2,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    background: 'rgba(99, 64, 160, 0.06)',
                    p: 0,
                    m: 0,
                  }}
                >
                  <AutoAwesomeIcon sx={{ color: '#8b5cf6', fontSize: 16 }} />
                </Box>

                <Typography sx={{ fontWeight: 800, fontSize: '0.95rem', m: 0 }}>
                  {peakTitle}
                </Typography>
              </Stack>

              <Typography
                sx={{
                  mt: 0.5,
                  color: '#243142',
                  fontSize: '0.85rem',
                  lineHeight: 1.3,
                }}
              >
                {peakBody}
              </Typography>
            </Box>
          </Box>

          {/* Needs Attention */}
          <Box
            className='utility-card'
            sx={{
              p: '1px',
              borderRadius: 2,
              background:
                'linear-gradient(90deg, rgba(255,96,92,0.08), rgba(124,58,237,0.08))',
              m: 0,
              /* add blue border as requested */
              border: '1px solid rgba(59,130,246,0.6)',
            }}
          >
            <Box
              sx={{
                background:
                  'linear-gradient(180deg, rgba(255,249,249,0.9), rgba(255,247,250,0.9))',
                borderRadius: 2,
                px: 2,
                py: 2,
                minHeight: 72,
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
              }}
            >
              <Stack
                direction='row'
                spacing={0.5}
                alignItems='center'
                sx={{ p: 0, m: 0 }}
              >
                <Box
                  sx={{
                    width: 28,
                    height: 28,
                    borderRadius: 2,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    background: 'rgba(255, 235, 238, 0.7)',
                    p: 0,
                    m: 0,
                  }}
                >
                  <ReportProblemIcon sx={{ color: '#e05252', fontSize: 16 }} />
                </Box>

                <Typography sx={{ fontWeight: 800, fontSize: '0.95rem', m: 0 }}>
                  {needTitle}
                </Typography>
              </Stack>

              <Typography
                sx={{
                  mt: 0.5,
                  color: '#243142',
                  fontSize: '0.85rem',
                  lineHeight: 1.3,
                }}
              >
                {needBody}
              </Typography>
            </Box>
          </Box>
        </Box>
      )}
    </Box>
  )
}
