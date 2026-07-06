import productionColDefs from '../../../assets/kendo_consumption_aop.json'
import productionColDefsElastomerJmd from '../../../assets/kendo_consumption_aop_elastomerJmd.json'
import productionColDefsElastomer from '../../../assets/kendo_consumption_aop_elastomer.json'
import productionColDefsElastomerHmdSbr from '../../../assets/kendo_consumption_aop_elastomer_sbr.json'
import productionColDefs1 from '../../../assets/kendo_consumption_aop_cracker.json'
import productionColCrackerC2Defs from '../../../assets/kendo_consumption_aop_cracker_c2.json'
import productionColDefsVcm from '../../../assets/kendo_consumption_aop_vcm.json'
import productionColDefsPta from '../../../assets/kendo_consumption_aop_pta.json'
import productionColDefsChemical from '../../../assets/kendo_consumption_aop_chemical.json'
// import productionColDefsVcmDmd from '../../../assets/kendo_consumption_aop_vcmdmd.json'
import { shouldLockColumn } from 'utils/columnLockUtils'

const getEnhancedColDefs = ({
  headerMap,
  lowerVertName,
  lowerSiteName,
  lowerPlantName,
  valueFormat,
}) => {
  let colDefs = productionColDefs

  // console.log('lowerVertName', lowerVertName)
  if (lowerVertName === 'cracker' && lowerSiteName === 'c2') {
    colDefs = productionColCrackerC2Defs
  } else if (
    lowerVertName === 'cracker' &&
    !(lowerVertName === 'cracker' && lowerSiteName === 'c2')
  ) {
    colDefs = productionColDefs1
  } else if (lowerVertName === 'elastomer' && lowerSiteName === 'jmd') {
    colDefs = productionColDefsElastomerJmd
  } else if (
    lowerVertName === 'elastomer' &&
    lowerSiteName === 'hmd' &&
    lowerPlantName === 'sbr'
  ) {
    colDefs = productionColDefsElastomerHmdSbr
  } else if (
    lowerVertName === 'elastomer' &&
    !(
      lowerVertName === 'elastomer' &&
      lowerSiteName === 'hmd' &&
      lowerPlantName === 'sbr'
    )
  ) {
    colDefs = productionColDefsElastomer
  } else if (lowerVertName === 'pta') {
    colDefs = productionColDefsPta
  } else if (lowerVertName === 'vcm') {
    colDefs = productionColDefsVcm
  } else if (lowerVertName === 'chemical') {
    colDefs = productionColDefsChemical
  }

  const enhancedColDefs = colDefs.map((col) => {
    let newCol = { ...col }
    if (shouldLockColumn(col)) {
      newCol.locked = true
    }

    if (headerMap && headerMap[col.headerName] !== undefined) {
      newCol = {
        ...newCol,
        title: headerMap[col.headerName],
        type: 'number',
        format: valueFormat || '{0:#.###}',
        editable: false,
        width: 120,
        minWidth: 100,
      }
    }

    if (col.field == 'avgOfAllMonths') {
      newCol = {
        ...newCol,
        format: valueFormat || '{0:#.###}',
        editable: false,
        type: 'number',
        minWidth: 100,
      }
    }
    if (col.field === 'wtAverage') {
      return {
        ...newCol,
        type: 'number',
        format: valueFormat || '{0:#.###}',
        minWidth: 100,
      }
    }

    return newCol
  })

  return enhancedColDefs
}

export default getEnhancedColDefs
