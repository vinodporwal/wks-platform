import AlkylationManualEntry from 'components/aop-phase-two/alkylation/production-norms-basis/ManualEntry'
import CokerManualEntry from 'components/aop-phase-two/coker/production-norms-basis/ManualEntry'
import MeroxManualEntry from 'components/aop-phase-two/merox/production-norms-basis/ManualEntry'
import NaphthaSplitterManualEntry from 'components/aop-phase-two/naphthasplitter/production-norms-basis/ManualEntry'
import PcgManualEntry from 'components/aop-phase-two/pcg/production-norms-basis/ManualEntry'
import PolyesterManualEntry from 'components/aop-phase-two/polyester/production-norms-basis/ReportManualEntry'
import SulphurManualEntry from 'components/aop-phase-two/sulphur/production-norms-basis/ManualEntry'
import VgohtManualEntry from 'components/aop-phase-two/vgoht/production-norms-basis/ManualEntry'

export const DEFAULT_MANUAL_ENTRY_VARIANT = {
  key: 'default',
  Component: VgohtManualEntry,
  config: {},
}

export const MANUAL_ENTRY_VARIANTS = [
  {
    key: 'alkylation',
    match: { vertical: 'alkylation' },
    Component: AlkylationManualEntry,
    config: {},
  },
  {
    key: 'coker',
    match: { vertical: 'coker' },
    Component: CokerManualEntry,
    config: {},
  },
  {
    key: 'merox',
    match: { vertical: 'merox' },
    Component: MeroxManualEntry,
    config: {},
  },
  {
    key: 'naphthasplitter',
    match: { vertical: ['naphthasplitter', 'naphtha splitter'] },
    Component: NaphthaSplitterManualEntry,
    config: {},
  },
  {
    key: 'pcg',
    match: { vertical: 'pcg' },
    Component: PcgManualEntry,
    config: {},
  },
  {
    key: 'polyester',
    match: {
      vertical: ['polyester', 'filament (pfy)', 'staple (psf)'],
    },
    Component: PolyesterManualEntry,
    config: {},
  },
  {
    key: 'sulphur',
    match: { vertical: 'sulphur' },
    Component: SulphurManualEntry,
    config: {},
  },
  {
    key: 'hydrotreater-vgoht',
    match: { vertical: ['hydrotreater', 'vgoht'] },
    Component: VgohtManualEntry,
    config: {},
  },
]

