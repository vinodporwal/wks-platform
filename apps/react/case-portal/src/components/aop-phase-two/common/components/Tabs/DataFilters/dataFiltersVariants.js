import CokerDataFilters from 'components/aop-phase-two/coker/production-norms-basis/DataFilters'
import DataFiltersTab from './DataFilters'
import { ProductionTabApiService } from 'components/aop-phase-two/services/common/productionTabApiService'

export const DEFAULT_DATA_FILTERS_VARIANT = {
  key: 'default',
  Component: DataFiltersTab,
  config: {
    apiService: ProductionTabApiService,
    groupBy: null,
    triggerNormCalculationOnSave: false,
    getColumns: null,
    getPermissions: null,
  },
}

export const DATA_FILTERS_VARIANTS = [
  {
    key: 'coker',
    match: { vertical: 'coker' },
    Component: CokerDataFilters,
    config: {},
  },
  {
    key: 'hydrotreater-vgoht',
    match: { vertical: ['hydrotreater', 'vgoht'] },
    Component: DataFiltersTab,
    config: {
      ...DEFAULT_DATA_FILTERS_VARIANT.config,
      apiService: ProductionTabApiService,
    },
  },
  {
    key: 'naphthasplitter',
    match: { vertical: ['naphthasplitter', 'naphtha splitter'] },
    Component: DataFiltersTab,
    config: {
      ...DEFAULT_DATA_FILTERS_VARIANT.config,
      apiService: ProductionTabApiService,
    },
  },
]

