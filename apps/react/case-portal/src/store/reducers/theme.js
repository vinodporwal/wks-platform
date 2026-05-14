import { createSlice } from '@reduxjs/toolkit'

// initial state
const initialState = {
  mode: localStorage.getItem('themeMode') || 'light',
}

// ==============================|| SLICE - THEME ||============================== //

const theme = createSlice({
  name: 'theme',
  initialState,
  reducers: {
    setMode(state, action) {
      state.mode = action.payload.mode
      localStorage.setItem('themeMode', action.payload.mode)
    },
  },
})

export default theme.reducer

export const { setMode } = theme.actions
