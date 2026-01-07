import PropTypes from 'prop-types'
import { useSelector } from 'react-redux'
import List from '@mui/material/List'
import ListSubheader from '@mui/material/ListSubheader'
import Typography from '@mui/material/Typography'
import NavItem from './NavItem'
import NavCollapse from './NavCollapse'

/* ===== COMPACT SIDEBAR GROUP STYLES ===== */
const GROUP_BG = 'transparent'
const GROUP_TEXT = '#9ca3af'

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
        mb: drawerOpen ? 0.5 : 0,
        py: 0,
        backgroundColor: GROUP_BG,
      }}
      subheader={
        item.title &&
        drawerOpen && (
          <ListSubheader
            disableSticky
            sx={{
              px: 0.75, // ?? reduced left padding
              py: 0.25,
              mb: 0.25,
              backgroundColor: 'transparent',
              lineHeight: 1,
            }}
          >
            <Typography
              sx={{
                fontSize: '0.6rem', // ?? smaller header
                fontWeight: 600,
                letterSpacing: '0.12em',
                textTransform: 'uppercase',
                color: GROUP_TEXT,
              }}
            >
              {item.title}
            </Typography>
          </ListSubheader>
        )
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
