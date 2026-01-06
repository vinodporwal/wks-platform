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

/* ===== LIGHT SIDEBAR COLORS (MATCH SCREENSHOT) ===== */
const ITEM_BASE = 'transparent'
const ITEM_HOVER = '#eef2ff'
const ITEM_ACTIVE = '#17206e'

const TEXT = '#111827'
const TEXT_MUTED = '#6b7280'
const ICON_MUTED = '#9ca3af'

const NavItem = ({ item, level }) => {
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
          mx: 0.75,
          mb: 0.25,
          px: 1.25,
          py: 0.75,
          borderRadius: '10px',
          transition: 'all 160ms ease',

          bgcolor: ITEM_BASE,
          color: TEXT,

          '&:hover': {
            bgcolor: ITEM_HOVER,
          },

          '&.Mui-selected': {
            bgcolor: ITEM_ACTIVE,
            color: '#ffffff',

            '&:hover': {
              bgcolor: ITEM_ACTIVE,
            },
          },
        }}
      >
        {/* ICON */}
        {Icon && (
          <ListItemIcon
            sx={{
              minWidth: 30,
              color: isSelected ? '#ffffff' : ICON_MUTED,
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
            primary={
              <Typography
                sx={{
                  fontSize: '0.8rem',
                  fontWeight: isSelected ? 600 : 500,
                  color: isSelected ? '#ffffff' : TEXT,
                  whiteSpace: 'nowrap',
                }}
              >
                {item.title}
              </Typography>
            }
          />
        )}

        {/* ACTIVE CHIP */}
        {(drawerOpen || level !== 1) && item.chip && (
          <Chip
            size='small'
            label={item.chip.label}
            avatar={
              item.chip.avatar ? <Avatar>{item.chip.avatar}</Avatar> : null
            }
            sx={{
              ml: 0.75,
              height: 18,
              fontSize: '0.6rem',
              fontWeight: 700,
              bgcolor: '#16a34a', // green ACTIVE badge
              color: '#ffffff',
              borderRadius: '6px',
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
