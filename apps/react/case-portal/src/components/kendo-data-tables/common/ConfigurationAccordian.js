import React, { useState } from 'react'
import {
  Box,
  Stack,
  Typography,
  Tooltip,
  Button,
  IconButton,
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
  const [startShow, setStartShow] = useState(false)
  const [endShow, setEndShow] = useState(false)

  const startDateConfig = configurationExecutionDetails.find(
    (item) => item.Name === 'StartDateNorms' || item.Name === 'StartDate',
  )

  const endDateConfig = configurationExecutionDetails.find(
    (item) => item.Name === 'EndDateNorms' || item.Name === 'EndDate',
  )

  const startDateFromConfig = new Date(startDateConfig?.AttributeValue)
  const endDateDateFromConfig = new Date(endDateConfig?.AttributeValue)

  return (
    <Box className='configuration-accordion-wrapper'>
      <CustomAccordion
        defaultExpanded
        disableGutters
        className='k-table-box configuration-accordion-root'
      >
        <CustomAccordionSummary
          expandIcon={<ExpandMoreIcon sx={{ fontSize: '1.2rem' }} />}
          className='configuration-accordion-summary'
        >
          <Stack direction='row' spacing={1} alignItems='center'>
            <SettingsIcon sx={{ color: '#0100cb', fontSize: '1rem' }} />
            <Typography className='configuration-accordion-title'>
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
                  disabled={Boolean(READ_ONLY)}
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
                  <ExpandMoreIcon sx={{ fontSize: '1rem', color: '#606060' }} />
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
                  disabled={Boolean(READ_ONLY)}
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
                  <ExpandMoreIcon sx={{ fontSize: '1rem', color: '#606060' }} />
                </IconButton>
              </Box>

              {/* LOAD BUTTON */}
              {!isOldYear && (
                <Tooltip title='Refresh Data'>
                  <Button
                    variant='outlined'
                    className='btn-load'
                    onClick={handleOpenDialog}
                    disabled={Boolean(READ_ONLY)}
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
                  <InfoIcon sx={{ fontSize: '0.9rem', color: '#00688C' }} />

                  <Typography className='last-refreshed-text'>
                    {`Last loaded data on ${formatDateForText(configurationExecutionDetails[0]?.ModifiedOn, true)} by ${configurationExecutionDetails[0]?.User ?? ''} for period ${formatDateForText(startDateFromConfig, false)} to ${formatDateForText(endDateDateFromConfig, false)}`}
                  </Typography>
                </Stack>
              </Tooltip>
            )}

            {/* ROW 2: AOP DESIGN BASIS */}
            {summaryEnabled && (
              <Box sx={{ width: '100%' }}>
                <Typography
                  variant='caption'
                  className='aop-design-basis-label'
                >
                  AOP DESIGN BASIS
                </Typography>

                <TextArea
                  className='vertical-resize-textarea'
                  disabled={Boolean(READ_ONLY)}
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
