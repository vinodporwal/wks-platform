const commonMonthCol = {
  editable: true,
  width: 120,
  type: 'number',
}

const monthNames = [
  'april',
  'may',
  'june',
  'july',
  'aug',
  'sep',
  'oct',
  'nov',
  'dec',
  'jan',
  'feb',
  'march',
]

const monthColumns = monthNames.map((month, i) => ({
  field: month,
  title: ((i + 3) % 12) + 1, // converts to 4 to 3
  ...commonMonthCol,
}))

export const ConfigurationCrackerColumns = [
  {
    field: 'particulars',
    title: 'Particulars',
    editable: false,
    width: 150, // changed from `widthT`
  },
  {
    field: 'uom',
    title: 'UOM',
    editable: false,
    width: 120,
  },

  ...monthColumns,

  {
    field: 'remarks',
    title: 'Remark',
    editable: false,
    width: 180,
    type: 'string',
  },
  {
    field: 'NormParameterFKID',
    title: 'NormParameterFKID',
    filterable: false,
    hidden: true,
  },
]

export const ConfigurationCrackerCompositionColumns = [
  {
    field: 'ParticularsType',
    title: 'Type',
    width: 110,
    groupable: true,
    editable: false,
    hidden: true,
  },
  {
    field: 'particulars',
    title: 'Particulars',
    editable: false,
    width: 150,
  },
  {
    field: 'uom',
    title: 'UOM',
    editable: false,
    width: 120,
  },
  ...monthColumns,
  {
    field: 'remarks',
    title: 'Remark',
    editable: false,
    width: 180,
    type: 'string',
  },
  {
    field: 'idFromApi',
    title: 'idFromApi',
    filterable: false,
    hidden: true,
  },
]
