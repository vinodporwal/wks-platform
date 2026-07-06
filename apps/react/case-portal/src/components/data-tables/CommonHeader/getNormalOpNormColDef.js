import {
  NormalOpNormElastomerColumns,
  NormalOpNormElastomerJmdColumns,
} from 'components/colums/ElastomerColums'
import { NormalOpNormMegColumns } from 'components/colums/MegColums'
import { NormalOpNormVcmColumns } from 'components/colums/VcmColumns'
import { CrackerColums } from 'components/colums/CrackerColums'
import { NormalOpNormPeColumns } from 'components/colums/PeColums'
import { NormalOpNormPpColumns } from 'components/colums/PpColums'
import { NormalOpNormPtaColumns } from 'components/colums/PtaColums'
import { NormalOpNormVcmDmdColumns } from 'components/colums/VcmDmdColumns'
import { verticalEnums } from 'enums/verticalEnums'
import { useSelector } from 'react-redux'
import { NormalOpNormChemicalColumns } from 'components/colums/ChemicalColums'
import { shouldLockColumn } from 'utils/columnLockUtils'

const colDefsCache = new Map()

const VERTICAL_COLDEFS_MAP = {
  [verticalEnums.PE]: NormalOpNormPeColumns,
  [verticalEnums.PP]: NormalOpNormPpColumns,
  [verticalEnums.PTA]: NormalOpNormPtaColumns,
  [verticalEnums.ELASTOMER]: NormalOpNormElastomerColumns,
  [verticalEnums.MEG]: NormalOpNormMegColumns,
  [verticalEnums.CRACKER]: CrackerColums,
  [verticalEnums.VCM]: NormalOpNormVcmColumns,
  [verticalEnums.CHEMICAL]: NormalOpNormChemicalColumns,
}

const getNormalOpNormColDef = ({
  headerMap,
  valueFormat,
  lowerVertName,
  lowerSiteName,
  lowerPlantName,
}) => {
  const cacheKey = `${lowerVertName}_${lowerSiteName}_${lowerPlantName}_${headerMap ? JSON.stringify(headerMap) : 'no_map'}`

  if (colDefsCache.has(cacheKey)) {
    return colDefsCache.get(cacheKey)
  }

  let cols = []
  if (lowerVertName === 'elastomer' && lowerSiteName === 'jmd') {
    cols = NormalOpNormElastomerJmdColumns
  } else {
    cols = VERTICAL_COLDEFS_MAP[lowerVertName] || NormalOpNormMegColumns
  }

  const enhancedColDefs = cols.map((col) => {
    let updatedCol = { ...col }
    if (!headerMap || headerMap[col.title] === undefined) {
      if (valueFormat) updatedCol.format = valueFormat
    } else {
      updatedCol = {
        ...updatedCol,
        title: headerMap[col.title],
        align: 'right',
        format: valueFormat || '{0:#.###}',
        type:
          col.type ||
          (lowerVertName === 'pe' && lowerSiteName === 'hmd'
            ? 'negativeNumber'
            : 'number'),
      }
    }
    if (shouldLockColumn(col)) {
      updatedCol.locked = true
    }
    return updatedCol
  })

  // if (lowerVertName === 'cracker' && lowerSiteName === 'c2') {
  //   enhancedColDefs.unshift({
  //     field: 'isChecked',
  //     type: 'switch2',
  //     widthT: 30,
  //     filter: false,
  //     minWidth: 60,
  //   })
  // }

  colDefsCache.set(cacheKey, enhancedColDefs)
  return enhancedColDefs
}

export const clearColDefsCache = () => colDefsCache.clear()

export default getNormalOpNormColDef
