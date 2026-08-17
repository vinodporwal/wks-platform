import React from 'react'
import { Box, Snackbar, Alert } from '@mui/material'
import useApprovalsInbox from './useApprovalsInbox'
import { getApprovalsColumns } from './approvalsColumns'
import ApprovalsHeader from './ApprovalsHeader'
import ApprovalsGrid from './ApprovalsGrid'
import AuditTrailDialog from './AuditTrailDialog'
import './AopMyApprovals.css'

/**
 * "My Approvals" inbox — displays pending AOP workflows with row-expandable stepper.
 */
const AopMyApprovals = ({ onClose }) => {
  const {
    items,
    loading,
    searchTerm,
    setSearchTerm,
    filteredItems,
    navigatingId,
    snackbarOpen,
    setSnackbarOpen,
    snackbarData,
    load,
    handleGoToPlant,
    toggleRowExpand,
    handleExpandChange,
    auditRow,
    handleOpenAudit,
    handleCloseAudit,
  } = useApprovalsInbox(onClose)

  const columns = getApprovalsColumns(
    filteredItems,
    items,
    navigatingId,
    handleGoToPlant,
    handleOpenAudit,
  )

  const isCompletedRow = (i) =>
    i.status === 'completed' ||
    i.status === 'approved' ||
    i.gateName === 'COMPLETED' ||
    String(i.gateDisplayName).toLowerCase() === 'approved'

  const approvedCount = filteredItems.filter((i) => isCompletedRow(i)).length
  const actionCount = filteredItems.filter(
    (i) => !isCompletedRow(i) && i.actions?.mode === 'ACTION',
  ).length
  const trackedCount = filteredItems.filter(
    (i) => !isCompletedRow(i) && i.actions?.mode !== 'ACTION',
  ).length

  return (
    <Box className='aop-my-approvals-container' sx={{ width: '100%' }}>
      <ApprovalsHeader
        onClose={onClose}
        itemCount={filteredItems.length}
        approvedCount={approvedCount}
        actionCount={actionCount}
        trackedCount={trackedCount}
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        load={load}
        loading={loading}
      />

      <Box className='aop-my-approvals-kendo-wrapper' sx={{ pt: 0.5 }}>
        <ApprovalsGrid
          rows={filteredItems}
          columns={columns}
          onExpandChange={handleExpandChange}
          loading={loading}
        />
      </Box>

      <AuditTrailDialog
        open={Boolean(auditRow)}
        onClose={handleCloseAudit}
        row={auditRow}
      />

      <Snackbar
        open={snackbarOpen}
        autoHideDuration={4000}
        onClose={() => setSnackbarOpen(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert
          onClose={() => setSnackbarOpen(false)}
          severity={snackbarData.severity || 'info'}
          variant='filled'
          sx={{ width: '100%' }}
        >
          {snackbarData.message}
        </Alert>
      </Snackbar>
    </Box>
  )
}

export default AopMyApprovals
