import CokerManualEntryFactors from 'components/aop-phase-two/coker/production-norms-basis/ManualEntryFactors'

export const DEFAULT_MANUAL_ENTRY_FACTORS_VARIANT = {
  key: 'default',
  Component: CokerManualEntryFactors,
  config: {},
}

export const MANUAL_ENTRY_FACTORS_VARIANTS = [
  {
    key: 'coker',
    match: { vertical: 'coker' },
    Component: CokerManualEntryFactors,
    config: {},
  },
]

