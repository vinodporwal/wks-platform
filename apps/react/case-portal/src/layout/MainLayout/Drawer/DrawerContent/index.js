import SimpleBar from 'components/third-party/SimpleBar'
import Navigation from './Navigation'

const DrawerContent = () => (
  <SimpleBar
    sx={{
      '& .simplebar-content': {
        display: 'flex',
        flexDirection: 'column',
        height: '100%', // Ensure it spans full height
      },
      bgcolor: '#f8fafc', // Matches the start of your Navigation gradient
    }}
  >
    <Navigation />
  </SimpleBar>
)

export default DrawerContent
