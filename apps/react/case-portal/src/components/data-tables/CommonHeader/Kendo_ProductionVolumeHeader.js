import production_coldefs_pe from '../../../assets/kendo_production_coldefs_pe.json'
import production_coldefs_meg from '../../../assets/kendo_production_coldefs_meg.json'
import production_coldefs_elastomer from '../../../assets/kendo_production_coldefs_elastomer.json'
import production_coldefs_vcm_dmd_edc from '../../../assets/kendo_production_coldefs_vcm_dmd_edc.json'
import production_coldefs_pp_hmd from '../../../assets/kendo_production_coldefs_pp_hmd.json'
import { useSelector } from 'react-redux'
import { shouldLockColumn } from 'utils/columnLockUtils'

const getEnhancedProductionColDefs = ({ headerMap, valueFormat }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { verticalChange, siteObject, plantObject } = dataGridStore
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || 'meg'
  const SITE_NAME = siteObject?.name?.toLowerCase()
  const PLANT_NAME = plantObject?.name?.toLowerCase()
  const IS_ELASTOMER_JMD = lowerVertName === 'elastomer' && SITE_NAME === 'jmd'
  const isPPDTAorHMD =
    lowerVertName === 'pp' && (SITE_NAME === 'hmd' || SITE_NAME === 'dta')
  const IS_VCM_DMD_EDC =
    lowerVertName === 'vcm' && SITE_NAME === 'dmd' && PLANT_NAME === 'edc'
  const baseCols =
    lowerVertName == 'pp' && (SITE_NAME === 'hmd' || SITE_NAME === 'dta')
      ? production_coldefs_pp_hmd
      : lowerVertName === 'pe' || lowerVertName == 'pp'
        ? production_coldefs_pe
        : IS_ELASTOMER_JMD
          ? production_coldefs_elastomer
          : IS_VCM_DMD_EDC
            ? production_coldefs_vcm_dmd_edc
            : production_coldefs_meg
  const nonAprilMonths = [
    'may',
    'june',
    'july',
    'august',
    'september',
    'october',
    'november',
    'december',
    'january',
    'february',
    'march',
  ]

  const enhancedColDefs = baseCols.map((col) => {
    let updatedCol = { ...col }

    if (headerMap && headerMap[col.title] !== undefined) {
      updatedCol = {
        ...updatedCol,
        title: headerMap[col.title],
        type: 'number',
        format: valueFormat,
        widthT: col?.widthT,
        minWidth: 100,
      }
    }
    if (isPPDTAorHMD && nonAprilMonths.includes(col.field)) {
      updatedCol = {
        ...updatedCol,
        editable: false, // ? not editable
        isDisabled: true, // ? grey style via k-number-right-disabled
        minWidth: 100,
      }
    }
    if (IS_VCM_DMD_EDC) {
      updatedCol = {
        ...updatedCol,
        editable: false, 
        isDisabled: true, 
        isVisible: false,
        minWidth: 100,
      }
    }
    if (shouldLockColumn(col)) {
      updatedCol.locked = true
    }

    return updatedCol
  })

  return enhancedColDefs
}

export default getEnhancedProductionColDefs
