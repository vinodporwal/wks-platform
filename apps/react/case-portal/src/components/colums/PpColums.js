export const BusinessDemandPpColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    width: 100,
    groupable: true,
    editable: false,
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },
  {
    field: 'normParameterId',
    title: 'Particulars',
    editable: false,
    width: 100,
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },

  {
    field: 'displayName',
    title: 'Particulars',
    editable: false,
    width: 120,
    minWidth: 100,
    locked: true,
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
  {
    field: 'idFromApi',
    title: 'ID from API',
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },
]

export const SlowDownPpColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 200,
    autoAdjust: false,
    minWidth: 200,
    locked: true,
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },

  {
    field: 'productName1',
    title: 'Particulars',
    editable: true,
    widthT: 140,
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
    width: 100,
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
    widthT: 150,
    minWidth: 120,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 230,
    autoAdjust: false,
    minWidth: 150,
  },
]
export const SlowDownPpDtaColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 150,
    autoAdjust: false,
    minWidth: 100,
    locked: true,
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },

  {
    field: 'productName1',
    title: 'Particulars',
    editable: true,
    widthT: 150,
    autoAdjust: false,
    minWidth: 100,
  },
  {
    field: 'lineDisplayName',
    title: 'Line',
    type: 'lineDropdown',
    editable: true,
    width: 100,
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
    width: 100,
    minWidth: 100,
  },

  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 140,
    minWidth: 100,
  },

  {
    field: 'rate',
    title: 'Reduced Rate (TPH)',
    editable: true,
    type: 'number',
    widthT: 130,
    minWidth: 120,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 200,
    autoAdjust: false,
    minWidth: 150,
  },
]

export const NormalOpNormPpColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    width: 100,
    groupable: true,
    editable: false,
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },
  {
    field: 'materialFkId',
    title: 'Particulars',
    width: 100,
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },
  {
    field: 'productName',
    title: 'Particulars',
    widthT: 120,
    minWidth: 100,
  },

  {
    field: 'UOM',
    title: 'UOM/MT',
    widthT: 60,
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
    minWidth: 100,
    isVisible: false,
  },
  {
    field: 'isEditable',
    title: 'isEditable',
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },
]

export const ShutdownConsumptionPpColumns = [
  {
    field: 'Particulars',
    headerName: 'Type',
    width: 100,
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },
  {
    field: 'materialFkId',
    headerName: 'Particulars',
    minWidth: 150,
    editable: false,
    hidden: true,
    width: 100,
    isVisible: false,
  },
  {
    field: 'productName',
    headerName: 'Particulars',
    width: 120,
    editable: false,
    minWidth: 130,
  },
  {
    field: 'UOM',
    headerName: 'UOM',
    widthT: 80,
    editable: false,
    minWidth: 100,
  },

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
      monthNumber: monthIndex,
      isDisabled: true,
      minWidth: 100,
    }
  }),
  {
    field: 'remarks',
    headerName: 'Remark',

    editable: false,
    minWidth: 100,
  },
]

export const SlowdownNormsPpColumns = [
  {
    field: 'Particulars',
    headerName: 'Type',
    width: 100,
    hidden: true,
    minWidth: 100,
    isVisible: false,
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
    minWidth: 130,
  },
  {
    field: 'UOM',
    headerName: 'UOM',
    widthT: 80,
    editable: false,
    minWidth: 100,
  },

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
      monthNumber: monthIndex,
      minWidth: 100,
    }
  }),

  {
    field: 'remarks',
    headerName: 'Remark',
    width: 100,
    editable: false,
    minWidth: 100,
  },
  {
    field: 'idFromApi',
    headerName: 'idFromApi',
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },
]
export const ConsumptionAopPpColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    editable: false,
    width: 100,
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },
  {
    field: 'NormParametersId',
    hidden: true,
    minWidth: 100,
    isVisible: false,
  },

  {
    field: 'productName',
    title: 'Particulars',
    editable: false,
    width: 120,
    minWidth: 100,
  },
  {
    field: 'UOM',
    title: 'UOM / MT',
    editable: false,
    width: 90,
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
    minWidth: 100,
    isVisible: false,
  },
]
