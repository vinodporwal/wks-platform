import React, { useEffect, useState } from 'react'
import { Box, Chip, CircularProgress, Typography } from '@mui/material'
import TimelineIcon from '@mui/icons-material/Timeline'
import { useSession } from 'SessionStoreContext'
import { AopApprovalService } from 'services/AopApprovalService'
import AopWorkflowStepper from 'components/Utilities/AopWorkflowStepper'

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

      let pid = row.plantId || row.pid || row.plant_id
      if (!pid && row.id && typeof row.id === 'string' && !row.id.includes('_') && row.id.length > 20) {
        pid = row.id
      }
      let sid = row.siteId || row.sid || row.sId || row.site_id
      let v_id = row.verticalId || row.v_id || row.vid || row.vertical_id
      const year = row.year

      if (!year || (!pid && !row.plantName)) {
        if (active) setError('Missing required plant information or year.')
        return
      }

      setLoading(true)
      setError(null)
      try {
        const status = await AopApprovalService.getStatus(keycloak, pid, year)
        if (!active) return

        const steps = status?.steps || []
        setMasterSteps(steps)

        const activeIdx = steps.findIndex((s) => s.status === 'inprogress')
        if (activeIdx > -1) {
          setActiveStep(activeIdx)
        } else if (steps.every((s) => s.status === 'completed')) {
          setActiveStep(steps.length)
        } else if (typeof status?.currentSequence === 'number' && status.currentSequence > 0) {
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
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, py: 1.5, px: 2 }}>
        <CircularProgress size={18} color='primary' />
        <Typography variant='body2' sx={{ color: '#0369a1', fontSize: '0.8rem', fontWeight: 600 }}>
          Loading workflow details...
        </Typography>
      </Box>
    )
  }

  if (error) {
    return (
      <Box sx={{ py: 1, px: 2 }}>
        <Typography variant='caption' sx={{ color: '#d32f2f', fontWeight: 600 }}>
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
      <AopWorkflowStepper steps={masterSteps} activeStep={activeStep > -1 ? activeStep : 0} />
    </Box>
  )
}

export default RowWorkflowStepper
