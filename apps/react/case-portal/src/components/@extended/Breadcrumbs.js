import PropTypes from 'prop-types'
import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { IconButton, Tooltip } from '@mui/material'
import InfoIcon from '@mui/icons-material/Info'
import Notification from 'components/Utilities/Notification'

import MuiBreadcrumbs from '@mui/material/Breadcrumbs'
import { Grid, Typography } from '@mui/material'
import MainCard from '../MainCard'
import { useSession } from 'SessionStoreContext'
import Config from 'consts/index'
import { useSelector } from 'react-redux'
import { useDispatch } from 'react-redux'
import { setScreenTitle } from 'store/reducers/dataGridStore'
import Chip from '@mui/material/Chip'
import Stack from '@mui/material/Stack'
import StepperNav from 'components/Utilities/StepperNav'
import { useNavigate } from 'react-router-dom'

import {
  Box,
  Button,
  Skeleton,
} from '../../../node_modules/@mui/material/index'
import { setVerticalChangeFromDashboard } from 'store/reducers/dataGridStore'

const Breadcrumbs = ({ navigation, title, ...others }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { verticalChange, plantObject, verticalObject, siteObject, year } =
    dataGridStore

  const dispatch = useDispatch()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)

  const PLANT_ID = plantObject?.id
  const VERTICAL_ID = verticalObject?.id
  const SITE_ID = siteObject?.id
  const AOP_YEAR = year?.selectedYear

  const PLANT_NAME = plantObject?.name
  const VERTICAL_NAME = verticalObject?.name
  const SITE_NAME = siteObject?.name

  const [notification, setNotification] = useState({
    open: false,
    message: '',
    severity: 'info',
  })

  useEffect(() => {
    if (PLANT_NAME && VERTICAL_NAME && SITE_NAME) {
      setLoading(false)
    } else {
      setLoading(true)
    }
  }, [PLANT_NAME, VERTICAL_NAME, SITE_NAME])

  async function handleOpenPdf(title) {
    const url = `${Config.StorageUrl}/storage/files/${VERTICAL_NAME}/${SITE_NAME}/${PLANT_NAME}/downloads/${title}.pdf?content-type=application/pdf`
    const headers = {
      Authorization: `Bearer ${keycloak.token}`,
    }

    try {
      const resp = await fetch(url, {
        method: 'GET',
        headers,
      })
      if (!resp.ok) {
        setNotification({
          open: true,
          message: 'Basis not found! Please try again.',
          severity: 'info',
        })
        return
      }
      const blob = await resp.blob()
      const fileURL = window.URL.createObjectURL(blob)
      window.open(fileURL, '_blank')
      return true
    } catch (e) {
      console.error('Error fetching PDF:', e)
      return Promise.reject(e)
    }
  }

  async function handleOpenPdfTemp(title) {
    // console.log('titletitle', title)
    // console.log('SITE_NAME', SITE_NAME?.toLowerCase())
    var url = ''
    if (title == 'configuration' && SITE_NAME?.toLowerCase() == 'nmd')
      url = `${window.location.origin}/files/Digital AOP Automation for NMD EOEG_Rev02.pdf`
    if (title == 'configuration' && SITE_NAME?.toLowerCase() == 'c2')
      url = `${window.location.origin}/files/Digital AOP Automation for C2 MEG_Rev2.pdf`
    if (title == 'configuration' && SITE_NAME?.toLowerCase() == 'dmd')
      url = `${window.location.origin}/files/Digital AOP Automation for DMD EOEG.pdf`
    if (title == 'configuration' && SITE_NAME?.toLowerCase() == 'hmd')
      url = `${window.location.origin}/files/Digital AOP Automation for HMD MEG.pdf`
    if (title == 'configuration' && SITE_NAME?.toLowerCase() == 'vmd')
      url = `${window.location.origin}/files/Digital AOP Automation for VMD EOEG_Rev2.pdf`

    try {
      const resp = await fetch(url, {
        method: 'GET',
      })

      if (!resp.ok) {
        setNotification({
          open: true,
          message: 'Basis not found! Please try again later.',
          severity: 'info',
        })
        return
      }

      const blob = await resp.blob()
      const fileURL = window.URL.createObjectURL(blob)
      window.open(fileURL, '_blank')
      return true
    } catch (e) {
      console.error('Error fetching file:', e)
      return Promise.reject(e)
    }
  }

  const infoIconSx = {
    fontSize: 14,
    color: '#023985ff', // slate-500 (subtle)
  }

  const infoButtonSx = {
    p: '2px',
    borderRadius: '50%',
    backgroundColor: '#f1f5f9',
    border: '1px solid #e5e7eb',
    '&:hover': {
      backgroundColor: '#e5e7eb',
    },
    width: 0,
    height: 0,
  }

  async function handleOpenPdfTempSSRS(title) {
    try {
      let baseurl = ''
      baseurl =
        'http://sjmnpb174/ReportServer/Pages/ReportViewer.aspx?%2fAOP&rs:Command=Render'
      const params = new URLSearchParams({
        verticalId: VERTICAL_ID,
        siteId: SITE_ID,
        plantId: PLANT_ID,
        finYear: AOP_YEAR,
      })
      const url = `${baseurl}?${params.toString()}`

      window.open(url, '_blank')
      return true
    } catch (e) {
      console.error('Error opening link:', e)
      return Promise.reject(e)
    }
  }

  const location = useLocation()
  const [main, setMain] = useState()
  const [item, setItem] = useState()

  useEffect(() => {
    let title = item?.title

    dispatch(
      setScreenTitle({
        title,
      }),
    )
  }, [item, VERTICAL_NAME])

  // set active item state
  const getCollapse = (menu) => {
    if (menu.children) {
      menu.children.filter((collapse) => {
        if (collapse.type && collapse.type === 'collapse') {
          getCollapse(collapse)
        } else if (collapse.type && collapse.type === 'item') {
          if (location.pathname === collapse.url) {
            setMain(menu)
            setItem(collapse)
            var title = collapse?.title
          }
        }
        return false
      })
    }
  }

  useEffect(() => {
    navigation?.items?.map((menu) => {
      if (menu.type && menu.type === 'group') {
        getCollapse(menu)
      }
      return false
    })
  })

  // only used for component demo breadcrumbs
  if (location.pathname === '/breadcrumbs') {
    location.pathname = '/dashboard/analytics'
  }

  let mainContent
  let itemContent
  let breadcrumbContent = <Typography />
  let itemTitle = ''

  // collapse item
  // if (main && main.type === 'collapse') {
  //   mainContent = (
  //     // <Typography component={Link} to={document.location.pathname} variant="h6" sx={{ textDecoration: 'none' }} color="textSecondary">
  //     <Typography
  //       variant='h6'
  //       sx={{ textDecoration: 'none' }}
  //       color='textSecondary'
  //     >
  //       {main.title}
  //     </Typography>
  //   )
  // }

  // items
  if (item && item.type === 'item') {
    itemTitle = item.title
    var title1 = itemTitle
    if (
      title1 === 'Business Demand' &&
      VERTICAL_NAME?.toLowerCase() === 'meg'
    ) {
      title1 = 'Business Demand (Percentage)'
    }

    if (title1 === 'Business Demand' && VERTICAL_NAME?.toLowerCase() === 'pe') {
      title1 = 'Business Demand (Absolute)'
    }

    const normalizedTitle = itemTitle?.toLowerCase().replace(/\s/g, '')

    // console.log('normalizedTitle', normalizedTitle)

    // if (['productionaop', 'consumptionaop'].includes(normalizedTitle)) {
    if (
      [
        'production&normsbasis',
        // 'overallaopconsumption(norm/quantity)',
      ].includes(normalizedTitle) &&
      VERTICAL_NAME?.toLowerCase() == 'meg'
    ) {
      itemContent = (
        <Typography
          variant='subtitle1'
          color='textPrimary'
          display='flex'
          alignItems='center'
        >
          {/* HIDE THE TITLE NAME  */}
          {/* {title1} */}
          <Tooltip title={`Basis`}>
            <IconButton
              size='small'
              disableRipple
              sx={infoButtonSx}
              onClick={() => handleOpenPdfTemp(item?.id)}
            >
              <InfoIcon sx={infoIconSx} />
            </IconButton>
          </Tooltip>
        </Typography>
      )
    } else if (
      ['aopapprovalflow'].includes(normalizedTitle) &&
      ['pe', 'pp'].includes(VERTICAL_NAME?.toLowerCase())
    ) {
      itemContent = (
        <Typography
          variant='subtitle1'
          color='textPrimary'
          display='flex'
          alignItems='center'
        >
          {/* HIDE THE TITLE NAME  */}
          {/* {title1} */}
          <Tooltip title={`Report`}>
            <IconButton
              size='small'
              sx={infoButtonSx}
              onClick={() => handleOpenPdfTempSSRS(item?.id)}
            >
              <InfoIcon sx={infoIconSx} /> {/* ?? was fontSize="medium" */}
            </IconButton>
          </Tooltip>
        </Typography>
      )
    } else {
      itemContent = null
    }

    // console.log('keycloak?.realmAccess?.roles', keycloak?.idTokenParsed)

    // main
    if (
      item.breadcrumbs !== false &&
      location?.pathname !== '/user-management' &&
      location?.pathname !== '/user-form' &&
      location?.pathname !== '/dashboard'
    ) {
      breadcrumbContent = (
        <MainCard
          border={false}
          sx={{ bgcolor: 'transparent' }}
          {...others}
          content={false}
        >
          {location?.pathname.startsWith('/production-norms-plan') && (
            <Box>{/* <StepperNav /> */}</Box>
          )}
          <Grid
            container
            direction='column'
            justifyContent='flex-start'
            alignItems='flex-start'
            spacing={1}
            // sx={{ marginTop: '-18px' }}
          >
            {/* <Grid item sx={{ ml: 1.5, display: none }}> */}
            {/* <MuiBreadcrumbs aria-label='breadcrumb'> */}
            {/* HIDE HOME OPTION FROM Navigators MENU */}
            {/* <Typography
                  component={Link}
                  to='/home'
                  color='textSecondary'
                  variant='h6'
                  sx={{ textDecoration: 'none' }}
                >
                  Home
                </Typography> */}

            {/* {mainContent} */}

            {/* <Typography
                  component='div'
                  sx={{
                    textDecoration: 'none',
                    fontWeight: 800,
                    color: 'black',
                    // fontStyle: 'italic',
                    fontSize: '1rem',
                  }}
                >
                  {verticalName} / {siteName} / {plantName}
                </Typography>
                {itemContent}
              </MuiBreadcrumbs>
            </Grid> */}

            <Grid
              container
              sx={{
                mt: 4,
                ml: 2,
                mr: 2,
                mb: 0.5,
                px: 0.5,
                py: 0.5,
                width: '100%',

                /* Modern surface */
                bgcolor: 'rgba(255, 255, 255, 0.85)',
                backdropFilter: 'blur(6px)',

                /* Border */
                border: '1px solid rgba(1, 0, 203, 0.15)',
                borderTop: '3px solid #82f160', // ?? modern accent

                /* Elevation */
                boxShadow: `
      0 1px 2px rgba(0,0,0,0.06),
      0 4px 12px rgba(1,0,203,0.08)
    `,

                /* Micro interaction */
                transition: 'all 0.2s ease',
                '&:hover': {
                  boxShadow: `
        0 2px 4px rgba(0,0,0,0.08),
        0 6px 18px rgba(1,0,203,0.12)
      `,
                  transform: 'translateY(-1px)',
                },
              }}
              justifyContent='space-between'
              alignItems='center'
            >
              <Grid item>
                {loading ? (
                  <Stack direction='row' spacing={1} alignItems='center'>
                    <Skeleton variant='circular' width={16} height={16} />
                    <Skeleton
                      variant='text'
                      width={180}
                      height={20}
                      animation='wave'
                    />
                  </Stack>
                ) : (
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {/* Hierarchy Path */}
                    <Typography
                      component='div'
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        fontWeight: 700,
                        fontSize: '0.72rem',
                        color: '#64748b', // Slate 500 for secondary text
                        textTransform: 'uppercase',
                        letterSpacing: '0.4px',
                        fontFamily:
                          '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
                      }}
                    >
                      {VERTICAL_NAME}
                      <Box
                        component='span'
                        sx={{ mx: 0.7, color: 'text.disabled' }}
                      >
                        |
                      </Box>
                      {SITE_NAME}
                      <Box
                        component='span'
                        sx={{ mx: 0.7, color: 'text.disabled' }}
                      >
                        |
                      </Box>
                      <Box
                        component='span'
                        sx={{ color: '#1e293b', fontWeight: 800 }}
                      >
                        {PLANT_NAME}
                      </Box>
                    </Typography>

                    {/* Status/Item Pill */}
                    {itemContent && (
                      <Box
                        sx={{
                          ml: 0,
                          px: 0,
                          py: 0,
                          bgcolor: '#e0e7ff', // Soft Indigo background
                          color: '#4338ca', // Indigo 700
                          borderRadius: '20px',
                          fontSize: '0.50rem',
                          fontWeight: 800,
                          display: 'flex',
                          alignItems: 'center',
                          border: '1px solid rgba(67, 56, 202, 0.1)',
                        }}
                      >
                        {itemContent}
                      </Box>
                    )}
                  </Box>
                )}
              </Grid>
            </Grid>

            {/* HIDE THE TITLE NAME */}
            {title && (
              <Grid item sx={{ mt: 0.5 }}>
                <Typography variant='h5'>{item.title}</Typography>
              </Grid>
            )}
          </Grid>
          {/* Notification Component */}
          <Notification
            open={notification.open}
            message={notification.message}
            severity={notification.severity}
            onClose={() => setNotification({ ...notification, open: false })}
          />
        </MainCard>
      )
    }
  }

  return breadcrumbContent
}

Breadcrumbs.propTypes = {
  navigation: PropTypes.object,
  title: PropTypes.bool,
}

export default Breadcrumbs
