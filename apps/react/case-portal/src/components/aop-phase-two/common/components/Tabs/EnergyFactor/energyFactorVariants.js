import { ProductionTabApiService } from 'components/aop-phase-two/services/common/productionTabApiService'
import EnergyFactorTab from './EnergyFactor'

export const DEFAULT_ENERGY_FACTOR_VARIANT = {
  key: 'default',
  Component: EnergyFactorTab,
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

export const ENERGY_FACTOR_VARIANTS = [
  {
    key: 'hydrotreater-vgoht',
    match: { vertical: ['hydrotreater', 'vgoht'] },
    Component: EnergyFactorTab,
    config: {
      ...DEFAULT_ENERGY_FACTOR_VARIANT.config,
      apiService: ProductionTabApiService,
    },
  },
  {
    key: 'naphthasplitter',
    match: { vertical: ['naphthasplitter', 'naphtha splitter'] },
    Component: EnergyFactorTab,
    config: {
      ...DEFAULT_ENERGY_FACTOR_VARIANT.config,
      apiService: ProductionTabApiService,
    },
  },
]

