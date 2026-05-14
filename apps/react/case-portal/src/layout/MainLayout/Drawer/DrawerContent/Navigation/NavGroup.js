import PropTypes from 'prop-types'
import { useSelector } from 'react-redux'
import List from '@mui/material/List'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListSubheader from '@mui/material/ListSubheader'
import Typography from '@mui/material/Typography'
import Tooltip from '@mui/material/Tooltip'
import NavItem from './NavItem'
import NavCollapse from './NavCollapse'
import AppsIcon from '@mui/icons-material/Apps' // Example static group icon

/* ===== COMPACT SIDEBAR GROUP STYLES ===== */
const GROUP_BG = 'transparent' // Changed to transparent for a cleaner look
const GROUP_TEXT = '#606060' // Updated color

const NavGroup = ({ item }) => {
  const { drawerOpen } = useSelector((state) => state.menu)

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
        backgroundColor: GROUP_BG,
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
                color: '#F0F0F0',
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
