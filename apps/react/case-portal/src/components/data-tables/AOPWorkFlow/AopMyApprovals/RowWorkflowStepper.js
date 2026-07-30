import React, { useEffect, useState } from 'react'
import { Box, CircularProgress, Typography } from '@mui/material'
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

      const pid = row.plantId || row.pid || row.plant_id || row.id
      const year = row.year

      if (!pid || !year) {
        if (active) setError('Missing required plantId or year.')
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
        <Typography variant='body2' sx={{ color: '#64748b', fontSize: '0.8rem', fontWeight: 500 }}>
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
        p: 1.5,
        my: 0.5,
        width: '100%',
        boxSizing: 'border-box',
        overflowX: 'auto',
        backgroundColor: '#f8fafc',
        borderRadius: '8px',
        border: '1px solid #cbd5e1',
        boxShadow: 'inset 0 1px 3px rgba(0, 0, 0, 0.04)',
      }}
    >
      <Typography
        variant='caption'
        sx={{
          display: 'block',
          mb: 1,
          fontWeight: 700,
          color: '#334155',
          fontSize: '0.72rem',
          letterSpacing: '0.5px',
          textTransform: 'uppercase',
        }}
      >
        Workflow Details
      </Typography>
      <AopWorkflowStepper steps={masterSteps} activeStep={activeStep > -1 ? activeStep : 0} />
    </Box>
  )
}

export default RowWorkflowStepper
