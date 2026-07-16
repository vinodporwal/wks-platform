import React, { useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Paper,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useSession } from 'SessionStoreContext'
import { AopApprovalService } from 'services/AopApprovalService'

/**
 * "My Approvals" inbox — every AOP workflow currently pending on one of the
 * logged-in user's roles, across all plants. Each row carries the taskId, so
 * Approve / Revert act directly without navigating. Server decides visibility;
 * only actionable items are returned.
 */
const AopMyApprovals = () => {
  const keycloak = useSession()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [rejectRow, setRejectRow] = useState(null)
  const [remark, setRemark] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [snack, setSnack] = useState({
    open: false,
    message: '',
    severity: 'info',
  })

  const load = async () => {
    setLoading(true)
    try {
      const data = await AopApprovalService.getMyPending(keycloak)
      setItems(Array.isArray(data) ? data : [])
    } catch (e) {
      setSnack({
        open: true,
        message: e.message || 'Failed to load approvals',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const act = async (row, decision, remarkText) => {
    if (
      decision === 'REVERTED' &&
      row.actions?.remarkMandatory &&
      !remarkText?.trim()
    ) {
      setSnack({
        open: true,
        message: 'A remark is required to revert',
        severity: 'error',
      })
      return
    }
    setBusyId(row.taskId)
    try {
      await AopApprovalService.act(keycloak, {
        taskId: row.taskId,
        plantId: row.plantId,
        year: row.year,
        gateName: row.gateName,
        decision,
        remark: remarkText || '',
        actorRole: row.assignedRole,
      })
      setSnack({
        open: true,
        message: decision === 'APPROVED' ? 'Approved' : 'Reverted for update',
        severity: 'success',
      })
      setRejectRow(null)
      setRemark('')
      await load()
    } catch (e) {
      setSnack({ open: true, message: e.message, severity: 'error' })
    } finally {
      setBusyId(null)
    }
  }

  return (
    <Box sx={{ p: 2 }}>
      <Stack
        direction='row'
        alignItems='center'
        justifyContent='space-between'
        sx={{ mb: 2 }}
      >
        <Typography variant='h5'>My Approvals</Typography>
        <Button variant='outlined' onClick={load} disabled={loading}>
          Refresh
        </Button>
      </Stack>

      {loading ? (
        <Stack alignItems='center' sx={{ py: 6 }}>
          <CircularProgress />
        </Stack>
      ) : items.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
          Nothing is pending your approval.
        </Paper>
      ) : (
        <TableContainer component={Paper}>
          <Table size='small'>
            <TableHead>
              <TableRow>
                <TableCell>Plant</TableCell>
                <TableCell>Site</TableCell>
                <TableCell>Vertical</TableCell>
                <TableCell>AOP Year</TableCell>
                <TableCell>Stage</TableCell>
                <TableCell>As role</TableCell>
                <TableCell align='right'>Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((row) => (
                <TableRow key={`${row.plantId}-${row.year}`} hover>
                  <TableCell>{row.plantName || row.plantId}</TableCell>
                  <TableCell>{row.siteName || '-'}</TableCell>
                  <TableCell>{row.verticalName || '-'}</TableCell>
                  <TableCell>{row.year}</TableCell>
                  <TableCell>
                    <Chip
                      size='small'
                      label={row.gateDisplayName || row.gateName}
                    />
                  </TableCell>
                  <TableCell>{row.assignedRole}</TableCell>
                  <TableCell align='right'>
                    <Stack
                      direction='row'
                      spacing={1}
                      justifyContent='flex-end'
                    >
                      <Button
                        size='small'
                        variant='contained'
                        disabled={
                          busyId === row.taskId || !row.actions?.canApprove
                        }
                        onClick={() => act(row, 'APPROVED')}
                      >
                        Approve
                      </Button>
                      <Button
                        size='small'
                        variant='outlined'
                        color='error'
                        disabled={
                          busyId === row.taskId || !row.actions?.canRevert
                        }
                        onClick={() => {
                          setRemark('')
                          setRejectRow(row)
                        }}
                      >
                        Revert
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Revert remark dialog */}
      <Dialog
        open={Boolean(rejectRow)}
        onClose={() => setRejectRow(null)}
        fullWidth
        maxWidth='sm'
      >
        <DialogTitle>Revert for Update / Improvement</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin='dense'
            label={
              rejectRow?.actions?.remarkMandatory
                ? 'Remark (required)'
                : 'Remark (optional)'
            }
            fullWidth
            multiline
            minRows={3}
            value={remark}
            onChange={(e) => setRemark(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectRow(null)}>Cancel</Button>
          <Button
            variant='contained'
            color='error'
            disabled={busyId === rejectRow?.taskId}
            onClick={() => act(rejectRow, 'REVERTED', remark)}
          >
            Revert
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snack.open}
        autoHideDuration={4000}
        onClose={() => setSnack((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          severity={snack.severity}
          onClose={() => setSnack((s) => ({ ...s, open: false }))}
        >
          {snack.message}
        </Alert>
      </Snackbar>
    </Box>
  )
}

export default AopMyApprovals
