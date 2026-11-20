import PolyesterProductionRange from 'components/aop-phase-two/polyester/production-norms-basis/ProductionRange'

export const DEFAULT_PRODUCTION_RANGE_VARIANT = {
  key: 'default',
  Component: PolyesterProductionRange,
  config: {},
}

export const PRODUCTION_RANGE_VARIANTS = [
  {
    key: 'polyester',
    match: {
      vertical: ['polyester', 'filament (pfy)', 'staple (psf)'],
    },
    Component: PolyesterProductionRange,
    config: {},
  },
]

