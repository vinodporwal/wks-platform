import SimpleBar from 'components/third-party/SimpleBar'
import Navigation from './Navigation'

const DrawerContent = () => (
  <SimpleBar
    sx={{
      height: '100%',
      bgcolor: 'transparent',

      /* Smooth feel */
      transition:
        'box-shadow 240ms cubic-bezier(.4,0,.2,1), background-color 240ms cubic-bezier(.4,0,.2,1)',

      '&:hover': {
        boxShadow:
          '6px 0 20px rgba(15,23,42,0.1), 1px 0 3px rgba(15,23,42,0.06)',
      },

      '& .simplebar-content': {
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        padding: 0,
      },

      /* Modern scrollbar */
      '& .simplebar-scrollbar:before': {
        background: 'linear-gradient(180deg, #6366f1, #3b82f6)',
        borderRadius: '999px',
        transition: 'opacity 200ms ease',
      },

      '& .simplebar-scrollbar.simplebar-visible:before': {
        opacity: 0.8,
      },
    }}
  >
    <Navigation />
  </SimpleBar>
)

export default DrawerContent
