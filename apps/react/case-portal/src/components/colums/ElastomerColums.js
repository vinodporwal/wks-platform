export const BusinessDemandElastomerColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    groupable: true,
    editable: false,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'normParameterId',
    title: 'Particulars',
    editable: false,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },

  {
    field: 'displayName',
    title: 'Particulars',
    editable: false,
    minWidth: 125,
    locked: true,
  },
  {
    field: 'april',
    title: 4,
    editable: true,
    minWidth: 100,
    rightAlign: true,
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'may',
    title: 5,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'june',
    title: 6,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'july',
    title: 7,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'aug',
    title: 8,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'sep',
    title: 9,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'oct',
    title: 10,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'nov',
    title: 11,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'dec',
    title: 12,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'jan',
    title: 1,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'feb',
    title: 2,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'march',
    title: 3,
    editable: true,
    minWidth: 100,
    rightAlign: 'left',
    headerAlign: 'left',
    type: 'number',
  },
  {
    field: 'remark',
    title: 'Remark',
    editable: false,
    widthT: 250,
    autoAdjust: false,
  },
  {
    field: 'idFromApi',
    title: 'ID from API',
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
]
export const BusinessDemandElastomerJmdColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    width: 100,
    groupable: true,
    editable: false,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'normParameterId',
    title: 'Particulars',
    editable: false,
    width: 125,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },

  {
    field: 'displayName',
    title: 'Particulars',
    editable: false,
    widthT: 220,
    autoAdjust: false,
    locked: true,
  },
  {
    field: 'april',
    title: 'Value',
    editable: true,
    widthT: 120,
    rightAlign: true,
    headerAlign: 'left',
    type: 'number',
  },

  {
    field: 'remark',
    title: 'Remark',
    editable: false,
    widthT: 250,
    autoAdjust: false,
  },
  {
    field: 'idFromApi',
    title: 'ID from API',
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
]

export const SlowDownElastomerColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 200,
    autoAdjust: false,
    locked: true
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
    field: 'maintStartDateTime',
    title: 'SD- From',
    type: 'dateTime',
    editable: true,
    widthT: 150,
  },

  {
    field: 'maintEndDateTime',
    title: 'SD- To',
    type: 'dateTime',
    editable: true,
    widthT: 150,
  },

  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 140,
  },

  {
    field: 'rate',
    title: 'Rate (TPH)',
    editable: true,
    type: 'number',
    widthT: 120,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 200,
    autoAdjust: false,
  },
]
export const SlowDown_Elastomer_JMD_Columns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 260,
    autoAdjust: false,
    locked: true
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
    isVisible: false,
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
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 150,
    minWidth: 100,
  },

  {
    field: 'rate',
    title: 'Rate (TPH)',
    editable: true,
    type: 'number',
    widthT: 150,
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
export const SlowDownElastomerColumnsSBR = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 230,
    autoAdjust: false,
    minWidth: 100,
    locked: true
  },
  {
    field: 'productName1',
    title: 'Particulars',
    widthT: 120,
    editable: true,
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
    width: 150,
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
    title: 'Rate (TPH)',
    editable: true,
    type: 'number',
    widthT: 110,
    minWidth: 100,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 230,
    autoAdjust: false,
    minWidth: 100,
  },
]
export const SlowDownElastomerColumnsPBR3 = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 230,
    autoAdjust: false,
    minWidth: 100,
    locked: true
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
    isVisible: false,
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
    width: 150,
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
    title: 'Rate (TPH)',
    editable: true,
    type: 'number',
    widthT: 110,
    minWidth: 100,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 230,
    autoAdjust: false,
    minWidth: 100,
  },
]
export const NormalOpNormElastomerColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    width: 110,
    groupable: true,
    editable: false,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'materialFkId',
    title: 'Particulars',
    width: 100,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'productName',
    title: 'Particulars',
    width: 120,
    minWidth: 100,
  },

  {
    field: 'UOM',
    title: 'UOM / MT',
    width: 90,
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
    width: 100,
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
export const NormalOpNormElastomerJmdColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    width: 100,
    groupable: true,
    editable: false,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'materialFkId',
    title: 'Particulars',
    width: 100,
    hidden: true,
    isVisible: false,
    minWidth: 100,
  },
  {
    field: 'productName',
    title: 'Particulars',
    width: 120,
    minWidth: 100,
  },

  {
    field: 'UOM',
    title: 'UOM',
    width: 80,
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
    width: 100,
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
export const ShutdownConsumptionElastomerColumns = [
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
    width: 120,
    editable: false,
    minWidth: 130,
  },
  {
    field: 'UOM',
    headerName: 'UOM',
    width: 80,
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
      isDisabled: true,
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
    isVisible: false,
    minWidth: 100,
  },
]
export const SlowdownNormsElastomerColumns = [
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
    width: 120,
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
    minWidth: 100,
    headerName: 'UOM/MT',
    width: 90,
    editable: false,
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
      isDisabled: true,
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
    isVisible: false,
    minWidth: 100,
  },
]
export const SlowdownNormsElastomerJmdColumns = [
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
    width: 120,
    editable: false,
    minWidth: 130,
  },
  {
    field: 'UOM',
    headerName: 'UOM',
    minWidth: 100,
    width: 80,
    editable: false,
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
      isDisabled: true,
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
    isVisible: false,
    minWidth: 100,
  },
]

export const ConsumptionAopElastomerColumns = [
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
    isVisible: false,
    minWidth: 100,
  },
]
