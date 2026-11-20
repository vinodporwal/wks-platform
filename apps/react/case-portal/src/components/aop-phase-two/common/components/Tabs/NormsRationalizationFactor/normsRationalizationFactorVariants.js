import { ProductionTabApiService } from 'components/aop-phase-two/services/common/productionTabApiService'
import NormsRationalizationFactorTab from './NormsRationalizationFactor'

export const DEFAULT_NORMS_RATIONALIZATION_FACTOR_VARIANT = {
  key: 'default',
  Component: NormsRationalizationFactorTab,
  config: {
    apiService: ProductionTabApiService,
    groupBy: ['type'],
    decimals: 2,
    triggerNormCalculationOnSave: false,
    getColumns: null,
    getPermissions: null,
    formatRow: null,
    buildSavePayload: null,
    validateRows: null,
  },
}

export const NORMS_RATIONALIZATION_FACTOR_VARIANTS = [
  {
    key: 'hydrotreater-vgoht',
    match: { vertical: ['hydrotreater', 'vgoht'] },
    Component: NormsRationalizationFactorTab,
    config: {
      ...DEFAULT_NORMS_RATIONALIZATION_FACTOR_VARIANT.config,
      apiService: ProductionTabApiService,
    },
  },
  {
    key: 'naphthasplitter',
    match: { vertical: ['naphthasplitter', 'naphtha splitter'] },
    Component: NormsRationalizationFactorTab,
    config: {
      ...DEFAULT_NORMS_RATIONALIZATION_FACTOR_VARIANT.config,
      apiService: ProductionTabApiService,
    },
  },
]

