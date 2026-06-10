import React, { useState } from 'react'
import {
  Box,
  Button,
  Typography,
} from '../../../../../../node_modules/@mui/material/index'
import RevConfirmDialog from './RevConfirmDialog'

const RevButtonSection = ({
  setSnackbarData,
  setSnackbarOpen,
  revisionUpdated,
  setRevisionUpdated,
}) => {
  const [openConfirmDialogRev, setOpenConfirmDialogRev] = useState(false)
  const [selectedRevNum, setSelectedRevNum] = useState(null)
  const [revision, setRevision] = useState('0')
  const [revisionDetails, setRevisionDetails] = useState(null)

  const handleOpenDialogRev = (num) => {
    setSelectedRevNum(num)
    setOpenConfirmDialogRev(true)
  }

  const handleCloseDialogRev = () => {
    setOpenConfirmDialogRev(false)
    setSelectedRevNum(null)
  }

  const handleConfirmLoadRev = () => {
    setOpenConfirmDialogRev(false)
    handleRevisionChange(selectedRevNum)
  }

  const handleRevisionChange = async (num) => {
    setRevision(num)
    console.log('revisionDetails:', revisionDetails)
    // if (!revisionDetails || revisionDetails.length === 0) {
    //   console.log('Returning early - revisionDetails is null or empty')
    //   return
    // }
    const payload = revisionDetails ? { ...revisionDetails } : {}
    payload.attributeValueVersion = num
    payload.attributeValue = num
    await updateRevision([payload])
  }

  const updateRevision = async (Payload) => {
    try {
      // var response = await DataService.updateRevision(
      //   keycloak,
      //   Payload,
      //   PLANT_ID,
      //   AOP_YEAR,
      // )
      // fetchData()
      setRevisionUpdated(true)
      console.log('Payload', Payload)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Revision updated successfully!',
        severity: 'success',
        duration: 3000,
      })
    } catch (error) {
      console.error('Error updating data:', error)
    }
  }

  return (
    <Box>
      <Box
        sx={{
          display: 'inline-flex',
          alignItems: 'center',
          p: '3px',
          bgcolor: 'rgba(0, 0, 0, 0.04)',
          borderRadius: '8px',
          border: '1px solid rgba(0, 0, 0, 0.05)',
        }}
      >
        <Typography
          variant='caption'
          sx={{
            px: 1,
            fontWeight: 700,
            color: 'text.secondary',
            fontSize: '0.65rem',
            textTransform: 'uppercase',
          }}
        >
          Revision
        </Typography>

        <Box sx={{ display: 'flex', gap: '2px' }}>
          {['0', '1', '2', '3'].map((num) => {
            const selected = revision === num

            return (
              <Button
                key={num}
                onClick={() => handleOpenDialogRev(num)}
                variant='text'
                size='small'
                sx={{
                  textTransform: 'none',
                  fontSize: '0.72rem',
                  minWidth: '45px',
                  height: '24px',
                  borderRadius: '6px',
                  transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',

                  // Active State
                  ...(selected && {
                    bgcolor: '#fff',
                    color: '#0100cb',
                    boxShadow: '0 2px 6px rgba(1, 0, 203, 0.15)',
                    fontWeight: 800,
                    '&:hover': { bgcolor: '#fff' },
                  }),

                  // Inactive State
                  ...(!selected && {
                    color: 'text.secondary',
                    fontWeight: 500,
                    '&:hover': {
                      bgcolor: 'rgba(1, 0, 203, 0.04)',
                      color: '#0100cb',
                    },
                  }),
                }}
              >
                R{num}
              </Button>
            )
          })}
        </Box>
      </Box>

      <RevConfirmDialog
        openConfirmDialogRev={openConfirmDialogRev}
        handleCloseDialogRev={handleCloseDialogRev}
        handleConfirmLoadRev={handleConfirmLoadRev}
      />
    </Box>
  )
}

export default RevButtonSection
