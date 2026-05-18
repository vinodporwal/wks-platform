import PropTypes from 'prop-types'
import { useSelector } from 'react-redux'
import List from '@mui/material/List'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListSubheader from '@mui/material/ListSubheader'
import Typography from '@mui/material/Typography'
import Tooltip from '@mui/material/Tooltip'
import { useTheme } from '@mui/material/styles'
import NavItem from './NavItem'
import NavCollapse from './NavCollapse'
import AppsIcon from '@mui/icons-material/Apps' // Example static group icon

const NavGroup = ({ item }) => {
  const { drawerOpen } = useSelector((state) => state.menu)
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'

  const GROUP_TEXT = isDark ? '#D0D0D0' : '#606060'

  const navCollapse = item.children?.map((menuItem) => {
    switch (menuItem.type) {
      case 'collapse':
        return <NavCollapse key={menuItem.id} menu={menuItem} level={1} />
      case 'item':
        return <NavItem key={menuItem.id} item={menuItem} level={1} />
      default:
        return (
          <Typography
            key={menuItem.id}
            sx={{
              color: 'error.main',
              textAlign: 'center',
              fontSize: '0.65rem',
            }}
          >
            Fix - Group Collapse or Items
          </Typography>
        )
    }
  })

  return (
    <List
      disablePadding
      sx={{
        mb: drawerOpen ? 1.5 : 0,
        py: 0,
        backgroundColor: 'transparent',
        mx: '3px',
      }}
      subheader={
        item.title &&
        (drawerOpen ? (
          <ListSubheader
            disableSticky
            sx={{
              px: 2,
              py: 0.5,
              mb: 0.25,
              backgroundColor: 'transparent',
              lineHeight: 1,
            }}
          >
            <Typography
              sx={{
                fontSize: '16px',
                fontWeight: 500,
                letterSpacing: 0,
                textTransform: 'capitalize',
                color: GROUP_TEXT,
                fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
                opacity: 0.8,
              }}
            >
              {item.title}
            </Typography>
          </ListSubheader>
        ) : (
          <Tooltip title={item.title} placement='right' arrow>
            <ListItemIcon
              sx={{
                minWidth: 0,
                display: 'flex',
                justifyContent: 'center',
                py: 1,
                color: isDark ? '#D0D0D0' : '#606060',
              }}
            >
              <AppsIcon sx={{ fontSize: 18 }} />
            </ListItemIcon>
          </Tooltip>
        ))
      }
    >
      {navCollapse}
    </List>
  )
}

NavGroup.propTypes = {
  item: PropTypes.object,
}

export default NavGroup
