export const SlowDownChemicalhmdColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    type: 'discriptionDrpdwn',
    minWidth: 200,
    locked: true,
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
    editable: true,
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

export const ShutDownChemicalDropdownColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'discriptionDrpdwn',
    minWidth: 200,
    locked: true,
  },

  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
    isVisible: false,
  },
  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
  },
  // {
  //   field: 'monthly',
  //   title: 'Month',
  //   type: 'monthDropdownPEPP',
  //   editable: true,
  //   width: 150,
  // },
  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
  },
]
