import { ShutdownConsumptionElastomerColumns } from 'components/colums/ElastomerColums'
import { ShutdownConsumptionMegColumns } from 'components/colums/MegColums'
import { ShutdownConsumptionCrackerColumns } from 'components/colums/CrackerColums'
import { ShutdownConsumptionPeColumns } from 'components/colums/PeColums'
import { ShutdownConsumptionPeColumnsPeLldpe } from 'components/colums/PeColums'
import { ShutdownConsumptionPpColumns } from 'components/colums/PpColums'
import {
  ShutdownConsumptionPtaColumns,
  ShutdownConsumptionPtadmdColumns,
  ShutdownConsumptionPtaPmdPiaColumns,
} from 'components/colums/PtaColums'
import { ShutdownConsumptionVcmColumns } from 'components/colums/VcmColums'
import { verticalEnums } from 'enums/verticalEnums'
import { useSelector } from 'react-redux'
import { shouldLockColumn } from 'utils/columnLockUtils'

const colDefsCache = new Map()

const VERTICAL_COLDEFS_MAP = {
  [verticalEnums.PE]: ShutdownConsumptionPeColumns,
  [verticalEnums.PP]: ShutdownConsumptionPpColumns,
  [verticalEnums.PTA]: ShutdownConsumptionPtaColumns,
  [verticalEnums.ELASTOMER]: ShutdownConsumptionElastomerColumns,
  [verticalEnums.AROMATICS]: ShutdownConsumptionElastomerColumns,
  [verticalEnums.VCM]: ShutdownConsumptionVcmColumns,
  [verticalEnums.PET]: ShutdownConsumptionPeColumns,
  [verticalEnums.MEG]: ShutdownConsumptionMegColumns,
  [verticalEnums.CRACKER]: ShutdownConsumptionCrackerColumns,
  [verticalEnums.CHEMICAL]: ShutdownConsumptionPtaColumns,
}

const getShutdownConsumptionColDef = ({
  headerMap,
  shutdownMonths,
  valueFormat,
}) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const vertName = dataGridStore.verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || verticalEnums.MEG

  const {
    verticalChange,
    screenTitle,
    plantObject,
    siteObject,
    verticalObject,
    year,
  } = dataGridStore

  const SITE_NAME_LOWERCASE = siteObject?.name?.toLowerCase()
  const PLANT_NAME_LOWERCASE = plantObject?.name?.toLowerCase()
  const SITE_NAME = siteObject?.name
  const IS_PTA_DMD = lowerVertName === 'pta' && SITE_NAME_LOWERCASE === 'dmd'

  const IS_PE_PP_VERTICAL_NMD_LLDPE =
    ['pe'].includes(lowerVertName) &&
    ['nmd'].includes(SITE_NAME_LOWERCASE) &&
    ['lldpe1', 'lldpe2'].includes(PLANT_NAME_LOWERCASE)
  const IS_PVC_VMD = lowerVertName === 'pvc' && SITE_NAME_LOWERCASE === 'vmd'
  const IS_PVC_DMD = lowerVertName === 'pvc' && SITE_NAME_LOWERCASE === 'dmd'
  const IS_PVC_HMD = lowerVertName === 'pvc' && SITE_NAME_LOWERCASE === 'hmd'
  const IS_PTA_PIA =
    lowerVertName === 'pta' &&
    SITE_NAME_LOWERCASE === 'pmd' &&
    PLANT_NAME_LOWERCASE === 'pia'
  let safeShutdownMonths = Array.isArray(shutdownMonths) ? shutdownMonths : []

  const cacheKey = `${lowerVertName}_${SITE_NAME_LOWERCASE}_${PLANT_NAME_LOWERCASE}_${JSON.stringify(headerMap)}_${safeShutdownMonths.join(',')}`

  if (colDefsCache.has(cacheKey)) {
    return colDefsCache.get(cacheKey)
  }

  let cols = []
  if (IS_PE_PP_VERTICAL_NMD_LLDPE) {
    cols = ShutdownConsumptionPeColumnsPeLldpe
  } else if (IS_PTA_DMD) {
    cols = ShutdownConsumptionPtadmdColumns
  } else if (IS_PTA_PIA) {
    cols = ShutdownConsumptionPtaPmdPiaColumns
  } else if (IS_PVC_VMD) {
    cols = ShutdownConsumptionPeColumns
  } else if (IS_PVC_DMD || IS_PVC_HMD) {
    cols = ShutdownConsumptionPpColumns
  } else {
    cols = VERTICAL_COLDEFS_MAP[lowerVertName] || []
  }

  const enhancedColDefs = cols.map((col) => {
    let newCol = { ...col }
    if (shouldLockColumn(col)) {
      newCol.locked = true
    }

    if (col.monthNumber) {
      const monthNum = col.monthNumber
      const isPEorPP = false
      const isEditable = IS_PTA_DMD
        ? true
        : safeShutdownMonths.includes(monthNum)

      return {
        ...newCol,
        headerName: headerMap?.[monthNum] || col.field,
        editable: isEditable,
        // isDisabled: !safeShutdownMonths.includes(monthNum),
        isDisabled: !isEditable,
        format: valueFormat,
      }
    }

    return valueFormat ? { ...newCol, format: valueFormat } : newCol
  })

  colDefsCache.set(cacheKey, enhancedColDefs)
  return enhancedColDefs
}

export const clearColDefsCache = () => colDefsCache.clear()

export default getShutdownConsumptionColDef
