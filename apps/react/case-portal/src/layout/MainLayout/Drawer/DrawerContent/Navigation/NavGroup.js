import PropTypes from 'prop-types'
import { useSelector } from 'react-redux'
import List from '@mui/material/List'
import ListSubheader from '@mui/material/ListSubheader'
import Typography from '@mui/material/Typography'
import NavItem from './NavItem'
import NavCollapse from './NavCollapse'

/* ===== COMPACT SIDEBAR GROUP STYLES ===== */
const GROUP_BG = 'transparent' // Changed to transparent for a cleaner look
const GROUP_TEXT = '#9ca3af' // Your "Target & Demand" grey

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
        mb: drawerOpen ? 1.5 : 0, // Increased spacing between groups
        py: 0,
        backgroundColor: GROUP_BG,
      }}
      subheader={
        item.title &&
        drawerOpen && (
          <ListSubheader
            disableSticky
            sx={{
              px: 2, // Standard alignment
              py: 0.5, // Modern breathing room
              mb: 0.25,
              backgroundColor: 'transparent',
              lineHeight: 1,
            }}
          >
            <Typography
              sx={{
                // THE "TARGETS & DEMAND" STYLE
                fontSize: '0.68rem',
                fontWeight: 700, // Bold like the image
                letterSpacing: '0.15em', // Key for that wide, modern look
                textTransform: 'uppercase',
                color: GROUP_TEXT,
                fontFamily: '"Public Sans", sans-serif', // Modern UI choice
                opacity: 0.8,
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
