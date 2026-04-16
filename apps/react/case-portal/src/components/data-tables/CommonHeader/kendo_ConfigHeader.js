import contineGradeChange from '../../../assets/kendo_config_contineGradeChange.json'
import crackerColumns from '../../../assets/kendo_config_cracker_coldefs.json'
import cracker_composition from '../../../assets/kendo_config_cracker_composition.json'
import cracker_constants from '../../../assets/kendo_config_cracker_constants_coldefs.json'
import cracker_yield from '../../../assets/kendo_config_cracker_yield_coldefs.json'
import cracker_yield_dmd from '../../../assets/kendo_config_cracker_yield_dmd_coldefs.json'
import cracker_yield_vmd from '../../../assets/kendo_config_cracker_yield_vmd_coldefs.json'
import disContineGradeChange from '../../../assets/kendo_config_disContineGradeChange.json'
import productionColumnsConstants from '../../../assets/kendo_config_meg constants.json'
import productionColumns from '../../../assets/kendo_config_meg.json'
import productionColumnsPE1 from '../../../assets/kendo_config_pe1.json'
import productionColumnsPE2 from '../../../assets/kendo_config_pe2.json'
import colDefsShutdownRate from '../../../assets/kendo_config_pe3.json'
import productionColumnsPE5 from '../../../assets/kendo_config_pe5.json'
import pioImpactColumns from '../../../assets/kendo_config_pio_impact.json'

import reportManualEntry from '../../../assets/kendo_config_report_mannual_entry.json'
import naphthaColumns from '../../../assets/kendo_config_cracker_naphtha_coldefs.json'
import rawMaterialColumns from '../../../assets/kendo_config_raw_material_coldefs.json'
import catchemColumns from '../../../assets/kendo_config_catchem_coldefs.json'
import exsternalSteamColumns from '../../../assets/kendo_config_exsternal_steam_coldefs.json'

import productionColumnsVmdYield from '../../../assets/kendo_config_vmd_yield_dynamic.json'

const getConfigByType = (configType) => {
  switch (configType) {
    case 'meg':
      return productionColumns
    case 'megConstantsMannualEntry':
      return productionColumns
    case 'megConstants':
      return productionColumnsConstants
    case 'PIO Impact':
      return pioImpactColumns
    case 'shutdownData':
      return pioImpactColumns
    case 'StartupLosses':
      return productionColumnsPE1
    case 'Configuration':
      return productionColumnsPE1
    case 'Otherlosses':
      return productionColumnsPE2
    case 'External_Streams':
      return exsternalSteamColumns

    //NEW BUILD 17 NOV

    case 'Constant':
      return colDefsShutdownRate

    case 'ShutdownNorms':
      return colDefsShutdownRate
    case 'Constants':
      return productionColumnsPE5
    case 'production':
      return productionColumns
    case 'consumption':
      return productionColumns
    case 'cracker_configuration':
      return productionColumns
    case 'cracker_composition':
      return cracker_composition
    case 'cracker':
      return crackerColumns
    case 'cracker_constants':
      return cracker_constants
    case 'cracker_yield':
      return cracker_yield
    case 'cracker_yield_dmd':
      return cracker_yield_dmd
    case 'cracker_yield_vmd':
      return productionColumnsVmdYield
    case 'ContineGradeChange':
      return contineGradeChange
    case 'DisContineGradeChange':
      return disContineGradeChange

    case 'Report Manual Entry':
      return reportManualEntry
    case 'Naphtha':
      return naphthaColumns
    case 'rawMaterial':
      return rawMaterialColumns
    case 'CatChem':
      return catchemColumns

    default:
      return productionColumns
  }
}

const getEnhancedAOPColDefs = ({
  allGradesReciepes,
  headerMap,
  configType,
  FORMATE_VALUE,
  allGradesRecipes,
}) => {
  var config = []

  if (configType == 'grades') {
    config = [
      {
        field: 'ReceipeName',
        title: 'Recipe',
        editable: false,
        width1: 100,
        minWidth: 100,
      },
      {
        field: 'UOM',
        title: 'UOM',
        editable: false,
        width1: 85,
        minWidth: 90,
      },
    ]
    allGradesReciepes?.forEach((field) => {
      config.push({
        field: field?.id?.toUpperCase(),
        title: field?.displayName,
        editable: true,
        width1: 100,
        type: 'number',
        format: FORMATE_VALUE,
        minWidth: 100,
      })
    })
  } else if (configType == 'lines') {
    config = [
      {
        field: 'GradeName',
        title: 'Grade',
        editable: false,
        widthT: 100,
        minWidth: 100,
      },
      {
        field: 'UOM',
        title: 'UOM',
        editable: false,
        widthT: 85,
        minWidth: 85,
      },
    ]
    allGradesRecipes?.forEach((line) => {
      config.push({
        field: line?.Id?.toUpperCase(), // use Id from API
        title: line?.DisplayName, // use DisplayName
        editable: true,
        widthT: 100,
        type: 'number',
        format: FORMATE_VALUE,
        minWidth: 100,
      })
    })
  } else {
    config = getConfigByType(configType)
  }

  var enhancedColDefs = []

  if (
    configType == 'ShutdownNorms' ||
    configType == 'cracker_constants' ||
    configType == 'megConstants' ||
    configType == 'Constant' ||
    configType == 'rawMaterial' ||
    configType == 'CatChem'
  ) {
    enhancedColDefs = config.map((col) => {
      if (col?.title == 'Value') {
        return {
          ...col,
          type: 'number',
          format: FORMATE_VALUE,
          minWidth: 100,
        }
      }
      if (col?.title == 'IIR' || col?.title == 'CIIR' || col?.title == 'BIIR') {
        return {
          ...col,
          type: 'number',
          format: FORMATE_VALUE,
          minWidth: 100,
        }
      }

      return col
    })
  } else if (
    configType == 'PIO Impact' ||
    configType == 'shutdownData' ||
    configType == 'cracker_configuration'
  ) {
    enhancedColDefs = config.map((col) => {
      if (headerMap && headerMap[col.title]) {
        return {
          ...col,
          title: headerMap[col.title],
          align: 'right',
          type: 'negativeNumber',
          format: FORMATE_VALUE,
          minWidth: 100,
        }
      }

      return col
    })
  } else if (configType == 'cracker_yield_dmd') {
    enhancedColDefs = config.map((col) => {
      if (headerMap && headerMap[col.title]) {
        return {
          ...col,
          title: headerMap[col.title],
          align: 'right',
          type: 'number',
          format: FORMATE_VALUE,
          widthT: 100,
          fixedWidth: 100,
          width: 100,
          minWidth: 100,
        }
      }

      return col
    })
  } else if (configType == 'cracker_yield_vmd') {
    enhancedColDefs = config.map((col) => {
      if (headerMap && headerMap[col.title]) {
        return {
          ...col,
          title: headerMap[col.title],
          align: 'right',
          type: 'number',
          format: FORMATE_VALUE,
          minWidth: 100,
        }
      }

      return col
    })
  } else {
    enhancedColDefs = config.map((col) => {
      if (headerMap && headerMap[col.title]) {
        return {
          ...col,
          title: headerMap[col.title],
          align: 'right',
          type: 'number',
          format: FORMATE_VALUE,
          minWidth: 100,
        }
      }

      return col
    })
  }

  return enhancedColDefs
}

export default getEnhancedAOPColDefs
