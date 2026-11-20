import CrudeReportManualEntry from 'components/aop-phase-two/crude/production-norms-basis/ReportManualEntry'
import FccReportManualEntry from 'components/aop-phase-two/fcc/production-norms-basis/ReportManualEntry'
import PcgManualEntry from 'components/aop-phase-two/pcg/production-norms-basis/ManualEntry'
import PolyesterReportManualEntry from 'components/aop-phase-two/polyester/production-norms-basis/ReportManualEntry'
import SulphurManualEntry from 'components/aop-phase-two/sulphur/production-norms-basis/ManualEntry'
import VgohtReportManualEntry from 'components/aop-phase-two/vgoht/production-norms-basis/ReportManualEntry'

export const DEFAULT_REPORT_MANUAL_ENTRY_VARIANT = {
  key: 'default',
  Component: CrudeReportManualEntry,
  config: {},
}

export const REPORT_MANUAL_ENTRY_VARIANTS = [
  {
    key: 'crude',
    match: { vertical: 'crude' },
    Component: CrudeReportManualEntry,
    config: {},
  },
  {
    key: 'fcc',
    match: { vertical: 'fcc' },
    Component: FccReportManualEntry,
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
    Component: PolyesterReportManualEntry,
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
    Component: VgohtReportManualEntry,
    config: {},
  },
]

