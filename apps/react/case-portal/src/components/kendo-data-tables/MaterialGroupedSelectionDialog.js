import React, { useState, useEffect, useCallback } from 'react'
import { Button, Dialog, DialogActions, DialogContent } from '@mui/material'
import MaterialGroupedSelection from './MaterialGroupedSelection'
import { checkMaterialGroupedSelectionRequired } from 'utils/materialGroupedSelectionPopupUtils'

/**
 * MaterialGroupedSelectionDialog Component
 * Contains all dialog JSX and MaterialGroupedSelection wrapper
 */
export const MaterialGroupedSelectionDialog = ({
  open,
  onClose,
  onSaveSuccess,
}) => {
  return (
    <Dialog
      open={open}
      onClose={(event, reason) => {
        if (reason !== 'backdropClick') {
          onClose()
        }
      }}
      maxWidth='md'
      fullWidth
      disableScrollLock
      disableEnforceFocus={true}
      PaperProps={{
        sx: {
          borderRadius: '20px',
          p: 1,
          backdropFilter: 'blur(8px)',
          boxShadow: '0 10px 30px rgba(0,0,0,0.08)',
        },
      }}
    >
      <DialogContent sx={{ p: 1 }}>
        <MaterialGroupedSelection
          onSaveSuccess={async () => {
            onClose()
            if (onSaveSuccess) {
              await onSaveSuccess()
            }
          }}
        />
      </DialogContent>
      <DialogActions sx={{ px: 1.5, pb: 1 }}>
        <Button
          onClick={onClose}
          variant='contained'
          className='btn-no'
          sx={{ textTransform: 'none' }}
        >
          Close
        </Button>
      </DialogActions>
    </Dialog>
  )
}

/**
 * Custom hook to handle all Pop-Up operations, API checks, state, and calculate triggering
 */
export const useMaterialGroupedSelectionPopup = ({
  keycloak,
  plantId,
  onCalculate,
}) => {
  const [openDialog, setOpenDialog] = useState(false)
  const [isPopupRequired, setIsPopupRequired] = useState(false)

  useEffect(() => {
    let isMounted = true
    const checkStatus = async () => {
      if (!plantId) {
        if (isMounted) setIsPopupRequired(false)
        return
      }
      const required = await checkMaterialGroupedSelectionRequired(
        keycloak,
        plantId,
      )
      if (isMounted) {
        setIsPopupRequired(required)
      }
    }

    checkStatus()

    return () => {
      isMounted = false
    }
  }, [keycloak, plantId])

  const handleCalculate = useCallback(() => {
    if (isPopupRequired) {
      setOpenDialog(true)
    } else if (onCalculate) {
      onCalculate()
    }
  }, [isPopupRequired, onCalculate])

  const handleClose = useCallback(() => {
    setOpenDialog(false)
  }, [])

  return {
    openDialog,
    setOpenDialog,
    isPopupRequired,
    handleCalculate,
    handleClose,
  }
}

export default MaterialGroupedSelectionDialog
