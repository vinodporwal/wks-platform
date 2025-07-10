import { BusinessDemandElastomerColumns } from 'components/colums/ELASTOMER/BusinessDemandElastomerColumns'
import { BusinessDemandMegColumns } from 'components/colums/MEG/BusinessDemandMegColumns'
import { BusinessDemandPeColumns } from 'components/colums/PE/BusinessDemandPeColumns'
import { BusinessDemandPpColumns } from 'components/colums/PP/BusinessDemandPpColumns'
import { BusinessDemandPtaColumns } from 'components/colums/PTA/BusinessDemandPtaColumns'
import { verticalEnums } from 'enums/verticalEnums'
import { useSelector } from 'react-redux'

const colDefsCache = new Map()

const VERTICAL_COLDEFS_MAP = {
  [verticalEnums.PE]: BusinessDemandPeColumns,
  [verticalEnums.PP]: BusinessDemandPpColumns,
  [verticalEnums.PTA]: BusinessDemandPtaColumns,
  [verticalEnums.ELASTOMER]: BusinessDemandElastomerColumns,
  [verticalEnums.MEG]: BusinessDemandMegColumns,
}

const kendoBusinessDemColDef = ({ headerMap }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const vertName = dataGridStore.verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || verticalEnums.MEG

  const cacheKey = `${lowerVertName}_${headerMap ? JSON.stringify(headerMap) : 'no_map'}`

  if (colDefsCache.has(cacheKey)) {
    return colDefsCache.get(cacheKey)
  }
  const cols = VERTICAL_COLDEFS_MAP[lowerVertName] || BusinessDemandMegColumns

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

export default kendoBusinessDemColDef
