import productionColDefs from '../../../assets/kendo_consumption_aop.json'
import productionColDefsElastomerJmd from '../../../assets/kendo_consumption_aop_elastomerJmd.json'
import productionColDefsElastomer from '../../../assets/kendo_consumption_aop_elastomer.json'
import productionColDefs1 from '../../../assets/kendo_consumption_aop_cracker.json'
import productionColDefsVcm from '../../../assets/kendo_consumption_aop_vcm.json'
import productionColDefsPta from '../../../assets/kendo_consumption_aop_pta.json'
import productionColDefsChemical from '../../../assets/kendo_consumption_aop_chemical.json'
// import productionColDefsVcmDmd from '../../../assets/kendo_consumption_aop_vcmdmd.json'
const getEnhancedColDefs = ({
  headerMap,
  lowerVertName,
  lowerSiteName,
  lowerPlantName,
  valueFormat,
}) => {
  let colDefs = productionColDefs

  // console.log('lowerVertName', lowerVertName)

  if (lowerVertName === 'cracker') {
    colDefs = productionColDefs1
  } else if (lowerVertName === 'elastomer' && lowerSiteName === 'jmd') {
    colDefs = productionColDefsElastomerJmd
  } else if (lowerVertName === 'elastomer') {
    colDefs = productionColDefsElastomer
  } else if (lowerVertName === 'pta') {
    colDefs = productionColDefsPta
  } else if (lowerVertName === 'vcm') {
    colDefs = productionColDefsVcm
  } else if (lowerVertName === 'chemical') {
    colDefs = productionColDefsChemical
  }

  const enhancedColDefs = colDefs.map((col) => {
    if (headerMap && headerMap[col.headerName] !== undefined) {
      col = {
        ...col,
        title: headerMap[col.headerName],
        type: 'number',
        format: valueFormat || '{0:#.###}',
        editable: false,
        width: 120,
        minWidth: 100,
      }
    }

    if (col.field == 'avgOfAllMonths') {
      col = {
        ...col,
        format: valueFormat || '{0:#.###}',
        editable: false,
        type: 'number',
        minWidth: 100,
      }
    }
    if (col.field === 'wtAverage') {
      return {
        ...col,
        type: 'number',
        format: valueFormat || '{0:#.###}',
        minWidth: 100,
      }
    }

    return col
  })

  return enhancedColDefs
}

export default getEnhancedColDefs
