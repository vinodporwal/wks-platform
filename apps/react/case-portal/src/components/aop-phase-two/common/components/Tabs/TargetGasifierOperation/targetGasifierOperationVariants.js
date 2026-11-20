import PcgGasifierOperation from 'components/aop-phase-two/pcg/production-norms-basis/gasifierOperation'
import SulphurGasifierOperation from 'components/aop-phase-two/sulphur/production-norms-basis/gasifierOperation'

export const DEFAULT_TARGET_GASIFIER_OPERATION_VARIANT = {
  key: 'default',
  Component: PcgGasifierOperation,
  config: {},
}

export const TARGET_GASIFIER_OPERATION_VARIANTS = [
  {
    key: 'pcg',
    match: { vertical: 'pcg' },
    Component: PcgGasifierOperation,
    config: {},
  },
  {
    key: 'sulphur',
    match: { vertical: 'sulphur' },
    Component: SulphurGasifierOperation,
    config: {},
  },
]

