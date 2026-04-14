export const ExclusionDateColumns = [
  {
    field: 'id',
    hidden: true,
  },
  {
    field: 'exclusionStartDate',
    title: 'From Date',
    editable: true,
    widthT: 150,
  },
  {
    field: 'exclusionEndDate',
    title: 'To Date',
    editable: true,
    widthT: 150,
  },

  {
    field: 'remark',
    title: 'Reason',
    editable: true,
    widthT: 250,
    autoAdjust: false
  },

  {
    field: 'originalRemark',
    hidden: true,
  },
]

export const ShutDownAllColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 260,
    autoAdjust: false
  },
  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
  },
  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
    widthT: 150,
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
    widthT: 150,
  },
  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 150,
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
    widthT: 250,
    autoAdjust: false
  },
]
export const ShutDown_Elastomer_JMD_IIR_Columns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 260,
    autoAdjust: false
  },
  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
  },
  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
    widthT: 150,
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
    widthT: 150,
  },
  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
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
export const ShutDown_Elastomer_JMD_HIIR_Columns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 260,
    autoAdjust: false
  },
  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
  },
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
export const SlowdownConfigColumns = [
  {
    field: 'description',
    title: 'Slowdown Desc',
    editable: true,
    widthT: 230,
    autoAdjust: false
  },

  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
    widthT: 140,
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
    widthT: 140,
  },
  {
    field: 'durationInHrs',
    title: 'Duration (Hrs)',
    editable: true,
    widthT: 140,
  },

  {
    field: 'rate',
    title: 'Rate',
    editable: true,
    widthT: 120,
  },
  {
    field: 'remarks',
    title: 'Remarks',
    editable: true,
    widthT: 200,
    autoAdjust: false
  },
]

// PE Shutdown Columns (adds productName)
export const ShutDownPeColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 250,
    autoAdjust: false
  },
  // {
  //   field: 'productName1',
  //   title: 'Particulars',
  //   editable: true,
  //   widthT: 130,
  // },
  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
  },
  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
    widthT: 150,
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
    widthT: 150,
  },
  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 150,
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
    widthT: 250,
    autoAdjust: false
  },
]
export const ShutDownPeColumnsldpe12 = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 260,
    autoAdjust: false
  },

  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
  },
  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
    widthT: 150,
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
    widthT: 150,
  },
  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 150,
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
    widthT: 250,
    autoAdjust: false
  },
]

// PP Shutdown Columns (same as PE)
export const ShutDownPpColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 270,
    autoAdjust: false
  },
  // {
  //   field: 'productName1',
  //   title: 'Particulars',
  //   editable: true,
  //   widthT: 130,
  // },
  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
  },
  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
    widthT: 150,
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
    widthT: 150,
  },
  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 150,
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
    widthT: 250,
    autoAdjust: false
  },
]
export const ShutDownPpDtaColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 225,
    autoAdjust: false
  },
  {
    field: 'lineId',
    title: 'Line',
    type: 'lineDropdown',
    editable: true,
    width: 100,
  },
  // {
  //   field: 'productName1',
  //   title: 'Particulars',
  //   editable: true,
  //   widthT: 130,
  // },
  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
  },
  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
    widthT: 150,
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
    widthT: 150,
  },
  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 150,
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
    widthT: 200,
    autoAdjust: false
  },
]

export const ShutDownPTAColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'discriptionDrpdwn',
    widthT: 260,
    autoAdjust: false
  },

  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
  },
  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
    widthT: 150,
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
    widthT: 150,
  },
  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
    widthT: 150,
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
    widthT: 250,
    autoAdjust: false
  },
]

export const ShutDownPTADMDColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'discriptionDrpdwn',
    widthT: 260,
    autoAdjust: false
  },

  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
  },
  // {
  //   field: 'maintStartDateTime',
  //   title: 'SD - From',
  //   editable: true,
  // },
  // {
  //   field: 'maintEndDateTime',
  //   title: 'SD - To',
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
    widthT: 150,
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
    widthT: 250,
    autoAdjust: false
  },
]
export const ShutDownChemicalColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 260,
    autoAdjust: false
  },

  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
  },
  // {
  //   field: 'maintStartDateTime',
  //   title: 'SD - From',
  //   editable: true,
  // },
  // {
  //   field: 'maintEndDateTime',
  //   title: 'SD - To',
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
    widthT: 150,
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
    widthT: 250,
    autoAdjust: false
  },
]
