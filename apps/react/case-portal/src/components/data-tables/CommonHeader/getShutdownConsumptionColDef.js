import { ShutdownConsumptionAromaticColumns } from 'components/colums/AROMATICS/ShutdownConsumptionAromaticColumns'
import { ShutdownConsumptionChemicalColumns } from 'components/colums/CHEMICAL/ShutdownConsumptionChemicalColumns'
import { ShutdownConsumptionElastomerColumns } from 'components/colums/ELASTOMER/ShutdownConsumptionElastomerColumns'
import { ShutdownConsumptionMegColumns } from 'components/colums/MEG/ShutdownConsumptionMegColumns'
import { ShutdownConsumptionPeColumns } from 'components/colums/PE/ShutdownConsumptionPeColumns'
import { ShutdownConsumptionPetColumns } from 'components/colums/PET/ShutdownConsumptionPetColumns'
import { ShutdownConsumptionPpColumns } from 'components/colums/PP/ShutdownConsumptionPpColumns'
import { ShutdownConsumptionPtaColumns } from 'components/colums/PTA/ShutdownConsumptionPtaColumns'
import { ShutdownConsumptionPvcColumns } from 'components/colums/PVC/ShutdownConsumptionPvcColumns'
import { ShutdownConsumptionVcmColumns } from 'components/colums/VCM/ShutdownConsumptionVcmColumns'
import { verticalEnums } from 'enums/verticalEnums'
import { useSelector } from 'react-redux'

const colDefsCache = new Map()

const VERTICAL_COLDEFS_MAP = {
  [verticalEnums.PE]: ShutdownConsumptionPeColumns,
  [verticalEnums.PP]: ShutdownConsumptionPpColumns,
  [verticalEnums.PTA]: ShutdownConsumptionPtaColumns,
  [verticalEnums.ELASTOMER]: ShutdownConsumptionElastomerColumns,
  [verticalEnums.MEG]: ShutdownConsumptionMegColumns,
  [verticalEnums.AROMATICS]: ShutdownConsumptionAromaticColumns,
  [verticalEnums.CHEMICAL]: ShutdownConsumptionChemicalColumns,
  [verticalEnums.PET]: ShutdownConsumptionPetColumns,
  [verticalEnums.PVC]: ShutdownConsumptionPvcColumns,
  [verticalEnums.VCM]: ShutdownConsumptionVcmColumns,
}

const getShutdownConsumptionColDef = ({ headerMap, shutdownMonths }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const vertName = dataGridStore.verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || verticalEnums.MEG

  const cacheKey = `${lowerVertName}_${JSON.stringify(headerMap)}_${shutdownMonths.join(',')}`

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
        editable: shutdownMonths.includes(monthNum),
        isDisabled: !shutdownMonths.includes(monthNum),
      }
    }

    return col
  })

  colDefsCache.set(cacheKey, enhancedColDefs)
  return enhancedColDefs
}

export const clearColDefsCache = () => colDefsCache.clear()

export default getShutdownConsumptionColDef
