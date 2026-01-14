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

// Internal Imports
import { verticalEnums } from 'enums/verticalEnums'
import NavItem from './NavItem'

const NavCollapse = ({ menu, level }) => {
  const theme = useTheme()
  const [open, setOpen] = useState(true)
  const [selected, setSelected] = useState(menu.id)

  const { plantID, verticalChange } = useSelector(
    (state) => state.dataGridStore,
  )

  const plantName = plantID?.plantName
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
      lowerVertName === verticalEnums.PE && plantName === 'LDPE'
    const menuItems = shouldFilterSlowdown
      ? menu.children.filter((item) => item.id !== 'slowdown-norms')
      : menu.children

    return menuItems.map(renderMenuItem)
  }, [menu?.children, lowerVertName, plantName, level])

  return (
    <>
      <ListItemButton
        onClick={handleClick}
        selected={selected === menu.id}
        sx={{
          minHeight: 32,
          px: 0.75,
          py: 0.25,
          borderRadius: 0,
          alignItems: 'center',
          position: 'relative',
          backgroundColor: 'transparent',
          // 1. Bottom Border
          borderBottom: '1px solid',
          borderColor: '#d1d1d1',
          '&:hover': {
            backgroundColor: '#f3f4f6',
          },
          // 2. Light "Blackish-Grey" Selected State
          '&.Mui-selected': {
            backgroundColor: '#e7e7e7', // Light grey selected background
            '&:hover': {
              backgroundColor: '#d1d1d1',
            },
            // Keep text dark so it is visible
            '& .MuiTypography-root': {
              color: '#111827', // Darker text for contrast
            },
            // Keep icons blue (or change to black if preferred)
            '& svg': {
              color: '#1d4ed8 !important',
            },
          },
        }}
      >
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
                  fontSize: '0.68rem',
                  fontWeight: 700,
                  letterSpacing: '0.12em',
                  textTransform: 'uppercase',
                  color: '#111827', // Default grey color
                  fontFamily: '"Public Sans", sans-serif',
                  lineHeight: 1.2,
                  display: 'block',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  width: '100%',
                }}
              >
                {menu.title}
              </Typography>
            </Tooltip>
          }
          sx={{ my: 0, overflow: 'hidden' }}
        />

        {open ? (
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
        )}
      </ListItemButton>

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
