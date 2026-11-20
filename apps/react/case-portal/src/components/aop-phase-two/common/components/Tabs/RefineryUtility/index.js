import React, { useMemo } from 'react'
import { Box } from '@mui/material'
import usePlantContext from '../../../hooks/usePlantContext'
import { resolveByPlantContext } from '../../../utilities/plantContextResolver'
import { REFINERY_UTILITY_TAB_CONFIGS } from './refineryUtilityTabsVariants'

const createRefineryUtilityTab = (tabKey) => {
  const entry = REFINERY_UTILITY_TAB_CONFIGS[tabKey]

  const RefineryUtilityTabWrapper = (props) => {
    const context = usePlantContext()

    const resolvedVariant = useMemo(
      () =>
        resolveByPlantContext(
          entry.variants,
          context,
          entry.defaultVariant,
        ),
      [context],
    )

    const VariantComponent =
      resolvedVariant?.Component || entry.defaultVariant.Component
    const variantConfig =
      resolvedVariant?.config || entry.defaultVariant.config
    const variantKey = resolvedVariant?.key || entry.defaultVariant.key

    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <VariantComponent
          key={`${tabKey}-${variantKey}-${context.contextKey}`}
          {...props}
          plantContext={context}
          variantConfig={variantConfig}
        />
      </Box>
    )
  }

  RefineryUtilityTabWrapper.displayName = `CommonRefineryUtility_${tabKey}`
  return RefineryUtilityTabWrapper
}

export const ProductionDemand = createRefineryUtilityTab('ProductionDemand')
export const UtilityConsumption = createRefineryUtilityTab('UtilityConsumption')
export const CatChemConsumption = createRefineryUtilityTab('CatChemConsumption')
export const TreatmentVendor = createRefineryUtilityTab('TreatmentVendor')
export const VendorConsumption = createRefineryUtilityTab('VendorConsumption')
export const CommoditySelection = createRefineryUtilityTab('CommoditySelection')
export const CommodityConsumption = createRefineryUtilityTab(
  'CommodityConsumption',
)
export const OwnerSelection = createRefineryUtilityTab('OwnerSelection')
export const ResinConsumption = createRefineryUtilityTab('ResinConsumption')
export const AntiScalant = createRefineryUtilityTab('AntiScalant')
export const AntiFoam = createRefineryUtilityTab('AntiFoam')
export const ResignRequired = createRefineryUtilityTab('ResignRequired')

export const REFINERY_UTILITY_TABS_REGISTRY = {
  'Production Demand': ProductionDemand,
  'Utility Consumption': UtilityConsumption,
  'CAT CHEM Consumption': CatChemConsumption,
  'Treatment Vendor': TreatmentVendor,
  'Vendor Chemical Consumption': VendorConsumption,
  'Commodity Chemicals': CommoditySelection,
  'Commodity Chemicals Consumption': CommodityConsumption,
  'Plant Owner': OwnerSelection,
  'Resin Consumption': ResinConsumption,
  AntiScalant,
  AntiFoam,
  'Resign Required': ResignRequired,
}

export default REFINERY_UTILITY_TABS_REGISTRY

