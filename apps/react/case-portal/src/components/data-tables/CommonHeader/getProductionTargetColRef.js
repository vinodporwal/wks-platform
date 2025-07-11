import { ProductionTargetAromaticColumns } from 'components/colums/AROMATICS/ProductionTargetAromaticColumns'
import { ProductionTargetChemicalColumns } from 'components/colums/CHEMICAL/ProductionTargetChemicalColumns'
import { ProductionTargetElastomerColumns } from 'components/colums/ELASTOMER/ProductionTargetElastomerColumns'
import { ProductionTargetMegColumns } from 'components/colums/MEG/ProductionTargetMegColumns'
import { ProductionTargetPeColumns } from 'components/colums/PE/ProductionTargetPeColumns'
import { ProductionTargetPetColumns } from 'components/colums/PET/ProductionTargetPetColumns'
import { ProductionTargetPpColumns } from 'components/colums/PP/ProductionTargetPpColumns'
import { ProductionTargetPtaColumns } from 'components/colums/PTA/ProductionTargetPtaColumns'
import { ProductionTargetPvcColumns } from 'components/colums/PVC/ProductionTargetPvcColumns'
import { ProductionTargetVcmColumns } from 'components/colums/VCM/ProductionTargetVcmColumns'
import { verticalEnums } from 'enums/verticalEnums'
import { useSelector } from 'react-redux'

const colDefsCache = new Map()

const VERTICAL_COLDEFS_MAP = {
  [verticalEnums.PE]: ProductionTargetPeColumns,
  [verticalEnums.PP]: ProductionTargetPpColumns,
  [verticalEnums.PTA]: ProductionTargetPtaColumns,
  [verticalEnums.ELASTOMER]: ProductionTargetElastomerColumns,
  [verticalEnums.MEG]: ProductionTargetMegColumns,
  [verticalEnums.AROMATICS]: ProductionTargetAromaticColumns,
  [verticalEnums.CHEMICAL]: ProductionTargetChemicalColumns,
  [verticalEnums.PET]: ProductionTargetPetColumns,
  [verticalEnums.PVC]: ProductionTargetPvcColumns,
  [verticalEnums.VCM]: ProductionTargetVcmColumns,
}

const getProductionTargetColRef = ({ headerMap, hideSummary }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const vertName = dataGridStore.verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || verticalEnums.MEG

  const cacheKey = `${lowerVertName}_${headerMap ? JSON.stringify(headerMap) : 'no_map'}`

  if (colDefsCache.has(cacheKey)) {
    return colDefsCache.get(cacheKey)
  }
  const cols = VERTICAL_COLDEFS_MAP[lowerVertName] || ProductionTargetMegColumns
  const updatedCols = hideSummary
    ? [
        ...cols,
        {
          field: 'normParametersFKId',
          title: 'Particulars',
          editable: false,
          hidden: true,
        },
      ]
    : [
        ...cols,
        {
          field: 'materialFKId',
          title: 'Particulars',
          editable: false,
          hidden: true,
        },
        {
          field: 'remarks',
          title: 'Remark',
          editable: false,
          width: 180,
        },
      ]
  const enhancedColDefs = updatedCols.map((col) => {
    if (!headerMap || headerMap[col.title] === undefined) {
      return col
    }
    if (hideSummary) {
      return {
        ...col,
        title: headerMap[col.title],
        align: 'left',
        headerAlign: 'left',
        format: '{0:#.###}',
        type: 'number',
        editable: false,
      }
    }
    return {
      ...col,
      title: headerMap[col.title],
      align: 'left',
      headerAlign: 'left',
      format: '{0:#.###}',
      type: 'number',
      editable: true,
    }
  })

  colDefsCache.set(cacheKey, enhancedColDefs)
  return enhancedColDefs
}

export const clearColDefsCache = () => colDefsCache.clear()

export default getProductionTargetColRef
