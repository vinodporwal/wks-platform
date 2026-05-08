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
} from '@mui/material'
import { Info, ExpandMore } from '@mui/icons-material'

import { DatePicker } from '@progress/kendo-react-dateinputs'
import { BusinessDemandDataApiService } from 'services/business-demand-data-api-service'
import { TextArea } from '@progress/kendo-react-inputs'
import { getRoleName } from 'services/role-service'
import CrakcerConstants from './CrakcerConstants'
import CrakcerProductionConst from './CrakcerProductionConst'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import './common/ConfigurationAccordian.css'
import { CalenderIcon } from 'assets/images/icons/index'

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
  const SITE_NAME = siteObject?.name
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = screenTitle?.title
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

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
      <Dialog
        open={openConfirmDialog}
        onClose={handleCloseDialog}
        aria-labelledby='alert-dialog-title'
        aria-describedby='alert-dialog-description'
      >
        <DialogTitle id='alert-dialog-title'>{'Load?'}</DialogTitle>
        <DialogContent>
          <DialogContentText
            id='alert-dialog-description'
            sx={{
              color: '#303030',
              fontSize: '14px',
              fontWeight: '500',
              fontFamily: '"Honeywell Sans Web", "Inter", sans-serif',
            }}
          >
            {`Are you sure you want to load data for the period from ${formatDateForText(startDate)} to ${formatDateForText(endDate)}?`}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog} variant='outlined'>
            Cancel
          </Button>
          <Button onClick={handleConfirmLoad} autoFocus variant='contained'>
            Load
          </Button>
        </DialogActions>
      </Dialog>
    )
  }, [openConfirmDialog])

  return (
    <React.Fragment>
      <Box
        className='k-table-box configuration-accordion-wrapper'
        sx={{ padding: '16px 20px' }}
      >
        <LoaderBackdrop open={!!loading} />

        {/* Header Section */}
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            mb: 2,
          }}
        >
          <Typography className='configuration-accordion-title'>
            AOP Basis
          </Typography>

          <Box sx={{ display: 'flex', gap: 2 }}>
            {!isOldYear && (
              <Button
                variant='contained'
                className='btn-save'
                startIcon={
                  <Box component='img' src={SaveIcon} className='w16-icon' />
                }
                onClick={saveSummary}
                disabled={READ_ONLY || !summaryEdited}
              >
                Save
              </Button>
            )}
            {/* <Button
              variant='contained'
              onClick={() => {}} // TODO: Implement Mark as Complete logic
              disabled={READ_ONLY}
              sx={{
                textTransform: 'none',
                fontWeight: 600,
                borderRadius: '8px',
                px: 3,
                bgcolor: '#6366F1',
                boxShadow: 'none',
                '&:hover': {
                  bgcolor: '#4F46E5',
                  boxShadow: 'none',
                },
              }}
            >
              Mark as Complete
            </Button> */}
          </Box>
        </Box>
        {/* NOT NEEDED */}
        {/* Info Alert Section */}
        {/* <Box
          className='last-refreshed-container'
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: '4px'
          }}
        >
          <Tooltip title='AOP Design Basis Blue Print'>
            <IconButton
              size='medium'
              sx={{
                backgroundColor: 'transparent',
                '&:hover': {
                  backgroundColor: 'rgba(0, 0, 0, 0.1)',
                },
                width: 0,
                height: 0,
              }}
              onClick={() => aopDesignBasisBluePrint()}
            >
              <Info sx={{ fontSize: '0.9rem', color: '#00688C' }} />
            </IconButton>
          </Tooltip>
          <Typography className='last-refreshed-text'>
          About AOP Design Basis. This is just the placeholder text, this will
          be replaced by actual useful information.
          </Typography>
        </Box> */}

        {/* {ConfigurationAccordian} */}
        <Box sx={{ width: '100%', my: 1 }}>
          <Typography variant='caption' className='aop-design-basis-label'>
            AOP Historical Period Basis for Production Target
          </Typography>
        </Box>
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
            <Box className='date-pill-wrapper'>
              <Box
                component='img'
                src={CalenderIcon}
                className='w16-icon'
                style={{ cursor: READ_ONLY ? 'not-allowed' : 'pointer' }}
                onClick={() => !READ_ONLY && setStartShow((v) => !v)}
              />
              <Box component='span' className='header-dropdown-label'>
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
                <ExpandMore sx={{ fontSize: '1rem', color: '#606060' }} />
              </IconButton>
            </Box>

            {/* END */}
            <Box className='date-pill-wrapper'>
              <Box
                component='img'
                src={CalenderIcon}
                className='w16-icon'
                style={{ cursor: READ_ONLY ? 'not-allowed' : 'pointer' }}
                onClick={() => !READ_ONLY && setEndShow((v) => !v)}
              />
              <Box component='span' className='header-dropdown-label'>
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
                <ExpandMore sx={{ fontSize: '1rem', color: '#606060' }} />
              </IconButton>
            </Box>

            {/* LOAD BUTTON */}
            {!isOldYear && (
              <Tooltip title='Refresh Data'>
                <Button
                  variant='outlined'
                  className='btn-load'
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
                className='last-refreshed-container'
              >
                <Info sx={{ fontSize: '0.9rem', color: '#00688C' }} />

                <Typography className='last-refreshed-text'>
                  {`Last loaded data on ${formatDateForText(configurationExecutionDetails[0]?.ModifiedOn, true)} by ${configurationExecutionDetails[0]?.User ?? ''} for period ${formatDateForText(startDate, false)} to ${formatDateForText(endDate, false)}`}
                </Typography>
              </Stack>
            </Tooltip>
          )}

          {/* ROW 2: AOP DESIGN BASIS */}
          <Box sx={{ width: '100%' }}>
            <Typography variant='caption' className='aop-design-basis-label'>
              AOP Design Basis
            </Typography>

            <TextArea
              className='vertical-resize-textarea'
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
