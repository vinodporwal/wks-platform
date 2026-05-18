import {
  BusinessDemandElastomerColumns,
  BusinessDemandElastomerJmdColumns,
} from 'components/colums/ElastomerColums'
import { BusinessDemandMegColumns } from 'components/colums/MegColums'
import { BusinessDemandPetColumns } from 'components/colums/PetColums'
import { BusinessDemandPeColumns } from 'components/colums/PeColums'
import { BusinessDemandPpColumns } from 'components/colums/PpColums'
import { BusinessDemandPtaColumns } from 'components/colums/PtaColums'
import { verticalEnums } from 'enums/verticalEnums'
import { useSelector } from 'react-redux'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'

const colDefsCache = new Map()

const VERTICAL_COLDEFS_MAP = {
  [verticalEnums.PE]: BusinessDemandPeColumns,
  [verticalEnums.PP]: BusinessDemandPpColumns,
  [verticalEnums.PTA]: BusinessDemandPtaColumns,
  [verticalEnums.ELASTOMER]: BusinessDemandElastomerColumns,
  [verticalEnums.MEG]: BusinessDemandMegColumns,
  [verticalEnums.PET]: BusinessDemandPetColumns,
}

const kendoBusinessDemColDef = ({ headerMap }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const vertName = dataGridStore.verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || verticalEnums.MEG
  const siteName = dataGridStore.siteObject?.name?.toLowerCase() // get site
  const IS_ELASTOMER_JMD = lowerVertName === 'elastomer' && siteName === 'jmd'
  const IS_CRACKER_HMD = lowerVertName === 'cracker' && siteName === 'hmd'
  const FORMATE_DECIMAL = ValueFormatterProduction()

  const cacheKey = `${lowerVertName}_${siteName}_${headerMap ? JSON.stringify(headerMap) : 'no_map'}`

  if (colDefsCache.has(cacheKey)) {
    return colDefsCache.get(cacheKey)
  }

  // Pick the right column definition
  let cols
  if (IS_ELASTOMER_JMD) {
    cols = BusinessDemandElastomerJmdColumns
  } else {
    cols = VERTICAL_COLDEFS_MAP[lowerVertName] || BusinessDemandMegColumns
  }

  const enhancedColDefs = cols.map((col) => {
    if (!headerMap || headerMap[col.title] === undefined) {
      return col
    }

    return {
      ...col,
      title: headerMap[col.title],
      align: 'right',
      format: FORMATE_DECIMAL,
      editable: IS_CRACKER_HMD ? col.title === 4 : col.editable,
      isDisabled: IS_CRACKER_HMD ? col.title !== 4 : false,
    }
  })

  // For Cracker HMD — inject a read-only Avg column just before Remark
  if (IS_CRACKER_HMD) {
    const remarkIndex = enhancedColDefs.findIndex(
      (col) => col.field === 'remark',
    )
    const insertAt = remarkIndex >= 0 ? remarkIndex : enhancedColDefs.length
    enhancedColDefs.splice(insertAt, 0, {
      field: 'avg',
      title: 'Avg',
      editable: false,
      align: 'right',
      format: FORMATE_DECIMAL,
      type: 'number',
      widthT: 100,
      minWidth: 100,
      isDisabled: true,
    })
  }

  colDefsCache.set(cacheKey, enhancedColDefs)
  return enhancedColDefs
}

export const clearColDefsCache = () => colDefsCache.clear()

export default kendoBusinessDemColDef
