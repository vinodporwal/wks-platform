import production_coldefs_pe from '../../../assets/kendo_production_coldefs_pe.json'
import production_coldefs_meg from '../../../assets/kendo_production_coldefs_meg.json'
import production_coldefs_elastomer from '../../../assets/kendo_production_coldefs_elastomer.json'
import { useSelector } from 'react-redux'

const getEnhancedProductionColDefs = ({ headerMap, valueFormat }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { verticalChange, siteObject } = dataGridStore
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || 'meg'
  const SITE_NAME = siteObject?.name?.toLowerCase()
  const IS_ELASTOMER_JMD = lowerVertName === 'elastomer' && SITE_NAME === 'jmd'

  const baseCols =
    lowerVertName === 'pe' || lowerVertName == 'pp'
      ? production_coldefs_pe
      : IS_ELASTOMER_JMD
        ? production_coldefs_elastomer
        : production_coldefs_meg

  const enhancedColDefs = baseCols.map((col) => {
    let updatedCol = { ...col }

    if (headerMap && headerMap[col.title] !== undefined) {
      updatedCol = {
        ...updatedCol,
        title: headerMap[col.title],
        type: 'number',
        format: valueFormat,
        widthT: col?.widthT,
      }
    }

    return updatedCol
  })

  return enhancedColDefs
}

export default getEnhancedProductionColDefs
