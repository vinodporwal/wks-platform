import PropTypes from 'prop-types'
import { useSelector } from 'react-redux'
import Box from '@mui/material/Box'
import List from '@mui/material/List'
import ListSubheader from '@mui/material/ListSubheader'
import Typography from '@mui/material/Typography'
import NavItem from './NavItem'
import NavCollapse from './NavCollapse'

const GROUP_BG = '#122d41ff' // group background
const GROUP_TEXT = '#ffffff' // PURE WHITE for visibility

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
              fontSize: '0.75rem',
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
        mb: drawerOpen ? 0.75 : 0,
        py: 0,
        backgroundColor: GROUP_BG,
      }}
      subheader={
        item.title &&
        drawerOpen && (
          <ListSubheader
            disableSticky
            sx={{
              px: 1.25,
              py: 0.6,
              mb: 0.25,
              backgroundColor: GROUP_BG,
              lineHeight: 1,
            }}
          >
            <Typography
              sx={{
                fontSize: '0.68rem',
                fontWeight: 600,
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                color: GROUP_TEXT,
                opacity: 0.9,
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
