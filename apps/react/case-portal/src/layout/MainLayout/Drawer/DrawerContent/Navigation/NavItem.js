import Avatar from '@mui/material/Avatar'
import Chip from '@mui/material/Chip'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import Typography from '@mui/material/Typography'
import PropTypes from 'prop-types'
import { useDispatch, useSelector } from 'react-redux'
import { useEffect } from 'react'
import { activeItem } from 'store/reducers/menu'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import Button from '@mui/material/Button'
import { useSafeNavigate } from './useSafeNavigate'
import { useLocation } from 'react-router-dom'
import { Tooltip } from '@mui/material'

const NavItem = ({ item, level }) => {
  const isDashboard = item.id === 'dashboard'

  const dispatch = useDispatch()
  const { drawerOpen, openItem } = useSelector((state) => state.menu)
  const { safeNavigate, confirmLeave, setDialogOpen, dialogOpen, itemHandler } =
    useSafeNavigate()
  const location = useLocation()

  const isSelected = openItem.includes(item.id)
  const Icon = item.icon

  const handleClick = () => {
    if (item.requiresConfirmation) {
      setDialogOpen(true)
    } else {
      dispatch(activeItem({ openItem: [item.id] }))
      itemHandler(item.id)
      safeNavigate(item.url)
    }
  }

  useEffect(() => {
    if (location.pathname.includes(item.id)) {
      dispatch(activeItem({ openItem: [item.id] }))
    }
  }, [location.pathname, item.id, dispatch])

  return (
    <>
      <ListItemButton
        disabled={item.disabled}
        onClick={handleClick}
        selected={isSelected}
        sx={{
          minHeight: 30,
          px: 1,
          py: 0.8,
          mx: 0,
          mb: 0,

          borderRadius: 0, // ? REMOVE PILL
          backgroundColor: 'transparent', // default sidebar

          color: '#cbd5e1',

          '&:hover': {
            backgroundColor: 'rgba(255,255,255,0.04)',
          },

          /* ? SELECTED STYLE */
          '&.Mui-selected': {
            backgroundColor: 'transparent', // ? NO CARD
            color: '#10b981',

            '&:hover': {
              backgroundColor: 'rgba(255,255,255,0.04)',
            },

            '& .MuiTypography-root': {
              color: '#10b981 !important',
            },

            '& .MuiListItemIcon-root': {
              color: '#10b981 !important',
            },
          },

          /* ? GREEN LEFT BORDER LIKE RELIANCE */
          '&.Mui-selected::before': {
            content: '""',
            position: 'absolute',
            left: 0,
            top: 0,
            bottom: 0,
            width: '3px',
            backgroundColor: '#10b981',
          },
        }}
      >
        {/* ICON */}
        {Icon && (
          <ListItemIcon
            sx={{
              minWidth: 30,
              color: isSelected || isDashboard ? '#ffffff' : '#94a3b8',

              '& svg': {
                width: 18,
                height: 18,
              },
            }}
          >
            <Icon size={18} strokeWidth={1.7} />
          </ListItemIcon>
        )}

        {/* TEXT */}
        {(drawerOpen || level !== 1) && (
          <ListItemText
            sx={{
              my: 0,
              overflow: 'hidden',
            }}
            primary={
              <Tooltip
                title={item.title}
                placement='right'
                arrow
                enterDelay={1000}
              >
                <Typography
                  sx={{
                    fontSize: '0.82rem',
                    fontWeight: isSelected ? 600 : 500,
                    color: isSelected || isDashboard ? '#ffffff' : '#cbd5e1',
                    letterSpacing: '0.01em',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    maxWidth: '160px',
                    cursor: 'default',
                  }}
                >
                  {item.title}
                </Typography>
              </Tooltip>
            }
          />
        )}

        {/* CHIP */}
        {(drawerOpen || level !== 1) && item.chip && (
          <Chip
            size='small'
            label={item.chip.label}
            avatar={
              item.chip.avatar ? <Avatar>{item.chip.avatar}</Avatar> : null
            }
            sx={{
              ml: 0.5,
              height: 16,
              fontSize: '0.55rem',
              fontWeight: 700,
              bgcolor: '#16a34a',
              color: '#ffffff',
              borderRadius: '1px',
            }}
          />
        )}
      </ListItemButton>

      {/* CONFIRMATION DIALOG */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogTitle>Unsaved Changes</DialogTitle>
        <DialogContent>
          You have unsaved changes. Are you sure you want to leave this page?
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)} color='inherit'>
            Stay
          </Button>
          <Button onClick={() => confirmLeave(item.id)} autoFocus>
            Leave
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

NavItem.propTypes = {
  item: PropTypes.object,
  level: PropTypes.number,
}

export default NavItem
