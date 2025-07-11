import { SlowdownNormsAromaticColumns } from 'components/colums/AROMATICS/SlowdownNormsAromaticColumns'
import { SlowdownNormsChemicalColumns } from 'components/colums/CHEMICAL/SlowdownNormsChemicalColumns'
import { SlowdownNormsElastomerColumns } from 'components/colums/ELASTOMER/SlowdownNormsElastomerColumns'
import { SlowdownNormsMegColumns } from 'components/colums/MEG/SlowdownNormsMegColumns'
import { SlowdownNormsPeColumns } from 'components/colums/PE/SlowdownNormsPeColumns'
import { SlowdownNormsPetColumns } from 'components/colums/PET/SlowdownNormsPetColumns'
import { SlowdownNormsPpColumns } from 'components/colums/PP/SlowdownNormsPpColumns'
import { SlowdownNormsPtaColumns } from 'components/colums/PTA/SlowdownNormsPtaColumns'
import { SlowdownNormsPvcColumns } from 'components/colums/PVC/SlowdownNormsPvcColumns'
import { SlowdownNormsVcmColumns } from 'components/colums/VCM/SlowdownNormsVcmColumns'
import { verticalEnums } from 'enums/verticalEnums'
import { useSelector } from 'react-redux'

const colDefsCache = new Map()

const VERTICAL_COLDEFS_MAP = {
  [verticalEnums.PE]: SlowdownNormsPeColumns,
  [verticalEnums.PP]: SlowdownNormsPpColumns,
  [verticalEnums.PTA]: SlowdownNormsPtaColumns,
  [verticalEnums.ELASTOMER]: SlowdownNormsElastomerColumns,
  [verticalEnums.MEG]: SlowdownNormsMegColumns,
  [verticalEnums.AROMATICS]: SlowdownNormsAromaticColumns,
  [verticalEnums.CHEMICAL]: SlowdownNormsChemicalColumns,
  [verticalEnums.PET]: SlowdownNormsPetColumns,
  [verticalEnums.PVC]: SlowdownNormsPvcColumns,
  [verticalEnums.VCM]: SlowdownNormsVcmColumns,
}

const getSlowdownNormsColDef = ({ headerMap, slowdownMonths }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const vertName = dataGridStore.verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || verticalEnums.MEG

  const cacheKey = `${lowerVertName}_${JSON.stringify(headerMap)}_${slowdownMonths.join(',')}`

  if (colDefsCache.has(cacheKey)) {
    return colDefsCache.get(cacheKey)
  }

  const cols = VERTICAL_COLDEFS_MAP[lowerVertName] || []

  const enhancedColDefs = cols.map((col) => {
    if (col.monthNumber) {
      const monthNum = col.monthNumber
      return {
        ...col,
        headerName: headerMap?.[monthNum] || col.field,
        editable: slowdownMonths.includes(monthNum),
        isDisabled: !slowdownMonths.includes(monthNum),
      }
    }

    return col
  })

  colDefsCache.set(cacheKey, enhancedColDefs)
  return enhancedColDefs
}

export const clearColDefsCache = () => colDefsCache.clear()

export default getSlowdownNormsColDef
