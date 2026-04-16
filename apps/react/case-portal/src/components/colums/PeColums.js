export const BusinessDemandPeColumns = [
  {
    field: 'displayName',
    title: 'Particulars',
    editable: false,
    widthT: 100,
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
    minWidth: 100,
  },
  {
    field: 'may',
    title: 5,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'june',
    title: 6,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'july',
    title: 7,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'aug',
    title: 8,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'sep',
    title: 9,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'oct',
    title: 10,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'nov',
    title: 11,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'dec',
    title: 12,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'jan',
    title: 1,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'feb',
    title: 2,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'march',
    title: 3,
    editable: true,
    width: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'remark',
    title: 'Remark',
    widthT: 80,
    editable: false,
    minWidth: 100,
  },
]

export const SlowDownPeColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 200,
    autoAdjust: false,
    minWidth: 100,
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },

  {
    field: 'productName1',
    title: 'Particulars',
    widthT: 150,
    editable: true,
    autoAdjust: false,
    minWidth: 100,
  },

  // {
  //   field: 'maintStartDateTime',
  //   title: 'SD- From',
  //   type: 'dateTime',
  //   editable: true,
  // },

  // {
  //   field: 'maintEndDateTime',
  //   title: 'SD- To',
  //   type: 'dateTime',
  //   editable: true,
  // },
  {
    field: 'monthly',
    title: 'Month',
    type: 'monthDropdownPEPP',
    editable: true,
    width: 120,
    minWidth: 100,
  },

  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 150,
    minWidth: 100,
  },

  {
    field: 'rate',
    title: 'Reduced Rate (TPH)',
    editable: true,
    type: 'number',
    minWidth: 100,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 250,
    autoAdjust: false,
    minWidth: 100,
  },
]

export const NormalOpNormPeColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    widthT: 100,
    groupable: true,
    editable: false,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'materialFkId',
    title: 'Particulars',
    widthT: 100,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'productName',
    title: 'Particulars',
    widthT: 120,
    minWidth: 100,
  },

  {
    field: 'UOM',
    title: 'UOM / MT',
    widthT: 90,
    editable: false,
    minWidth: 100,
  },

  {
    field: 'april',
    title: 4,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'may',
    title: 5,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'june',
    title: 6,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'july',
    title: 7,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },

  {
    field: 'august',
    title: 8,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'september',
    title: 9,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'october',
    title: 10,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'november',
    title: 11,
    editable: true,

    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'december',
    title: 12,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'january',
    title: 1,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'february',
    title: 2,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'march',
    title: 3,
    editable: true,
    width: 100,
    align: 'right',
    format: '{0:#.###}',
    type: 'number',
    minWidth: 100,
  },
  {
    field: 'remarks',
    title: 'Remark',
    widthT: 80,
    editable: true,
    minWidth: 100,
  },

  {
    field: 'idFromApi',
    title: 'idFromApi',
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'isEditable',
    title: 'isEditable',
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
]

export const ShutdownConsumptionPeColumns = [
  {
    field: 'Particulars',
    headerName: 'Type',
    width: 100,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'materialFkId',
    headerName: 'Particulars',
    minWidth: 150,
    editable: false,
    hidden: true,
    isVisible: false,
    width: 100,
  },
  {
    field: 'productName',
    headerName: 'Particulars',
    widthT: 120,
    editable: false,
    minWidth: 100,
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
      minWidth: 100,
      format: '{0:#.###}',
      editable: false,
      isDisabled: true,

      monthNumber: monthIndex,
    }
  }),

  {
    field: 'remarks',
    headerName: 'Remark',
    editable: false,
    minWidth: 100,
  },

  {
    field: 'idFromApi',
    headerName: 'idFromApi',
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
]

export const ShutdownConsumptionPeColumnsPeLldpe = [
  {
    field: 'Particulars',
    headerName: 'Type',
    width: 100,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'materialFkId',
    headerName: 'Particulars',
    minWidth: 150,
    editable: false,
    hidden: true,
    isVisible: false,
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
      minWidth: 100,
      format: '{0:#.###}',
      editable: false,
      isDisabled: true,
      monthNumber: monthIndex,
    }
  }),

  {
    field: 'idFromApi',
    headerName: 'idFromApi',
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
]

export const SlowdownNormsPeColumns = [
  {
    field: 'Particulars',
    headerName: 'Type',
    width: 100,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'materialFkId',
    headerName: 'Particulars',
    minWidth: 150,
    editable: false,
    hidden: true,
    isVisible: false,
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
      minWidth: 100,
      format: '{0:#.###}',
      editable: false,
      monthNumber: monthIndex,
    }
  }),

  {
    field: 'idFromApi',
    headerName: 'idFromApi',
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
]
export const ConsumptionAopPeColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    editable: false,
    width: 100,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'NormParametersId',
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },

  {
    field: 'productName',
    title: 'Particulars',
    editable: false,
    widthT: 120,
    minWidth: 100,
  },
  {
    field: 'UOM',
    title: 'UOM / MT',
    editable: false,
    widthT: 90,
    minWidth: 100,
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
    isVisible: false,
    minWidth: 100,
  },
]
