import { ProductionNormsApiService as NaphthaSplitterApiService } from 'components/aop-phase-two/services/naphthasplitter/productionNormsApiService'
import { ProductionNormsApiService as VgohtApiService } from 'components/aop-phase-two/services/vgoht/productionNormsApiService'
import CokerDataFilters from 'components/aop-phase-two/coker/production-norms-basis/DataFilters'
import DataFiltersTab from './DataFilters'

export const DEFAULT_DATA_FILTERS_VARIANT = {
  key: 'default',
  Component: DataFiltersTab,
  config: {
    apiService: NaphthaSplitterApiService,
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
      apiService: VgohtApiService,
    },
  },
  {
    key: 'naphthasplitter',
    match: { vertical: ['naphthasplitter', 'naphtha splitter'] },
    Component: DataFiltersTab,
    config: {
      ...DEFAULT_DATA_FILTERS_VARIANT.config,
      apiService: NaphthaSplitterApiService,
    },
  },
]

