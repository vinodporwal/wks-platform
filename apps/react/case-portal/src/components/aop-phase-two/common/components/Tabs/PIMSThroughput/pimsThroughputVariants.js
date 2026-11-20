import { ProductionNormsApiService as CokerApiService } from 'components/aop-phase-two/services/coker/productionNormsApiService'
import { ProductionNormsApiService as CrudeApiService } from 'components/aop-phase-two/services/crude/productionNormsApiService'
import { ProductionNormsApiService as MeroxApiService } from 'components/aop-phase-two/services/merox/productionNormsApiService'
import { ProductionNormsApiService as NaphthaSplitterApiService } from 'components/aop-phase-two/services/naphthasplitter/productionNormsApiService'
import { ProductionNormsApiService as RefineryUtilityApiService } from 'components/aop-phase-two/services/refineryUtility/productionNormsApiService'
import { ProductionNormsApiService as SulphurApiService } from 'components/aop-phase-two/services/sulphur/productionNormsApiService'
import { ProductionNormsApiService as VgohtApiService } from 'components/aop-phase-two/services/vgoht/productionNormsApiService'
import PIMSThroughputTab from './PIMSThroughput'

export const DEFAULT_PIMS_THROUGHPUT_VARIANT = {
  key: 'default',
  Component: PIMSThroughputTab,
  config: {
    apiService: CrudeApiService,
    groupBy: ['normParameterType'],
    decimals: 5,
    nameField: 'displayName',
    uomField: 'uom',
    valueField: 'attributeValue',
    requireDatesOnSave: true,
    triggerNormCalculationOnSave: false,
    excelPageName: 'Production_Norms_Basis_PIMS_Throughput',
    getColumns: null,
    getPermissions: null,
    formatRow: null,
    buildSavePayload: null,
    validateRows: null,
  },
}

export const PIMS_THROUGHPUT_VARIANTS = [
  {
    key: 'crude-pcg',
    match: { vertical: ['crude', 'pcg'] },
    Component: PIMSThroughputTab,
    config: {
      ...DEFAULT_PIMS_THROUGHPUT_VARIANT.config,
      apiService: CrudeApiService,
    },
  },
  {
    key: 'alkylation-merox',
    match: { vertical: ['alkylation', 'merox'] },
    Component: PIMSThroughputTab,
    config: {
      ...DEFAULT_PIMS_THROUGHPUT_VARIANT.config,
      apiService: MeroxApiService,
    },
  },
  {
    key: 'sulphur',
    match: { vertical: 'sulphur' },
    Component: PIMSThroughputTab,
    config: {
      ...DEFAULT_PIMS_THROUGHPUT_VARIANT.config,
      apiService: SulphurApiService,
    },
  },
  {
    key: 'naphthasplitter',
    match: { vertical: ['naphthasplitter', 'naphtha splitter'] },
    Component: PIMSThroughputTab,
    config: {
      ...DEFAULT_PIMS_THROUGHPUT_VARIANT.config,
      apiService: NaphthaSplitterApiService,
      groupBy: ['normType'],
      nameField: 'productName',
      uomField: 'UOM',
      valueField: 'value',
    },
  },
  {
    key: 'refinery-utility',
    match: { vertical: ['refinery utility', 'refineryutility'] },
    Component: PIMSThroughputTab,
    config: {
      ...DEFAULT_PIMS_THROUGHPUT_VARIANT.config,
      apiService: RefineryUtilityApiService,
      groupBy: ['normType'],
      nameField: 'productName',
      uomField: 'UOM',
      valueField: 'value',
    },
  },
  {
    key: 'hydrotreater-vgoht',
    match: { vertical: ['hydrotreater', 'vgoht'] },
    Component: PIMSThroughputTab,
    config: {
      ...DEFAULT_PIMS_THROUGHPUT_VARIANT.config,
      apiService: VgohtApiService,
      groupBy: ['TypeDisplayName'],
      nameField: 'productName',
      uomField: 'UOM',
      valueField: 'value',
      formatRow: (item, index) => ({
        ...item,
        remarks: item.remarks || '',
        value: item.apr,
        id: item?.id || index + 1,
      }),
    },
  },
  {
    key: 'fcc',
    match: { vertical: 'fcc' },
    Component: PIMSThroughputTab,
    config: {
      ...DEFAULT_PIMS_THROUGHPUT_VARIANT.config,
      apiService: VgohtApiService,
      groupBy: ['normType'],
      nameField: 'productName',
      uomField: 'UOM',
      valueField: 'value',
    },
  },
  {
    key: 'coker',
    match: { vertical: 'coker' },
    Component: PIMSThroughputTab,
    config: {
      ...DEFAULT_PIMS_THROUGHPUT_VARIANT.config,
      apiService: CokerApiService,
      groupBy: ['normType'],
      nameField: 'productName',
      uomField: 'UOM',
      valueField: 'value',
      triggerNormCalculationOnSave: true,
    },
  },
]

