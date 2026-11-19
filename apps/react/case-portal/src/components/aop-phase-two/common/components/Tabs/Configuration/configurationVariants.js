import AlkylationConfiguration from 'components/aop-phase-two/alkylation/production-norms-basis/Configuration'
import CokerConfiguration from 'components/aop-phase-two/coker/production-norms-basis/Configuration'
import CrudeConfiguration from 'components/aop-phase-two/crude/production-norms-basis/Configuration'
import FccConfiguration from 'components/aop-phase-two/fcc/production-norms-basis/Configuration'
import MeroxConfiguration from 'components/aop-phase-two/merox/production-norms-basis/Configuration'
import NaphthaSplitterConfiguration from 'components/aop-phase-two/naphthasplitter/production-norms-basis/Configuration'
import PcgConfigurationRange from 'components/aop-phase-two/pcg/production-norms-basis/Configuration/ConfigurationRange'
import PcgConfigurationRangePsa from 'components/aop-phase-two/pcg/production-norms-basis/Configuration/ConfigurationRangePsa'
import PolyesterConfiguration from 'components/aop-phase-two/polyester/production-norms-basis/Configuration'
import RefineryUtilityConfiguration from 'components/aop-phase-two/refineryUtility/production-norms-basis/Configuration'
import SulphurConfiguration from 'components/aop-phase-two/sulphur/production-norms-basis/Configuration'
import VgohtConfiguration from 'components/aop-phase-two/vgoht/production-norms-basis/Configuration'

export const DEFAULT_CONFIGURATION_VARIANT = {
  key: 'default',
  Component: CrudeConfiguration,
  config: {},
}

export const CONFIGURATION_VARIANTS = [
  {
    key: 'pcg-psa',
    match: { vertical: 'pcg', site: ['dta', 'sez'], plant: 'psa' },
    Component: PcgConfigurationRangePsa,
    config: {},
  },
  {
    key: 'pcg',
    match: { vertical: 'pcg' },
    Component: PcgConfigurationRange,
    config: {},
  },
  {
    key: 'alkylation',
    match: { vertical: 'alkylation' },
    Component: AlkylationConfiguration,
    config: {},
  },
  {
    key: 'coker',
    match: { vertical: 'coker' },
    Component: CokerConfiguration,
    config: {},
  },
  {
    key: 'crude',
    match: { vertical: 'crude' },
    Component: CrudeConfiguration,
    config: {},
  },
  {
    key: 'fcc',
    match: { vertical: 'fcc' },
    Component: FccConfiguration,
    config: {},
  },
  {
    key: 'merox',
    match: { vertical: 'merox' },
    Component: MeroxConfiguration,
    config: {},
  },
  {
    key: 'naphthasplitter',
    match: { vertical: ['naphthasplitter', 'naphtha splitter'] },
    Component: NaphthaSplitterConfiguration,
    config: {},
  },
  {
    key: 'polyester',
    match: {
      vertical: ['polyester', 'filament (pfy)', 'staple (psf)'],
    },
    Component: PolyesterConfiguration,
    config: {},
  },
  {
    key: 'refinery-utility',
    match: { vertical: ['refinery utility', 'refineryutility'] },
    Component: RefineryUtilityConfiguration,
    config: {},
  },
  {
    key: 'sulphur',
    match: { vertical: 'sulphur' },
    Component: SulphurConfiguration,
    config: {},
  },
  {
    key: 'hydrotreater-vgoht',
    match: { vertical: ['hydrotreater', 'vgoht'] },
    Component: VgohtConfiguration,
    config: {},
  },
]

