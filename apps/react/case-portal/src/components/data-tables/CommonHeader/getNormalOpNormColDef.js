import { NormalOpNormAromaticColumns } from 'components/colums/AROMATICS/NormalOpNormAromaticColumns'
import { NormalOpNormChemicalColumns } from 'components/colums/CHEMICAL/NormalOpNormChemicalColumns'
import { NormalOpNormElastomerColumns } from 'components/colums/ELASTOMER/NormalOpNormElastomerColumns'
import { NormalOpNormMegColumns } from 'components/colums/MEG/NormalOpNormMegColumns'
import { NormalOpNormPeColumns } from 'components/colums/PE/NormalOpNormPeColumns'
import { NormalOpNormPetColumns } from 'components/colums/PET/NormalOpNormPetColumns'
import { NormalOpNormPpColumns } from 'components/colums/PP/NormalOpNormPpColumns'
import { NormalOpNormPtaColumns } from 'components/colums/PTA/NormalOpNormPtaColumns'
import { NormalOpNormPvcColumns } from 'components/colums/PVC/NormalOpNormPvcColumns'
import { NormalOpNormVcmColumns } from 'components/colums/VCM/NormalOpNormVcmColumns'
import { verticalEnums } from 'enums/verticalEnums'
import { useSelector } from 'react-redux'

const colDefsCache = new Map()

const VERTICAL_COLDEFS_MAP = {
  [verticalEnums.PE]: NormalOpNormPeColumns,
  [verticalEnums.PP]: NormalOpNormPpColumns,
  [verticalEnums.PTA]: NormalOpNormPtaColumns,
  [verticalEnums.ELASTOMER]: NormalOpNormElastomerColumns,
  [verticalEnums.MEG]: NormalOpNormMegColumns,
  [verticalEnums.AROMATICS]: NormalOpNormAromaticColumns,
  [verticalEnums.CHEMICAL]: NormalOpNormChemicalColumns,
  [verticalEnums.PET]: NormalOpNormPetColumns,
  [verticalEnums.PVC]: NormalOpNormPvcColumns,
  [verticalEnums.VCM]: NormalOpNormVcmColumns,
}

const getNormalOpNormColDef = ({ headerMap }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const vertName = dataGridStore.verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || verticalEnums.MEG

  const cacheKey = `${lowerVertName}_${headerMap ? JSON.stringify(headerMap) : 'no_map'}`

  if (colDefsCache.has(cacheKey)) {
    return colDefsCache.get(cacheKey)
  }
  const cols = VERTICAL_COLDEFS_MAP[lowerVertName] || NormalOpNormMegColumns

  const enhancedColDefs = cols.map((col) => {
    if (!headerMap || headerMap[col.title] === undefined) {
      return col
    }

    return {
      ...col,
      title: headerMap[col.title],
      align: 'right',
      format: '{0:#.###}',
    }
  })

  colDefsCache.set(cacheKey, enhancedColDefs)
  return enhancedColDefs
}

export const clearColDefsCache = () => colDefsCache.clear()

export default getNormalOpNormColDef
