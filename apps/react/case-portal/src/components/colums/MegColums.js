export const BusinessDemandMegColumns = [
  {
    field: 'displayName',
    title: 'Particulars',
    editable: false,
    widthT: 150,
    minWidth: 120,
  },
  {
    field: 'UOM',
    title: 'UOM',
    editable: false,
    widthT: 80,
    minWidth: 100,
  },
  {
    field: 'april',
    title: 4,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'may',
    title: 5,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'june',
    title: 6,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'july',
    title: 7,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'aug',
    title: 8,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'sep',
    title: 9,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },

  {
    field: 'oct',
    title: 10,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'nov',
    title: 11,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'dec',
    title: 12,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'jan',
    title: 1,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'feb',
    title: 2,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'march',
    title: 3,
    editable: true,
    width: 50,
    align: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'remark',
    title: 'Remark',
    editable: false,
    widthT: 50,
    type: 'string',
    minWidth: 100,
  },
]

export const SlowDownMegColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 230,
    autoAdjust: false,
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
  },

  {
    field: 'maintStartDateTime',
    title: 'SD- From',
    type: 'dateTime',
    editable: true,
    widthT: 120,
  },

  {
    field: 'maintEndDateTime',
    title: 'SD- To',
    type: 'dateTime',
    editable: true,
    widthT: 120,
  },

  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 100,
  },

  {
    field: 'rateEOE',
    title: 'EOE Production Rate',
    editable: true,
    type: 'number',
    widthT: 100,
  },
  {
    field: 'rateEO',
    title: 'EO Production Rate',
    editable: true,
    type: 'number',
    widthT: 100,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 200,
    autoAdjust: false,
  },
]

export const NormalOpNormMegColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    widthT: 100,
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
    widthT: 120,
  },

  {
    field: 'UOM',
    title: 'UOM / MT',
    widthT: 80,
    editable: false,
  },

  {
    field: 'april',
    title: 4,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'may',
    title: 5,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'june',
    title: 6,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'july',
    title: 7,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },

  {
    field: 'august',
    title: 8,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'september',
    title: 9,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'october',
    title: 10,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'november',
    title: 11,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'december',
    title: 12,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'january',
    title: 1,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'february',
    title: 2,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'march',
    title: 3,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'negativeNumber',
  },
  {
    field: 'remarks',
    title: 'Remark',
    widthT: 80,
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
export const CrackerColums = [
  {
    field: 'Particulars',
    title: 'Type',
    widthT: 100,
    groupable: true,
    editable: false,
    hidden: true,
  },
  {
    widthT: 100,

    columnType: 'checkbox',
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
    widthT: 120,
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
    widthT: 100,
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

export const ShutdownConsumptionMegColumns = [
  {
    field: 'Particulars',
    headerName: 'Type',
    width: 100,
    hidden: true,
  },
  {
    field: 'materialFkId',
    headerName: 'Particulars',
    minWidth: 100,
    editable: false,
    hidden: true,
    width: 100,
  },
  {
    field: 'productName',
    headerName: 'Particulars',
    widthT: 120,
    editable: false,
  },
  { field: 'UOM', headerName: 'UOM', widthT: 80, editable: false },

  ...Array.from({ length: 12 }, (_, i) => {
    const monthIndex = (i + 4) % 12 || 12
    const monthField = new Date(2000, monthIndex - 1)
      .toLocaleString('en-US', { month: 'long' })
      .toLowerCase()

    return {
      field: monthField,
      width: 100,
      type: 'negativeNumber',
      format: '{0:#.###}',
      editable: false,
      isDisabled: true,
      monthNumber: monthIndex,
    }
  }),

  {
    field: 'remarks',
    headerName: 'Remark',
    // widthT: 160,
    editable: false,
  },
  {
    field: 'idFromApi',
    headerName: 'idFromApi',
    hidden: true,
  },
]

export const SlowdownNormsMegColumns = [
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
    widthT: 120,
    editable: false,
  },
  { field: 'UOM', headerName: 'UOM', widthT: 80, editable: false },

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
    widthT: 100,
    editable: false,
  },
  {
    field: 'idFromApi',
    headerName: 'idFromApi',
    hidden: true,
  },
]

export const ConsumptionAopMegColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    editable: false,
    width: 100,
    hidden: true,
    minWidth: 150,
  },
  {
    field: 'NormParametersId',
    hidden: true,
    minWidth: 150,
  },

  {
    field: 'productName',
    title: 'Particulars',
    editable: false,
    widthT: 120,
    minWidth: 150,
  },
  {
    field: 'UOM',
    title: 'UOM / MT',
    editable: false,
    widthT: 90,
    minWidth: 150,
  },
  {
    field: 'april',
    title: 4,
    minWidth: 100,
  },
  {
    field: 'may',
    title: 5,
    minWidth: 100,
  },
  {
    field: 'june',
    title: 6,
    minWidth: 100,
  },
  {
    field: 'july',
    title: 7,
    minWidth: 100,
  },
  {
    field: 'aug',
    title: 8,
    minWidth: 100,
  },
  {
    field: 'sep',
    title: 9,
    minWidth: 100,
  },
  {
    field: 'oct',
    title: 10,
    minWidth: 100,
  },
  {
    field: 'nov',
    title: 11,
    minWidth: 100,
  },
  {
    field: 'dec',
    title: 12,
    minWidth: 100,
  },
  {
    field: 'jan',
    title: 1,
    minWidth: 100,
  },
  {
    field: 'feb',
    title: 2,
    minWidth: 100,
  },
  {
    field: 'march',
    title: 3,
    minWidth: 100,
  },
  {
    field: 'isEditable',
    title: 'isEditable',
    hidden: true,
    minWidth: 100,
  },
]
