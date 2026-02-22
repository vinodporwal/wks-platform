import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { verticalEnums } from 'enums/verticalEnums'
// import { usePermissions } from 'hooks/usePermissions'
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import { useSession } from 'SessionStoreContext'
import {
  CustomAccordion,
  CustomAccordionDetails,
  CustomAccordionSummary,
} from 'utils/CustomAccrodian'
import {
  Backdrop,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  TextField,
  Tooltip,
  Typography,
} from '../../../node_modules/@mui/material/index'
import InfoIcon from '@mui/icons-material/Info'

import { DatePicker } from '../../../node_modules/@progress/kendo-react-dateinputs/index'
import { BusinessDemandDataApiService } from 'services/business-demand-data-api-service'
import { TextArea } from '../../../node_modules/@progress/kendo-react-inputs/index'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const AopDesignBasis = () => {
  const hasExecutedRef = useRef(false)

  const keycloak = useSession()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = screenTitle?.title
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR)

  const vertName = verticalChange?.selectedVertical

  const [tabIndex, setTabIndex] = useState(0)
  const [loading, setLoading] = useState(false)
  const [loading1, setLoading1] = useState(false)
  const [summaryEdited, setSummaryEdited] = useState(false)
  const [configurationRows, setConfigurationRows] = useState([])
  const [startUpRows, setStartUpRows] = useState([])
  const [otherLossRows, setOtherLossRows] = useState([])
  const [shutdownNormsRows, setShutdownRows] = useState([])
  const [constantsRows, setConstantsRows] = useState([])
  const [productionRows, setProductionRows] = useState([])
  const [elastomerRows, setElastomerRows] = useState([])
  const [productionRowsConstants, setProductionRowsConstants] = useState([])
  const [pioImpactRows, setPioImpactRows] = useState([])
  const [
    productionRowsConstantsMannualEntry,
    setProductionRowsConstantsMannualEntry,
  ] = useState([])
  const [gradeData, setGradeData] = useState([])
  const [continiousGradeData, setContiniousGradeData] = useState([])
  const [discontiniousGradeData, setDiscontiniousGradeData] = useState([])
  const [tabs, setTabs] = useState([])
  const [availableTabs, setAvailableTabs] = useState([])
  const [summary, setSummary] = useState('')
  const [debouncedSummary, setDebouncedSummary] = useState('')

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSummary(summary)
    }, 300)
    return () => clearTimeout(handler)
  }, [summary])
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [startDate, setStartDate] = useState()
  const [endDate, setEndDate] = useState()
  const [startDateObj, setStartDateObj] = useState([])
  const [endDateObj, setEndDateObj] = useState([])
  const [configurationExecutionDetails, setConfigurationExecutionDetails] =
    useState([])
  const [openConfirmDialog, setOpenConfirmDialog] = useState(false)
  const [gradeId, setGradeId] = React.useState(null)

  const handleOpenDialog = () => {
    setOpenConfirmDialog(true)
  }
  const handleCloseDialog = () => {
    setOpenConfirmDialog(false)
  }
  const handleConfirmLoad = () => {
    setOpenConfirmDialog(false)
    onLoad()
  }

  useEffect(() => {
    if (!PLANT_ID || !AOP_YEAR) return
    setTabIndex(0)
    setLoading1(true)
    carryForwardRecords()

    hasExecutedRef.current = false
  }, [PLANT_ID, AOP_YEAR])

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
        today.getFullYear() - 5,
        today.getMonth(),
        1,
      )
      setStartDate(fallbackStartDate)
      setEndDate(fallbackEndDate)
    }
  }, [configurationExecutionDetails, PLANT_ID])

  useEffect(() => {
    computeAndSetDates()
  }, [computeAndSetDates])

  function formatDate(date) {
    if (!date) return ''
    const year = date?.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  function formatDateForText(date, time = false) {
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

  const getAopSummary = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      setSummary('')
      var res = await DataService.getAopSummary(keycloak, PLANT_ID, AOP_YEAR)
      if (res?.code == 200) {
        setSummary(res?.data?.summary)
      } else {
        setSummary('')
      }
    } catch (error) {
      console.error('Error fetching data:', error)
    }
  }

  const onLoadTest = async (startDateObj, endDateObj) => {
    setLoading1(true)
    const plantId = PLANT_ID
    const auditYear = AOP_YEAR
    const today = new Date()
    const endDate = new Date(today.getFullYear(), today.getMonth(), 0)
    const startDate = new Date(today.getFullYear() - 5, today.getMonth(), 1)
    const createPayloadItem = (obj, date) => ({
      apr: date,
      UOM: '',
      auditYear,
      normParameterFKId: obj?.NormParameter_FK_Id,
      remarks: 'Initiated',
      id: obj?.Id || null,
      plantId,
    })
    const payload = [
      createPayloadItem(startDateObj, formatDate(startDate)),
      createPayloadItem(endDateObj, formatDate(endDate)),
    ]
    try {
      const response = await DataService.executeConfiguration(payload, keycloak)
      if (response?.code === 200) {
        await getConfigurationExecutionDetails()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Execution Failed!',
          severity: 'error',
        })
      }
      getAopSummary()
      return response
    } catch (error) {
      console.error('Execution Failed!', error)
    } finally {
      setLoading(false)
      setLoading1(false)
    }
  }

  const getConfigurationExecutionDetails = async () => {
    try {
      const response = await DataService.getConfigurationExecutionDetails(
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
      }
    } catch (error) {
      console.error('Error fetching getConfigurationExecutionDetails:', error)
    } finally {
      // setLoading1(false)
    }
  }

  const carryForwardRecords = async () => {
    try {
      const response = await DataService.carryForwardRecords(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      getAopSummary()

      if (response && response.code === 200) {
        getConfigurationExecutionDetails()
        setLoading1(false)
      } else {
        console.warn(
          `Carry forward request completed but status was not 200: ${response?.status}`,
        )
      }
    } catch (error) {
      console.error('Error fetching getConfigurationExecutionDetails:', error)
    } finally {
      // setLoading1(false)
    }
  }

  const onLoad = async () => {
    if (startDate && endDate && startDate > endDate) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Please Choose Valid Dates!',
        severity: 'warning',
      })

      return
    }
    setLoading1(true)
    const startDateObj = configurationExecutionDetails.find(
      (item) => item.Name === 'StartDate',
    )
    const endDateObj = configurationExecutionDetails.find(
      (item) => item.Name === 'EndDate',
    )
    if (!startDateObj?.Id || !endDateObj?.Id) {
      console.warn(
        'StartDate or EndDate object is missing Id. Aborting execution.',
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Start/End date configuration is incomplete.',
        severity: 'error',
      })
      setLoading(false)

      return
    }
    setLoading(true)
    try {
      setStartDateObj(startDateObj)
      setEndDateObj(endDateObj)
      const payload = [
        {
          apr: formatDate(startDate),
          UOM: '',
          auditYear: AOP_YEAR,
          normParameterFKId: startDateObj?.NormParameter_FK_Id,
          remarks: 'Initiated',
          id: startDateObj?.Id || null,
          plantId: PLANT_ID,
        },
        {
          apr: formatDate(endDate),
          UOM: '',
          auditYear: AOP_YEAR,
          normParameterFKId: endDateObj?.NormParameter_FK_Id,
          remarks: 'Initiated',
          id: endDateObj?.Id || null,
          plantId: PLANT_ID,
        },
      ]
      const response = await DataService.executeConfiguration(payload, keycloak)
      if (response) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Execution Started Successfully!',
          severity: 'success',
        })
        getConfigurationExecutionDetails()
        setLoading(false)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Execution Falied!',
          severity: 'error',
        })
      }
      getAopSummary()
      return response
    } catch (error) {
      console.error('Execution Falied!', error)
      setLoading(false)
    } finally {
      setLoading(false)
      setLoading1(false)
    }
  }

  useEffect(() => {
    if (tabIndex >= tabs.length) {
      setTabIndex(0)
    }
  }, [tabs])

  const startDateConfig = configurationExecutionDetails.find(
    (item) => item.Name === 'StartDate',
  )

  const endDateConfig = configurationExecutionDetails.find(
    (item) => item.Name === 'EndDate',
  )

  const startDateFromConfig = new Date(startDateConfig?.AttributeValue)
  const endDateDateFromConfig = new Date(endDateConfig?.AttributeValue)

  const aopDesignBasisBluePrint = async () => {
    const response =
      await BusinessDemandDataApiService.aopDesignBasisBluePrint(keycloak)
  }

  const saveSummary = async () => {
    try {
      const response = await DataService.saveSummaryAOPConsumptionNorm(
        PLANT_ID,
        AOP_YEAR,
        summary,
        keycloak,
      )

      if (response?.code == 200) {
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setSummaryEdited(false)

        setLoading(false)
        setSnackbarOpen(true)
        // setIsEdited(false)
      } else {
        setSnackbarData({
          message: 'Saved Failed!',
          severity: 'error',
        })
        setLoading(false)
        // setSnackbarOpen(true)
      }

      //

      // setLoading(false)
      return response
    } catch (error) {
      console.error('Error saving Summary!', error)
    } finally {
      //
      setLoading(false)
    }
  }

  const ConfigurationAccordian = useMemo(() => {
    return (
      <Box sx={{ mb: '16px' }}>
        <Box
          display='flex'
          alignItems='center'
          sx={{
            mb: 2,
            animation: 'fadeIn 0.5s ease-out',
            '@keyframes fadeIn': {
              from: { opacity: 0, transform: 'translateY(-10px)' },
              to: { opacity: 1, transform: 'translateY(0)' },
            },
          }}
        >
          <Typography
            className='text-note'
            variant='body2'
            sx={{
              fontWeight: 500,
              color: 'text.secondary',
              letterSpacing: '0.02em',
            }}
          >
            *AOP Design Basis Blue Print
          </Typography>
          <Tooltip
            title='AOP Design Basis Blue Print'
            slotProps={{
              tooltip: {
                sx: {
                  bgcolor: 'rgba(0, 0, 0, 0.9)',
                  backdropFilter: 'blur(8px)',
                  fontSize: '0.8125rem',
                  py: 1,
                  px: 1.5,
                  boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                },
              },
            }}
          >
            <IconButton
              size='medium'
              sx={{
                ml: 1,
                backgroundColor: 'transparent',
                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                '&:hover': {
                  backgroundColor: 'rgba(1, 0, 203, 0.08)',
                  transform: 'scale(1.1)',
                },
                padding: '6px',
              }}
              onClick={() => aopDesignBasisBluePrint()}
            >
              <InfoIcon
                fontSize='medium'
                sx={{
                  color: '#0100cb',
                  transition: 'all 0.3s ease',
                }}
              />
            </IconButton>
          </Tooltip>
        </Box>

        <CustomAccordion
          defaultExpanded
          disableGutters
          sx={{
            background:
              'linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(250, 250, 255, 0.95) 100%)',
            backdropFilter: 'blur(12px)',
            boxShadow:
              '0 4px 20px rgba(0, 0, 0, 0.08), 0 1px 4px rgba(0, 0, 0, 0.04)',
            border: '1px solid rgba(1, 0, 203, 0.12)',
            borderRadius: '12px !important',
            overflow: 'hidden',
            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            '&:hover': {
              boxShadow:
                '0 6px 24px rgba(0, 0, 0, 0.12), 0 2px 6px rgba(0, 0, 0, 0.06)',
              borderColor: 'rgba(1, 0, 203, 0.2)',
            },
            '&::before': {
              display: 'none',
            },
            '&.Mui-expanded': {
              margin: '0 !important',
            },
          }}
        >
          <CustomAccordionSummary
            aria-controls='meg-grid-content'
            id='meg-grid-header'
            sx={{
              background:
                'linear-gradient(90deg, rgba(1, 0, 203, 0.03) 0%, rgba(91, 89, 255, 0.02) 100%)',
              borderBottom: '1px solid rgba(1, 0, 203, 0.1)',
              transition: 'all 0.3s ease',
              '&:hover': {
                background:
                  'linear-gradient(90deg, rgba(1, 0, 203, 0.05) 0%, rgba(91, 89, 255, 0.03) 100%)',
              },
            }}
          >
            <Typography
              className='accordian-title'
              sx={{
                fontWeight: 600,
                letterSpacing: '0.01em',
              }}
            >
              AOP Historical Period Basis for Production Target
            </Typography>
          </CustomAccordionSummary>
          <CustomAccordionDetails
            sx={{
              p: 3,
            }}
          >
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-end',
                mt: 0,
              }}
            >
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 2,
                  marginTop: '5px',
                  flexWrap: 'wrap',
                }}
              >
                {true && (
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: 2,
                      flexWrap: 'wrap',
                    }}
                  >
                    {/* Start Date */}
                    <Box
                      sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 0.5,
                        transition: 'all 0.3s ease',
                      }}
                    >
                      <Typography
                        className='button-title'
                        sx={{
                          whiteSpace: 'nowrap',
                          fontWeight: 500,
                          fontSize: '0.875rem',
                          color: 'text.primary',
                        }}
                      >
                        Start Date
                      </Typography>
                      <Box
                        sx={{
                          '& .k-datepicker': {
                            borderRadius: '8px',
                            border: '1px solid rgba(1, 0, 203, 0.2)',
                            transition: 'all 0.3s ease',
                            '&:hover': {
                              borderColor: 'rgba(1, 0, 203, 0.4)',
                              boxShadow: '0 2px 8px rgba(1, 0, 203, 0.1)',
                            },
                            '&:focus-within': {
                              borderColor: '#0100cb',
                              boxShadow: '0 0 0 3px rgba(1, 0, 203, 0.1)',
                            },
                          },
                        }}
                      >
                        <DatePicker
                          id='start-date'
                          format='dd-MM-yyyy'
                          value={startDate}
                          onChange={(e) => setStartDate(e.value)}
                          style={{ height: '80px' }}
                          size={'medium'}
                          disabled={READ_ONLY}
                        />
                      </Box>
                    </Box>
                    <Box
                      sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 0.5,
                        transition: 'all 0.3s ease',
                      }}
                    >
                      <Typography
                        className='button-title'
                        sx={{
                          whiteSpace: 'nowrap',
                          fontWeight: 500,
                          fontSize: '0.875rem',
                          color: 'text.primary',
                        }}
                      >
                        End Date
                      </Typography>
                      <Box
                        sx={{
                          '& .k-datepicker': {
                            borderRadius: '8px',
                            border: '1px solid rgba(1, 0, 203, 0.2)',
                            transition: 'all 0.3s ease',
                            '&:hover': {
                              borderColor: 'rgba(1, 0, 203, 0.4)',
                              boxShadow: '0 2px 8px rgba(1, 0, 203, 0.1)',
                            },
                            '&:focus-within': {
                              borderColor: '#0100cb',
                              boxShadow: '0 0 0 3px rgba(1, 0, 203, 0.1)',
                            },
                          },
                        }}
                      >
                        <DatePicker
                          id='end-date'
                          format='dd-MM-yyyy'
                          value={endDate}
                          onChange={(e) => setEndDate(e.value)}
                          style={{ height: '80px' }}
                          size={'medium'}
                          disabled={READ_ONLY}
                        />
                      </Box>
                    </Box>

                    {/* Load Button */}
                    {!isOldYear && (
                      <Button
                        variant='contained'
                        onClick={handleOpenDialog}
                        className='btn-save'
                        disabled={READ_ONLY}
                        sx={{
                          alignSelf: 'flex-end',
                          background:
                            'linear-gradient(135deg, #0100cb 0%, #5b59ff 100%)',
                          borderRadius: '8px',
                          textTransform: 'none',
                          fontWeight: 600,
                          px: 3,
                          py: 1,
                          boxShadow: '0 4px 12px rgba(1, 0, 203, 0.25)',
                          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                          '&:hover': {
                            background:
                              'linear-gradient(135deg, #0000b3 0%, #4947e6 100%)',
                            boxShadow: '0 6px 16px rgba(1, 0, 203, 0.35)',
                            transform: 'translateY(-2px)',
                          },
                          '&:active': {
                            transform: 'translateY(0)',
                          },
                          '&:disabled': {
                            background:
                              'linear-gradient(135deg, rgba(0, 0, 0, 0.12) 0%, rgba(0, 0, 0, 0.08) 100%)',
                            boxShadow: 'none',
                          },
                        }}
                      >
                        Load
                      </Button>
                    )}

                    {!isOldYear && (
                      <Button
                        variant='contained'
                        onClick={saveSummary}
                        className='btn-save'
                        disabled={READ_ONLY || !summaryEdited}
                        sx={{
                          alignSelf: 'flex-end',
                          background:
                            'linear-gradient(135deg, #0100cb 0%, #5b59ff 100%)',
                          borderRadius: '8px',
                          textTransform: 'none',
                          fontWeight: 600,
                          px: 3,
                          py: 1,
                          boxShadow: '0 4px 12px rgba(1, 0, 203, 0.25)',
                          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                          '&:hover': {
                            background:
                              'linear-gradient(135deg, #0000b3 0%, #4947e6 100%)',
                            boxShadow: '0 6px 16px rgba(1, 0, 203, 0.35)',
                            transform: 'translateY(-2px)',
                          },
                          '&:active': {
                            transform: 'translateY(0)',
                          },
                          '&:disabled': {
                            background:
                              'linear-gradient(135deg, rgba(0, 0, 0, 0.12) 0%, rgba(0, 0, 0, 0.08) 100%)',
                            boxShadow: 'none',
                          },
                        }}
                      >
                        Save
                      </Button>
                    )}
                  </Box>
                )}
                {configurationExecutionDetails[0]?.ModifiedOn && (
                  <Typography
                    className={
                      READ_ONLY ? 'summary-title-disabled' : 'summary-title'
                    }
                    sx={{
                      whiteSpace: 'normal',
                      alignSelf: 'flex-end',
                      fontSize: '0.8125rem',
                      color: 'text.secondary',
                      fontStyle: 'italic',
                      px: 2,
                      py: 1,
                      borderRadius: '6px',
                      background: 'rgba(1, 0, 203, 0.03)',
                      border: '1px solid rgba(1, 0, 203, 0.1)',
                    }}
                  >
                    {`(Last refreshed data on: ${formatDateForText(configurationExecutionDetails[0]?.ModifiedOn, true)} for the period from ${formatDateForText(startDateFromConfig)} to ${formatDateForText(endDateDateFromConfig)})`}
                  </Typography>
                )}
              </Box>
            </Box>

            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'flex-start',
                gap: 1,
                mt: 3,
              }}
            >
              <Typography
                className='button-title'
                sx={{
                  whiteSpace: 'nowrap',
                  fontWeight: 500,
                  fontSize: '0.875rem',
                  color: 'text.primary',
                }}
              >
                AOP Design Basis
              </Typography>

              <Box
                sx={{
                  width: '100%',
                  '& .k-textarea': {
                    borderRadius: '8px',
                    border: '1px solid rgba(1, 0, 203, 0.2)',
                    transition: 'all 0.3s ease',
                    fontFamily:
                      "'Segoe UI', system-ui, -apple-system, 'Open Sans', Arial, sans-serif",
                    '&:hover': {
                      borderColor: 'rgba(1, 0, 203, 0.4)',
                      boxShadow: '0 2px 8px rgba(1, 0, 203, 0.1)',
                    },
                    '&:focus, &:focus-within': {
                      borderColor: '#0100cb',
                      boxShadow: '0 0 0 3px rgba(1, 0, 203, 0.1)',
                      outline: 'none',
                    },
                  },
                }}
              >
                <TextArea
                  disabled={READ_ONLY}
                  value={summary}
                  rows={6}
                  onChange={(e) => {
                    setSummary(e.target.value)
                    setSummaryEdited(true)
                  }}
                />
              </Box>
            </Box>
          </CustomAccordionDetails>
        </CustomAccordion>
      </Box>
    )
  }, [
    startDate,
    endDate,
    summary,
    startDateFromConfig,
    endDateDateFromConfig,
    READ_ONLY,
    summaryEdited,
    configurationExecutionDetails,
  ])

  const ConfigurationDialog = useMemo(() => {
    return (
      <Dialog
        open={openConfirmDialog}
        onClose={handleCloseDialog}
        aria-labelledby='alert-dialog-title'
        aria-describedby='alert-dialog-description'
        sx={{
          '& .MuiDialog-paper': {
            borderRadius: '16px',
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.12)',
            background:
              'linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(250, 250, 255, 0.95) 100%)',
            backdropFilter: 'blur(12px)',
            border: '1px solid rgba(1, 0, 203, 0.1)',
          },
        }}
      >
        <DialogTitle
          id='alert-dialog-title'
          sx={{
            fontWeight: 600,
            fontSize: '1.25rem',
            pb: 1,
          }}
        >
          {'Load?'}
        </DialogTitle>
        <DialogContent>
          <DialogContentText
            id='alert-dialog-description'
            sx={{
              color: 'text.secondary',
              fontSize: '0.9375rem',
            }}
          >
            {`Are you sure you want to load data for the period from ${formatDateForText(startDate)} to ${formatDateForText(endDate)}?`}
          </DialogContentText>
        </DialogContent>
        <DialogActions
          sx={{
            px: 3,
            pb: 2,
            pt: 1,
            gap: 1,
          }}
        >
          <Button
            onClick={handleCloseDialog}
            sx={{
              borderRadius: '8px',
              textTransform: 'none',
              fontWeight: 500,
              px: 2,
              color: 'text.secondary',
              transition: 'all 0.3s ease',
              '&:hover': {
                background: 'rgba(0, 0, 0, 0.04)',
              },
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleConfirmLoad}
            autoFocus
            variant='contained'
            sx={{
              background: 'linear-gradient(135deg, #0100cb 0%, #5b59ff 100%)',
              borderRadius: '8px',
              textTransform: 'none',
              fontWeight: 600,
              px: 3,
              boxShadow: '0 4px 12px rgba(1, 0, 203, 0.25)',
              transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
              '&:hover': {
                background: 'linear-gradient(135deg, #0000b3 0%, #4947e6 100%)',
                boxShadow: '0 6px 16px rgba(1, 0, 203, 0.35)',
                transform: 'translateY(-2px)',
              },
            }}
          >
            Load
          </Button>
        </DialogActions>
      </Dialog>
    )
  }, [openConfirmDialog, startDate, endDate])

  // HERE LOADING1
  //loading1

  return (
    <div>
      <LoaderBackdrop open={!!loading1} />
      {ConfigurationAccordian}
      <Notification
        open={snackbarOpen}
        message={snackbarData?.message || ''}
        severity={snackbarData?.severity || 'info'}
        onClose={() => setSnackbarOpen(false)}
      />
      {ConfigurationDialog}

      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
        }}
      ></div>
    </div>
  )
}
export default AopDesignBasis
