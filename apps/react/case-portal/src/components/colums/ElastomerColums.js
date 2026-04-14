export const BusinessDemandElastomerColumns = [
  {
    field: 'Particulars',
    title: 'Type',
    groupable: true,
    editable: false,
    hidden: true,
  },
  {
    field: 'normParameterId',
    title: 'Particulars',
    editable: false,
    hidden: true,
  },

  {
    field: 'displayName',
    title: 'Particulars',
    editable: false,
    width: 125,
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
    editable: false,
    widthT: 250,
    autoAdjust: false
  },
  {
    field: 'idFromApi',
    title: 'ID from API',
    hidden: true,
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
  },
  {
    field: 'normParameterId',
    title: 'Particulars',
    editable: false,
    width: 125,
    hidden: true,
  },

  {
    field: 'displayName',
    title: 'Particulars',
    editable: false,
    widthT: 220,
    autoAdjust: false
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
    autoAdjust: false
  },
  {
    field: 'idFromApi',
    title: 'ID from API',
    hidden: true,
  },
]

export const SlowDownElastomerColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 200,
    autoAdjust: false
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
    autoAdjust: false
  },
]
export const SlowDown_Elastomer_JMD_Columns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 260,
    autoAdjust: false
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
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
  },

  {
    field: 'rate',
    title: 'Rate (TPH)',
    editable: true,
    type: 'number',
    widthT: 150,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 250,
    autoAdjust: false
  },
]
export const SlowDownElastomerColumnsSBR = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 230,
    autoAdjust: false
  },
  {
    field: 'productName1',
    title: 'Particulars',
    widthT: 120,
    editable: true,
    autoAdjust: false
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
    widthT: 140,
  },

  {
    field: 'maintEndDateTime',
    title: 'SD- To',
    type: 'dateTime',
    editable: true,
    widthT: 140,
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
    widthT: 110,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 230,
    autoAdjust: false
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
    width: 120,
  },

  {
    field: 'UOM',
    title: 'UOM / MT',
    width: 90,
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
export const NormalOpNormElastomerJmdColumns = [
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
    width: 120,
  },

  {
    field: 'UOM',
    title: 'UOM',
    width: 80,
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
export const ShutdownConsumptionElastomerColumns = [
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
export const SlowdownNormsElastomerColumns = [
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
    width: 120,
  },
  {
    field: 'productName',
    headerName: 'Particulars',
    width: 120,
    editable: false,
  },
  { field: 'UOM', headerName: 'UOM / MT', width: 90, editable: false },

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
export const SlowdownNormsElastomerJmdColumns = [
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

export const ConsumptionAopElastomerColumns = [
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
