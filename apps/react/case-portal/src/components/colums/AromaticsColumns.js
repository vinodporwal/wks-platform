export const SlowDownAromaticsColumns = [
  {
    field: 'discription',
    title: 'Slowdown Desc',
    editable: true,
    type: 'descLimit',
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
    autoAdjust: false
  },
]