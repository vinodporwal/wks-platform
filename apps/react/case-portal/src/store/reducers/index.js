// third-party
import { combineReducers } from 'redux'

// project import
import menu from './menu'
import theme from './theme'
import dataGridStore from './dataGridStore'

// ==============================|| COMBINE REDUCERS ||============================== //

const reducers = combineReducers({ menu, theme, dataGridStore })

export default reducers
