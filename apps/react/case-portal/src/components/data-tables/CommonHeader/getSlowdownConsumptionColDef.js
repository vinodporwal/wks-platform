import { SlowdownConsumptionAromaticColumns } from 'components/colums/AROMATICS/SlowdownConsumptionAromaticColumns'
import { SlowdownConsumptionChemicalColumns } from 'components/colums/CHEMICAL/SlowdownConsumptionChemicalColumns'
import { SlowdownConsumptionElastomerColumns } from 'components/colums/ELASTOMER/SlowdownConsumptionElastomerColumns'
import { SlowdownConsumptionMegColumns } from 'components/colums/MEG/SlowdownConsumptionMegColumns'
import { SlowdownConsumptionPeColumns } from 'components/colums/PE/SlowdownConsumptionPeColumns'
import { SlowdownConsumptionPetColumns } from 'components/colums/PET/SlowdownConsumptionPetColumns'
import { SlowdownConsumptionPpColumns } from 'components/colums/PP/SlowdownConsumptionPpColumns'
import { SlowdownConsumptionPtaColumns } from 'components/colums/PTA/SlowdownConsumptionPtaColumns'
import { SlowdownConsumptionPvcColumns } from 'components/colums/PVC/SlowdownConsumptionPvcColumns'
import { SlowdownConsumptionVcmColumns } from 'components/colums/VCM/SlowdownConsumptionVcmColumns'
import { verticalEnums } from 'enums/verticalEnums'
import { useSelector } from 'react-redux'

const colDefsCache = new Map()

const VERTICAL_COLDEFS_MAP = {
  [verticalEnums.PE]: SlowdownConsumptionPeColumns,
  [verticalEnums.PP]: SlowdownConsumptionPpColumns,
  [verticalEnums.PTA]: SlowdownConsumptionPtaColumns,
  [verticalEnums.ELASTOMER]: SlowdownConsumptionElastomerColumns,
  [verticalEnums.MEG]: SlowdownConsumptionMegColumns,
  [verticalEnums.AROMATICS]: SlowdownConsumptionAromaticColumns,
  [verticalEnums.CHEMICAL]: SlowdownConsumptionChemicalColumns,
  [verticalEnums.PET]: SlowdownConsumptionPetColumns,
  [verticalEnums.PVC]: SlowdownConsumptionPvcColumns,
  [verticalEnums.VCM]: SlowdownConsumptionVcmColumns,
}

const getSlowdownConsumptionColDef = ({ headerMap, slowdownMonths }) => {
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

export default getSlowdownConsumptionColDef
