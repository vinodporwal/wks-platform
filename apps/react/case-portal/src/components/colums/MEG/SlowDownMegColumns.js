export const SlowDownMegColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
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
    field: 'rateEOE',
    title: 'Rate (EOE)',
    editable: true,
    type: 'number',
  },
  {
    field: 'rateEO',
    title: 'Rate (EO)',
    editable: true,
    type: 'number',
  },

  {
    field: 'remark',
    title: 'Remarks',
    editable: true,
  },
]
