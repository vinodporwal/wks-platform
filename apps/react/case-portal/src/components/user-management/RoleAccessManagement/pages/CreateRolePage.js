import React from 'react'
import { Box } from '@mui/material'
import CreateRolePanel from '../utilities/CreateRolePanel'

const CreateRolePage = ({
  roleName,
  setRoleName,
  roleDescription,
  setRoleDescription,
  creatingRole,
  handleCreateRole,
}) => {
  return (
    <Box sx={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <CreateRolePanel
        roleName={roleName}
        setRoleName={setRoleName}
        roleDescription={roleDescription}
        setRoleDescription={setRoleDescription}
        creatingRole={creatingRole}
        handleCreateRole={handleCreateRole}
      />
    </Box>
  )
}

export default CreateRolePage
