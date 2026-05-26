import { createSlice } from '@reduxjs/toolkit'

const getStorageYear = () => {
  const year = localStorage.getItem('year')
  return year ? year : ''
}

const initialState = {
  sitePlantChange: false,
  verticalChange: {},
  isBlocked: false,
  year: { selectedYear: getStorageYear() },
  yearChanged: false,
  currentYear: null,
  oldYear: null,
  siteID: null,
  plantID: null,
  plantObject: null,
  siteObject: null,
  verticalObject: null,
  verticalChangeFromDashboard: null,
  isReleased: 0,
  jmdSelectedPlants: [], // multi-select plant list for CPP-JMD; index 0 = primary plant
}

const dataGridStore = createSlice({
  name: 'menu',
  initialState,
  reducers: {
    setSitePlantChange(state, action) {
      state.sitePlantChange = action.payload
    },
    setYearChange(state, action) {
      state.yearChanged = action.payload
    },
    setVerticalChange(state, action) {
      state.verticalChange = action.payload
    },
    setIsBlocked(state, action) {
      state.isBlocked = action.payload
    },
    setScreenTitle(state, action) {
      state.screenTitle = action.payload
    },
    setAopYear(state, action) {
      state.year = action.payload
    },
    setCurrentYear(state, action) {
      state.currentYear = action.payload
    },
    setOldYear(state, action) {
      state.oldYear = action.payload
    },
    setSiteID(state, action) {
      state.siteID = action.payload
    },
    setPlantID(state, action) {
      state.plantID = action.payload
    },

    setPlantObject(state, action) {
      state.plantObject = action.payload
      if (action.payload && action.payload.id !== undefined) {
        state.plantID = action.payload.id
      }
    },
    setSiteObject(state, action) {
      state.siteObject = action.payload
      if (action.payload && action.payload.id !== undefined) {
        state.siteID = action.payload.id
      }
    },
    setVerticalObject(state, action) {
      state.verticalObject = action.payload
      if (action.payload && action.payload.id !== undefined) {
        state.verticalID = action.payload.id
      }
    },
    setVerticalChangeFromDashboard(state, action) {
      state.verticalChangeFromDashboard = action.payload
    },

    setIsReleased: (state, action) => {
      state.isReleased = action.payload.isReleased
    },
    setJmdSelectedPlants(state, action) {
      // payload: array of { id, name } objects
      // index 0 is always the primary plant (mirrors plantID)
      state.jmdSelectedPlants = action.payload
      if (action.payload.length > 0) {
        state.plantID = {
          plantId: action.payload[0].id,
          plantName: action.payload[0].name,
        }
        state.plantObject = {
          id: action.payload[0].id,
          name: action.payload[0].name,
        }
      }
    },
  },
})

export default dataGridStore.reducer

export const {
  setSitePlantChange,
  setVerticalChange,
  setIsBlocked,
  setScreenTitle,
  setAopYear,
  setYearChange,
  setCurrentYear,
  setOldYear,
  setSiteID,
  setPlantID,
  setPlantObject,
  setSiteObject,
  setVerticalObject,
  setVerticalChangeFromDashboard,
  setIsReleased,
  setJmdSelectedPlants,
} = dataGridStore.actions
