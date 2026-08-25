import React from 'react'
import { Box, Button, Chip, IconButton, Stack, Tooltip, Typography } from '@mui/material'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import HistoryIcon from '@mui/icons-material/History'
import FactoryIcon from '@mui/icons-material/Factory'
import LocationOnIcon from '@mui/icons-material/LocationOn'
import AccountTreeIcon from '@mui/icons-material/AccountTree'
import CalendarTodayIcon from '@mui/icons-material/CalendarToday'
import BadgeIcon from '@mui/icons-material/Badge'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import HourglassTopIcon from '@mui/icons-material/HourglassTop'
import RuleIcon from '@mui/icons-material/Rule'
import AccessTimeIcon from '@mui/icons-material/AccessTime'
import { RoleApprovalsTooltip, formatRoleName } from 'components/Utilities/AopWorkflowStepper'

export const formatActionDate = (ts) => {
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

/**
 * Calculates dynamic column width based on maximum character length of row values
 */
export const getColumnWidth = (
  field,
  headerTitle,
  filteredItems,
  items,
  baseMin = 110,
  padding = 70,
) => {
  let maxLen = String(headerTitle || '').length
  const dataset =
    filteredItems && filteredItems.length > 0 ? filteredItems : items

  if (Array.isArray(dataset) && dataset.length > 0) {
    dataset.forEach((row) => {
      let val = ''
      if (field === 'plantName') val = row.plantName || row.plant || ''
      else if (field === 'siteName') val = row.siteName || row.site || ''
      else if (field === 'verticalName')
        val = row.verticalName || row.vertical || ''
      else if (field === 'year') val = String(row.year || '')
      else if (field === 'actionTakenDate')
        val = formatActionDate(row.actionTakenDate)
      else if (field === 'gateDisplayName') {
        const isCompleted =
          row.status === 'completed' ||
          row.gateName === 'COMPLETED' ||
          String(row.gateDisplayName || '').toLowerCase().includes('approved')
        val = isCompleted ? 'All Approved' : (row.gateDisplayName || row.gateName || '')
      }
      else if (field === 'assignedRole') val = row.assignedRole || ''
      else if (field === 'action') val = 'Go to Plant'
      else val = String(row[field] || '')

      const strLen = String(val).trim().length
      if (strLen > maxLen) {
        maxLen = strLen
      }
    })
  }

  // Approx 8.5px per character + icon + chip padding
  const calcWidth = Math.ceil(maxLen * 8.5 + padding)
  return Math.max(calcWidth, baseMin)
}

/**
 * Permissions config object for KendoDataTables wrapper
 */
export const APPROVALS_PERMISSIONS = {
  hideUploadExcel: true,
  hideDownloadExcel: false,
  ExcelName: 'AOP_My_Pending_Approvals',
  hideCalculateButton: true,
  hideSaveButton: true,
  deleteButton: false,
  makePagable: true,
}

/**
 * Generates Kendo data table column definitions for approvals
 */
export const getApprovalsColumns = (
  filteredItems,
  items,
  navigatingId,
  handleGoToPlant,
  handleOpenAudit,
) => [
  {
    field: 'year',
    title: 'AOP Year',
    minWidth: getColumnWidth('year', 'AOP Year', filteredItems, items, 120),
    editable: false,
    cell: (props) => {
      const val = props.dataItem?.year || '-'
      return (
        <td style={{ padding: '6px 12px' }}>
          <Chip
            className='aop-chip aop-chip-year'
            size='small'
            icon={<CalendarTodayIcon style={{ fontSize: 13 }} />}
            label={val}
          />
        </td>
      )
    },
  },
  // {
  //   field: 'verticalName',
  //   title: 'Vertical',
  //   minWidth: getColumnWidth('verticalName', 'Vertical', filteredItems, items, 130),
  //   editable: false,
  //   cell: (props) => {
  //     const val = props.dataItem?.verticalName || props.dataItem?.vertical || '-'
  //     return (
  //       <td style={{ padding: '6px 12px' }}>
  //         <Chip
  //           className='aop-chip aop-chip-vertical'
  //           size='small'
  //           icon={<AccountTreeIcon style={{ fontSize: 14 }} />}
  //           label={val}
  //         />
  //       </td>
  //     )
  //   },
  // },
  {
    field: 'siteName',
    title: 'Site',
    minWidth: getColumnWidth('siteName', 'Site', filteredItems, items, 130),
    editable: false,
    cell: (props) => {
      const val = props.dataItem?.siteName || props.dataItem?.site || '-'
      return (
        <td style={{ padding: '6px 12px' }}>
          <Chip
            className='aop-chip aop-chip-site'
            size='small'
            icon={<LocationOnIcon style={{ fontSize: 14 }} />}
            label={val}
          />
        </td>
      )
    },
  },
  {
    field: 'plantName',
    title: 'Plant',
    minWidth: getColumnWidth('plantName', 'Plant', filteredItems, items, 130),
    editable: false,
    cell: (props) => {
      const val = props.dataItem?.plantName || props.dataItem?.plant || '-'
      return (
        <td style={{ padding: '6px 12px' }}>
          <Chip
            className='aop-chip aop-chip-plant'
            size='small'
            icon={<FactoryIcon style={{ fontSize: 14 }} />}
            label={val}
          />
        </td>
      )
    },
  },
  {
    field: 'gateDisplayName',
    title: 'Stage',
    minWidth: getColumnWidth(
      'gateDisplayName',
      'Stage',
      filteredItems,
      items,
      160,
    ),
    editable: false,
    cell: (props) => {
      const row = props.dataItem || {}
      const rawLabel = row.gateDisplayName || row.gateName || 'Pending'
      const rolesList = Array.isArray(row.listOfRoles) ? row.listOfRoles : []
      let stageClass = 'aop-chip-stage-default'
      let StageIcon = RuleIcon

      const isCompleted =
        String(rawLabel).toLowerCase().includes('approved') ||
        String(rawLabel).toLowerCase().includes('completed') ||
        row.status === 'completed' ||
        row.gateName === 'COMPLETED'

      const label = isCompleted ? 'All Approved' : rawLabel

      if (isCompleted) {
        stageClass = 'aop-chip-stage-approved'
        StageIcon = CheckCircleOutlineIcon
      } else if (
        String(label).toLowerCase().includes('pending') ||
        String(label).toLowerCase().includes('review')
      ) {
        stageClass = 'aop-chip-stage-pending'
        StageIcon = HourglassTopIcon
      } else if (
        String(label).toLowerCase().includes('gate') ||
        String(label).toLowerCase().includes('l1') ||
        String(label).toLowerCase().includes('l2')
      ) {
        stageClass = 'aop-chip-stage-gate'
        StageIcon = RuleIcon
      }

      const approvedCount = rolesList.filter((r) => r.approved).length
      const totalRoles = rolesList.length

      const chipElement = (
        <Chip
          className={`aop-chip ${stageClass}`}
          size='small'
          icon={<StageIcon style={{ fontSize: 14 }} />}
          label={label}
        />
      )

      return (
        <td style={{ textAlign: 'left', padding: '6px 12px' }}>
          {rolesList.length > 0 ? (
            <RoleApprovalsTooltip rolesList={rolesList}>
              {chipElement}
            </RoleApprovalsTooltip>
          ) : (
            chipElement
          )}
        </td>
      )
    },
  },
  {
    field: 'statusMode',
    title: 'Status',
    minWidth: 155,
    editable: false,
    cell: (props) => {
      const row = props.dataItem || {}
      const isCompleted =
        row.status === 'completed' ||
        row.gateName === 'COMPLETED' ||
        String(row.gateDisplayName).toLowerCase() === 'approved'
      const isAction = row.actions?.mode === 'ACTION'

      return (
        <td style={{ padding: '6px 12px' }}>
          {isCompleted ? (
            <Chip
              size='small'
              label='Approved'
              sx={{
                backgroundColor: '#dcfce7',
                color: '#15803d',
                fontWeight: 700,
                fontSize: '11px',
                border: '1px solid #86efac',
              }}
            />
          ) : isAction ? (
            <Chip
              size='small'
              label='Approval Pending'
              sx={{
                backgroundColor: '#eff6ff',
                color: '#1d4ed8',
                fontWeight: 600,
                fontSize: '11px',
                border: '1px solid #93c5fd',
              }}
            />
          ) : (
            <Chip
              size='small'
              label='In Progress'
              sx={{
                backgroundColor: '#fef3c7',
                color: '#b45309',
                fontWeight: 500,
                fontSize: '11px',
                border: '1px solid #fde68a',
              }}
            />
          )}
        </td>
      )
    },
  },
  // {
  //   field: 'assignedRole',
  //   title: 'Current Role',
  //   minWidth: getColumnWidth(
  //     'assignedRole',
  //     'Current Role',
  //     filteredItems,
  //     items,
  //     140,
  //   ),
  //   editable: false,
  //   cell: (props) => {
  //     const val = props.dataItem?.assignedRole || '-'
  //     return (
  //       <td style={{ padding: '6px 12px' }}>
  //         <Chip
  //           className='aop-chip aop-chip-role'
  //           size='small'
  //           icon={<BadgeIcon style={{ fontSize: 14 }} />}
  //           label={val}
  //         />
  //       </td>
  //     )
  //   },
  // },
  {
    field: 'actionTakenDate',
    title: 'ACTION AT',
    minWidth: getColumnWidth(
      'actionTakenDate',
      'ACTION AT',
      filteredItems,
      items,
      160,
    ),
    editable: false,
    cell: (props) => {
      const val = props.dataItem?.actionTakenDate
      const formatted = formatActionDate(val)
      return (
        <td style={{ padding: '6px 12px' }}>
          {val ? (
            <Chip
              size='small'
              icon={
                <AccessTimeIcon style={{ fontSize: 13, color: '#475569' }} />
              }
              label={formatted}
              sx={{
                backgroundColor: '#f8fafc',
                color: '#334155',
                fontSize: '11px',
                fontWeight: 500,
                border: '1px solid #cbd5e1',
              }}
            />
          ) : (
            <Typography variant='caption' sx={{ color: '#94a3b8' }}>
              -
            </Typography>
          )}
        </td>
      )
    },
  },
  {
    field: 'action',
    title: 'Action',
    minWidth: 120,
    editable: false,
    filterable: false,
    sortable: false,
    cell: (props) => {
      const row = props.dataItem || {}
      const rowId =
        row.id ||
        row.taskId ||
        (row.plantId && row.year ? `${row.plantId}_${row.year}` : '')
      const isNavigating = navigatingId === rowId

      return (
        <td style={{ textAlign: 'center', padding: '6px 12px' }}>
          <Stack
            direction='row'
            alignItems='center'
            justifyContent='center'
            spacing={0.75}
          >
            <Tooltip title='Go to Plant'>
              <span>
                <IconButton
                  size='small'
                  onClick={() => handleGoToPlant(row)}
                  disabled={Boolean(navigatingId)}
                  sx={{
                    backgroundColor: '#005eb8',
                    color: '#ffffff',
                    p: '6px',
                    borderRadius: '6px',
                    '&:hover': { backgroundColor: '#004b93' },
                    '&.Mui-disabled': {
                      backgroundColor: '#cbd5e1',
                      color: '#94a3b8',
                    },
                  }}
                >
                  <ArrowForwardIcon sx={{ fontSize: 16 }} />
                </IconButton>
              </span>
            </Tooltip>

            {typeof handleOpenAudit === 'function' && (
              <Tooltip title='View Audit Trail'>
                <IconButton
                  size='small'
                  onClick={() => handleOpenAudit(row)}
                  sx={{
                    backgroundColor: '#ffffff',
                    color: '#475569',
                    border: '1px solid #cbd5e1',
                    p: '5px',
                    borderRadius: '6px',
                    '&:hover': {
                      backgroundColor: '#f1f5f9',
                      color: '#0f172a',
                      borderColor: '#94a3b8',
                    },
                  }}
                >
                  <HistoryIcon sx={{ fontSize: 17 }} />
                </IconButton>
              </Tooltip>
            )}
          </Stack>
        </td>
      )
    },
  },
]
