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
import { useTheme } from '@mui/material/styles'

// Icons
import AddIcon from '@mui/icons-material/Add'
import RemoveIcon from '@mui/icons-material/Remove'
import AppsIcon from '@mui/icons-material/Apps'

// Internal Imports
import { verticalEnums } from 'enums/verticalEnums'
import NavItem from './NavItem'

const NavCollapse = ({ menu, level }) => {
  const theme = useTheme()
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState(null)

  const { drawerOpen } = useSelector((state) => state.menu)
  const { plantID, verticalChange, siteObject } = useSelector(
    (state) => state.dataGridStore,
  )

  const plantName = plantID?.plantName

  const SITE_NAME = siteObject?.name?.toLowerCase()

  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || 'meg'

  const handleClick = () => {
    setOpen(!open)
    setSelected(!selected ? menu.id : null)
  }

  const menus = useMemo(() => {
    if (!menu?.children) return []

    const renderMenuItem = (item) => {
      const props = { key: item.id, level: level + 1 }
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

    // Filter combined-production-norms for PP vertical when site is NOT sez/hmd/dta
    const shouldFilterCombinedProduction =
      lowerVertName === verticalEnums.PP &&
      !['sez', 'hmd', 'dta'].includes(SITE_NAME?.toLowerCase())

    let menuItems = menu.children

    // Filter slowdown-norms if needed
    if (shouldFilterSlowdown) {
      menuItems = menuItems.filter((item) => item.id !== 'slowdown-norms')
    }

    // Filter combined-production-norms for PP vertical when NOT sez/hmd/dta
    if (shouldFilterCombinedProduction) {
      menuItems = menuItems.filter(
        (item) => item.id !== 'combined-production-norms',
      )
    }
    return menuItems.map(renderMenuItem)
  }, [menu?.children, lowerVertName, plantName, level, SITE_NAME])

  const collapseButton = (
    <ListItemButton
      onClick={handleClick}
      selected={selected === menu.id}
      sx={{
        minHeight: 36,
        px: 1,
        py: 0.4,
        borderRadius: 1,
        alignItems: 'center',
        justifyContent: drawerOpen ? 'initial' : 'center',
        backgroundColor: 'transparent',

        '&:hover': {
          backgroundColor: 'rgba(255,255,255,0.05)',
        },

        // SELECTED STATE (?? IMPORTANT)
        '&.Mui-selected': {
          background: '#575bee',
          color: '#fff',
          borderRadius: '6px',

          '&:hover': {
            background: '#575bee',
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
      {drawerOpen && (
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
                  fontSize: '0.7rem',
                  fontWeight: 600,
                  letterSpacing: '0.08em',
                  textTransform: 'uppercase',
                  color: '#9ca3af',
                }}
              >
                {menu.title}
              </Typography>
            </Tooltip>
          }
          sx={{ my: 0, overflow: 'hidden' }}
        />
      )}

      {drawerOpen &&
        (open ? (
          <RemoveIcon
            sx={{
              fontSize: 13,
              color: '#1d4ed8',
              flexShrink: 0,
              transition: 'all 200ms cubic-bezier(.4,0,.2,1)',
            }}
          />
        ) : (
          <AddIcon
            sx={{
              fontSize: 13,
              color: '#1d4ed8',
              flexShrink: 0,
              transform: 'rotate(90deg)',
              transition: 'all 200ms cubic-bezier(.4,0,.2,1)',
            }}
          />
        ))}

      {!drawerOpen && (
        <AppsIcon
          sx={{
            fontSize: 18,
            color: selected === menu.id ? '#fff' : '#6a7b92',
          }}
        />
      )}
    </ListItemButton>
  )

  return (
    <>
      {drawerOpen ? (
        collapseButton
      ) : (
        <Tooltip title={menu.title} placement='right' arrow>
          {collapseButton}
        </Tooltip>
      )}

      <Collapse in={open} timeout='auto' unmountOnExit>
        <List component='div' disablePadding sx={{ pl: 0 }}>
          {menus}
        </List>
      </Collapse>
    </>
  )
}

NavCollapse.propTypes = {
  menu: PropTypes.object,
  level: PropTypes.number,
}

export default NavCollapse
