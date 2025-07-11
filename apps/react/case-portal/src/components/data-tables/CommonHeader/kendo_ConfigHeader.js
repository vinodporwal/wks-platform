// import productionColumns from '../../../assets/kendo_config_meg.json';
// import productionColumnsConstants from '../../../assets/kendo_config_meg constants.json';
// import productionColumnsPE1 from '../../../assets/kendo_config_pe1.json';
// import productionColumnsPE2 from '../../../assets/kendo_config_pe2.json';
// import productionColumnsPE3 from '../../../assets/kendo_config_pe3.json';
// // import productionColumnsPE4 from '../../../assets/kendo_config_pe4.json';
// import crackerColumns from '../../../assets/kendo_config_cracker_coldefs.json';
// import cracker_composition from '../../../assets/kendo_config_cracker_composition.json';

import {
  ConfigurationCrackerColumns,
  ConfigurationCrackerCompositionColumns,
} from 'components/colums/CRAKER/ConfigurationCrackerColumns'
import {
  ConfigurationMegColumns,
  ConfigurationMegConstantsColumns,
} from 'components/colums/MEG/ConfigurationMegColumns'
import {
  ConfigurationPeColumns,
  ShutdownNormsConfigurationPeColumns,
} from 'components/colums/PE/ConfigurationPeColumns'

const CONFIG_MAP = {
  meg: ConfigurationMegColumns,
  megConstantsMannualEntry: ConfigurationMegColumns,
  megConstants: ConfigurationMegConstantsColumns,
  StartupLosses: ConfigurationPeColumns,
  Otherlosses: ConfigurationPeColumns,
  ShutdownNorms: ShutdownNormsConfigurationPeColumns,
  production: ConfigurationMegColumns,
  consumption: ConfigurationMegColumns,
  cracker: ConfigurationCrackerColumns,
  cracker_composition: ConfigurationCrackerCompositionColumns,
}

const getConfigByType = (configType) =>
  CONFIG_MAP[configType] || ConfigurationMegColumns

const getEnhancedAOPColDefs = ({
  allGradesReciepes,
  headerMap,
  configType,
}) => {
  let columnConfig = []

  if (configType === 'grades') {
    columnConfig = [
      {
        field: 'ReceipeName',
        title: 'Receipe',
        editable: false,
        width1: 200,
      },
      ...allGradesReciepes?.map((field) => ({
        field: field?.id?.toUpperCase(),
        title: field?.displayName,
        editable: true,
        width1: 200,
        type: 'Receipe',
      })),
    ]
  } else {
    columnConfig = getConfigByType(configType)
  }

  return columnConfig.map((col) =>
    headerMap?.[col.title]
      ? {
          ...col,
          title: headerMap[col.title],
          align: 'right',
          type: 'number',
          format: '{0:#.###}',
        }
      : col,
  )
}

export default getEnhancedAOPColDefs
