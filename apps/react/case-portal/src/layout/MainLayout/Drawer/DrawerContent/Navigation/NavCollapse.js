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
          minHeight: 36,
          px: 1,
          py: 0.4,
          borderRadius: 1,
          alignItems: 'center',
          backgroundColor: 'transparent',

          '&:hover': {
            backgroundColor: 'rgba(255,255,255,0.05)',
          },

          // SELECTED STATE (?? IMPORTANT)
          '&.Mui-selected': {
            background: 'linear-gradient(90deg, #2563eb 0%, #9333ea 100%)',
            color: '#fff',
            borderRadius: '6px',

            '&:hover': {
              background: 'linear-gradient(90deg, #2563eb 0%, #9333ea 100%)',
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
