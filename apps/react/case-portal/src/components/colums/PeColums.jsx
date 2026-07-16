export const BusinessDemandPeColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    width: 100,
    groupable: true,
    editable: false,
    hidden: true,
    minWidth: 100,
  },
  {
    field: 'normParameterId',
    title: 'Particulars',
    editable: false,
    width: 125,
    hidden: true,
    minWidth: 100,
  },

  {
    field: 'displayName',
    title: 'Particulars',
    editable: false,
    width: 125,
    minWidth: 100,
  },
  {
    field: 'april',
    title: 4,
    editable: true,
    width: 100,
    rightAlign: true,
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'may',
    title: 5,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'june',
    title: 6,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'july',
    title: 7,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'aug',
    title: 8,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'sep',
    title: 9,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'oct',
    title: 10,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'nov',
    title: 11,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'dec',
    title: 12,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'jan',
    title: 1,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'feb',
    title: 2,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'march',
    title: 3,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'remark',
    title: 'Remark',
    width: 100,
    editable: false,
  },
  {
    field: 'idFromApi',
    title: 'ID from API',
    hidden: true,
  },
]

export const SlowDownPeColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    locked: true,
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
  },

  {
    field: 'productName1',
    title: 'Particulars',
    editable: true,
  },

  {
    field: 'maintStartDateTime',
    title: 'SD- From',
    type: 'dateTime',
    editable: true,
  },

  {
    field: 'maintEndDateTime',
    title: 'SD- To',
    type: 'dateTime',
    editable: true,
  },

  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
  },

  {
    field: 'rate',
    title: 'Rate (TPH)',
    editable: true,
    type: 'number',
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
  },
]

export const NormalOpNormPeColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    width: 100,
    groupable: true,
    editable: false,
    hidden: true,
  },
  {
    field: 'materialFkId',
    title: 'Particulars',
    width: 100,
    hidden: true,
  },
  {
    field: 'productName',
    title: 'Particulars',
    widthT: 220,
  },

  {
    field: 'UOM',
    title: 'UOM / MT',
    widthT: 90,
    editable: false,
  },

  {
    field: 'april',
    title: 4,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'may',
    title: 5,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'june',
    title: 6,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'july',
    title: 7,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },

  {
    field: 'august',
    title: 8,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'september',
    title: 9,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'october',
    title: 10,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'november',
    title: 11,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'december',
    title: 12,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'january',
    title: 1,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'february',
    title: 2,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'march',
    title: 3,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
  },
  {
    field: 'remarks',
    title: 'Remark',
    width: 100,
    editable: true,
  },

  {
    field: 'idFromApi',
    title: 'idFromApi',
    hidden: true,
  },
  {
    field: 'isEditable',
    title: 'isEditable',
    hidden: true,
  },
]

export const ShutdownConsumptionPeColumns = [
  {
    field: 'Particulars',
    headerName: 'Type',
    width: 100,
    hidden: true,
  },
  {
    field: 'materialFkId',
    headerName: 'Particulars',
    minWidth: 150,
    editable: false,
    hidden: true,
    width: 100,
  },
  {
    field: 'productName',
    headerName: 'Particulars',
    width: 120,
    editable: false,
  },
  { field: 'UOM', headerName: 'UOM', width: 80, editable: false },

  ...Array.from({ length: 12 }, (_, i) => {
    const monthIndex = (i + 4) % 12 || 12
    const monthField = new Date(2000, monthIndex - 1)
      .toLocaleString('en-US', { month: 'long' })
      .toLowerCase()

    return {
      field: monthField,
      width: 100,
      type: 'number',
      format: '{0:#.###}',
      editable: false,
      isDisabled: true,
      monthNumber: monthIndex,
    }
  }),

  {
    field: 'remarks',
    headerName: 'Remark',
    width: 100,
    editable: false,
  },
  {
    field: 'idFromApi',
    headerName: 'idFromApi',
    hidden: true,
  },
]
export const SlowdownNormsPeColumns = [
  {
    field: 'Particulars',
    headerName: 'Type',
    width: 100,
    hidden: true,
  },
  {
    field: 'materialFkId',
    headerName: 'Particulars',
    minWidth: 150,
    editable: false,
    hidden: true,
    width: 100,
  },
  {
    field: 'productName',
    headerName: 'Particulars',
    width: 120,
    editable: false,
  },
  { field: 'UOM', headerName: 'UOM', width: 80, editable: false },

  ...Array.from({ length: 12 }, (_, i) => {
    const monthIndex = (i + 4) % 12 || 12
    const monthField = new Date(2000, monthIndex - 1)
      .toLocaleString('en-US', { month: 'long' })
      .toLowerCase()

    return {
      field: monthField,
      width: 100,
      type: 'number',
      format: '{0:#.###}',
      editable: false,
      isDisabled: true,
      monthNumber: monthIndex,
    }
  }),

  {
    field: 'remarks',
    headerName: 'Remark',
    width: 100,
    editable: false,
  },
  {
    field: 'idFromApi',
    headerName: 'idFromApi',
    hidden: true,
  },
]
export const ConsumptionAopPeColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    editable: false,
    width: 100,
    hidden: true,
  },
  {
    field: 'NormParametersId',
    hidden: true,
  },

  {
    field: 'productName',
    title: 'Particulars',
    editable: false,
    width: 120,
  },
  {
    field: 'UOM',
    title: 'UOM / MT',
    editable: false,
    width: 90,
  },
  {
    field: 'april',
    title: 4,
  },
  {
    field: 'may',
    title: 5,
  },
  {
    field: 'june',
    title: 6,
  },
  {
    field: 'july',
    title: 7,
  },
  {
    field: 'aug',
    title: 8,
  },
  {
    field: 'sep',
    title: 9,
  },
  {
    field: 'oct',
    title: 10,
  },
  {
    field: 'nov',
    title: 11,
  },
  {
    field: 'dec',
    title: 12,
  },
  {
    field: 'jan',
    title: 1,
  },
  {
    field: 'feb',
    title: 2,
  },
  {
    field: 'march',
    title: 3,
  },
  {
    field: 'isEditable',
    title: 'isEditable',
    hidden: true,
  },
]
