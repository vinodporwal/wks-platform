import React, { useEffect, useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  Typography,
  IconButton,
  Button,
  CircularProgress,
  Chip,
  Paper,
  Stack,
  Divider,
  ToggleButtonGroup,
  ToggleButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material'
import HistoryIcon from '@mui/icons-material/History'
import CloseIcon from '@mui/icons-material/Close'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import UndoIcon from '@mui/icons-material/Undo'
import SendIcon from '@mui/icons-material/Send'
import PersonIcon from '@mui/icons-material/Person'
import BadgeIcon from '@mui/icons-material/Badge'
import AccessTimeIcon from '@mui/icons-material/AccessTime'
import CommentIcon from '@mui/icons-material/Comment'
import FactoryIcon from '@mui/icons-material/Factory'
import CalendarTodayIcon from '@mui/icons-material/CalendarToday'
import ViewAgendaIcon from '@mui/icons-material/ViewAgenda'
import TableChartIcon from '@mui/icons-material/TableChart'
import TimelineIcon from '@mui/icons-material/Timeline'
import AccountTreeIcon from '@mui/icons-material/AccountTree'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import { useSession } from 'SessionStoreContext'
import { AopApprovalService } from 'services/AopApprovalService'

/**
 * AuditTrailDialog Component
 * Supports 4 layout view modes: Cards, Table, Timeline (Graph), and Tree
 */
const AuditTrailDialog = ({ open, onClose, row }) => {
  const keycloak = useSession()
  const [loading, setLoading] = useState(false)
  const [history, setHistory] = useState([])
  const [error, setError] = useState(null)
  const [viewMode, setViewMode] = useState('cards') // 'cards' | 'table' | 'timeline' | 'tree'

  const getYearStr = (y) => {
    if (!y) return ''
    if (typeof y === 'string') return y
    if (typeof y === 'object' && y !== null) {
      return y.selectedYear || y.year || y.aopYear || y.oldYear || y.value || ''
    }
    return String(y)
  }

  const rawYear = row?.aopYear || row?.AOP_YEAR || row?.year || row?.selectedYear || row?.yearChanged
  const yearStr = getYearStr(rawYear) || getYearStr(row?.year)

  useEffect(() => {
    let active = true
    const fetchHistory = async () => {
      if (!open || !row) return
      const pid = row.plantId || row.pid || row.plant_id || row.id
      const yearVal = getYearStr(row.aopYear || row.AOP_YEAR || row.year || row.selectedYear || row.yearChanged)

      if (!pid || !yearVal) {
        if (active) setError('Missing required plant ID or year for audit trail.')
        return
      }

      setLoading(true)
      setError(null)
      try {
        const data = await AopApprovalService.getAuditTrail(keycloak, pid, yearVal)
        if (active) {
          setHistory(Array.isArray(data) ? data : [])
        }
      } catch (err) {
        if (active) {
          setError(err.message || 'Failed to fetch audit trail.')
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    fetchHistory()
    return () => {
      active = false
    }
  }, [open, row?.plantId, row?.pid, row?.id, yearStr, keycloak])

  const formatTimestamp = (ts) => {
    if (!ts) return '-'
    try {
      const date = new Date(ts)
      if (isNaN(date.getTime())) return String(ts)
      return date.toLocaleString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    } catch {
      return String(ts)
    }
  }

  const getActionChip = (actionStr) => {
    const act = String(actionStr || '').toUpperCase()
    if (act.includes('APPROV')) {
      return (
        <Chip
          size='small'
          icon={<CheckCircleOutlineIcon style={{ fontSize: 14 }} />}
          label={actionStr || 'Approved'}
          sx={{
            backgroundColor: '#ecfdf5',
            color: '#047857',
            border: '1px solid #a7f3d0',
            fontWeight: 700,
            fontSize: '0.72rem',
          }}
        />
      )
    }
    if (act.includes('REVERT')) {
      return (
        <Chip
          size='small'
          icon={<UndoIcon style={{ fontSize: 14 }} />}
          label={actionStr || 'Reverted'}
          sx={{
            backgroundColor: '#fef2f2',
            color: '#b91c1c',
            border: '1px solid #fca5a5',
            fontWeight: 700,
            fontSize: '0.72rem',
          }}
        />
      )
    }
    return (
      <Chip
        size='small'
        icon={<SendIcon style={{ fontSize: 13 }} />}
        label={actionStr || 'Submitted'}
        sx={{
          backgroundColor: '#eff6ff',
          color: '#1d4ed8',
          border: '1px solid #bfdbfe',
          fontWeight: 700,
          fontSize: '0.72rem',
        }}
      />
    )
  }

  const plantName = row?.plantName || row?.plant || 'Plant'
  const year = row?.year || '-'

  // Group history items by Gate for Tree View
  const groupedTreeData = history.reduce((acc, item) => {
    const gate = item.gateDisplayName || item.gateName || 'Stage General'
    if (!acc[gate]) acc[gate] = []
    acc[gate].push(item)
    return acc
  }, {})

  // 1. CARDS VIEW
  const renderCardsView = () => (
    <Stack spacing={1.5}>
      {history.map((item, index) => {
        const gate = item.gateDisplayName || item.gateName || `Stage ${item.sequence || index + 1}`
        const user = item.actorUserId || 'System User'
        const role = item.actorRole || '-'
        const dateStr = formatTimestamp(item.actionAt)
        const remarkText = item.remark

        return (
          <Paper
            key={item.id || index}
            elevation={0}
            sx={{
              p: 2,
              borderRadius: '10px',
              border: '1px solid #cbd5e1',
              backgroundColor: '#ffffff',
              boxShadow: '0 2px 4px rgba(0, 0, 0, 0.02)',
              transition: 'all 0.2s ease',
              '&:hover': {
                borderColor: '#94a3b8',
                boxShadow: '0 4px 8px rgba(0, 0, 0, 0.05)',
              },
            }}
          >
            <Stack direction='row' justifyContent='space-between' alignItems='center' flexWrap='wrap' gap={1} sx={{ mb: 1 }}>
              <Typography variant='subtitle2' sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.9rem' }}>
                {gate}
              </Typography>
              <Stack direction='row' spacing={1} alignItems='center'>
                {getActionChip(item.action)}
                <Chip
                  size='small'
                  icon={<AccessTimeIcon style={{ fontSize: 12 }} />}
                  label={dateStr}
                  variant='outlined'
                  sx={{ height: 22, fontSize: '0.68rem', color: '#64748b', borderColor: '#cbd5e1' }}
                />
              </Stack>
            </Stack>

            <Stack direction='row' spacing={2} alignItems='center' flexWrap='wrap' sx={{ mb: remarkText ? 1.25 : 0 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <PersonIcon sx={{ fontSize: 15, color: '#005eb8' }} />
                <Typography variant='caption' sx={{ color: '#334155', fontWeight: 600 }}>
                  User: <span style={{ color: '#0f172a', fontWeight: 700 }}>{user}</span>
                </Typography>
              </Box>

              {role && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <BadgeIcon sx={{ fontSize: 15, color: '#64748b' }} />
                  <Typography variant='caption' sx={{ color: '#334155', fontWeight: 600 }}>
                    Role: <span style={{ color: '#0f172a', fontWeight: 700 }}>{role}</span>
                  </Typography>
                </Box>
              )}
            </Stack>

            {remarkText && (
              <Box
                sx={{
                  p: 1.25,
                  borderRadius: '6px',
                  backgroundColor: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 1,
                }}
              >
                <CommentIcon sx={{ fontSize: 15, color: '#64748b', mt: 0.2 }} />
                <Typography variant='body2' sx={{ fontSize: '0.8rem', color: '#334155', fontStyle: 'italic', lineHeight: 1.4 }}>
                  &quot;{remarkText}&quot;
                </Typography>
              </Box>
            )}
          </Paper>
        )
      })}
    </Stack>
  )

  // 2. TABLE VIEW
  const renderTableView = () => (
    <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid #cbd5e1', borderRadius: '10px', overflow: 'hidden' }}>
      <Table size='small'>
        <TableHead sx={{ backgroundColor: '#f1f5f9' }}>
          <TableRow>
            <TableCell sx={{ fontWeight: 800, color: '#334155', fontSize: '0.75rem' }}>#</TableCell>
            <TableCell sx={{ fontWeight: 800, color: '#334155', fontSize: '0.75rem' }}>Stage / Gate</TableCell>
            <TableCell sx={{ fontWeight: 800, color: '#334155', fontSize: '0.75rem' }}>Action</TableCell>
            <TableCell sx={{ fontWeight: 800, color: '#334155', fontSize: '0.75rem' }}>User</TableCell>
            <TableCell sx={{ fontWeight: 800, color: '#334155', fontSize: '0.75rem' }}>Role</TableCell>
            <TableCell sx={{ fontWeight: 800, color: '#334155', fontSize: '0.75rem' }}>Timestamp</TableCell>
            <TableCell sx={{ fontWeight: 800, color: '#334155', fontSize: '0.75rem' }}>Remarks</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {history.map((item, idx) => (
            <TableRow key={item.id || idx} hover sx={{ '&:nth-of-type(even)': { backgroundColor: '#fafafa' } }}>
              <TableCell sx={{ fontSize: '0.78rem', color: '#64748b', fontWeight: 600 }}>{idx + 1}</TableCell>
              <TableCell sx={{ fontSize: '0.8rem', fontWeight: 700, color: '#0f172a' }}>
                {item.gateDisplayName || item.gateName || '-'}
              </TableCell>
              <TableCell>{getActionChip(item.action)}</TableCell>
              <TableCell sx={{ fontSize: '0.78rem', fontWeight: 600, color: '#1e293b' }}>{item.actorUserId || '-'}</TableCell>
              <TableCell sx={{ fontSize: '0.78rem', color: '#475569' }}>{item.actorRole || '-'}</TableCell>
              <TableCell sx={{ fontSize: '0.75rem', color: '#64748b', whiteSpace: 'nowrap' }}>{formatTimestamp(item.actionAt)}</TableCell>
              <TableCell sx={{ fontSize: '0.78rem', color: '#334155', fontStyle: item.remark ? 'italic' : 'normal' }}>
                {item.remark || '-'}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )

  // 3. TIMELINE / GRAPH VIEW
  const renderTimelineView = () => (
    <Box sx={{ position: 'relative', pl: 3, pr: 1, py: 1 }}>
      {/* Connected Line */}
      <Box
        sx={{
          position: 'absolute',
          top: 24,
          bottom: 24,
          left: 19,
          width: 3,
          backgroundColor: '#cbd5e1',
          borderRadius: 2,
        }}
      />
      <Stack spacing={2.5}>
        {history.map((item, idx) => {
          const act = String(item.action || '').toUpperCase()
          let nodeBg = '#005eb8'
          let NodeIcon = CheckCircleOutlineIcon
          if (act.includes('REVERT')) {
            nodeBg = '#ef4444'
            NodeIcon = UndoIcon
          } else if (act.includes('SUBMIT')) {
            nodeBg = '#2563eb'
            NodeIcon = SendIcon
          }

          return (
            <Box key={item.id || idx} sx={{ position: 'relative', display: 'flex', alignItems: 'flex-start', gap: 2 }}>
              {/* Node Circle */}
              <Box
                sx={{
                  position: 'absolute',
                  left: -32,
                  top: 2,
                  width: 28,
                  height: 28,
                  borderRadius: '50%',
                  backgroundColor: nodeBg,
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
                  zIndex: 2,
                }}
              >
                <NodeIcon sx={{ fontSize: 16 }} />
              </Box>

              <Paper
                elevation={0}
                sx={{
                  flex: 1,
                  p: 1.75,
                  borderRadius: '10px',
                  border: '1px solid #cbd5e1',
                  backgroundColor: '#ffffff',
                  boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
                }}
              >
                <Stack direction='row' justifyContent='space-between' alignItems='center' flexWrap='wrap' gap={1}>
                  <Typography variant='subtitle2' sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.88rem' }}>
                    {item.gateDisplayName || item.gateName || `Node ${idx + 1}`}
                  </Typography>
                  {getActionChip(item.action)}
                </Stack>

                <Stack direction='row' spacing={2} alignItems='center' sx={{ mt: 1, mb: item.remark ? 1 : 0 }}>
                  <Typography variant='caption' sx={{ color: '#475569', fontWeight: 600 }}>
                    By: <span style={{ color: '#0f172a', fontWeight: 700 }}>{item.actorUserId || 'User'}</span> ({item.actorRole || 'Role'})
                  </Typography>
                  <Typography variant='caption' sx={{ color: '#64748b' }}>
                    {formatTimestamp(item.actionAt)}
                  </Typography>
                </Stack>

                {item.remark && (
                  <Typography variant='body2' sx={{ fontSize: '0.78rem', color: '#334155', fontStyle: 'italic', mt: 0.5 }}>
                    &quot;{item.remark}&quot;
                  </Typography>
                )}
              </Paper>
            </Box>
          )
        })}
      </Stack>
    </Box>
  )

  // 4. TREE VIEW
  const renderTreeView = () => (
    <Stack spacing={1}>
      {Object.entries(groupedTreeData).map(([gateName, items], gIdx) => (
        <Accordion
          key={gateName || gIdx}
          defaultExpanded
          elevation={0}
          sx={{
            border: '1px solid #cbd5e1',
            borderRadius: '10px !important',
            overflow: 'hidden',
            '&:before': { display: 'none' },
          }}
        >
          <AccordionSummary
            expandIcon={<ExpandMoreIcon sx={{ color: '#005eb8' }} />}
            sx={{ backgroundColor: '#f1f5f9', minHeight: 44, px: 2 }}
          >
            <Stack direction='row' alignItems='center' spacing={1.25}>
              <AccountTreeIcon sx={{ fontSize: 18, color: '#005eb8' }} />
              <Typography variant='subtitle2' sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.88rem' }}>
                {gateName}
              </Typography>
              <Chip
                size='small'
                label={`${items.length} event${items.length > 1 ? 's' : ''}`}
                sx={{ height: 20, fontSize: '0.65rem', fontWeight: 700, backgroundColor: '#e2e8f0', color: '#334155' }}
              />
            </Stack>
          </AccordionSummary>
          <AccordionDetails sx={{ p: 1.5, backgroundColor: '#ffffff' }}>
            <Stack spacing={1}>
              {items.map((item, idx) => (
                <Box
                  key={item.id || idx}
                  sx={{
                    p: 1.25,
                    borderRadius: '8px',
                    backgroundColor: '#f8fafc',
                    border: '1px solid #e2e8f0',
                  }}
                >
                  <Stack direction='row' justifyContent='space-between' alignItems='center' flexWrap='wrap'>
                    <Stack direction='row' spacing={1} alignItems='center'>
                      {getActionChip(item.action)}
                      <Typography variant='caption' sx={{ color: '#0f172a', fontWeight: 700 }}>
                        {item.actorUserId} ({item.actorRole})
                      </Typography>
                    </Stack>
                    <Typography variant='caption' sx={{ color: '#64748b' }}>
                      {formatTimestamp(item.actionAt)}
                    </Typography>
                  </Stack>
                  {item.remark && (
                    <Typography variant='body2' sx={{ fontSize: '0.78rem', color: '#334155', fontStyle: 'italic', mt: 0.75 }}>
                      &quot;{item.remark}&quot;
                    </Typography>
                  )}
                </Box>
              ))}
            </Stack>
          </AccordionDetails>
        </Accordion>
      ))}
    </Stack>
  )

  return (
    <Dialog
      open={Boolean(open)}
      onClose={onClose}
      maxWidth='md'
      fullWidth
      PaperProps={{
        sx: {
          borderRadius: '12px',
          boxShadow: '0 16px 40px rgba(15, 23, 42, 0.2)',
          overflow: 'hidden',
        },
      }}
    >
      {/* Header */}
      <DialogTitle
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          py: 1.5,
          px: 2.5,
          background: 'linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%)',
          borderBottom: '1px solid #cbd5e1',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
          <Box
            sx={{
              width: 32,
              height: 32,
              borderRadius: '8px',
              backgroundColor: '#e0f2fe',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '1px solid #bae6fd',
            }}
          >
            <HistoryIcon sx={{ color: '#005eb8', fontSize: 20 }} />
          </Box>
          <Box>
            <Typography variant='h6' sx={{ fontWeight: 800, fontSize: '1.05rem', color: '#0f172a' }}>
              Workflow Audit Trail
            </Typography>
            <Stack direction='row' spacing={1} alignItems='center' sx={{ mt: 0.25 }}>
              <Chip
                size='small'
                icon={<FactoryIcon style={{ fontSize: 12 }} />}
                label={plantName}
                sx={{ height: 20, fontSize: '0.68rem', backgroundColor: '#f0fdf4', color: '#15803d', border: '1px solid #bbf7d0' }}
              />
              <Chip
                size='small'
                icon={<CalendarTodayIcon style={{ fontSize: 11 }} />}
                label={`AOP ${year}`}
                sx={{ height: 20, fontSize: '0.68rem', backgroundColor: '#fff7ed', color: '#c2410c', border: '1px solid #ffedd5' }}
              />
            </Stack>
          </Box>
        </Box>

        <IconButton
          onClick={onClose}
          size='small'
          sx={{
            color: '#64748b',
            '&:hover': { backgroundColor: '#e2e8f0', color: '#0f172a' },
          }}
        >
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      {/* Subheader Toolbar with 4 View Toggle Buttons */}
      <Box
        sx={{
          px: 2.5,
          py: 1.25,
          backgroundColor: '#ffffff',
          borderBottom: '1px solid #e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 1,
        }}
      >
        <Typography variant='caption' sx={{ fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
          Select Layout Presentation Mode:
        </Typography>

        <ToggleButtonGroup
          value={viewMode}
          exclusive
          onChange={(e, newMode) => newMode && setViewMode(newMode)}
          size='small'
          sx={{
            '& .MuiToggleButton-root': {
              textTransform: 'none',
              fontWeight: 700,
              fontSize: '0.75rem',
              px: 1.5,
              py: 0.5,
              color: '#475569',
              borderColor: '#cbd5e1',
              '&.Mui-selected': {
                backgroundColor: '#005eb8',
                color: '#ffffff',
                '&:hover': { backgroundColor: '#004b93' },
              },
            },
          }}
        >
          <ToggleButton value='cards'>
            <ViewAgendaIcon sx={{ fontSize: 16, mr: 0.6 }} />
            Cards
          </ToggleButton>
          <ToggleButton value='table'>
            <TableChartIcon sx={{ fontSize: 16, mr: 0.6 }} />
            Table
          </ToggleButton>
          <ToggleButton value='timeline'>
            <TimelineIcon sx={{ fontSize: 16, mr: 0.6 }} />
            Timeline / Graph
          </ToggleButton>
          <ToggleButton value='tree'>
            <AccountTreeIcon sx={{ fontSize: 16, mr: 0.6 }} />
            Tree
          </ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {/* Content */}
      <DialogContent sx={{ p: 2.5, backgroundColor: '#f8fafc', maxHeight: '60vh', overflowY: 'auto' }}>
        {loading ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 5, gap: 1.5 }}>
            <CircularProgress size={32} color='primary' />
            <Typography variant='body2' sx={{ color: '#64748b', fontWeight: 500 }}>
              Loading audit trail records...
            </Typography>
          </Box>
        ) : error ? (
          <Box sx={{ py: 3, px: 2, textAlign: 'center' }}>
            <Typography variant='body2' sx={{ color: '#d32f2f', fontWeight: 600 }}>
              {error}
            </Typography>
          </Box>
        ) : history.length === 0 ? (
          <Box sx={{ py: 5, textCenter: 'center', textAlign: 'center' }}>
            <HistoryIcon sx={{ fontSize: 40, color: '#cbd5e1', mb: 1 }} />
            <Typography variant='body2' sx={{ color: '#64748b', fontWeight: 600 }}>
              No audit trail records found.
            </Typography>
            <Typography variant='caption' sx={{ color: '#94a3b8', display: 'block', mt: 0.5 }}>
              Decisions and submissions for this plant will appear here once recorded.
            </Typography>
          </Box>
        ) : (
          <>
            {viewMode === 'cards' && renderCardsView()}
            {viewMode === 'table' && renderTableView()}
            {viewMode === 'timeline' && renderTimelineView()}
            {viewMode === 'tree' && renderTreeView()}
          </>
        )}
      </DialogContent>

      <Divider />

      <DialogActions sx={{ py: 1.25, px: 2.5, backgroundColor: '#ffffff' }}>
        <Button
          onClick={onClose}
          variant='outlined'
          size='small'
          sx={{
            textTransform: 'none',
            fontWeight: 600,
            color: '#475569',
            borderColor: '#cbd5e1',
            borderRadius: '6px',
            px: 2.5,
            '&:hover': { backgroundColor: '#f1f5f9', borderColor: '#94a3b8' },
          }}
        >
          Close
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default AuditTrailDialog
