import { useSelector } from 'react-redux'

import { ProductionAopAromaticColumns } from 'components/colums/AROMATICS/ProductionAopAromaticColumns'
import { ProductionAopChemicalColumns } from 'components/colums/CHEMICAL/ProductionAopChemicalColumns'
import { ProductionAopCrackerColumns } from 'components/colums/CRAKER/ProductionAopCrackerColumns'
import { ProductionAopElastomerColumns } from 'components/colums/ELASTOMER/ProductionAopElastomerColumns'
import { ProductionAopMegColumns } from 'components/colums/MEG/ProductionAopMegColumns'
import { ProductionAopPeColumns } from 'components/colums/PE/ProductionAopPeColumns'
import { ProductionAopPetColumns } from 'components/colums/PET/ProductionAopPetColumns'
import { ProductionAopPpColumns } from 'components/colums/PP/ProductionAopPpColumns'
import { ProductionAopPtaColumns } from 'components/colums/PTA/ProductionAopPtaColumns'
import { ProductionAopPvcColumns } from 'components/colums/PVC/ProductionAopPvcColumns'
import { ProductionAopVcmColumns } from 'components/colums/VCM/ProductionAopVcmColumns'
import { verticalEnums } from 'enums/verticalEnums'

const colDefsCache = new Map()

const VERTICAL_COLDEFS_MAP = {
  [verticalEnums.MEG]: ProductionAopMegColumns,
  [verticalEnums.PE]: ProductionAopPeColumns,
  [verticalEnums.PP]: ProductionAopPpColumns,
  [verticalEnums.CRACKER]: ProductionAopCrackerColumns,
  [verticalEnums.PTA]: ProductionAopPtaColumns,
  [verticalEnums.ELASTOMER]: ProductionAopElastomerColumns,
  [verticalEnums.AROMATICS]: ProductionAopAromaticColumns,
  [verticalEnums.CHEMICAL]: ProductionAopChemicalColumns,
  [verticalEnums.PET]: ProductionAopPetColumns,
  [verticalEnums.PVC]: ProductionAopPvcColumns,
  [verticalEnums.VCM]: ProductionAopVcmColumns,
}

const getEnhancedColDefs = ({ headerMap }) => {
  const { verticalChange } = useSelector((state) => state.dataGridStore)
  const selectedVertical =
    verticalChange?.selectedVertical?.toLowerCase() || verticalEnums.MEG

  const cacheKey = `${selectedVertical}_${JSON.stringify(headerMap)}`

  if (colDefsCache.has(cacheKey)) {
    return colDefsCache.get(cacheKey)
  }

  const baseCols =
    VERTICAL_COLDEFS_MAP[selectedVertical] || ProductionAopMegColumns
  const cols = [...baseCols]
  if (!cols.some((col) => col.field === 'averageTPH')) {
    cols.push({
      field: 'averageTPH',
      title: 'Total',
    })
  }

  const enhancedColDefs = cols.map((col) => ({
    ...col,
    title: headerMap?.[col.title] ?? col.title,
  }))

  colDefsCache.set(cacheKey, enhancedColDefs)
  return enhancedColDefs
}

export const clearColDefsCache = () => colDefsCache.clear()

export default getEnhancedColDefs
