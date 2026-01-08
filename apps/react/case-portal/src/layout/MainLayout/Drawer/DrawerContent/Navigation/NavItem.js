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
import { Tooltip } from '../../../../../../node_modules/@mui/material/index'

/* ===== COMPACT SIDEBAR COLORS ===== */
const ITEM_BASE = 'transparent'
const ITEM_HOVER = '#e5e7eb'
const ITEM_ACTIVE = '#17206e'

const TEXT = '#374151'
const ICON = '#6366f1'

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
          minHeight: 30,
          px: 1,
          py: 0.5,
          mx: 0,
          mb: 0,
          borderRadius: '10px',
          transition: 'all 160ms ease',

          fontFamily: '"Segoe UI", Open Sans, Helvetica, Arial, sans-serif',
          fontSize: '0.82rem',
          fontWeight: 500,

          bgcolor: 'transparent',
          color: '#3f3f46', // ?? soft dark gray (unselected text)

          '&:hover': {
            background:
              'linear-gradient(135deg, rgba(18, 88, 179, 0.12) 0%, rgba(18, 88, 179, 0.22) 100%)',
            transform: 'translateY(-1px)',
            boxShadow: '0 4px 10px rgba(18, 88, 179, 0.18)',
            transition: 'all 200ms cubic-bezier(0.4, 0, 0.2, 1)',
          },

          '&.Mui-selected': {
            background: 'linear-gradient(135deg, #0b3d91 0%, #1258b3 100%)',
            color: '#ffffff',
            fontWeight: 600,

            '&:hover': {
              background: 'linear-gradient(135deg, #0a347a 0%, #1a63c6 100%)',
              transform: 'translateY(-1px)',
              boxShadow: '0 6px 14px rgba(10, 60, 150, 0.35)',
              transition: 'all 220ms cubic-bezier(0.4, 0, 0.2, 1)',
            },
          },
        }}
      >
        {/* ICON */}
        {Icon && (
          <ListItemIcon
            sx={{
              minWidth: 30,
              color: !isSelected ? '#030303ff' : '#ffffff',
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
              overflow: 'hidden', // important for ellipsis
            }}
            primary={
              <Tooltip
                title={item.title}
                placement='right'
                arrow
                enterDelay={1000} // ?? avoids annoying instant pop
              >
                <Typography
                  sx={{
                    fontFamily:
                      '"Segoe UI", Open Sans, Helvetica, Arial, sans-serif',
                    fontSize: '0.82rem',
                    fontWeight: isSelected ? 600 : 500,
                    color: isSelected ? '#ffffff' : '#3f3f46',
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

      {/* CONFIRMATION DIALOG (UNCHANGED) */}
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
