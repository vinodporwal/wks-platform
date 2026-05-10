import { useState, useMemo } from 'react'
import { useSelector } from 'react-redux'
import PropTypes from 'prop-types'

// Material UI Imports
import Collapse from '@mui/material/Collapse'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemText from '@mui/material/ListItemText'
import Typography from '@mui/material/Typography'
import Tooltip from '@mui/material/Tooltip' // Added Tooltip
import Popover from '@mui/material/Popover' // Added Popover
import { useTheme } from '@mui/material/styles'

// Icons
import AddIcon from '@mui/icons-material/Add'
import RemoveIcon from '@mui/icons-material/Remove'
import AppsIcon from '@mui/icons-material/Apps'
import IconChevronRight from '@mui/icons-material/ChevronRight'
import Box from '@mui/material/Box'

// Internal Imports
import { verticalEnums } from 'enums/verticalEnums'
import NavItem from './NavItem'
import Divider from '@mui/material/Divider'

const NavCollapse = ({ menu, level, onItemClick, isPopover }) => {
  const theme = useTheme()
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState(null)
  const [anchorEl, setAnchorEl] = useState(null)

  const { drawerOpen } = useSelector((state) => state.menu)
  const { plantID, verticalChange, siteObject } = useSelector(
    (state) => state.dataGridStore,
  )

  const plantName = plantID?.plantName

  const SITE_NAME = siteObject?.name?.toLowerCase()

  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || 'meg'

  const handleClick = (event) => {
    if (drawerOpen || isPopover) {
      setOpen(!open)
      setSelected(!selected ? menu.id : null)
    } else {
      setAnchorEl(event.currentTarget)
      if (onItemClick) onItemClick()
    }
  }

  const handleClose = () => {
    setAnchorEl(null)
  }

  const menus = useMemo(() => {
    if (!menu?.children) return []

    const renderMenuItem = (item) => {
      const props = {
        key: item.id,
        level: level + 1,
        onItemClick: !drawerOpen || isPopover ? handleClose : undefined,
        isPopover: !drawerOpen || isPopover,
      }
      switch (item.type) {
        case 'collapse':
          return <NavCollapse menu={item} {...props} />
        case 'item':
          return <NavItem item={item} {...props} />
        default:
          return (
            <Typography key={item.id} variant='h6' color='error' align='center'>
              Menu Items Error
            </Typography>
          )
      }
    }
    const shouldFilterSlowdown =
      // Condition 1: PE vertical AND LDPE plant
      (lowerVertName === verticalEnums.PE && plantName === 'LDPE') ||
      // Condition 2: PE vertical AND DMD site
      (lowerVertName === verticalEnums.PE && SITE_NAME === 'dmd')

    // New Condition: Cracker vertical AND VMD site
    const shouldHideUtilitiesNorms =
      lowerVertName === verticalEnums.CRACKER && SITE_NAME === 'vmd'

    let menuItems = menu.children

    // Filter slowdown-norms if needed
    if (shouldFilterSlowdown) {
      menuItems = menuItems.filter((item) => item.id !== 'slowdown-norms')
    }

    // Filter utility-norm-basis for Cracker/VMD
    if (shouldHideUtilitiesNorms) {
      menuItems = menuItems.filter(
        (item) => item.id !== 'utilities-norms-basis',
      )
    }
    return menuItems.map(renderMenuItem)
  }, [
    menu?.children,
    lowerVertName,
    plantName,
    level,
    SITE_NAME,
    isPopover,
    drawerOpen,
  ])

  const collapseButton = (
    <ListItemButton
      onClick={handleClick}
      selected={selected === menu.id}
      sx={{
        minHeight: 36,
        pr: drawerOpen ? 1 : 0,
        py: 0.4,
        mx: '4px',
        mb: 0.5,
        borderRadius: '6px',
        alignItems: 'center',
        justifyContent: drawerOpen || isPopover ? 'initial' : 'center',
        backgroundColor: 'transparent',
        transition: 'all 0.3s ease',
        fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
        '&:hover': {
          backgroundColor: 'rgba(87, 91, 238, 0.08)',
          '& .MuiTypography-root': {
            color: '#4A4DDA !important',
          },
          '& svg': {
            color: '#4A4DDA !important',
          },
        },

        // SELECTED STATE (?? IMPORTANT)
        '&.Mui-selected': {
          background: '#4A4DDA',
          color: '#F0F0F0',

          '&:hover': {
            background: '#4f46e5',
            '& .MuiTypography-root': {
              color: '#fff !important',
            },
            '& svg': {
              color: '#fff !important',
            },
          },

          '& .MuiTypography-root': {
            color: '#fff',
          },

          '& svg': {
            color: '#fff !important',
          },
        },
      }}
    >
      {(drawerOpen || isPopover) && (
        <ListItemText
          primary={
            <Tooltip
              title={menu.title}
              placement='right'
              arrow
              disableInteractive
            >
              <Typography
                noWrap
                sx={{
                  fontSize: '16px',
                  fontWeight: 500,
                  letterSpacing: 0,
                  textTransform: 'capitalize',
                  color: '#606060',
                  fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif"
                }}
              >
                {menu.title}
              </Typography>
            </Tooltip>
          }
          sx={{ my: 0, overflow: 'hidden' }}
        />
      )}

      {(drawerOpen || isPopover) &&
        (open ? (
          <RemoveIcon
            sx={{
              fontSize: 13,
              color: '#4046CA',
              flexShrink: 0,
              transition: 'all 200ms cubic-bezier(.4,0,.2,1)',
            }}
          />
        ) : (
          <AddIcon
            sx={{
              fontSize: 13,
              color: '#4046CA',
              flexShrink: 0,
              transform: 'rotate(90deg)',
              transition: 'all 200ms cubic-bezier(.4,0,.2,1)',
            }}
          />
        ))}

      {!drawerOpen && !isPopover && (
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <AppsIcon
            sx={{
              fontSize: 18,
              color: selected === menu.id ? '#F0F0F0' : '#606060',
            }}
          />
          <IconChevronRight
            sx={{
              fontSize: 18,
              color: selected === menu.id ? '#F0F0F0' : '#606060',
            }}
          />
        </Box>
      )}
    </ListItemButton>
  )

  return (
    <>
      {drawerOpen || isPopover ? (
        collapseButton
      ) : (
        <Tooltip title={menu.title} placement='right' arrow>
          {collapseButton}
        </Tooltip>
      )}

      <Collapse
        in={open && (drawerOpen || isPopover)}
        timeout='auto'
        unmountOnExit
      >
        <List component='div' disablePadding sx={{ pl: 0 }}>
          {menus}
        </List>
      </Collapse>

      <Popover
        id={`popover-${menu.id}`}
        open={Boolean(anchorEl) && !drawerOpen && !isPopover}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
        PaperProps={{
          sx: {
            mt: 0,
            ml: 0.75,
            minWidth: 250,
            background: '#ffffff',
            boxShadow:
              '0 12px 28px 0 rgba(0, 0, 0, 0.12), 0 2px 4px 0 rgba(0, 0, 0, 0.08)',
            borderRadius: '10px',
            border: '1px solid rgba(226, 232, 240, 0.8)',
            overflow: 'hidden',
          },
        }}
      >
        <List
          component='div'
          disablePadding
          sx={{
            py: 0,
            bgcolor: '#ffffff',
          }}
        >
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              py: 1.25,
              px: 2,
              borderBottom: '1px solid #f1f5f9',
              background: '#f8fafc', // Very subtle light gray/blue
              gap: 1,
            }}
          >
            <AppsIcon sx={{ fontSize: 16, color: '#4046CA' }} />
            <Typography
              sx={{
                fontWeight: 700,
                fontSize: '16px',
                color: '#4046CA', // Brand color header
                textTransform: 'capitalize',
                letterSpacing: 0,
                fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif"
              }}
            >
              {menu.title}
            </Typography>
          </Box>
          <Divider />
          {/* ITEM LIST */}
          <Box sx={{ paddingLeft: 1 }}>{menus}</Box>
        </List>
      </Popover>
    </>
  )
}

NavCollapse.propTypes = {
  menu: PropTypes.object,
  level: PropTypes.number,
  onItemClick: PropTypes.func,
  isPopover: PropTypes.bool,
}

export default NavCollapse
