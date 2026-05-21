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
import { useTheme } from '@mui/material'

const Breadcrumbs = ({ navigation, title, ...others }) => {
  const keycloak = useSession()
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'
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

    //MEG
    if (
      title == 'configuration' &&
      SITE_NAME?.toLowerCase() == 'nmd' &&
      VERTICAL_NAME?.toLowerCase() == 'meg'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for NMD EOEG_Rev02.pdf`
    if (
      title == 'configuration' &&
      SITE_NAME?.toLowerCase() == 'c2' &&
      VERTICAL_NAME?.toLowerCase() == 'meg'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for C2 MEG_Rev2.pdf`
    if (
      title == 'configuration' &&
      SITE_NAME?.toLowerCase() == 'dmd' &&
      VERTICAL_NAME?.toLowerCase() == 'meg'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for DMD EOEG.pdf`
    if (
      title == 'configuration' &&
      SITE_NAME?.toLowerCase() == 'hmd' &&
      VERTICAL_NAME?.toLowerCase() == 'meg'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for HMD MEG.pdf`
    if (
      title == 'configuration' &&
      SITE_NAME?.toLowerCase() == 'vmd' &&
      VERTICAL_NAME?.toLowerCase() == 'meg'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for VMD EOEG_Rev2.pdf`

    //PE NMD
    if (
      title == 'configuration' &&
      SITE_NAME?.toLowerCase() == 'nmd' &&
      VERTICAL_NAME?.toLowerCase() == 'pe'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for NMD PE_Revisedlogic_Rev6.pdf`
    //PP NMD
    if (
      title == 'configuration' &&
      PLANT_NAME?.toLowerCase() == 'hdpe' &&
      SITE_NAME?.toLowerCase() == 'dmd' &&
      VERTICAL_NAME?.toLowerCase() == 'pe'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for DMD HDPE_Rev1.pdf`
    //PE HMD PE1
    if (
      title == 'configuration' &&
      PLANT_NAME?.toLowerCase() == 'pe1' &&
      SITE_NAME?.toLowerCase() == 'hmd' &&
      VERTICAL_NAME?.toLowerCase() == 'pe'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for HMD PE_Rev1.pdf`

    //PP NMD
    if (
      title == 'configuration' &&
      SITE_NAME?.toLowerCase() == 'nmd' &&
      VERTICAL_NAME?.toLowerCase() == 'pp'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for NMD PP_Rev1 (1).pdf`
    //VCM VMD
    if (
      title == 'configuration' &&
      SITE_NAME?.toLowerCase() == 'vmd' &&
      VERTICAL_NAME?.toLowerCase() == 'vcm'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for VMD VCM_Rev0.pdf`
    //VCM DMD
    if (
      title == 'configuration' &&
      SITE_NAME?.toLowerCase() == 'dmd' &&
      VERTICAL_NAME?.toLowerCase() == 'vcm'
    )
      url = `${window.location.origin}/files/Digital AOP Automation for DMD VCM_Rev0.pdf`

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
    color: isDark ? '#B1E4F7' : '#023985ff', // slate-500 (subtle)
  }

  const infoButtonSx = {
    p: '1px',
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
      normalizedTitle === 'production&normsbasis' &&
      (VERTICAL_NAME?.toLowerCase() === 'meg' ||
        VERTICAL_NAME?.toLowerCase() === 'vcm' ||
        (VERTICAL_NAME?.toLowerCase() === 'pe' &&
          SITE_NAME?.toLowerCase() === 'nmd') ||
        (VERTICAL_NAME?.toLowerCase() === 'pp' &&
          SITE_NAME?.toLowerCase() === 'nmd') ||
        (VERTICAL_NAME?.toLowerCase() === 'pe' &&
          SITE_NAME?.toLowerCase() === 'dmd' &&
          PLANT_NAME?.toLowerCase() === 'hdpe') ||
        (PLANT_NAME?.toLowerCase() == 'pe1' &&
          SITE_NAME?.toLowerCase() == 'hmd' &&
          VERTICAL_NAME?.toLowerCase() == 'pe'))
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
        <Box
          border={false}
          className={isDark ? 'breadcrumbs-box-dark' : 'breadcrumbs-box'}
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
                m: 0,
                // p: 0.5,
                width: '100%',
                transition: 'none',
                '&:hover': {
                  boxShadow: 'none',
                  transform: 'none',
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
                        color: isDark ? '#F0F0F0' : '#64748b',
                        textTransform: 'uppercase',
                        letterSpacing: '0.4px',
                        fontFamily:
                          "'Honeywell Sans Web', 'Inter', Arial, sans-serif",
                      }}
                    >
                      {VERTICAL_NAME}
                      <Box component='span' sx={{ mx: 0.7 }}>
                        |
                      </Box>
                      {SITE_NAME}
                      <Box component='span' sx={{ mx: 0.7 }}>
                        |
                      </Box>
                      <Box component='span' sx={{ mx: 0.7 }}>
                        {PLANT_NAME}
                      </Box>
                    </Typography>

                    {/* Status/Item Pill */}
                    {itemContent && <Box>{itemContent}</Box>}
                  </Box>
                )}
              </Grid>
              {/* <Stack spacing={0.5} sx={{ alignItems: 'center' }}>
                <Grid item>
                  <Chip
                    color='primary'
                    variant='outlined'
                    // label={getRoleName(verticalId, item?.id)}
                    className='role-name'
                    sx={{ border: 'none' }} // Remove the border
                  />
                </Grid>
              </Stack> */}
            </Grid>

            {/* HIDE THE TITLE NAME */}
            {title && (
              <Grid item sx={{ mt: 0.5, color: isDark ? '#F0F0F0' : '#64748b', fontFamily:
                          "'Honeywell Sans Web', 'Inter', Arial, sans-serif", }}>
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
        </Box>
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
