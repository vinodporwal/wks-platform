import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord'
import Collapse from '@mui/material/Collapse'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import Typography from '@mui/material/Typography'
import { useTheme } from '@mui/material/styles'
import { IconChevronDown, IconChevronUp } from '@tabler/icons-react'
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
  const menuIcon = menu.icon ? <Icon strokeWidth={1.6} size='1.1rem' /> : null

  return (
    <>
      <ListItemButton
        onClick={handleClick}
        selected={selected === menu.id}
        sx={{
          mb: 0.5,
          px: 2,
          py: 1.25,
          borderRadius: '10px',
          alignItems: 'center',
          position: 'relative',
          transition: 'all 0.25s ease',

          // base
          backgroundColor: 'transparent',

          // hover
          '&:hover': {
            backgroundColor: 'rgba(255,255,255,0.06)',
            transform: 'translateX(2px)',
          },

          // selected
          '&.Mui-selected': {
            backgroundColor: 'rgba(144,202,249,0.14)',
            '&:hover': {
              backgroundColor: 'rgba(144,202,249,0.2)',
            },
            '&::before': {
              content: '""',
              position: 'absolute',
              left: 0,
              top: 8,
              bottom: 8,
              width: '3px',
              borderRadius: '3px',
              backgroundColor: theme.palette.primary.main,
            },
          },
        }}
      >
        {menuIcon && (
          <ListItemIcon sx={{ minWidth: 32, color: 'inherit' }}>
            {menuIcon}
          </ListItemIcon>
        )}
        <ListItemText
          primary={
            <Typography
              sx={{
                fontSize: '0.9rem',
                fontWeight: selected === menu.id ? 600 : 500,
                letterSpacing: '0.02em',
              }}
            >
              {menu.title}
            </Typography>
          }
          secondary={
            menu.caption && (
              <Typography
                variant='caption'
                sx={{ ...theme.typography.subMenuCaption }}
                display='block'
                gutterBottom
              >
                {menu.caption}
              </Typography>
            )
          }
        />
        {open ? (
          <IconChevronUp
            stroke={1.5}
            size='1rem'
            style={{ marginTop: 'auto', marginBottom: 'auto' }}
          />
        ) : (
          <IconChevronDown
            stroke={1.5}
            size='1rem'
            style={{ marginTop: 'auto', marginBottom: 'auto' }}
          />
        )}
      </ListItemButton>
      <Collapse in={open} timeout='auto' unmountOnExit>
        <List
          component='div'
          disablePadding
          sx={{
            position: 'relative',
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
