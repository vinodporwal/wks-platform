const commonHidden = { hidden: true }
const commonReadOnly = { editable: false }
const commonMonthCol = {
  editable: false,
  type: 'number',
  align: 'left',
  format: '{0:#.##}',
}

// Month fields
const monthColumns = [
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
].map((month, index) => ({
  field: month,
  title: `${((index + 3) % 12) + 1}`,
  ...commonMonthCol,
}))

export const ProductionAopPetColumns = [
  { field: 'idFromApi', title: 'ID', ...commonHidden },
  { field: 'aopCaseId', title: 'Case ID', ...commonHidden, ...commonReadOnly },
  { field: 'aopType', title: 'Type', width: 80, ...commonHidden },
  { field: 'aopYear', title: 'Year', width: 80, ...commonHidden },
  { field: 'plantFkId', title: 'Plant ID', width: 80, ...commonHidden },

  {
    field: 'Particulars',
    title: 'Type',
    width: 100,
    groupable: true,
    headerClass: 'bold-header',
    filterable: 'false',
    ...commonHidden,
    ...commonReadOnly,
  },
  {
    field: 'normParameterId',
    title: 'Particulars',
    width: 150,
    ...commonHidden,
    ...commonReadOnly,
  },
  { field: 'materialFKId', ...commonHidden, ...commonReadOnly },
  { field: 'normParameterDisplayName', ...commonHidden, ...commonReadOnly },

  { field: 'displayName', title: 'Products', width: 125, ...commonReadOnly },

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
    ...commonHidden,
    ...commonReadOnly,
  },
]
