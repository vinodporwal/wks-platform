import React, { useState } from 'react'
import {
  Box,
  Stack,
  Typography,
  Tooltip,
  Button,
  IconButton,
  useTheme,
} from '@mui/material'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import SettingsIcon from '@mui/icons-material/Settings'
import InfoIcon from '@mui/icons-material/Info'
import { DatePicker } from '@progress/kendo-react-dateinputs'
import { CalenderIcon } from 'assets/images/icons'
import {
  CustomAccordion,
  CustomAccordionDetails,
  CustomAccordionSummary,
} from 'utils/CustomAccrodian'

import './ConfigurationAccordian.css'
import { TextArea } from '../../../../node_modules/@progress/kendo-react-inputs/index'
import { CalenderDarkIcon } from 'assets/images/icons/index'

const ConfigurationAccordian = ({
  startDate,
  endDate,
  summary,
  READ_ONLY,
  isOldYear,
  configurationExecutionDetails,
  setStartDate,
  setEndDate,
  setDateEdited,
  setSummary,
  setSummaryEdited,
  handleOpenDialog,
  summaryEnabled = true,
  formatDateForText,
}) => {
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'
  const [startShow, setStartShow] = useState(false)
  const [endShow, setEndShow] = useState(false)

  return (
    <Box className='configuration-accordion-wrapper'>
      <CustomAccordion
        defaultExpanded
        disableGutters
        className={`${isDark ? 'k-table-box-dark' : 'k-table-box'} configuration-accordion-root`}
      >
        <CustomAccordionSummary
          expandIcon={<ExpandMoreIcon sx={{ fontSize: '1.2rem' }} />}
          className={isDark ? 'configuration-accordion-summary-dark' : 'configuration-accordion-summary'}
        >
          <Stack direction='row' spacing={1} alignItems='center'>
            <SettingsIcon sx={{ color: '#0100cb', fontSize: '1rem' }} />
            <Typography className={isDark ? 'configuration-accordion-title-dark' : 'configuration-accordion-title'}>
              AOP Historical Period Basis
            </Typography>
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
                  <InfoIcon sx={{ fontSize: '0.9rem', color: isDark ? '#B1E4F7' : '#00688C' }} />

                  <Typography className={isDark ? 'last-refreshed-text-dark' : 'last-refreshed-text'}>
                    {`Last loaded data on ${formatDateForText(configurationExecutionDetails[0]?.ModifiedOn, true)} by ${configurationExecutionDetails[0]?.User ?? ''} for period ${formatDateForText(startDate, false)} to ${formatDateForText(endDate, false)}`}
                  </Typography>
                </Stack>
              </Tooltip>
            )}

            {/* ROW 2: AOP DESIGN BASIS */}
            {summaryEnabled && (
              <Box sx={{ width: '100%' }}>
                <Typography
                  variant='caption'
                  className={isDark ? 'aop-design-basis-label-dark' : 'aop-design-basis-label'}
                >
                  AOP DESIGN BASIS
                </Typography>

                <TextArea
                  className={isDark ? 'aop-design-basis-textarea-dark' : 'aop-design-basis-textarea'}
                  disabled={READ_ONLY}
                  value={summary}
                  rows={2}
                  onChange={(e) => {
                    setSummary(e.target.value)
                    setSummaryEdited(true)
                  }}
                />
              </Box>
            )}
          </Stack>
        </CustomAccordionDetails>
      </CustomAccordion>
    </Box>
  )
}

export default ConfigurationAccordian
