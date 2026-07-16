export const SlowDownAromaticsColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 200,
    autoAdjust: false,
    locked: true,
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
    widthT: 120,
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
    widthT: 230,
    autoAdjust: false,
  },
]
export const ShutDownAROMATICSHMDColumns = [
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
