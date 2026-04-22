import React, { useMemo, useState, useEffect, useCallback, useRef } from 'react'
import { Box, Button, Stack, Tooltip, Typography } from '@mui/material'
import { DatePicker } from '@progress/kendo-react-dateinputs'
import {
  CustomAccordion,
  CustomAccordionDetails,
  CustomAccordionSummary,
} from 'utils/CustomAccrodian'
import { useSession } from 'SessionStoreContext'
import Notification from 'components/aop-phase-two/common/utilities/Notification'
import {
  validateDateRange,
  buildConfigurationPayload,
} from '../../crude/production-norms-basis/utils/utility'
import ConfigurationDialog from './ConfigurationDialog'
import { HistoricPeriodBasisApiService } from 'components/aop-phase-two/services/common/historicPeriodBasisApiService'
import dataGridStore from 'store/reducers/dataGridStore'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { styled } from '@mui/material/styles'
import SettingsIcon from '@mui/icons-material/Settings'
import SyncIcon from '@mui/icons-material/Sync'
import HistoryIcon from '@mui/icons-material/History'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'

const CompactAccordion = styled(CustomAccordion)({
  mb: 0,
  borderRadius: '0px !important',
  boxShadow: 'none',
  borderBottom: '1px solid #bbc0c6',
  '&:before': { display: 'none' },
})

const ConfigurationAccordian = ({
  PLANT_ID,
  AOP_YEAR,
  isOldYear,
  isSummaryRequired = false,
  yearGap = 1,
  onDatesChange,
  onLoadNormCalculation = () => {},
  normCalculationLoading = false,
}) => {
  const keycloak = useSession()
  const hasExecutedRef = useRef(false)
  const { isReleased, oldYear } = dataGridStore
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  // State management
  const [startDate, setStartDate] = useState()
  const [endDate, setEndDate] = useState()
  const [summary, setSummary] = useState('')
  const [lastModifiedBy, setLastModifiedBy] = useState('')
  const [dateEdited, setDateEdited] = useState(false)
  const [summaryEdited, setSummaryEdited] = useState(false)
  const [configurationExecutionDetails, setConfigurationExecutionDetails] =
    useState([])
  const [openConfirmDialog, setOpenConfirmDialog] = useState(false)
  const [loading, setLoading] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  // Helper function to format dates for API
  const formatDate = (date) => {
    if (!date) return ''
    const year = date?.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  // Helper function to format dates for display
  const formatDateForText = (date, time = false) => {
    if (!date) return ''
    const parsedDate = new Date(date)
    if (isNaN(parsedDate)) return 'Invalid Date'
    const day = String(parsedDate.getDate()).padStart(2, '0')
    const month = String(parsedDate.getMonth() + 1).padStart(2, '0')
    const year = parsedDate.getFullYear()
    let formatted = `${day}-${month}-${year}`
    if (time) {
      let hours = parsedDate.getHours()
      const minutes = String(parsedDate.getMinutes()).padStart(2, '0')
      const ampm = hours >= 12 ? 'PM' : 'AM'
      hours = hours % 12
      hours = hours ? hours : 12
      const formattedTime = `${String(hours).padStart(2, '0')}:${minutes} ${ampm}`
      formatted += ` ${formattedTime}`
    }
    return formatted
  }

  // Fetch configuration execution details
  const fetchConfigurationDetails = async () => {
    try {
      const response =
        await HistoricPeriodBasisApiService.getConfigurationExecutionDetails(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      const details = response?.data || []
      if (details.length === 0) {
        console.warn(
          'getConfigurationExecutionDetails returned an empty array:',
          response,
        )
      }
      const hasNoModifiedOn = details.length && !details[0]?.ModifiedOn
      if (hasNoModifiedOn && !hasExecutedRef.current) {
        const startDateObj = details.find((item) => item.Name === 'StartDate')
        const endDateObj = details.find((item) => item.Name === 'EndDate')
        hasExecutedRef.current = true
        await onLoadTest(startDateObj, endDateObj)
      } else {
        setConfigurationExecutionDetails(details)
        // Capture who last modified the data
        if (details[0]?.User) {
          setLastModifiedBy(details[0].User)
        }
      }
    } catch (error) {
      console.error('Error fetching getConfigurationExecutionDetails:', error)
    }
  }

  // Initial load with configurable year period
  const onLoadTest = async (startDateObj, endDateObj) => {
    const today = new Date()
    const endDate = new Date(today.getFullYear(), today.getMonth(), 0)
    const startDate = new Date(
      today.getFullYear() - yearGap,
      today.getMonth(),
      1,
    )

    const createPayloadItem = (obj, date) => ({
      apr: date,
      UOM: '',
      auditYear: AOP_YEAR,
      normParameterFKId: obj?.NormParameter_FK_Id,
      remarks: 'Initiated',
      id: obj?.Id || null,
      plantId: PLANT_ID,
    })

    const payload = [
      createPayloadItem(startDateObj, formatDate(startDate)),
      createPayloadItem(endDateObj, formatDate(endDate)),
    ]

    try {
      setLoading(true)
      const response = await HistoricPeriodBasisApiService.executeConfiguration(
        payload,
        keycloak,
      )
      if (response?.code === 200) {
        await fetchConfigurationDetails()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Execution Failed!',
          severity: 'error',
        })
      }
      await fetchSummary()
      return response
    } catch (error) {
      console.error('Execution Failed!', error)
    } finally {
      setLoading(false)
    }
  }

  // Fetch AOP summary
  const fetchSummary = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const res = await HistoricPeriodBasisApiService.getAopSummary(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (res?.code === 200) {
        setSummary(res?.data?.summary || '')
      } else {
        setSummary('')
      }
    } catch (error) {
      console.error('Error fetching summary:', error)
    }
  }

  // Compute and set dates based on configuration details
  const computeAndSetDates = useCallback(() => {
    setStartDate('')
    setEndDate('')
    const hasModifiedOn = configurationExecutionDetails[0]?.ModifiedOn
    if (hasModifiedOn) {
      const getDateValue = (name) =>
        new Date(
          configurationExecutionDetails.find(
            (item) => item.Name === name,
          )?.AttributeValue,
        )
      setStartDate(getDateValue('StartDate'))
      setEndDate(getDateValue('EndDate'))
    } else {
      const today = new Date()
      const fallbackEndDate = new Date(today.getFullYear(), today.getMonth(), 0)
      const fallbackStartDate = new Date(
        today.getFullYear() - yearGap,
        today.getMonth(),
        1,
      )
      setStartDate(fallbackStartDate)
      setEndDate(fallbackEndDate)
    }
  }, [configurationExecutionDetails])

  // Dialog handlers
  const handleOpenDialog = () => {
    // Validate summary if required
    if (isSummaryRequired) {
      if (!summary || summary.trim() === '') {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Please add AOP Design Basis.',
          severity: 'error',
        })
        return
      }

      if (!summaryEdited) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Please update AOP Design Basis.',
          severity: 'error',
        })
        return
      }
    }

    setOpenConfirmDialog(true)
  }

  const handleCloseDialog = () => {
    setOpenConfirmDialog(false)
  }

  const handleConfirmLoad = async () => {
    setOpenConfirmDialog(false)
    await onLoad()
  }

  // Load data with user-selected dates
  const onLoad = async () => {
    // Validate dates
    const validation = validateDateRange(startDate, endDate)
    if (!validation.valid) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validation.message,
        severity: 'warning',
      })
      return
    }

    try {
      setLoading(true)

      // Save summary first if required
      if (isSummaryRequired) {
        await saveSummary(summary)
        setSummaryEdited(false)
      }

      // Build payload
      const payload = buildConfigurationPayload(
        startDate,
        endDate,
        configurationExecutionDetails,
        PLANT_ID,
        AOP_YEAR,
      )

      if (!payload) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Start/End date configuration is incomplete.',
          severity: 'error',
        })
        setLoading(false)
        return
      }

      // Execute configuration
      const response = await HistoricPeriodBasisApiService.executeConfiguration(
        payload,
        keycloak,
      )

      if (response) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Execution Started Successfully!',
          severity: 'success',
        })
        await fetchConfigurationDetails()
        await fetchSummary()

        // Trigger norm calculation after successful configuration load
        if (onLoadNormCalculation) {
          onLoadNormCalculation()
        }
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Execution Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Execution Failed!', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Execution Failed!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
      setDateEdited(false)
    }
  }

  const saveSummary = async (summary) => {
    try {
      const response = await HistoricPeriodBasisApiService.saveAOPSummary(
        PLANT_ID,
        AOP_YEAR,
        summary,
        keycloak,
      )

      return response
    } catch (error) {
      // console.error('Error saving Summary!', error)
    } finally {
      //
      // setLoading(false)
      fetchSummary()
    }
  }

  // Initialize on mount and when PLANT_ID/AOP_YEAR changes
  useEffect(() => {
    if (!PLANT_ID || !AOP_YEAR) return
    hasExecutedRef.current = false
    fetchConfigurationDetails()
    fetchSummary()
    setSummaryEdited(false)
    setDateEdited(false)
  }, [PLANT_ID, AOP_YEAR])

  // Compute dates when configuration details change
  useEffect(() => {
    computeAndSetDates()
  }, [computeAndSetDates])

  // Notify parent component when dates change
  useEffect(() => {
    if (onDatesChange && startDate && endDate) {
      onDatesChange(startDate, endDate)
    }
  }, [startDate, endDate, onDatesChange])

  const startDateConfig = configurationExecutionDetails.find(
    (item) => item.Name === 'StartDate',
  )

  const endDateConfig = configurationExecutionDetails.find(
    (item) => item.Name === 'EndDate',
  )

  const startDateFromConfig = new Date(startDateConfig?.AttributeValue)
  const endDateDateFromConfig = new Date(endDateConfig?.AttributeValue)

  const accordian = useMemo(() => {
    return (
      <Box sx={{ mb: 1 }}>
        <CompactAccordion defaultExpanded disableGutters>
          <CustomAccordionSummary
            expandIcon={
              <ExpandMoreIcon sx={{ fontSize: '1.1rem', color: '#0100cb' }} />
            }
            sx={{
              minHeight: '36px !important',
              px: 0.5,
              bgcolor: '#ffffff',
              '& .MuiAccordionSummary-content': { my: '4px !important' },
            }}
          >
            <Stack direction='row' spacing={1} alignItems='center'>
              <SettingsIcon sx={{ color: '#0100cb', fontSize: '1rem' }} />
              <Typography
                sx={{
                  fontWeight: 700,
                  fontSize: '0.8rem',
                  fontFamily:
                    "'Segoe UI', system-ui, -apple-system, 'Open Sans', Arial, sans-serif",
                  color: '#334155',
                }}
              >
                AOP Historical Period Basis
              </Typography>
            </Stack>
          </CustomAccordionSummary>

          <CustomAccordionDetails sx={{ p: 0.5, pt: 0 }}>
            <Stack direction='column' spacing={1.5}>
              {/* ROW 1: Date pickers + Refresh + Last refreshed */}
              <Stack
                direction='row'
                sx={{ columnGap: 1, rowGap: 0 }}
                alignItems='flex-start'
                flexWrap='wrap'
              >
                {/* START */}
                <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                  <Typography
                    variant='caption'
                    sx={{
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      fontFamily:
                        "'Segoe UI', system-ui, -apple-system, 'Open Sans', Arial, sans-serif",
                      color: '#334155',
                      letterSpacing: '0.3px',
                    }}
                  >
                    START
                  </Typography>
                  <DatePicker
                    id='start-date'
                    format='dd-MM-yyyy'
                    value={startDate}
                    onChange={(e) => {
                      setStartDate(e.value)
                      setDateEdited(true)
                    }}
                    style={{ width: '130px', height: '28px' }}
                    disabled={READ_ONLY}
                  />
                </Box>

                {/* END */}
                <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                  <Typography
                    variant='caption'
                    sx={{
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      fontFamily:
                        "'Segoe UI', system-ui, -apple-system, 'Open Sans', Arial, sans-serif",
                      color: '#334155',
                      letterSpacing: '0.3px',
                    }}
                  >
                    END
                  </Typography>
                  <DatePicker
                    id='end-date'
                    format='dd-MM-yyyy'
                    value={endDate}
                    onChange={(e) => {
                      setEndDate(e.value)
                      setDateEdited(true)
                    }}
                    style={{ width: '130px', height: '28px' }}
                    disabled={READ_ONLY}
                  />
                </Box>

                {/* REFRESH BUTTON */}
                {!isOldYear && (
                  <Tooltip title='Refresh Data'>
                    <Button
                      variant='contained'
                      className='btn-load'
                      startIcon={<SyncIcon />}
                      onClick={handleOpenDialog}
                      disabled={READ_ONLY}
                      sx={{
                        height: 28,
                        px: 1.5,
                        mt: 'auto',
                      }}
                    >
                      Refresh
                    </Button>
                  </Tooltip>
                )}

                {/* LAST REFRESHED */}
                {configurationExecutionDetails[0]?.ModifiedOn && (
                  <Tooltip
                    title={`Last Refreshed: ${formatDateForText(
                      configurationExecutionDetails[0]?.ModifiedOn,
                      true,
                    )}`}
                  >
                    <Stack
                      direction='row'
                      spacing={0.5}
                      alignItems='center'
                      sx={{
                        color: '#16a34a',
                        whiteSpace: 'nowrap',
                        mt: 'auto',
                        height: 28,
                      }}
                    >
                      <HistoryIcon sx={{ fontSize: '0.9rem' }} />
                      <Typography
                        sx={{
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          lineHeight: 1.2,
                          fontFamily:
                            "'Segoe UI', system-ui, -apple-system, 'Open Sans', Arial, sans-serif",
                        }}
                      >
                        Last refreshed on{' '}
                        <strong>
                          {
                            formatDateForText(
                              configurationExecutionDetails[0]?.ModifiedOn,
                            ).split(' ')[0]
                          }
                        </strong>
                        {lastModifiedBy ? (
                          <>
                            {' by '}
                            <strong>{lastModifiedBy}</strong>
                          </>
                        ) : null}
                        {' | '}
                        Period:{' '}
                        <strong>
                          {formatDateForText(startDateFromConfig, true)}
                        </strong>
                        {' - '}
                        <strong>
                          {formatDateForText(endDateDateFromConfig, true)}
                        </strong>
                      </Typography>
                    </Stack>
                  </Tooltip>
                )}
              </Stack>

              {/* ROW 2: AOP DESIGN BASIS (only when required) */}
              {isSummaryRequired && (
                <Box sx={{ width: '100%' }}>
                  <Typography
                    variant='caption'
                    sx={{
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      fontFamily:
                        "'Segoe UI', system-ui, -apple-system, 'Open Sans', Arial, sans-serif",
                      color: '#334155',
                      letterSpacing: '0.3px',
                    }}
                  >
                    AOP DESIGN BASIS
                  </Typography>
                  <textarea
                    disabled={READ_ONLY}
                    value={summary}
                    rows={2}
                    onChange={(e) => {
                      setSummary(e.target.value)
                      setSummaryEdited(true)
                    }}
                    style={{
                      width: '100%',
                      padding: '6px 8px',
                      borderRadius: '6px',
                      border: '1px solid #cbd5e1',
                      fontSize: '0.8rem',
                      fontFamily:
                        "'Segoe UI', system-ui, -apple-system, 'Open Sans', Arial, sans-serif",
                      resize: 'none',
                      backgroundColor: READ_ONLY ? '#f8fafc' : '#fff',
                    }}
                  />
                </Box>
              )}
            </Stack>
          </CustomAccordionDetails>
        </CompactAccordion>
      </Box>
    )
  }, [
    startDate,
    endDate,
    summary,
    configurationExecutionDetails,
    isOldYear,
    READ_ONLY,
  ])

  return (
    <>
      <LoaderBackdrop open={!!loading} />
      {accordian}

      {/* Confirmation Dialog */}
      <ConfigurationDialog
        open={openConfirmDialog}
        onClose={handleCloseDialog}
        onConfirm={handleConfirmLoad}
        startDate={startDate}
        endDate={endDate}
      />

      {/* Notification */}
      <Notification
        open={snackbarOpen}
        onClose={() => setSnackbarOpen(false)}
        message={snackbarData.message}
        severity={snackbarData.severity}
      />
    </>
  )
}

export default ConfigurationAccordian
