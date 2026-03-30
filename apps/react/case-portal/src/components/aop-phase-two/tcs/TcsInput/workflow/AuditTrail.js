import React, { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Divider,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Box,
  CircularProgress,
  Tooltip,
} from '@mui/material'
import { TcsWorkflowApiService } from 'components/aop-phase-two/services/tcs/tcsWorkflowApiService'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import WorkflowTimeline from './WorkflowTimeline'
import { parseApprovalStatusResponse } from './utilityFunctions'
import { ROLES } from '../../utils/roleUtils'
import { formatToIST } from 'components/aop-phase-two/common/commonUtilityFunctions'

const RemarkCell = ({ text, maxLength = 150 }) => {
  const [expanded, setExpanded] = useState(false)
  const isLong = text && text.length > maxLength

  if (!text || text === '-' || text === '') {
    return (
      <Typography variant='body2' sx={{ color: 'text.secondary' }}>
        -
      </Typography>
    )
  }

  return (
    <Box>
      <Typography
        variant='body2'
        sx={{
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
        }}
      >
        {expanded || !isLong ? text : `${text.substring(0, maxLength)}...`}
      </Typography>
      {isLong && (
        <Button
          size='small'
          onClick={() => setExpanded(!expanded)}
          sx={{
            textTransform: 'none',
            minWidth: 'auto',
            p: 0,
            mt: 0.5,
            fontSize: '0.75rem',
            '&:hover': {
              backgroundColor: 'transparent',
              textDecoration: 'underline',
            },
          }}
        >
          {expanded ? 'View Less' : 'View More'}
        </Button>
      )}
    </Box>
  )
}

const AuditTrail = ({
  open,
  onClose,
  title = 'Audit Trail',
  plantId = null,
  userRole = null,
  timelineData = [],
}) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore

  const PLANT_ID = plantId || plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear

  const [auditTrailData, setAuditTrailData] = useState([])
  const [loading, setLoading] = useState(false)

  // For Plant Manager, show plant-specific workflow
  // For other roles (AOM, EPS Head, Cluster Head), show site-level workflow
  const selectedPlant = userRole === ROLES.PLANT_MANAGER ? PLANT_NAME : null

  const workflowSteps = parseApprovalStatusResponse(
    timelineData || [],
    selectedPlant,
  )
  useEffect(() => {
    const fetchAuditTrail = async () => {
      if (
        !open ||
        !keycloak ||
        !PLANT_ID ||
        !SITE_ID ||
        !VERTICAL_ID ||
        !AOP_YEAR
      ) {
        return
      }

      setLoading(true)

      try {
        const response = await TcsWorkflowApiService.getAuditTrail(
          keycloak,
          VERTICAL_ID,
          SITE_ID,
          AOP_YEAR,
        )

        const sortedData = (response || []).sort((a, b) => {
          const parseDate = (dateStr) => {
            if (!dateStr) return new Date(0)
            return new Date(dateStr)
          }

          const dateA = parseDate(
            a.timestamp || a.submissionDateTime || a.verifiedDateTime,
          )
          const dateB = parseDate(
            b.timestamp || b.submissionDateTime || b.verifiedDateTime,
          )

          return dateB.getTime() - dateA.getTime()
        })

        setAuditTrailData(sortedData)
      } catch (err) {
        console.error('Error fetching audit trail:', err)
        setAuditTrailData([])
      } finally {
        setLoading(false)
      }
    }

    fetchAuditTrail()
  }, [open, keycloak, PLANT_ID, SITE_ID, VERTICAL_ID, AOP_YEAR])

  const columns = [
    {
      field: 'plantName',
      header: 'Plant Name',
      width: '12%',
      minWidth: '120px',
    },
    {
      field: 'submissionDateTime',
      header: 'Submission Date',
      width: '14%',
      minWidth: '160px',
    },
    {
      field: 'userName',
      header: 'Submission User',
      width: '12%',
      minWidth: '120px',
    },
    {
      field: 'submittedBy',
      header: 'Submission Role',
      width: '12%',
      minWidth: '120px',
    },
    {
      field: 'submissionRemark',
      header: 'Submission Remarks',
      width: '20%',
      minWidth: '180px',
    },

    {
      field: 'status',
      header: 'Status',
      width: '10%',
      minWidth: '100px',
      isChip: true,
    },
  ]

  const getStatusColor = (status) => {
    switch (status?.toLowerCase()) {
      case 'approved':
        return 'success'
      case 'rejected':
        return 'error'
      case 'submitted':
        return 'warning'
      default:
        return 'default'
    }
  }

  const getTitle = () => {
    switch (userRole) {
      case ROLES.PLANT_MANAGER:
        return 'CTS Engineer History'
      case ROLES.CTS_HEAD:
      case ROLES.EPS_HEAD:
        return 'EPS/CTS Head History'
      case ROLES.EPS_ENGINEER:
        return 'AOM History'
      default:
        return title
    }
  }
  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth='md'
      fullWidth
      PaperProps={{
        sx: {
          minHeight: '400px',
          maxHeight: '80vh',
        },
      }}
    >
      <DialogTitle>
        <Typography variant='h6' component='div' fontWeight='600'>
          {/* {title} */}
          {getTitle()}
        </Typography>
      </DialogTitle>

      <Divider />

      <DialogContent sx={{ p: 0 }}>
        {/* Timeline Tab */}
        <Box sx={{ width: '100%', p: 3 }}>
          <WorkflowTimeline steps={workflowSteps} />
        </Box>

        {/* Audit Trail Tab */}
        <>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : auditTrailData.length === 0 ? (
            <Typography
              variant='body2'
              color='text.secondary'
              sx={{ textAlign: 'center', py: 4 }}
            >
              No history available
            </Typography>
          ) : (
            <TableContainer
              component={Paper}
              elevation={0}
              sx={{
                ml: 'auto',
                mr: 'auto',
                maxWidth: '95%',
                maxHeight: '350px',
                overflowX: 'auto',
                overflowY: 'auto',
                mt: 2,
                '&::-webkit-scrollbar': {
                  height: '6px',
                  width: '6px',
                },
                '&::-webkit-scrollbar-track': {
                  bgcolor: '#f5f5f5',
                },
                '&::-webkit-scrollbar-thumb': {
                  bgcolor: '#bdbdbd',
                  borderRadius: 1,
                  '&:hover': {
                    bgcolor: '#9e9e9e',
                  },
                },
              }}
            >
              <Table stickyHeader>
                <TableHead>
                  <TableRow>
                    {columns.map((col) => (
                      <TableCell
                        key={col.field}
                        sx={{
                          fontWeight: 600,
                          bgcolor: '#f5f5f5',
                          borderBottom: '2px solid #e0e0e0',
                          width: col.width,
                          minWidth: col.minWidth,
                          whiteSpace: 'nowrap',
                          position: 'sticky',
                          top: 0,
                          zIndex: 2,
                        }}
                      >
                        {col.header}
                      </TableCell>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {auditTrailData.map((item, index) => (
                    <TableRow
                      key={item.id || index}
                      sx={{
                        '&:hover': { bgcolor: 'action.hover' },
                        bgcolor: index === 0 ? '#f0f7ff' : 'inherit',
                      }}
                    >
                      {columns.map((col) => (
                        <TableCell
                          key={col.field}
                          sx={{
                            width: col.width,
                            minWidth: col.minWidth,
                          }}
                        >
                          {col.isChip ? (
                            <Tooltip title={item[col.field] || 'N/A'} arrow>
                              <Chip
                                label={item[col.field] || 'N/A'}
                                color={getStatusColor(item[col.field])}
                                size='small'
                                sx={{ fontWeight: 400 }}
                              />
                            </Tooltip>
                          ) : col.field === 'submissionRemark' ||
                            col.field === 'verifiedRemark' ? (
                            <RemarkCell text={item[col.field]} />
                          ) : col.field === 'submissionDateTime' ? (
                            <Typography
                              variant='body2'
                              sx={{
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-word',
                              }}
                            >
                              {formatToIST(item[col.field]) || '-'}
                            </Typography>
                          ) : (
                            <Typography
                              variant='body2'
                              sx={{
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-word',
                              }}
                            >
                              {item[col.field] || '-'}
                            </Typography>
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </>
      </DialogContent>

      <Divider />

      <DialogActions sx={{ p: 2, gap: 1 }}>
        <Typography
          variant='body2'
          color='text.secondary'
          sx={{ flex: 1, ml: 1 }}
        >
          Total Records: {auditTrailData.length}
        </Typography>
        <Button onClick={onClose} variant='outlined' color='error'>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default AuditTrail
