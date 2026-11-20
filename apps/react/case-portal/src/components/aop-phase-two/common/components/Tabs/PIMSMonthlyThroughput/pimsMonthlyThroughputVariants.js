import { ProductionNormsApiService } from 'components/aop-phase-two/services/crude/productionNormsApiService'
import PIMSMonthlyThroughputTab from './PIMSMonthlyThroughput'

export const DEFAULT_PIMS_MONTHLY_THROUGHPUT_VARIANT = {
  key: 'default',
  Component: PIMSMonthlyThroughputTab,
  config: {
    apiService: ProductionNormsApiService,
    groupBy: ['normParameterType'],
    decimals: 5,
    triggerNormCalculationOnSave: false,
    getColumns: null,
    getPermissions: null,
    formatRow: null,
    buildSavePayload: null,
    validateRows: null,
  },
}

/**
 * Declarative variant registry for PIMS Monthly Throughput.
 * Currently all 10 verticals share the default configuration; add overrides here
 * when a specific (vertical, site, plant) diverges.
 */
export const PIMS_MONTHLY_THROUGHPUT_VARIANTS = []

