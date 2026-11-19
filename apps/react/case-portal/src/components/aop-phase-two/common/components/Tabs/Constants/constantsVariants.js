import { ProductionNormsApiService as AlkylationApiService } from 'components/aop-phase-two/services/alkylation/productionNormsApiService'
import { ProductionNormsApiService as CokerApiService } from 'components/aop-phase-two/services/coker/productionNormsApiService'
import { ProductionNormsApiService as CrudeApiService } from 'components/aop-phase-two/services/crude/productionNormsApiService'
import { ProductionNormsApiService as FccApiService } from 'components/aop-phase-two/services/fcc/productionNormsApiService'
import { ProductionNormsApiService as MeroxApiService } from 'components/aop-phase-two/services/merox/productionNormsApiService'
import { ProductionNormsApiService as NaphthaSplitterApiService } from 'components/aop-phase-two/services/naphthasplitter/productionNormsApiService'
import { ProductionNormsApiService as PcgApiService } from 'components/aop-phase-two/services/pcg/productionNormsApiService'
import { ProductionNormsApiService as RefineryUtilityApiService } from 'components/aop-phase-two/services/refineryUtility/productionNormsApiService'
import { ProductionNormsApiService as SulphurApiService } from 'components/aop-phase-two/services/sulphur/productionNormsApiService'
import { ProductionNormsApiService as VgohtApiService } from 'components/aop-phase-two/services/vgoht/productionNormsApiService'
import ConstantsTab from './Constants'
import { SulphurConstants } from './columns'

export const DEFAULT_CONSTANTS_VARIANT = {
  key: 'default',
  Component: ConstantsTab,
  config: {
    apiService: CrudeApiService,
    groupBy: ['type'],
    decimals: 5,
    titleName: 'Constants',
    excelPageName: 'Production_Norms_Constants',
    requireDatesOnSave: false,
    requireDatesOnImport: false,
    validationLabelField: 'productName',
    triggerNormCalculationOnSave: false,
    getColumns: null,
    getPermissions: null,
    formatRow: null,
    buildSavePayload: null,
    validateRows: null,
  },
}

export const CONSTANTS_VARIANTS = [
  {
    key: 'pcg-gasifier',
    match: { vertical: 'pcg', site: ['dta', 'sez'], plant: 'gasifier' },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: PcgApiService,
      decimals: 10,
      titleName: 'Manual Entry',
      excelPageName: 'Production_Norms_Manual_Entry',
    },
  },
  {
    key: 'pcg',
    match: { vertical: 'pcg' },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: PcgApiService,
      excelPageName: 'Production_Norms_Constant',
    },
  },
  {
    key: 'alkylation',
    match: { vertical: 'alkylation' },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: AlkylationApiService,
      excelPageName: 'Production Norms Basis - Constants',
    },
  },
  {
    key: 'coker',
    match: { vertical: 'coker' },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: CokerApiService,
    },
  },
  {
    key: 'crude',
    match: { vertical: 'crude' },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: CrudeApiService,
      decimals: 2,
      requireDatesOnSave: false,
      requireDatesOnImport: false,
      validationLabelField: 'particulars',
    },
  },
  {
    key: 'fcc',
    match: { vertical: 'fcc' },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: FccApiService,
    },
  },
  {
    key: 'merox',
    match: { vertical: 'merox' },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: MeroxApiService,
    },
  },
  {
    key: 'naphthasplitter',
    match: { vertical: ['naphthasplitter', 'naphtha splitter'] },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: NaphthaSplitterApiService,
    },
  },
  {
    key: 'refinery-utility',
    match: { vertical: ['refinery utility', 'refineryutility'] },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: RefineryUtilityApiService,
      groupBy: ['normTypeName'],
    },
  },
  {
    key: 'sulphur',
    match: { vertical: 'sulphur' },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: SulphurApiService,
      getColumns: SulphurConstants,
    },
  },
  {
    key: 'hydrotreater-vgoht',
    match: { vertical: ['hydrotreater', 'vgoht'] },
    Component: ConstantsTab,
    config: {
      ...DEFAULT_CONSTANTS_VARIANT.config,
      apiService: VgohtApiService,
      groupBy: ['normType'],
    },
  },
]

