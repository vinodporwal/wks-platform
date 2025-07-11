const commonHidden = { hidden: true }
const commonReadOnly = { editable: false }
const commonMonthCol = {
  editable: false,
  type: 'number',
  align: 'left',
  headerAlign: 'left',
  format: '{0:#.##}',
}

// Month column generator
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

const monthColumns = monthNames.map((month, idx) => ({
  field: month,
  title: `${((idx + 3) % 12) + 1}`, // Converts to month number 4 to 3
  ...commonMonthCol,
}))

export const ProductionAopPtaColumns = [
  { field: 'idFromApi', title: 'ID', ...commonHidden },
  { field: 'aopCaseId', title: 'Case ID', ...commonReadOnly, ...commonHidden },
  { field: 'aopType', title: 'Type', width: 80, ...commonHidden },
  { field: 'aopYear', title: 'Year', width: 80, ...commonHidden },
  { field: 'plantFkId', title: 'Plant ID', width: 80, ...commonHidden },

  {
    field: 'normParametersFKId',
    title: 'Particulars/Grade',
    width: 125,
    ...commonReadOnly,
    ...commonHidden,
  },
  {
    field: 'normParameterName',
    title: 'Particulars/Grade',
    width: 125,
    ...commonReadOnly,
    ...commonHidden,
  },
  {
    field: 'displayName',
    title: 'Products',
    width: 125,
    ...commonReadOnly,
  },

  ...monthColumns,

  {
    field: 'averageTPH',
    title: 'Total',
    type: 'number',
    ...commonReadOnly,
    format: '{0:#.##}',
  },

  { field: 'isEditable', title: 'isEditable', ...commonHidden },
  {
    field: 'aopStatus',
    title: 'aopStatus',
    ...commonReadOnly,
    ...commonHidden,
  },
]
