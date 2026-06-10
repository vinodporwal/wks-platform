export const SlowDownVcmColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    type: 'discriptionDrpdwn',
    minWidth: 200,
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
    minWidth: 200,
    isVisible: false,
  },

  {
    field: 'maintStartDateTime',
    title: 'SD- From',
    type: 'dateTime',
    editable: true,
    minWidth: 100,
  },

  {
    field: 'maintEndDateTime',
    title: 'SD- To',
    type: 'dateTime',
    editable: true,
    minWidth: 100,
  },

  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    minWidth: 100,
  },

  {
    field: 'rate',
    title: 'Rate (TPH)',
    editable: true,
    type: 'number',
    minWidth: 100,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
  },
]
export const SlowDownVcmhmdColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    type: 'discriptionDrpdwn',
    minWidth: 200,
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
    minWidth: 200,
    isVisible: false,
  },

  {
    field: 'maintStartDateTime',
    title: 'SD- From',
    type: 'dateTime',
    editable: true,
    minWidth: 200,
  },

  {
    field: 'maintEndDateTime',
    title: 'SD- To',
    type: 'dateTime',
    editable: true,
    minWidth: 200,
  },

  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    minWidth: 200,
  },

  {
    field: 'rate',
    title: 'Rate (TPH)',
    editable: false,
    isDisabled: true,
    type: 'number',
    minWidth: 200,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    minWidth: 200,
  },
]

export const SlowDownDmdVcmColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    minWidth: 200,
  },

  {
    field: 'maintenanceId',
    title: 'maintenanceId',
    editable: false,
    hidden: true,
    isVisible: false,
  },

  {
    field: 'maintStartDateTime',
    title: 'SD- From',
    type: 'dateTime',
    editable: true,
    minWidth: 100,
  },

  {
    field: 'maintEndDateTime',
    title: 'SD- To',
    type: 'dateTime',
    editable: true,
    minWidth: 100,
  },

  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    minWidth: 100,
  },

  {
    field: 'rate',
    title: 'Rate (TPH)',
    editable: true,
    type: 'number',
    minWidth: 100,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    minWidth: 100,
  },
]
export const ShutdownConsumptionVcmColumns = [
  {
    field: 'Particulars',
    headerName: 'Type',
    width: 120,
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
  },
  {
    field: 'productName',
    headerName: 'Particulars',
    minWidth: 180,
    editable: false,
  },
  // { field: 'UOM/MT', headerName: 'UOM', width: 150, editable: false },
  { field: 'UOM', headerName: 'UOM/MT', minWidth: 80, editable: false },

  ...Array.from({ length: 12 }, (_, i) => {
    const monthIndex = (i + 4) % 12 || 12
    const monthField = new Date(2000, monthIndex - 1)
      .toLocaleString('en-US', { month: 'long' })
      .toLowerCase()

    return {
      field: monthField,
      width: 120,
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
    width: 120,
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
