import React, { useEffect, useState } from 'react'
import { Box, Chip, CircularProgress, Typography } from '@mui/material'
import TimelineIcon from '@mui/icons-material/Timeline'
import { useSession } from 'SessionStoreContext'
import { AopApprovalService } from 'services/AopApprovalService'
import AopWorkflowStepper from 'components/Utilities/AopWorkflowStepper'
import { DataService } from 'services/DataService'

/**
 * RowWorkflowStepper Component
 * Fetches workflow status via /aop-approval/status (maps prepareRework → prepare).
 */
const RowWorkflowStepper = ({ row }) => {
  const keycloak = useSession()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [masterSteps, setMasterSteps] = useState([])
  const [activeStep, setActiveStep] = useState(0)

  useEffect(() => {
    let active = true
    const fetchCaseDetails = async () => {
      if (!row) return

      let pid =
        row.plantId ||
        row.plantFKId ||
        row.plant_fk_id ||
        row.pid ||
        row.plant_id
      const year = row.year ? String(row.year) : ''

      if (!pid && row.plantName) {
        try {
          const hierarchy = await DataService.getAllSites(keycloak)
          if (active && Array.isArray(hierarchy)) {
            for (const vertical of hierarchy) {
              for (const site of vertical.sites || []) {
                for (const plant of site.plants || []) {
                  if (
                    String(plant.id).toUpperCase() ===
                      String(row.plantName).toUpperCase() ||
                    String(
                      plant.name || plant.displayName || '',
                    ).toUpperCase() === String(row.plantName).toUpperCase()
                  ) {
                    pid = plant.id
                    break
                  }
                }
                if (pid) break
              }
              if (pid) break
            }
          }
        } catch (e) {
          console.warn('Plant ID resolution from DataService failed:', e)
        }
      }

      if (!year || !pid) {
        if (active) setError('Missing required plant information or year.')
        return
      }

      pid = String(pid).toUpperCase()

      setLoading(true)
      setError(null)
      try {
        const status = await AopApprovalService.getStatus(keycloak, pid, year)
        if (!active) return

        const steps = status?.steps || []
        const enrichedSteps = steps.map((s) => {
          const hasRoles =
            (Array.isArray(s.listOfRoles) && s.listOfRoles.length > 0) ||
            (Array.isArray(s.roles) && s.roles.length > 0)
          if (hasRoles) return s

          const isMatchingGate =
            s.name === row?.gateName ||
            s.gateName === row?.gateName ||
            s.displayName === row?.gateDisplayName ||
            s.status === 'inprogress'

          if (isMatchingGate && (row?.listOfRoles || status?.listOfRoles)) {
            return {
              ...s,
              listOfRoles: row?.listOfRoles || status?.listOfRoles,
            }
          }
          return s
        })
        setMasterSteps(enrichedSteps)

        const activeIdx = steps.findIndex((s) => s.status === 'inprogress')
        if (activeIdx > -1) {
          setActiveStep(activeIdx)
        } else if (steps.every((s) => s.status === 'completed')) {
          setActiveStep(steps.length)
        } else if (
          typeof status?.currentSequence === 'number' &&
          status.currentSequence > 0
        ) {
          setActiveStep(Math.max(0, status.currentSequence - 1))
        } else {
          setActiveStep(0)
        }
      } catch (err) {
        if (active) setError(err.message || 'Failed to fetch workflow status.')
      } finally {
        if (active) setLoading(false)
      }
    }

    fetchCaseDetails()
    return () => {
      active = false
    }
  }, [keycloak, row])

  if (loading) {
    return (
      <Box
        sx={{ display: 'flex', alignItems: 'center', gap: 1.5, py: 1.5, px: 2 }}
      >
        <CircularProgress size={18} color='primary' />
        <Typography
          variant='body2'
          sx={{ color: '#0369a1', fontSize: '0.8rem', fontWeight: 600 }}
        >
          Loading workflow details...
        </Typography>
      </Box>
    )
  }

  if (error) {
    return (
      <Box sx={{ py: 1, px: 2 }}>
        <Typography
          variant='caption'
          sx={{ color: '#d32f2f', fontWeight: 600 }}
        >
          {error}
        </Typography>
      </Box>
    )
  }

  if (!masterSteps || masterSteps.length === 0) {
    return (
      <Box sx={{ py: 1, px: 2 }}>
        <Typography variant='caption' sx={{ color: '#64748b' }}>
          No workflow steps available.
        </Typography>
      </Box>
    )
  }

  return (
    <Box
      sx={{
        p: 1.75,
        my: 0.75,
        width: '100%',
        boxSizing: 'border-box',
        overflowX: 'auto',
        backgroundColor: '#ffffff',
        borderRadius: '10px',
        border: '1px solid #bae6fd',
        borderLeft: '5px solid #005eb8',
        boxShadow: '0 4px 14px rgba(0, 94, 184, 0.08)',
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1.25 }}>
        <Chip
          icon={<TimelineIcon style={{ fontSize: 15, color: '#005eb8' }} />}
          label='Workflow Details'
          size='small'
          sx={{
            fontWeight: 800,
            fontSize: '0.72rem',
            letterSpacing: '0.5px',
            textTransform: 'uppercase',
            backgroundColor: '#e0f2fe',
            color: '#0369a1',
            border: '1px solid #bae6fd',
            px: 0.5,
          }}
        />
      </Box>
      <AopWorkflowStepper
        steps={masterSteps}
        activeStep={activeStep > -1 ? activeStep : 0}
      />
    </Box>
  )
}

export default RowWorkflowStepper
