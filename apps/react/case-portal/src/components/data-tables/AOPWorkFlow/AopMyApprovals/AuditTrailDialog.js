import React, { useEffect, useRef, useState } from 'react'
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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
} from '@mui/material'
import HistoryIcon from '@mui/icons-material/History'
import CloseIcon from '@mui/icons-material/Close'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import UndoIcon from '@mui/icons-material/Undo'
import SendIcon from '@mui/icons-material/Send'
import CommentIcon from '@mui/icons-material/Comment'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import {
  ExcelExport,
  ExcelExportColumn,
} from '@progress/kendo-react-excel-export'
import { useSession } from 'SessionStoreContext'
import { AopApprovalService } from 'services/AopApprovalService'

/**
 * AuditTrailDialog Component
 * Displays compact, grid-lined table of workflow history with Excel export & full remark modal
 */
const AuditTrailDialog = ({ open, onClose, row }) => {
  const keycloak = useSession()
  const _excelExporter = useRef(null)

  const [loading, setLoading] = useState(false)
  const [history, setHistory] = useState([])
  const [error, setError] = useState(null)
  const [selectedRemarkItem, setSelectedRemarkItem] = useState(null)

  const getYearStr = (y) => {
    if (!y) return ''
    if (typeof y === 'string') return y
    if (typeof y === 'object' && y !== null) {
      return y.selectedYear || y.year || y.aopYear || y.oldYear || y.value || ''
    }
    return String(y)
  }

  const rawYear =
    row?.aopYear ||
    row?.AOP_YEAR ||
    row?.year ||
    row?.selectedYear ||
    row?.yearChanged
  const yearStr = getYearStr(rawYear) || getYearStr(row?.year)

  useEffect(() => {
    let active = true
    const fetchHistory = async () => {
      if (!open || !row) return
      const pid = row.plantId || row.pid || row.plant_id || row.id
      const yearVal = getYearStr(
        row.aopYear ||
          row.AOP_YEAR ||
          row.year ||
          row.selectedYear ||
          row.yearChanged,
      )

      if (!pid || !yearVal) {
        if (active)
          setError('Missing required plant ID or year for audit trail.')
        return
      }

      setLoading(true)
      setError(null)
      try {
        const data = await AopApprovalService.getAuditTrail(
          keycloak,
          pid,
          yearVal,
        )
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
    let dotColor = '#2563eb'
    let textColor = '#1d4ed8'
    let textLabel = actionStr || 'Submitted'

    if (act.includes('APPROV')) {
      dotColor = '#16a34a'
      textColor = '#15803d'
      textLabel = actionStr || 'Approved'
    } else if (act.includes('REVERT')) {
      dotColor = '#dc2626'
      textColor = '#b91c1c'
      textLabel = actionStr || 'Reverted'
    }

    return (
      <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75 }}>
        <Box
          sx={{
            width: 7,
            height: 7,
            borderRadius: '50%',
            backgroundColor: dotColor,
            flexShrink: 0,
          }}
        />
        <Typography
          component='span'
          sx={{
            fontSize: '0.78rem',
            fontWeight: 700,
            color: textColor,
            lineHeight: 1,
            cursor: 'default',
            userSelect: 'none',
          }}
        >
          {textLabel}
        </Typography>
      </Box>
    )
  }

  const siteName =
    row?.siteName ||
    row?.site ||
    row?.site_name ||
    row?.sidName ||
    row?.sName ||
    ''
  const plantName = row?.plantName || row?.plant || 'Plant'
  const year = yearStr || row?.year || '-'

  const handleExportExcel = () => {
    if (_excelExporter.current) {
      _excelExporter.current.save()
    }
  }

  // Render Table View with grid lines and solid compact headers
  const renderTableView = () => (
    <TableContainer
      component={Paper}
      elevation={0}
      sx={{
        border: '1px solid #cbd5e1',
        borderRadius: '8px',
        overflow: 'hidden',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.04)',
      }}
    >
      <Table size='small' sx={{ borderCollapse: 'collapse' }}>
        <TableHead>
          <TableRow sx={{ backgroundColor: '#cbd5e1' }}>
            <TableCell
              sx={{
                fontWeight: 800,
                color: '#0f172a',
                fontSize: '0.75rem',
                textTransform: 'uppercase',
                py: 1,
                px: 1.25,
                borderRight: '1px solid #94a3b8',
                borderBottom: '1px solid #94a3b8',
                width: '45px',
                textAlign: 'center',
              }}
            >
              #
            </TableCell>
            <TableCell
              sx={{
                fontWeight: 800,
                color: '#0f172a',
                fontSize: '0.75rem',
                textTransform: 'uppercase',
                py: 1,
                px: 1.25,
                borderRight: '1px solid #94a3b8',
                borderBottom: '1px solid #94a3b8',
              }}
            >
              Stage
            </TableCell>
            <TableCell
              sx={{
                fontWeight: 800,
                color: '#0f172a',
                fontSize: '0.75rem',
                textTransform: 'uppercase',
                py: 1,
                px: 1.25,
                borderRight: '1px solid #94a3b8',
                borderBottom: '1px solid #94a3b8',
              }}
            >
              Action
            </TableCell>
            <TableCell
              sx={{
                fontWeight: 800,
                color: '#0f172a',
                fontSize: '0.75rem',
                textTransform: 'uppercase',
                py: 1,
                px: 1.25,
                borderRight: '1px solid #94a3b8',
                borderBottom: '1px solid #94a3b8',
              }}
            >
              User
            </TableCell>
            <TableCell
              sx={{
                fontWeight: 800,
                color: '#0f172a',
                fontSize: '0.75rem',
                textTransform: 'uppercase',
                py: 1,
                px: 1.25,
                borderRight: '1px solid #94a3b8',
                borderBottom: '1px solid #94a3b8',
              }}
            >
              Role
            </TableCell>
            <TableCell
              sx={{
                fontWeight: 800,
                color: '#0f172a',
                fontSize: '0.75rem',
                textTransform: 'uppercase',
                py: 1,
                px: 1.25,
                borderRight: '1px solid #94a3b8',
                borderBottom: '1px solid #94a3b8',
                whiteSpace: 'nowrap',
              }}
            >
              Date &amp; Time
            </TableCell>
            <TableCell
              sx={{
                fontWeight: 800,
                color: '#0f172a',
                fontSize: '0.75rem',
                textTransform: 'uppercase',
                py: 1,
                px: 1.25,
                borderBottom: '1px solid #94a3b8',
              }}
            >
              Remarks
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {history.map((item, idx) => {
            const remarkText = item.remark || ''
            const isLongRemark = remarkText.length > 50

            return (
              <TableRow
                key={item.id || idx}
                hover
                sx={{
                  backgroundColor: idx % 2 === 0 ? '#ffffff' : '#f8fafc',
                  '&:hover': { backgroundColor: '#eff6ff !important' },
                }}
              >
                <TableCell
                  sx={{
                    fontSize: '0.75rem',
                    color: '#64748b',
                    fontWeight: 600,
                    py: 0.8,
                    px: 1.25,
                    borderRight: '1px solid #e2e8f0',
                    borderBottom: '1px solid #cbd5e1',
                    textAlign: 'center',
                  }}
                >
                  {idx + 1}
                </TableCell>
                <TableCell
                  sx={{
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    color: '#0f172a',
                    py: 0.8,
                    px: 1.25,
                    borderRight: '1px solid #e2e8f0',
                    borderBottom: '1px solid #cbd5e1',
                  }}
                >
                  {item.gateDisplayName || item.gateName || '-'}
                </TableCell>
                <TableCell
                  sx={{
                    py: 0.8,
                    px: 1.25,
                    borderRight: '1px solid #e2e8f0',
                    borderBottom: '1px solid #cbd5e1',
                  }}
                >
                  {getActionChip(item.action)}
                </TableCell>
                <TableCell
                  sx={{
                    fontSize: '0.78rem',
                    fontWeight: 600,
                    color: '#1e293b',
                    py: 0.8,
                    px: 1.25,
                    borderRight: '1px solid #e2e8f0',
                    borderBottom: '1px solid #cbd5e1',
                  }}
                >
                  {item.actorUserId || '-'}
                </TableCell>
                <TableCell
                  sx={{
                    fontSize: '0.78rem',
                    color: '#475569',
                    py: 0.8,
                    px: 1.25,
                    borderRight: '1px solid #e2e8f0',
                    borderBottom: '1px solid #cbd5e1',
                  }}
                >
                  {item.actorRole || '-'}
                </TableCell>
                <TableCell
                  sx={{
                    fontSize: '0.75rem',
                    color: '#475569',
                    fontWeight: 500,
                    whiteSpace: 'nowrap',
                    py: 0.8,
                    px: 1.25,
                    borderRight: '1px solid #e2e8f0',
                    borderBottom: '1px solid #cbd5e1',
                  }}
                >
                  {formatTimestamp(item.actionAt)}
                </TableCell>
                <TableCell
                  sx={{
                    fontSize: '0.78rem',
                    color: '#334155',
                    py: 0.8,
                    px: 1.25,
                    borderBottom: '1px solid #cbd5e1',
                  }}
                >
                  {remarkText ? (
                    <Tooltip
                      title={
                        <Box
                          sx={{
                            maxWidth: 380,
                            maxHeight: 220,
                            overflowY: 'auto',
                            whiteSpace: 'pre-wrap',
                            wordBreak: 'break-word',
                            fontSize: '0.82rem',
                            lineHeight: 1.5,
                            p: 0.25,
                          }}
                        >
                          {remarkText}
                        </Box>
                      }
                      placement='top'
                      arrow
                      componentsProps={{
                        tooltip: {
                          sx: {
                            backgroundColor: '#0f172a',
                            color: '#f8fafc',
                            boxShadow: '0 8px 24px rgba(0,0,0,0.25)',
                            borderRadius: '8px',
                            p: 1.25,
                          },
                        },
                        arrow: {
                          sx: {
                            color: '#0f172a',
                          },
                        },
                      }}
                    >
                      <Typography
                        variant='body2'
                        sx={{
                          fontSize: '0.78rem',
                          color: '#334155',
                          fontStyle: 'italic',
                          maxWidth: 280,
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          cursor: 'default',
                        }}
                      >
                        &quot;{remarkText}&quot;
                      </Typography>
                    </Tooltip>
                  ) : (
                    <span style={{ color: '#94a3b8' }}>-</span>
                  )}
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </TableContainer>
  )

  return (
    <>
      {/* Hidden Kendo ExcelExport Wrapper */}
      <ExcelExport
        data={history.map((item, idx) => ({
          seq: idx + 1,
          stage: item.gateDisplayName || item.gateName || '-',
          action: item.action || '-',
          user: item.actorUserId || '-',
          role: item.actorRole || '-',
          timestamp: formatTimestamp(item.actionAt),
          remarks: item.remark || '-',
        }))}
        fileName={`Audit_Trail_${plantName}_${year}.xlsx`}
        ref={_excelExporter}
      >
        <ExcelExportColumn field='seq' title='#' />
        <ExcelExportColumn field='stage' title='Stage' />
        <ExcelExportColumn field='action' title='Action' />
        <ExcelExportColumn field='user' title='User' />
        <ExcelExportColumn field='role' title='Role' />
        <ExcelExportColumn field='timestamp' title='Date & Time' />
        <ExcelExportColumn field='remarks' title='Remarks' />
      </ExcelExport>

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
        {/* Dialog Header */}
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
              <Typography
                variant='h6'
                sx={{ fontWeight: 800, fontSize: '1.05rem', color: '#0f172a' }}
              >
                Audit Trail
              </Typography>
              <Typography
                variant='caption'
                sx={{ color: '#475569', fontSize: '0.78rem', fontWeight: 500 }}
              >
                {siteName && (
                  <>
                    Site:{' '}
                    <strong style={{ color: '#0f172a', fontWeight: 700 }}>
                      {siteName}
                    </strong>
                    &nbsp;&bull;&nbsp;
                  </>
                )}
                Plant:{' '}
                <strong style={{ color: '#0f172a', fontWeight: 700 }}>
                  {plantName}
                </strong>
                &nbsp;&bull;&nbsp; Year:{' '}
                <strong style={{ color: '#0f172a', fontWeight: 700 }}>
                  AOP {year}
                </strong>
              </Typography>
            </Box>
          </Box>

          <Stack direction='row' alignItems='center' spacing={1}>
            <Tooltip title='Export Audit Trail to Excel'>
              <IconButton
                onClick={handleExportExcel}
                size='small'
                sx={{
                  color: '#005eb8',
                  border: '1px solid #cbd5e1',
                  borderRadius: '6px',
                  p: '6px',
                  backgroundColor: '#ffffff',
                  '&:hover': {
                    backgroundColor: '#eff6ff',
                    borderColor: '#93c5fd',
                    color: '#004b93',
                  },
                }}
              >
                <FileDownloadIcon sx={{ fontSize: 18 }} />
              </IconButton>
            </Tooltip>

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
          </Stack>
        </DialogTitle>

        {/* Dialog Content */}
        <DialogContent
          sx={{
            p: 2.5,
            backgroundColor: '#f8fafc',
            maxHeight: '65vh',
            overflowY: 'auto',
          }}
        >
          {loading ? (
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                py: 5,
                gap: 1.5,
              }}
            >
              <CircularProgress size={32} color='primary' />
              <Typography
                variant='body2'
                sx={{ color: '#64748b', fontWeight: 500 }}
              >
                Loading audit trail records...
              </Typography>
            </Box>
          ) : error ? (
            <Box sx={{ py: 3, px: 2, textAlign: 'center' }}>
              <Typography
                variant='body2'
                sx={{ color: '#d32f2f', fontWeight: 600 }}
              >
                {error}
              </Typography>
            </Box>
          ) : history.length === 0 ? (
            <Box sx={{ py: 5, textAlign: 'center' }}>
              <HistoryIcon sx={{ fontSize: 40, color: '#cbd5e1', mb: 1 }} />
              <Typography
                variant='body2'
                sx={{ color: '#64748b', fontWeight: 600 }}
              >
                No audit trail records found.
              </Typography>
              <Typography
                variant='caption'
                sx={{ color: '#94a3b8', display: 'block', mt: 0.5 }}
              >
                Decisions and submissions for this plant will appear here once
                recorded.
              </Typography>
            </Box>
          ) : (
            renderTableView()
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

      {/* Dedicated Full Remark Dialog Popup */}
      <Dialog
        open={Boolean(selectedRemarkItem)}
        onClose={() => setSelectedRemarkItem(null)}
        maxWidth='sm'
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: '10px',
            p: 0.5,
            boxShadow: '0 12px 32px rgba(15, 23, 42, 0.2)',
          },
        }}
      >
        <DialogTitle
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            pb: 1,
            pt: 1.5,
            px: 2,
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <CommentIcon sx={{ color: '#005eb8', fontSize: 20 }} />
            <Typography
              variant='h6'
              sx={{ fontWeight: 800, fontSize: '1rem', color: '#0f172a' }}
            >
              Workflow Remark -{' '}
              {selectedRemarkItem?.gateDisplayName ||
                selectedRemarkItem?.gateName ||
                'Stage'}
            </Typography>
          </Box>
          <IconButton size='small' onClick={() => setSelectedRemarkItem(null)}>
            <CloseIcon fontSize='small' />
          </IconButton>
        </DialogTitle>

        <DialogContent dividers sx={{ px: 2, py: 1.5 }}>
          <Box
            sx={{
              mb: 1.5,
              display: 'flex',
              gap: 2,
              flexWrap: 'wrap',
              backgroundColor: '#f1f5f9',
              p: 1,
              borderRadius: '6px',
            }}
          >
            <Typography
              variant='caption'
              sx={{ color: '#475569', fontWeight: 600 }}
            >
              User:{' '}
              <strong style={{ color: '#0f172a' }}>
                {selectedRemarkItem?.actorUserId || '-'}
              </strong>
            </Typography>
            <Typography
              variant='caption'
              sx={{ color: '#475569', fontWeight: 600 }}
            >
              Role:{' '}
              <strong style={{ color: '#0f172a' }}>
                {selectedRemarkItem?.actorRole || '-'}
              </strong>
            </Typography>
            <Typography
              variant='caption'
              sx={{ color: '#475569', fontWeight: 600 }}
            >
              Date &amp; Time:{' '}
              <strong style={{ color: '#0f172a' }}>
                {formatTimestamp(selectedRemarkItem?.actionAt)}
              </strong>
            </Typography>
          </Box>

          <Paper
            elevation={0}
            sx={{
              p: 2,
              backgroundColor: '#f8fafc',
              border: '1px solid #cbd5e1',
              borderRadius: '8px',
              maxHeight: '260px',
              overflowY: 'auto',
            }}
          >
            <Typography
              variant='body2'
              sx={{
                fontSize: '0.88rem',
                color: '#1e293b',
                lineHeight: 1.6,
                whiteSpace: 'pre-wrap',
                fontStyle: 'italic',
              }}
            >
              &quot;{selectedRemarkItem?.remark}&quot;
            </Typography>
          </Paper>
        </DialogContent>

        <DialogActions sx={{ pt: 1.25, pb: 1.5, px: 2 }}>
          <Button
            size='small'
            startIcon={<ContentCopyIcon sx={{ fontSize: 15 }} />}
            onClick={() => {
              if (selectedRemarkItem?.remark) {
                navigator.clipboard.writeText(selectedRemarkItem.remark)
              }
            }}
            sx={{
              color: '#005eb8',
              textTransform: 'none',
              fontWeight: 600,
              fontSize: '0.8rem',
            }}
          >
            Copy Remark
          </Button>
          <Button
            onClick={() => setSelectedRemarkItem(null)}
            variant='contained'
            size='small'
            sx={{
              backgroundColor: '#005eb8',
              textTransform: 'none',
              fontWeight: 600,
              px: 2.5,
              borderRadius: '6px',
            }}
          >
            Close
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

export default AuditTrailDialog
