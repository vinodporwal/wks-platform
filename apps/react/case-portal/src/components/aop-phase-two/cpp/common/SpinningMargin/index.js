import SpinningMarginJMD from './SpinningMarginJMD'
import SpinningMarginDMD from './SpinningMarginDMD'
import IntersiteSteamTransfer from '../../jmd/Inputs/components/IntersiteSteamTransfer'
import { Stack } from '@mui/material'
import { useSelector } from 'react-redux'

const SpinningMargin = () => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { siteObject } = dataGridStore
  const IS_JMD = siteObject?.name?.toLowerCase() == 'jmd'
  return (
    <div>
      {IS_JMD ? (
        <>
          <SpinningMarginJMD />
          <Stack sx={{ mt: 3 }}>
            <IntersiteSteamTransfer />
          </Stack>
        </>
      ) : (
        <SpinningMarginDMD />
      )}
    </div>
  )
}

export default SpinningMargin
