import React from 'react'
import { Box } from '@mui/material'
import UsersByRolesPanel from '../utilities/UsersByRolesPanel'

const UsersByRolesPage = ({
  rolesFormattedForSelect,
  selectedRoles,
  setSelectedRoles,
  onFetchUsers,
  onClearUsers,
  usersData,
  loading,
  totalUsers,
  page,
  pageSize,
  onPageChange,
  onPageSizeChange,
}) => {
  return (
    <Box sx={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <UsersByRolesPanel
        rolesFormattedForSelect={rolesFormattedForSelect}
        selectedRoles={selectedRoles}
        setSelectedRoles={setSelectedRoles}
        onFetchUsers={onFetchUsers}
        onClearUsers={onClearUsers}
        usersData={usersData}
        loading={loading}
        totalUsers={totalUsers}
        page={page}
        pageSize={pageSize}
        onPageChange={onPageChange}
        onPageSizeChange={onPageSizeChange}
      />
    </Box>
  )
}

export default UsersByRolesPage
