import React from 'react'
import { Stack } from '@mui/material/index'
import FixedNorms from './FixedNorms'
import Quantity from './Quantity'

const InputNorms = () => {
  return (
    <Stack>
      <FixedNorms />
      <Quantity />
    </Stack>
  )
}

export default InputNorms
