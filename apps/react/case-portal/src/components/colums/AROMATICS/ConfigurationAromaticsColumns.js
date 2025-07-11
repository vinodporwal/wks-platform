const commonHidden = { hidden: true }
const commonEditable = { editable: true }
const commonReadOnly = { editable: false }
const monthKeys = [
  'apr',
  'may',
  'jun',
  'jul',
  'aug',
  'sep',
  'oct',
  'nov',
  'dec',
  'jan',
  'feb',
  'mar',
]

const monthColumns = monthKeys.map((key, index) => ({
  field: key,
  title: ((index + 3) % 12) + 1,
  ...commonEditable,
}))

export const ConfigurationAromaticsColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    width: 125,
    ...commonReadOnly,
    ...commonHidden,
  },
  {
    field: 'normParameterFKId',
    ...commonHidden,
  },
  {
    field: 'productName',
    title: 'Particulars',
    ...commonReadOnly,
  },
  {
    field: 'UOM',
    title: 'UOM',
    ...commonReadOnly,
  },

  ...monthColumns,

  {
    field: 'remarks',
    title: 'Remark',
    width: 150,
    ...commonEditable,
  },
  {
    field: 'isEditable',
    title: 'isEditable',
    ...commonHidden,
  },
]
