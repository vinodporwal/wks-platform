import React from 'react'
import { Box } from '@mui/material'
import SystemRolesCatalogPanel from '../utilities/SystemRolesCatalogPanel'

const SystemRolesCatalogPage = ({
  filteredRolesList,
  roleSearchQuery,
  setRoleSearchQuery,
  fetchRoles,
  rolesLoading,
  availableScreens = [],
  handleUpdateRole,
  setSelectedRoles,
  showNotification,
  setRoleToDelete,
  setDeleteDialogOpen,
}) => {
  return (
    <Box sx={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <SystemRolesCatalogPanel
        filteredRolesList={filteredRolesList}
        roleSearchQuery={roleSearchQuery}
        setRoleSearchQuery={setRoleSearchQuery}
        fetchRoles={fetchRoles}
        rolesLoading={rolesLoading}
        availableScreens={availableScreens}
        handleUpdateRole={handleUpdateRole}
        setSelectedRoles={setSelectedRoles}
        showNotification={showNotification}
        setRoleToDelete={setRoleToDelete}
        setDeleteDialogOpen={setDeleteDialogOpen}
      />
    </Box>
  )
}

export default SystemRolesCatalogPage
