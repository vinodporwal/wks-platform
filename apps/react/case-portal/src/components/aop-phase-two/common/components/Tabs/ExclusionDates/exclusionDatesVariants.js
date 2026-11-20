import FccManualExclusionDates from 'components/aop-phase-two/fcc/production-norms-basis/ManualExclusionDates'
import RefineryUtilityExclusionDate from 'components/aop-phase-two/refineryUtility/production-norms-basis/ExclusionDate'

export const DEFAULT_EXCLUSION_DATES_VARIANT = {
  key: 'default',
  Component: FccManualExclusionDates,
  config: {},
}

export const EXCLUSION_DATES_VARIANTS = [
  {
    key: 'fcc',
    match: { vertical: 'fcc' },
    Component: FccManualExclusionDates,
    config: {},
  },
  {
    key: 'refinery-utility',
    match: { vertical: ['refinery utility', 'refineryutility'] },
    Component: RefineryUtilityExclusionDate,
    config: {},
  },
]

