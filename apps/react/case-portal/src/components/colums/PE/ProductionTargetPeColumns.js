export const ProductionTargetPeColumns = [
  {
    field: 'idFromApi',
    title: 'ID',
    hidden: true,
  },
  {
    field: 'aopCaseId',
    title: 'Case ID',
    hidden: true,
  },

  {
    field: 'productName',
    title: 'Particulars',

    editable: false,
  },
  ...Array.from({ length: 12 }, (_, i) => {
    const monthIndex = (i + 4) % 12 || 12
    const monthField = new Date(2000, monthIndex - 1)
      .toLocaleString('en-US', { month: 'long' })
      .toLowerCase()

    return {
      field: monthField,
      title: monthIndex,

      monthNumber: monthIndex,
      width: 120,
    }
  }),

  {
    field: 'avgTph',
    title: 'AVG',

    editable: false,
    hidden: true,
  },
  {
    field: 'isEditable',
    title: 'isEditable',
    hidden: true,
  },
]
