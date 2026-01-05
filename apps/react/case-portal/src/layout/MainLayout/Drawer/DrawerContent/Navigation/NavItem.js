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

/* ===== MODERN SIDEBAR COLORS ===== */
const ITEM_BASE = 'rgba(255,255,255,0.02)'
const ITEM_HOVER = 'rgba(255,255,255,0.06)'
const ITEM_ACTIVE = 'rgba(57,166,255,0.14)'
const ACCENT = '#39a6ff'
const TEXT = 'rgba(255,255,255,0.88)'
const TEXT_MUTED = 'rgba(255,255,255,0.65)'

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
          px: drawerOpen ? 1.25 : 0.75,
          py: 0.75,
          borderRadius: '10px',
          position: 'relative',
          overflow: 'hidden',
          transition: 'all 0.25s ease',

          bgcolor: ITEM_BASE,
          color: TEXT,

          '&:hover': {
            bgcolor: ITEM_HOVER,
            transform: 'translateX(2px)',
          },

          '&.Mui-selected': {
            bgcolor: ITEM_ACTIVE,
            color: '#ffffff',
            boxShadow: '0 6px 20px rgba(57,166,255,0.25)',

            '&:hover': {
              bgcolor: ITEM_ACTIVE,
            },

            /* LEFT ACCENT BAR */
            '&::before': {
              content: '""',
              position: 'absolute',
              left: 0,
              top: 8,
              bottom: 8,
              width: '3px',
              borderRadius: '6px',
              background: `linear-gradient(180deg, ${ACCENT}, #6fb9ff)`,
            },
          },
        }}
      >
        {/* ICON */}
        {Icon && (
          <ListItemIcon
            sx={{
              minWidth: 30,
              color: isSelected ? ACCENT : TEXT_MUTED,
              transition: 'all 0.25s ease',
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
                  letterSpacing: '0.02em',
                  color: isSelected ? '#ffffff' : TEXT,
                  whiteSpace: 'nowrap',
                }}
              >
                {item.title}
              </Typography>
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
              ml: 0.75,
              height: 18,
              fontSize: '0.62rem',
              fontWeight: 600,
              bgcolor: 'rgba(255,255,255,0.18)',
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
