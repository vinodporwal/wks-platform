import AntiFoam from 'components/aop-phase-two/refineryUtility/production-norms-basis/AntiFoam'
import AntiScalant from 'components/aop-phase-two/refineryUtility/production-norms-basis/AntiScalant'
import CatChemConsumption from 'components/aop-phase-two/refineryUtility/production-norms-basis/CatChemConsumption'
import CommodityConsumption from 'components/aop-phase-two/refineryUtility/production-norms-basis/CommodityConsumption'
import CommoditySelection from 'components/aop-phase-two/refineryUtility/production-norms-basis/CommoditySelection'
import OwnerSelection from 'components/aop-phase-two/refineryUtility/production-norms-basis/OwnerSelection'
import ProductionDemand from 'components/aop-phase-two/refineryUtility/production-norms-basis/ProductionDemand'
import ResignRequired from 'components/aop-phase-two/refineryUtility/production-norms-basis/ResignRequired'
import ResinConsumption from 'components/aop-phase-two/refineryUtility/production-norms-basis/ResinConsumption'
import TreatmentVendor from 'components/aop-phase-two/refineryUtility/production-norms-basis/TreatmentVendor'
import UtilityConsumption from 'components/aop-phase-two/refineryUtility/production-norms-basis/UtilityConsumption'
import VendorConsumption from 'components/aop-phase-two/refineryUtility/production-norms-basis/Vendorconsumption'

/**
 * Variant definitions for each Refinery Utility production-norms-basis tab.
 * Each entry has `{ defaultVariant, variants }` so plant/site overrides can be
 * added cleanly without touching existing files under `refineryUtility/`.
 */
export const REFINERY_UTILITY_TAB_CONFIGS = {
  ProductionDemand: {
    tabName: 'Production Demand',
    defaultVariant: { key: 'default', Component: ProductionDemand, config: {} },
    variants: [],
  },
  UtilityConsumption: {
    tabName: 'Utility Consumption',
    defaultVariant: {
      key: 'default',
      Component: UtilityConsumption,
      config: {},
    },
    variants: [],
  },
  CatChemConsumption: {
    tabName: 'CAT CHEM Consumption',
    defaultVariant: {
      key: 'default',
      Component: CatChemConsumption,
      config: {},
    },
    variants: [],
  },
  TreatmentVendor: {
    tabName: 'Treatment Vendor',
    defaultVariant: { key: 'default', Component: TreatmentVendor, config: {} },
    variants: [],
  },
  VendorConsumption: {
    tabName: 'Vendor Chemical Consumption',
    defaultVariant: {
      key: 'default',
      Component: VendorConsumption,
      config: {},
    },
    variants: [],
  },
  CommoditySelection: {
    tabName: 'Commodity Chemicals',
    defaultVariant: {
      key: 'default',
      Component: CommoditySelection,
      config: {},
    },
    variants: [],
  },
  CommodityConsumption: {
    tabName: 'Commodity Chemicals Consumption',
    defaultVariant: {
      key: 'default',
      Component: CommodityConsumption,
      config: {},
    },
    variants: [],
  },
  OwnerSelection: {
    tabName: 'Plant Owner',
    defaultVariant: { key: 'default', Component: OwnerSelection, config: {} },
    variants: [],
  },
  ResinConsumption: {
    tabName: 'Resin Consumption',
    defaultVariant: { key: 'default', Component: ResinConsumption, config: {} },
    variants: [],
  },
  AntiScalant: {
    tabName: 'AntiScalant',
    defaultVariant: { key: 'default', Component: AntiScalant, config: {} },
    variants: [],
  },
  AntiFoam: {
    tabName: 'AntiFoam',
    defaultVariant: { key: 'default', Component: AntiFoam, config: {} },
    variants: [],
  },
  ResignRequired: {
    tabName: 'Resign Required',
    defaultVariant: { key: 'default', Component: ResignRequired, config: {} },
    variants: [],
  },
}

