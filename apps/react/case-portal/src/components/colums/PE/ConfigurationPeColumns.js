const commonReadOnly = { editable: false }
const commonAlign = { align: 'left', headerAlign: 'left' }

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
  editable: true,
  renderEditCell: 'NumericInputOnly',
  width: 120,
  ...commonAlign,
}))

export const ConfigurationPeColumns = [
  {
    field: 'TypeDisplayName',
    title: 'Type',
    width: 100,
    groupable: true,
    renderCell: 'renderStrongCell',
    ...commonReadOnly,
    hidden: true,
  },
  {
    field: 'normParameterFKId',
    title: 'Particulars',
    width: 160,
    valueGetter: 'defaultValueGetter',
    valueFormatter: 'productDisplayNameFormatter',
    renderEditCell: 'renderNormParameterSelectCell',
    ...commonReadOnly,
    hidden: true,
  },
  {
    field: 'productName',
    title: 'Particulars',
    width: 160,
    valueGetter: 'defaultValueGetter',
    valueFormatter: 'productDisplayNameFormatter',
    renderEditCell: 'renderNormParameterSelectCell',
    ...commonReadOnly,
  },
  {
    field: 'UOM',
    title: 'UOM',
    ...commonAlign,
    ...commonReadOnly,
  },
  ...monthColumns,
  {
    field: 'remarks',
    title: 'Remark',
    width: 150,
    editable: true,
    renderCell: 'renderRemarkCell',
  },
]

export const ShutdownNormsConfigurationPeColumns = [
  {
    field: 'ConfigTypeDisplayName',
    title: 'Constant',
    groupable: true,
    editable: false,
  },
  {
    field: 'TypeDisplayName',
    title: 'Type',
    groupable: true,
    editable: false,
    hidden: true,
  },
  {
    field: 'normParameterFKId',
    title: 'Particulars',
    editable: false,
    hidden: true,
  },
  {
    field: 'productName',
    title: 'Particulars',
    editable: false,
  },
  {
    field: 'apr',
    title: 'Values',
    editable: true,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    format: '{0:#.###}',
  },
  {
    field: 'remarks',
    title: 'Remark',
    editable: true,
    renderCell: 'renderRemarkCell',
    width: 400,
  },
]
