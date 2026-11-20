import VgohtShutdownCatChem from 'components/aop-phase-two/vgoht/production-norms-basis/ShutdownCatChem'

export const DEFAULT_SHUTDOWN_CAT_CHEM_VARIANT = {
  key: 'default',
  Component: VgohtShutdownCatChem,
  config: {},
}

export const SHUTDOWN_CAT_CHEM_VARIANTS = [
  {
    key: 'hydrotreater-vgoht',
    match: { vertical: ['hydrotreater', 'vgoht'] },
    Component: VgohtShutdownCatChem,
    config: {},
  },
]

