import {
  FileExportIcon,
  FileImportIcon,
  SaveIcon,
  CalculateIcon,
} from 'assets/images/icons'
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
  Stack,
  Zoom,
  useTheme,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import { Info, ExpandMore } from '@mui/icons-material'
import SettingsIcon from '@mui/icons-material/Settings'
import CloseIcon from '@mui/icons-material/Close'
import CloudDownloadIcon from '@mui/icons-material/CloudDownload'
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'

import { DatePicker } from '@progress/kendo-react-dateinputs'
import { BusinessDemandDataApiService } from 'services/business-demand-data-api-service'
import { TextArea } from '@progress/kendo-react-inputs'
import { getRoleName } from 'services/role-service'
import CrakcerConstants from './CrakcerConstants'
import CrakcerProductionConst from './CrakcerProductionConst'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import './common/ConfigurationAccordian.css'
import { CalenderIcon, CalenderDarkIcon } from 'assets/images/icons/index'

const StyledConfirmDialog = styled(Dialog)(({theme}) => ({
  '& .MuiPaper-root': {
    borderRadius: '24px',
    padding: '12px',
    background: theme.palette.mode === 'dark' ? '#131726' : 'rgba(255, 255, 255, 0.98)',
    backdropFilter: 'blur(16px)',
    boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.2)',
    border: theme.palette.mode === 'dark' ? '1px solid #00000033' : '1px solid #ffffff',
  },
}))

const DateHighlight = styled(Box)(({theme}) => ({
  display: 'inline-flex',
  alignItems: 'center',
  backgroundColor: theme.palette.mode === 'dark' ? 'rgba(167, 168, 255, 0.1)' : 'rgba(1, 0, 203, 0.05)',
  color: theme.palette.mode === 'dark' ? '#A7A8FF' : '#0100cb',
  padding: '4px 12px',
  borderRadius: '8px',
  fontWeight: 700,
  fontSize: '0.85rem',
  margin: '0 4px',
}))

const AopDesignBasis = () => {
  const hasExecutedRef = useRef(false)
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
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
  const SITE_NAME = siteObject?.name
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = screenTitle?.title
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const lowerVertName = VERTICAL_NAME?.toLowerCase()
  const lowerSiteName = SITE_NAME?.toLowerCase()

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

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
  const [startShow, setStartShow] = useState(false)
  const [endShow, setEndShow] = useState(false)

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
  const [dateEdited, setDateEdited] = useState()
  const [startDate, setStartDate] = useState()
  const [endDate, setEndDate] = useState()
  const [startDateObj, setStartDateObj] = useState([])
  const [endDateObj, setEndDateObj] = useState([])
  const [configurationExecutionDetails, setConfigurationExecutionDetails] =
    useState([])
  const [openConfirmDialog, setOpenConfirmDialog] = useState(false)
  const [gradeId, setGradeId] = React.useState(null)

  // const { isReadOnly, isReadWrite, isFullAccess, isApproveOnly } =
  //   usePermissions()
  const IS_CRACKER_HMD = lowerVertName == 'cracker' && lowerSiteName == 'hmd'
  const isSummaryRequired = IS_CRACKER_HMD

  const handleOpenDialog = () => {
    if (isSummaryRequired) {
      if (!summary?.trim()) {
        setSnackbarData({
          message: 'AOP Design Basis summary cannot be empty.',
          severity: 'warning',
        })
        setSnackbarOpen(true)
        return
      } else if (!summaryEdited) {
        setSnackbarData({
          message: 'Please update aop design basis.',
          severity: 'info',
        })
        setSnackbarOpen(true)
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
    if (summaryEdited) {
      await saveSummary()
    }
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
      hours = hours ? hours : 12 // 0 becomes 12
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
        // setLoading1(false)
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
        // console.log('Carry forward successful, status 200.')
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
        // setIsLoadEnabled(false)
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

  const ConfigurationDialog = useMemo(() => {
    return (
      <StyledConfirmDialog
        open={openConfirmDialog}
        onClose={handleCloseDialog}
        TransitionComponent={Zoom}
        transitionDuration={300}
        disableScrollLock
      >
        {/* Close button */}
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: -1 }}>
          <IconButton onClick={handleCloseDialog} size='small'>
            <CloseIcon fontSize='small' />
          </IconButton>
        </Box>

        {/* Icon + Title */}
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            pt: 0,
          }}
        >
          <Box
            sx={{
              p: 0.5,
              borderRadius: '50%',
              bgcolor: 'rgba(1, 0, 203, 0.1)',
              color: '#2563eb',
              mb: 0.5,
              animation: 'pulse 2s infinite',
            }}
          >
            <CloudDownloadIcon sx={{ fontSize: 32 }} />
          </Box>

          <DialogTitle
            sx={{
              textAlign: 'center',
              fontWeight: 800,
              fontSize: '1.15rem',
              color: isDark ? '#f0f0f0' : '#1e293b',
              pb: 0,
            }}
          >
            Confirm Data Refresh
          </DialogTitle>
        </Box>

        {/* Body */}
        <DialogContent sx={{ textAlign: 'center', pt: 1 }}>
          <DialogContentText
            sx={{
              color: isDark ? '#D0D0D0' : '#64748b',
              fontSize: '0.85rem',
              lineHeight: 1.45,
            }}
          >
            You are about to synchronize data for the selected period:
          </DialogContentText>

          {/* Date range highlight */}
          <Box
            sx={{
              mt: 2,
              mb: 1,
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              gap: 0.75,
            }}
          >
            <DateHighlight>
              <CalendarMonthIcon sx={{ fontSize: '0.9rem', mr: 0.5 }} />
              {formatDateForText(startDate)}
            </DateHighlight>

            <Typography
              variant='caption'
              fontWeight={900}
              color='text.disabled'
            >
              TO
            </Typography>

            <DateHighlight>
              <CalendarMonthIcon sx={{ fontSize: '0.9rem', mr: 0.5 }} />
              {formatDateForText(endDate)}
            </DateHighlight>
          </Box>
        </DialogContent>

        {/* Actions */}
        <DialogActions
          sx={{
            justifyContent: 'center',
            gap: 1.5,
            pb: 0,
            px: 0,
          }}
        >
          <Button onClick={handleCloseDialog} className={isDark ? 'btn-dark-no' : 'btn-save'}>
            No
          </Button>

          <Button
            onClick={handleConfirmLoad}
            variant='contained'
            autoFocus
            className={isDark ? 'btn-dark-yes' : 'btn-save'}
          >
            Yes, Refresh Data
          </Button>
        </DialogActions>
      </StyledConfirmDialog>
    )
  }, [openConfirmDialog, startDate, endDate])

  return (
    <React.Fragment>
      <Box className='configuration-accordion-wrapper'>
        <LoaderBackdrop open={!!loading} />

        <CustomAccordion
          defaultExpanded
          disableGutters
          className={`${isDark ? 'k-table-box-dark' : 'k-table-box'} configuration-accordion-root`}
        >
          <CustomAccordionSummary
            expandIcon={<ExpandMoreIcon sx={{ fontSize: '1.2rem' }} />}
            className={isDark ? 'configuration-accordion-summary-dark' : 'configuration-accordion-summary'}
          >
            <Stack
              direction='row'
              alignItems='center'
              justifyContent='space-between'
              sx={{ width: '100%', pr: 1 }}
            >
              <Stack direction='row' spacing={1} alignItems='center'>
                <SettingsIcon sx={{ color: '#0100cb', fontSize: '1rem' }} />
                <Typography className={isDark ? 'configuration-accordion-title-dark' : 'configuration-accordion-title'}>
                  AOP Historical Period Basis for Production Target
                </Typography>
              </Stack>

              {!isOldYear && (
                <Button
                  variant={isDark ? 'outlined' : 'contained'}
                  className={isDark ? 'btn-dark-no' : 'btn-save'}
                  startIcon={
                    <Box component='img' src={SaveIcon} className='w16-icon' />
                  }
                  onClick={(e) => {
                    e.stopPropagation()
                    saveSummary()
                  }}
                  disabled={READ_ONLY || !summaryEdited}
                >
                  Save
                </Button>
              )}
            </Stack>
          </CustomAccordionSummary>

          <CustomAccordionDetails sx={{ p: 0.5, pt: 1.5 }}>
            <Stack direction='column' spacing={1.5}>
              {/* ROW 1: All in ONE straight line */}
              <Stack
                direction='row'
                sx={{
                  columnGap: 1,
                  rowGap: 0,
                }}
                alignItems='center'
                flexWrap='wrap'
              >
                {/* START */}
                <Box className={`date-pill-wrapper ${isDark ? 'date-pill-dark' : 'date-pill-light'}`}>
                  <Box
                    component='img'
                    src={isDark ? CalenderDarkIcon : CalenderIcon}
                    className='w16-icon'
                    style={{ cursor: READ_ONLY ? 'not-allowed' : 'pointer' }}
                    onClick={() => !READ_ONLY && setStartShow((v) => !v)}
                  />
                  <Box component='span' className={`${isDark ? 'header-dropdown-label-dark' : 'header-dropdown-label'}`}>
                    Start Date:
                  </Box>
                  <DatePicker
                    format='dd-MM-yyyy'
                    value={startDate}
                    show={startShow}
                    onClose={() => setStartShow(false)}
                    onChange={(e) => {
                      setStartDate(e.value)
                      setDateEdited(true)
                    }}
                    disabled={READ_ONLY}
                  />
                  <IconButton
                    style={{
                      cursor: READ_ONLY ? 'not-allowed' : 'pointer',
                      p: 0,
                      width: 0,
                      height: 0,
                    }}
                    onClick={() => !READ_ONLY && setStartShow((v) => !v)}
                    size='small'
                  >
                    <ExpandMoreIcon sx={{ fontSize: '1rem', color: isDark ? '#D0D0D0' : '#606060' }} />
                  </IconButton>
                </Box>

                {/* END */}
                <Box className={`date-pill-wrapper ${isDark ? 'date-pill-dark' : 'date-pill-light'}`}>
                  <Box
                    component='img'
                    src={isDark ? CalenderDarkIcon : CalenderIcon}
                    className='w16-icon'
                    style={{ cursor: READ_ONLY ? 'not-allowed' : 'pointer' }}
                    onClick={() => !READ_ONLY && setEndShow((v) => !v)}
                  />
                  <Box component='span' className={`${isDark ? 'header-dropdown-label-dark' : 'header-dropdown-label'}`}>
                    End Date:
                  </Box>
                  <DatePicker
                    format='dd-MM-yyyy'
                    value={endDate}
                    show={endShow}
                    onClose={() => setEndShow(false)}
                    onChange={(e) => {
                      setEndDate(e.value)
                      setDateEdited(true)
                    }}
                    disabled={READ_ONLY}
                  />
                  <IconButton
                    style={{
                      cursor: READ_ONLY ? 'not-allowed' : 'pointer',
                      p: 0,
                      width: 0,
                      height: 0,
                    }}
                    onClick={() => !READ_ONLY && setEndShow((v) => !v)}
                    size='small'
                  >
                    <ExpandMoreIcon sx={{ fontSize: '1rem', color: isDark ? '#D0D0D0' : '#606060' }} />
                  </IconButton>
                </Box>

                {/* LOAD BUTTON */}
                {!isOldYear && (
                  <Tooltip title='Refresh Data'>
                    <Button
                      variant='outlined'
                      className={isDark ? 'btn-dark-no' : 'btn-load'}
                      onClick={handleOpenDialog}
                      disabled={READ_ONLY}
                      sx={{
                        height: 28,
                        px: 1.5,
                        mt: 'auto',
                      }}
                    >
                      Load
                    </Button>
                  </Tooltip>
                )}
              </Stack>

              {/* LAST REFRESHED */}
              {configurationExecutionDetails[0]?.ModifiedOn && (
                <Tooltip
                  title={`Last loaded data : ${formatDateForText(
                    configurationExecutionDetails[0]?.ModifiedOn,
                    true,
                  )}`}
                >
                  <Stack
                    direction='row'
                    spacing={0.5}
                    alignItems='center'
                    className={isDark ? 'last-refreshed-container-dark' : 'last-refreshed-container'}
                  >
                    <Info sx={{ fontSize: '0.9rem', color: isDark ? '#B1E4F7' : '#00688C' }} />

                    <Typography className={isDark ? 'last-refreshed-text-dark' : 'last-refreshed-text'}>
                      {`Last loaded data on ${formatDateForText(configurationExecutionDetails[0]?.ModifiedOn, true)} by ${configurationExecutionDetails[0]?.User ?? ''} for period ${formatDateForText(startDate, false)} to ${formatDateForText(endDate, false)}`}
                    </Typography>
                  </Stack>
                </Tooltip>
              )}

              {/* ROW 2: AOP DESIGN BASIS */}
              <Box sx={{ width: '100%' }}>
                <Typography
                  variant='caption'
                  className={isDark ? 'aop-design-basis-label-dark' : 'aop-design-basis-label'}
                >
                  AOP DESIGN BASIS
                </Typography>

                <TextArea
                  className={isDark ? 'vertical-resize-textarea-dark' : 'vertical-resize-textarea'}
                  disabled={READ_ONLY}
                  value={summary}
                  rows={2}
                  onChange={(e) => {
                    setSummary(e.target.value)
                    setSummaryEdited(true)
                  }}
                />
              </Box>
            </Stack>
          </CustomAccordionDetails>
        </CustomAccordion>

        <Notification
          open={snackbarOpen}
          message={snackbarData?.message || ''}
          severity={snackbarData?.severity || 'info'}
          onClose={() => setSnackbarOpen(false)}
        />
        {ConfigurationDialog}
      </Box>
      {SITE_NAME === 'VMD' && <CrakcerProductionConst />}
    </React.Fragment>
  )
}
export default AopDesignBasis
