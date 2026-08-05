import React from 'react'
import { Box } from '@mui/material'
import AssignRolesPanel from '../utilities/AssignRolesPanel'

const AssignRolesPage = ({
  userSearchOptions,
  selectedUsers,
  setSelectedUsers,
  handleUserSearchForAssign,
  userSearchLoading,
  rolesFormattedForSelect,
  selectedRoles,
  setSelectedRoles,
  assigningRoles,
  handleAssignRoles,
}) => {
  return (
    <Box sx={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <AssignRolesPanel
        userSearchOptions={userSearchOptions}
        selectedUsers={selectedUsers}
        setSelectedUsers={setSelectedUsers}
        handleUserSearchForAssign={handleUserSearchForAssign}
        userSearchLoading={userSearchLoading}
        rolesFormattedForSelect={rolesFormattedForSelect}
        selectedRoles={selectedRoles}
        setSelectedRoles={setSelectedRoles}
        assigningRoles={assigningRoles}
        handleAssignRoles={handleAssignRoles}
      />
    </Box>
  )
}

export default AssignRolesPage
