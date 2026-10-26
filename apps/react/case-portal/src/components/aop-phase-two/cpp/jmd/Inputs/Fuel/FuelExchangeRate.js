import { useCallback, useEffect, useState } from 'react'
import { Box, Button, Stack, TextField, Tooltip } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { FuelAvailabilityAPIService } from 'components/aop-phase-two/services/cpp/jmd/fuelAvailabilityAPIService'
import Notification from 'components/aop-phase-two/common/utilities/Notification'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

// Exchange rate + remark row shown above the JMD Fuel Cost grid.
// Values are stored per site + AOP year (dbo.CPPFuelExchangeRate) via the
// fuel-availability-cost exchange-rate APIs.
const FuelExchangeRate = ({ onRateLoaded }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { siteObject, year } = dataGridStore
  const SITE_ID = siteObject?.id
  const AOP_YEAR = year?.selectedYear

  const [exchangeRate, setExchangeRate] = useState('')
  const [remarks, setRemarks] = useState('')
  const [originalValues, setOriginalValues] = useState({
    value: '',
    remarks: '',
  })
  const [loading, setLoading] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const showSnackbar = (message, severity = 'info') => {
    setSnackbarData({ message, severity })
    setSnackbarOpen(true)
  }

  const fetchExchangeRate = useCallback(async () => {
    if (!keycloak?.token || !SITE_ID || !AOP_YEAR) return
    try {
      const response = await FuelAvailabilityAPIService.getFuelExchangeRate(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )
      const data = response?.data
      const loaded = {
        value: data?.value ?? '',
        remarks: data?.remarks ?? '',
      }
      setOriginalValues(loaded)
      setExchangeRate(loaded.value)
      setRemarks(loaded.remarks)
      onRateLoaded?.(loaded.value)
    } catch (error) {
      console.error('Error fetching fuel exchange rate:', error)
      setOriginalValues({ value: '', remarks: '' })
      setExchangeRate('')
      setRemarks('')
      onRateLoaded?.('')
    }
  }, [keycloak, SITE_ID, AOP_YEAR, onRateLoaded])

  useEffect(() => {
    fetchExchangeRate()
  }, [fetchExchangeRate])

  const isDirty =
    String(exchangeRate) !== String(originalValues.value) ||
    remarks !== originalValues.remarks

  const handleSave = async () => {
    if (!SITE_ID || !AOP_YEAR) return
    if (exchangeRate === '' || exchangeRate === null) {
      showSnackbar('Please enter Exchange Rate', 'error')
      return
    }
    setLoading(true)
    try {
      await FuelAvailabilityAPIService.saveFuelExchangeRate(
        keycloak,
        SITE_ID,
        AOP_YEAR,
        exchangeRate,
        remarks,
      )
      showSnackbar('Exchange rate saved successfully!', 'success')
      fetchExchangeRate()
    } catch (error) {
      console.error('Error saving fuel exchange rate:', error)
      showSnackbar('Failed to save exchange rate. Please try again.', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <Box className='k-table-box' sx={{ p: 1.5 }}>
        <Stack
          direction='row'
          sx={{ columnGap: 1.5, rowGap: 1.5 }}
          alignItems='center'
          flexWrap='wrap'
        >
          <TextField
            size='small'
            type='number'
            label='Exchange Rate'
            placeholder='1$ = INR'
            InputLabelProps={{ shrink: true }}
            value={exchangeRate}
            onChange={(e) => setExchangeRate(e.target.value)}
            sx={{ width: 300 }}
          />
          <TextField
            size='small'
            label='Remark'
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
            sx={{ width: 560 }}
          />
          <Tooltip title='Save Exchange Rate'>
            <Button
              variant='outlined'
              className='btn-load'
              onClick={handleSave}
              disabled={!isDirty || !SITE_ID || !AOP_YEAR}
              sx={{ height: 28, px: 1.5 }}
            >
              Save
            </Button>
          </Tooltip>
        </Stack>
      </Box>
      <Notification
        open={snackbarOpen}
        onClose={() => setSnackbarOpen(false)}
        message={snackbarData.message}
        severity={snackbarData.severity}
      />
    </Box>
  )
}

export default FuelExchangeRate
