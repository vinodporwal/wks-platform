import Collapse from '@mui/material/Collapse'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import Typography from '@mui/material/Typography'
import { useTheme } from '@mui/material/styles'
import { IconChevronDown, IconChevronUp } from '@tabler/icons-react'
import AddIcon from '@mui/icons-material/Add'
import RemoveIcon from '@mui/icons-material/Remove'
import { verticalEnums } from 'enums/verticalEnums'
import PropTypes from 'prop-types'
import { useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
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

  const Icon = menu.icon
  const menuIcon = menu.icon ? <Icon strokeWidth={1.6} size='1rem' /> : null

  return (
    <>
      <ListItemButton
        onClick={handleClick}
        selected={selected === menu.id}
        sx={{
          minHeight: 32, // ?? compact height
          px: 0.75, // ?? no left padding
          py: 0.25,
          borderRadius: 0,
          alignItems: 'center',
          position: 'relative',
          backgroundColor: 'transparent',

          '&:hover': {
            backgroundColor: '#e5e7eb',
          },

          '&.Mui-selected': {
            backgroundColor: '#e0e7ff',
            '&:hover': {
              backgroundColor: '#c7d2fe',
            },
            '&::before': {
              content: '""',
              position: 'absolute',
              left: 0,
              top: 4,
              bottom: 4,
              width: '2px',
              backgroundColor: theme.palette.primary.main,
            },
          },
        }}
      >
        {menuIcon && (
          <ListItemIcon
            sx={{
              minWidth: 26,
              color: '#277424ff',
            }}
          >
            {menuIcon}
          </ListItemIcon>
        )}

        <ListItemText
          primary={
            <Typography
              sx={{
                fontSize: '0.75rem',
                fontWeight: selected === menu.id ? 600 : 500,
                lineHeight: 1.2,
                color: '#374151',
              }}
            >
              {menu.title}
            </Typography>
          }
          sx={{ my: 0 }}
        />

        {open ? (
          <RemoveIcon
            sx={{
              fontSize: 13,
              color: '#1d4ed8',
              transform: 'rotate(0deg)',
              transition: 'all 200ms cubic-bezier(.4,0,.2,1)',
            }}
          />
        ) : (
          <AddIcon
            sx={{
              fontSize: 13,
              color: '#1d4ed8',
              transform: 'rotate(90deg)',
              transition: 'all 200ms cubic-bezier(.4,0,.2,1)',
            }}
          />
        )}
      </ListItemButton>

      <Collapse in={open} timeout='auto' unmountOnExit>
        <List
          component='div'
          disablePadding
          sx={{
            pl: 0, // ?? no indent
          }}
        >
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
