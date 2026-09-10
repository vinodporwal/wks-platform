import React, { useState, useEffect, useCallback } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { setIsReleased } from 'store/reducers/dataGridStore'
import { ReleaseAPIService } from 'components/aop-phase-two/services/common/releaseAPIService'
import ReleaseDialog from 'components/aop-phase-two/common/components/ReleaseDialog'

/**
 * Custom hook to manage the AOP Release workflow across vertical screens.
 * Handles release status checking, confirmation dialog state, release API submission,
 * snackbar feedback, and Redux state synchronization.
 *
 * @param {Object} [options]
 * @param {string} [options.plantId] - Optional explicit Plant ID (defaults to dataGridStore)
 * @param {string} [options.aopYear] - Optional explicit AOP Year (defaults to dataGridStore)
 * @param {Function} [options.setSnackbarOpen] - State setter for snackbar visibility
 * @param {Function} [options.setSnackbarData] - State setter for snackbar message and severity
 * @param {Function} [options.onReleaseSuccess] - Optional callback after successful release
 * @returns {Object} { isReleaseDisabled, setIsReleaseDisabled, openReleaseDialogBox, handleRelease, closeReleaseDialogBox, submitConfirmation, getIsReleased, releaseLoading, renderReleaseDialog, ReleaseDialogComponent }
 */
export const useReleaseAOP = (options = {}) => {
  const {
    plantId,
    aopYear,
    setSnackbarOpen,
    setSnackbarData,
    onReleaseSuccess,
  } = options

  const keycloak = useSession()
  const dispatch = useDispatch()
  const dataGridStore = useSelector((state) => state.dataGridStore)

  const PLANT_ID = plantId || dataGridStore?.plantObject?.id
  const AOP_YEAR = aopYear || dataGridStore?.year?.selectedYear

  const [openReleaseDialogBox, setOpenReleaseDialogBox] = useState(false)
  const [isReleaseDisabled, setIsReleaseDisabled] = useState(true)
  const [releaseLoading, setReleaseLoading] = useState(false)

  const getIsReleased = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    try {
      const response = await ReleaseAPIService.getReleaseAOPStatus(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.data && Object.keys(response.data).length > 0) {
        setIsReleaseDisabled(true)
      } else {
        setIsReleaseDisabled(false)
      }
    } catch (error) {
      console.error('Error fetching release status:', error)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      getIsReleased()
    }
  }, [PLANT_ID, AOP_YEAR, getIsReleased])

  const handleRelease = useCallback(() => {
    setOpenReleaseDialogBox(true)
  }, [])

  const closeReleaseDialogBox = useCallback(() => {
    setOpenReleaseDialogBox(false)
  }, [])

  const submitConfirmation = useCallback(async () => {
    setOpenReleaseDialogBox(false)
    setReleaseLoading(true)
    try {
      await ReleaseAPIService.releaseAOPReport(keycloak, PLANT_ID, AOP_YEAR)

      if (setSnackbarOpen && setSnackbarData) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Released Successfully!',
          severity: 'success',
        })
      }
      setIsReleaseDisabled(true)
      dispatch(setIsReleased({ isReleased: 1 }))
      if (onReleaseSuccess) {
        onReleaseSuccess()
      }
    } catch (error) {
      console.error('Error releasing report:', error)
      if (setSnackbarOpen && setSnackbarData) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Release Failed!',
          severity: 'error',
        })
      }
    } finally {
      setReleaseLoading(false)
    }
  }, [
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    dispatch,
    setSnackbarOpen,
    setSnackbarData,
    onReleaseSuccess,
  ])

  const renderReleaseDialog = () => (
    <ReleaseDialog
      openReleaseDialogBox={openReleaseDialogBox}
      closeReleaseDialogBox={closeReleaseDialogBox}
      submitConfirmation={submitConfirmation}
    />
  )

  return {
    isReleaseDisabled,
    setIsReleaseDisabled,
    openReleaseDialogBox,
    handleRelease,
    closeReleaseDialogBox,
    submitConfirmation,
    getIsReleased,
    releaseLoading,
    renderReleaseDialog,
    ReleaseDialogComponent: renderReleaseDialog(),
  }
}

export default useReleaseAOP
