import React from 'react'
import {
  Box,
  Stepper,
  Step,
  StepLabel,
  StepConnector,
  Typography,
  Chip,
  Paper,
  Tooltip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  styled,
} from '@mui/material'
import CheckIcon from '@mui/icons-material/Check'
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty'
import PriorityHighIcon from '@mui/icons-material/PriorityHigh'

/**
 * Format role identifier string (e.g. gms_head -> GMS Head)
 */
export const formatRoleName = (roleStr) => {
  if (!roleStr) return '-'
  return String(roleStr)
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (char) => char.toUpperCase())
}

/**
 * Shared Light Theme Role Approvals Mini-Table Tooltip
 */
export const RoleApprovalsTooltip = ({ rolesList = [], children }) => {
  if (!Array.isArray(rolesList) || rolesList.length === 0) {
    return children
  }

  const approvedCount = rolesList.filter((r) => r.approved).length
  const totalRoles = rolesList.length

  const tooltipContent = (
    <Box sx={{ p: 0.25, minWidth: 220 }}>
      <Typography
        sx={{
          fontWeight: 800,
          fontSize: '0.7rem',
          color: '#475569',
          mb: 0.75,
          textTransform: 'uppercase',
          letterSpacing: '0.4px',
        }}
      >
        Role Approvals ({approvedCount}/{totalRoles})
      </Typography>
      <TableContainer
        component={Paper}
        elevation={0}
        sx={{
          backgroundColor: '#ffffff',
          border: '1px solid #cbd5e1',
          borderRadius: '6px',
          overflow: 'hidden',
        }}
      >
        <Table size='small' sx={{ borderCollapse: 'collapse' }}>
          <TableHead sx={{ backgroundColor: '#f1f5f9' }}>
            <TableRow>
              <TableCell
                sx={{
                  color: '#334155',
                  fontWeight: 800,
                  fontSize: '0.68rem',
                  textTransform: 'uppercase',
                  py: 0.5,
                  px: 1,
                  borderRight: '1px solid #cbd5e1',
                  borderBottom: '1px solid #cbd5e1',
                }}
              >
                Role
              </TableCell>
              <TableCell
                sx={{
                  color: '#334155',
                  fontWeight: 800,
                  fontSize: '0.68rem',
                  textTransform: 'uppercase',
                  py: 0.5,
                  px: 1,
                  borderBottom: '1px solid #cbd5e1',
                  textAlign: 'center',
                }}
              >
                Status
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rolesList.map((r, idx) => (
              <TableRow
                key={idx}
                sx={{
                  '&:nth-of-type(even)': {
                    backgroundColor: '#f8fafc',
                  },
                }}
              >
                <TableCell
                  sx={{
                    color: '#0f172a',
                    fontWeight: 600,
                    fontSize: '0.72rem',
                    py: 0.5,
                    px: 1,
                    borderRight: '1px solid #e2e8f0',
                    borderBottom:
                      idx < rolesList.length - 1 ? '1px solid #e2e8f0' : 'none',
                  }}
                >
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 0.75,
                    }}
                  >
                    <Box
                      sx={{
                        width: 6,
                        height: 6,
                        borderRadius: '50%',
                        backgroundColor: r.approved ? '#16a34a' : '#d97706',
                      }}
                    />
                    {formatRoleName(r.role)}
                  </Box>
                </TableCell>
                <TableCell
                  sx={{
                    color: r.approved ? '#16a34a' : '#b45309',
                    fontWeight: 700,
                    fontSize: '0.68rem',
                    py: 0.5,
                    px: 1,
                    textAlign: 'center',
                    borderBottom:
                      idx < rolesList.length - 1 ? '1px solid #e2e8f0' : 'none',
                  }}
                >
                  {r.approved ? 'Approved' : 'Pending'}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )

  return (
    <Tooltip
      title={tooltipContent}
      placement='top'
      arrow
      enterDelay={100}
      componentsProps={{
        tooltip: {
          sx: {
            backgroundColor: '#ffffff',
            color: '#0f172a',
            boxShadow: '0 10px 25px rgba(15, 23, 42, 0.15)',
            border: '1px solid #cbd5e1',
            borderRadius: '8px',
            p: 1.25,
          },
        },
        arrow: {
          sx: { color: '#ffffff' },
        },
      }}
    >
      <Box component='span' sx={{ display: 'inline-block', width: '100%', cursor: 'pointer' }}>
        {children}
      </Box>
    </Tooltip>
  )
}

// Custom Connector with gradient/solid transition
const ColorlibConnector = styled(StepConnector)(() => ({
  [`&.${StepConnector.alternativeLabel}`]: {
    top: 15,
  },
  [`& .${StepConnector.line}`]: {
    height: 2,
    border: 0,
    backgroundColor: '#cbd5e1',
    borderRadius: 2,
  },
  [`&.${StepConnector.active} .${StepConnector.line}`]: {
    background: 'linear-gradient(90deg, #005eb8 0%, #2563eb 100%)',
  },
  [`&.${StepConnector.completed} .${StepConnector.line}`]: {
    background: '#005eb8',
  },
}))

// Custom Step Icon Component
const CustomStepIcon = (props) => {
  const { active, completed, error, icon, status } = props

  let iconContent = icon
  let bgColor = '#f8fafc'
  let textColor = '#64748b'
  let borderStyle = '1px solid #cbd5e1'
  let boxShadowStyle = 'none'

  if (status === 'completed' || completed) {
    bgColor = '#005eb8'
    textColor = '#ffffff'
    borderStyle = 'none'
    iconContent = <CheckIcon sx={{ fontSize: 16, color: '#ffffff' }} />
    boxShadowStyle = '0 2px 6px rgba(0, 94, 184, 0.25)'
  } else if (status === 'error' || error) {
    bgColor = '#ef4444'
    textColor = '#ffffff'
    borderStyle = 'none'
    iconContent = <PriorityHighIcon sx={{ fontSize: 16, color: '#ffffff' }} />
    boxShadowStyle = '0 2px 6px rgba(239, 68, 68, 0.25)'
  } else if (status === 'inprogress' || active) {
    bgColor = '#ffffff'
    textColor = '#005eb8'
    borderStyle = '2.5px solid #005eb8'
    iconContent = <HourglassEmptyIcon sx={{ fontSize: 14, color: '#005eb8' }} />
    boxShadowStyle = '0 0 0 3px rgba(0, 94, 184, 0.15)'
  }

  return (
    <Box
      sx={{
        width: 30,
        height: 30,
        borderRadius: '50%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: bgColor,
        color: textColor,
        border: borderStyle,
        boxShadow: boxShadowStyle,
        fontWeight: 700,
        fontSize: '0.78rem',
        fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
        transition: 'all 0.2s ease-in-out',
        cursor: 'pointer',
        '&:hover': {
          transform: 'scale(1.08)',
        },
      }}
    >
      {iconContent}
    </Box>
  )
}

/**
 * AopWorkflowStepper Utility Component (Compact Edition)
 */
const AopWorkflowStepper = ({ steps = [], activeStep = 0, onStepClick }) => {
  if (!steps || steps.length === 0) return null

  return (
    <Paper
      elevation={0}
      sx={{
        py: 1.5,
        px: 2,
        mb: 0.5,
        width: '100%',
        boxSizing: 'border-box',
        overflowX: 'auto',
        borderRadius: '8px',
        border: '1px solid #bae6fd',
        background: 'linear-gradient(180deg, #f0f7ff 0%, #e6f2fe 100%)',
        boxShadow:
          'inset 0 1px 3px rgba(0, 94, 184, 0.04), 0 2px 6px rgba(0, 94, 184, 0.05)',
      }}
    >
      <Stepper
        activeStep={activeStep}
        alternativeLabel
        connector={<ColorlibConnector />}
        sx={{
          minWidth: '850px',
          width: '100%',
          py: 0.5,
        }}
      >
        {steps.map((step, index) => {
          const status = (step.status || '').toLowerCase()
          const isCompleted = status === 'completed'
          const isInProgress = status === 'inprogress'
          const isError = status === 'error'
          const isPending = !isCompleted && !isInProgress && !isError

          let statusLabel = 'Pending'
          let statusColor = {
            bg: '#f1f5f9',
            color: '#64748b',
            border: '#cbd5e1',
          }

          if (isCompleted) {
            statusLabel = 'Completed'
            statusColor = { bg: '#e0f2fe', color: '#0369a1', border: '#bae6fd' }
          } else if (isInProgress) {
            statusLabel = 'In Progress'
            statusColor = { bg: '#fef3c7', color: '#b45309', border: '#fde68a' }
          } else if (isError) {
            statusLabel = 'Reverted'
            statusColor = { bg: '#fee2e2', color: '#b91c1c', border: '#fca5a5' }
          } else if (isPending) {
            statusLabel = 'Pending'
          }

          const rolesList =
            Array.isArray(step.listOfRoles) && step.listOfRoles.length > 0
              ? step.listOfRoles
              : Array.isArray(step.roles) && step.roles.length > 0
                ? step.roles
                : step.assignedRole
                  ? [{ role: step.assignedRole, approved: isCompleted }]
                  : []

          const stepLabelContent = (
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 0.5,
                cursor: 'pointer',
              }}
            >
              <Typography
                variant='body2'
                sx={{
                  fontWeight: isInProgress ? 700 : isCompleted ? 600 : 500,
                  fontSize: '0.72rem',
                  fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                  color: isInProgress
                    ? '#005eb8'
                    : isCompleted
                      ? '#1e293b'
                      : '#64748b',
                  lineHeight: 1.2,
                  textAlign: 'center',
                  wordBreak: 'break-word',
                  maxWidth: '135px',
                }}
              >
                {step.displayName}
              </Typography>

              <Chip
                label={statusLabel}
                size='small'
                sx={{
                  height: '18px',
                  fontSize: '0.62rem',
                  fontWeight: 600,
                  backgroundColor: statusColor.bg,
                  color: statusColor.color,
                  border: `1px solid ${statusColor.border}`,
                  '& .MuiChip-label': {
                    px: 0.8,
                    py: 0,
                  },
                }}
              />
            </Box>
          )

          return (
            <Step
              key={step.displayName || index}
              completed={isCompleted}
              onClick={() => onStepClick && onStepClick(step, index)}
              sx={{
                cursor: onStepClick ? 'pointer' : 'default',
                minWidth: '120px',
                px: 0.5,
              }}
            >
              <StepLabel
                StepIconComponent={(iconProps) =>
                  rolesList.length > 0 ? (
                    <RoleApprovalsTooltip rolesList={rolesList}>
                      <Box component='span'>
                        <CustomStepIcon
                          {...iconProps}
                          status={step.status}
                          icon={index + 1}
                        />
                      </Box>
                    </RoleApprovalsTooltip>
                  ) : (
                    <CustomStepIcon
                      {...iconProps}
                      status={step.status}
                      icon={index + 1}
                    />
                  )
                }
                sx={{
                  '&.MuiStepLabel-alternativeLabel': {
                    position: 'relative !important',
                    top: 'auto !important',
                    left: '0 !important',
                    right: '0 !important',
                    marginTop: '6px !important',
                    width: '100% !important',
                  },
                  '& .MuiStepLabel-labelContainer': {
                    width: '100% !important',
                  },
                  '& .MuiStepLabel-label.MuiStepLabel-alternativeLabel': {
                    position: 'relative !important',
                    top: 'auto !important',
                    left: '0 !important',
                    right: '0 !important',
                    marginTop: '6px !important',
                    width: '100% !important',
                  },
                }}
              >
                {rolesList.length > 0 ? (
                  <RoleApprovalsTooltip rolesList={rolesList}>
                    {stepLabelContent}
                  </RoleApprovalsTooltip>
                ) : (
                  stepLabelContent
                )}
              </StepLabel>
            </Step>
          )
        })}
      </Stepper>
    </Paper>
  )
}

export default AopWorkflowStepper
