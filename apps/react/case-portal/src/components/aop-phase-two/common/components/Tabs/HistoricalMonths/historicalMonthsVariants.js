import CokerHistoricalMonths from 'components/aop-phase-two/coker/production-norms-basis/HistoricalMonths'
import VgohtHistoricalMonths from 'components/aop-phase-two/vgoht/production-norms-basis/HistoricalMonths'

export const DEFAULT_HISTORICAL_MONTHS_VARIANT = {
  key: 'default',
  Component: VgohtHistoricalMonths,
  config: {},
}

export const HISTORICAL_MONTHS_VARIANTS = [
  {
    key: 'coker',
    match: { vertical: 'coker' },
    Component: CokerHistoricalMonths,
    config: {},
  },
  {
    key: 'hydrotreater-vgoht',
    match: { vertical: ['hydrotreater', 'vgoht'] },
    Component: VgohtHistoricalMonths,
    config: {},
  },
]

