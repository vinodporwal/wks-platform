const commonHidden = { hidden: true }
const commonReadOnly = { editable: false }
const commonMonthCol = {
  editable: false,
  align: 'left',
  headerAlign: 'left',
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

const monthColumns = monthNames.map((month, index) => ({
  field: month,
  title: `${((index + 3) % 12) + 1}`,
  ...commonMonthCol,
}))

export const ProductionAopCrackerColumns = [
  { field: 'idFromApi', title: 'ID', ...commonHidden },
  { field: 'aopCaseId', title: 'Case ID', ...commonReadOnly, ...commonHidden },
  { field: 'aopType', title: 'Type', width: 80, ...commonHidden },
  { field: 'aopYear', title: 'Year', width: 80, ...commonHidden },
  { field: 'plantFkId', title: 'Plant ID', width: 80, ...commonHidden },

  {
    field: 'normParametersFKId',
    title: 'Particulars',
    width: 125,
    ...commonReadOnly,
  },
  {
    field: 'uom',
    title: 'UOM',
    width: 125,
    ...commonReadOnly,
  },

  ...monthColumns,

  {
    field: 'averageTPH',
    title: 'Total',
    ...commonReadOnly,
    ...commonHidden,
  },
  {
    field: 'isEditable',
    title: 'isEditable',
    ...commonHidden,
  },
  {
    field: 'aopStatus',
    title: 'aopStatus',
    ...commonReadOnly,
    ...commonHidden,
  },
]
