import React from 'react'
import { Box, Button, Chip, Typography } from '@mui/material'
import PersonAddIcon from '@mui/icons-material/PersonAdd'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'

/**
 * Extracts roles array from getRoles API response
 */
export const extractRoleItems = (res) => {
  if (Array.isArray(res?.data)) {
    return res.data
  } else if (Array.isArray(res?.data?.roles)) {
    return res.data.roles
  } else if (Array.isArray(res?.data?.roleDetails)) {
    return res.data.roleDetails
  } else if (Array.isArray(res?.roles)) {
    return res.roles
  } else if (Array.isArray(res)) {
    return res
  }
  return []
}

/**
 * Extracts user roles array from getUserRoles API response
 */
export const extractUserRoles = (rolesRes) => {
  if (Array.isArray(rolesRes?.data?.roles)) {
    return rolesRes.data.roles
  } else if (Array.isArray(rolesRes?.data?.roleDetails)) {
    return rolesRes.data.roleDetails.map((r) => r.name || r)
  } else if (Array.isArray(rolesRes?.roles)) {
    return rolesRes.roles
  } else if (Array.isArray(rolesRes?.data)) {
    return rolesRes.data
  } else if (Array.isArray(rolesRes)) {
    return rolesRes
  }
  return []
}

/**
 * Filters roles list based on search query
 */
export const filterRoles = (rolesList = [], query = '') => {
  if (!query) return rolesList
  const q = query.toLowerCase()
  return rolesList.filter((r) => {
    const nameStr = typeof r === 'string' ? r : r.name || r.code || ''
    const descStr = typeof r === 'string' ? '' : r.description || ''
    return (
      nameStr.toLowerCase().includes(q) ||
      descStr.toLowerCase().includes(q)
    )
  })
}

/**
 * Formats roles for Autocomplete selection
 */
export const formatRolesForSelect = (rolesList = []) => {
  return rolesList.map((r) =>
    typeof r === 'string'
      ? { id: r, name: r }
      : { id: r.name || r.id, name: r.name || r.code },
  )
}

/**
 * Formats rows for MUI DataGrid
 */
export const formatGridRows = (filteredRolesList = []) => {
  return filteredRolesList.map((r, index) => {
    const nameStr = typeof r === 'string' ? r : r.name || r.code || '-'
    const descStr =
      typeof r === 'string'
        ? 'System Realm Role'
        : r.description || 'System Realm Role'
    return {
      id: typeof r === 'string' ? r : r.id || r.name || index,
      name: nameStr,
      description: descStr,
      rawRole: r,
    }
  })
}

/**
 * Columns definition for System Roles Catalog DataGrid
 */
export const getRoleCatalogColumns = ({ onAssign, onDelete }) => [
  {
    field: 'name',
    headerName: 'Role Name',
    flex: 1,
    minWidth: 180,
    renderCell: (params) => (
      <Chip
        label={params.value}
        size="small"
        sx={{
          fontWeight: 700,
          backgroundColor: '#e0f2fe',
          color: '#0369a1',
          borderRadius: '4px',
          fontSize: '0.75rem',
          height: '22px',
        }}
      />
    ),
  },
  {
    field: 'description',
    headerName: 'Description',
    flex: 2,
    minWidth: 260,
    renderCell: (params) => (
      <Typography
        variant="body2"
        sx={{ color: '#475569', fontSize: '0.8rem' }}
      >
        {params.value}
      </Typography>
    ),
  },
  {
    field: 'actions',
    headerName: 'Actions',
    flex: 1,
    minWidth: 180,
    sortable: false,
    filterable: false,
    renderCell: (params) => {
      const rName = params.row.name
      return (
        <Box sx={{ display: 'flex', gap: 0.8, alignItems: 'center' }}>
          <Button
            size="small"
            variant="outlined"
            color="primary"
            startIcon={<PersonAddIcon style={{ fontSize: 14 }} />}
            onClick={() => onAssign(params.row.rawRole, rName)}
            sx={{
              textTransform: 'none',
              fontWeight: 700,
              borderRadius: '4px',
              fontSize: '0.7rem',
              py: 0.2,
              px: 1,
              minWidth: 'auto',
            }}
          >
            Assign
          </Button>
          <Button
            size="small"
            variant="outlined"
            color="error"
            startIcon={<DeleteOutlineIcon style={{ fontSize: 14 }} />}
            onClick={() => onDelete(rName)}
            sx={{
              textTransform: 'none',
              fontWeight: 700,
              borderRadius: '4px',
              fontSize: '0.7rem',
              py: 0.2,
              px: 1,
              minWidth: 'auto',
            }}
          >
            Delete
          </Button>
        </Box>
      )
    },
  },
]
