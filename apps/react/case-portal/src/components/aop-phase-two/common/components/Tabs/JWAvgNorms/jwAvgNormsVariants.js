import { JWAvgNormsApiService } from 'components/aop-phase-two/services/common/jwAvgNormsApiService'
import JWAvgNormsTab from './JWAvgNorms'

/**
 * Default variant configuration used when no vertical/site/plant rule matches.
 */
export const DEFAULT_JW_AVG_NORMS_VARIANT = {
  key: 'default',
  Component: JWAvgNormsTab,
  config: {
    apiService: JWAvgNormsApiService,
    groupBy: null,
    getColumns: null, // Optional: (baseColumns, context) => columns
    getPermissions: null, // Optional: (basePermissions, context) => permissions
    formatRow: null, // Optional: (item, index, context) => formattedRow
    buildSavePayload: null, // Optional: (rows, context) => payload
    validateRows: null, // Optional: (data, originalRows, context) => errorMsg | null
    excelPageName: 'Job_Work_Avg_Norms',
  },
}

/**
 * Declarative variant registry for Job Work Avg Norms.
 * Rules are resolved by highest specificity:
 *   vertical + site + plant > vertical + site > vertical > default
 *
 * Add config-only overrides (reusing JWAvgNormsTab) or custom Component entries
 * (from `./variants/...`) whenever a specific vertical/site/plant diverges.
 */
export const JW_AVG_NORMS_VARIANTS = [
  // Example 1 — Config-only override:
  // {
  //   key: 'pcg-psa',
  //   match: { vertical: 'pcg', site: ['dta', 'sez'], plant: 'psa' },
  //   Component: JWAvgNormsTab,
  //   config: {
  //     ...DEFAULT_JW_AVG_NORMS_VARIANT.config,
  //     groupBy: ['groupDisplayName'],
  //     getPermissions: (base) => ({ ...base, showImport: false }),
  //   },
  // },
  //
  // Example 2 — Custom component when UI structure differs completely:
  // {
  //   key: 'custom-plant',
  //   match: { vertical: '...', site: '...', plant: '...' },
  //   Component: CustomJWAvgNormsVariant,
  //   config: {},
  // },
]

