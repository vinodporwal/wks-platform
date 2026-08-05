import React from 'react'
import { Box } from '@mui/material'
import UserRoleInspectorPanel from '../utilities/UserRoleInspectorPanel'

const UserRoleInspectorPage = ({
  lookupUser,
  lookupUserOptions,
  handleUserSearchForLookup,
  lookupUserLoading,
  handleRetrieveUserRoles,
  retrievedUserRoles,
  retrievingRoles,
  handleOpenUnassignDialog,
  rolesFormattedForSelect = [],
  showNotification,
  keycloak,
}) => {
  return (
    <Box sx={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <UserRoleInspectorPanel
        lookupUser={lookupUser}
        lookupUserOptions={lookupUserOptions}
        handleUserSearchForLookup={handleUserSearchForLookup}
        lookupUserLoading={lookupUserLoading}
        handleRetrieveUserRoles={handleRetrieveUserRoles}
        retrievedUserRoles={retrievedUserRoles}
        retrievingRoles={retrievingRoles}
        handleOpenUnassignDialog={handleOpenUnassignDialog}
        rolesFormattedForSelect={rolesFormattedForSelect}
        showNotification={showNotification}
        keycloak={keycloak}
      />
    </Box>
  )
}

export default UserRoleInspectorPage
