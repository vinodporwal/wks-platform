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
import { useTheme, Tooltip } from '@mui/material'

const NavItem = ({ item, level, onItemClick, isPopover }) => {
  const isDashboard = item.id === 'dashboard'

  const dispatch = useDispatch()
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'

  const { drawerOpen, openItem } = useSelector((state) => state.menu)
  const { safeNavigate, confirmLeave, setDialogOpen, dialogOpen, itemHandler } =
    useSafeNavigate()
  const location = useLocation()

  const isSelected = openItem.includes(item.id)
  const Icon = item.icon

  const primaryColor = isDark ? '#4046CA' : '#4046CA'
  const textColor = isDark ? '#606060' : '#606060'
  const hoverBg = isDark ? 'rgba(129, 140, 248, 0.15)' : 'rgba(87, 91, 238, 0.08)'

  const handleClick = () => {
    if (onItemClick) onItemClick()
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

  const itemContent = (
    <ListItemButton
      disabled={item.disabled}
      onClick={handleClick}
      selected={isSelected}
      sx={{
        minHeight: 30,
        px: 1,
        py: 0.8,
        mx: '4px',
        mb: 0.5,
        justifyContent: drawerOpen || isPopover ? 'flex-start' : 'center',

        borderRadius: '6px', // ? RESTORE PILL RADIUS
        backgroundColor: 'transparent', // default sidebar

        color: textColor,
        transition: 'all 0.3s ease',

        '&:hover': {
          backgroundColor: hoverBg,
          color: primaryColor,
          '& .MuiTypography-root': {
            color: `${primaryColor} !important`,
          },
          '& .MuiListItemIcon-root': {
            color: `${primaryColor} !important`,
          },
        },

        /* ? SELECTED STYLE */
        '&.Mui-selected': {
          backgroundColor: isDark ? '#EDEEFF' : '#fff', // Brand light blue/purple bg
          color: '#4046CA',
          marginRight: '10px',

          '&:hover': {
            backgroundColor: isDark ? '#EDEEFF' : 'rgba(87, 91, 238, 0.1)',
          },

          '& .MuiTypography-root': {
            color: '#4046CA !important',
          },

          '& .MuiListItemIcon-root': {
            color: '#4046CA !important',
          },
        },

        /* ? SELECTED LEFT BORDER INDICATOR - HIDDEN IN POPOVER */
        '&.Mui-selected::before': {
          content: '""',
          position: 'absolute',
          left: 0,
          top: '10%',
          bottom: '10%',
          width: '3px',
          borderRadius: '0 4px 4px 0',
          backgroundColor: isPopover ? 'transparent' : (isDark ? '#818cf8' : '#575bee'),
        },
      }}
    >
      {/* ICON */}
      {Icon && (
        <ListItemIcon
          sx={{
            minWidth: drawerOpen || isPopover ? 30 : 0,
            color: isSelected || isDashboard ? primaryColor : textColor,
            justifyContent: 'center',

            '& svg': {
              width: 18,
              height: 18,
            },
          }}
        >
          <Icon size={18} strokeWidth={1.7} />
        </ListItemIcon>
      )}

      {/* TEXT - HIDDEN IN MINI MODE FOR ALL LEVELS */}
      {(drawerOpen || isPopover) && (
        <ListItemText
          sx={{
            my: 0,
            overflow: 'hidden',
          }}
          primary={
            <Typography
              sx={{
                display: 'block',
                visibility: 'visible',
                fontSize: isPopover ? '14px' : '16px',
                fontWeight: isSelected ? 700 : 500,
                color: isSelected ? primaryColor : textColor,
                letterSpacing: isPopover ? '0.012em' : '0.01em',
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                maxWidth: '180px',
                fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
              }}
            >
              {item.title}
            </Typography>
          }
        />
      )}

      {/* CHIP */}
      {drawerOpen && item.chip && (
        <Chip
          size='small'
          label={item.chip.label}
          avatar={item.chip.avatar ? <Avatar>{item.chip.avatar}</Avatar> : null}
          sx={{
            ml: 0.5,
            height: 16,
            fontSize: '0.55rem',
            fontWeight: 700,
            bgcolor: isDark ? '#818cf8' : '#575bee',
            color: '#ffffff',
            borderRadius: '1px',
          }}
        />
      )}
    </ListItemButton>
  )

  return (
    <>
      {drawerOpen || isPopover ? (
        itemContent
      ) : (
        <Tooltip title={item.title} placement='right' arrow>
          {itemContent}
        </Tooltip>
      )}

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
  onItemClick: PropTypes.func,
  isPopover: PropTypes.bool,
}

export default NavItem
