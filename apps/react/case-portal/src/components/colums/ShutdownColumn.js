export const ExclusionDateColumns = [
  {
    field: 'id',
    hidden: true,
    isVisible: false,
  },
  {
    field: 'exclusionStartDate',
    title: 'From Date',
    editable: true,
    fixedWidth: '200px',
  },
  {
    field: 'exclusionEndDate',
    title: 'To Date',
    editable: true,
    fixedWidth: '200px',
  },

  {
    field: 'remark',
    title: 'Reason',
    editable: true,
    fixedWidth: '200px',
  },

  {
    field: 'originalRemark',
    hidden: true,
    isVisible: false,
  },
]

export const ShutDownAllColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 350,
    autoAdjust: false,
    minWidth: 350,
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
    autoAdjust: false,
  },
]
export const ShutDown_Elastomer_JMD_IIR_Columns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 260,
    autoAdjust: false,
    minWidth: 350,
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
    autoAdjust: false,
  },
]
export const ShutDown_Elastomer_JMD_HIIR_Columns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 260,
    autoAdjust: false,
    minWidth: 350,
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
    autoAdjust: false,
  },
]
export const SlowdownConfigColumns = [
  {
    field: 'description',
    title: 'Slowdown Desc',
    editable: true,
    minWidth: 230,
    autoAdjust: false,
    locked: true,
  },

  {
    field: 'maintStartDateTime',
    title: 'SD - From',
    editable: true,
    minWidth: 100,
  },
  {
    field: 'maintEndDateTime',
    title: 'SD - To',
    editable: true,
    minWidth: 100,
  },
  {
    field: 'durationInHrs',
    title: 'Duration (Hrs)',
    editable: true,
    minWidth: 100,
  },

  {
    field: 'rate',
    title: 'Rate',
    editable: true,
    minWidth: 100,
  },
  {
    field: 'remarks',
    title: 'Remarks',
    editable: true,
    minWidth: 200,
    autoAdjust: false,
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
    autoAdjust: false,
    minWidth: 350,
    locked: true,
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
    isVisible: false,
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
  },
]
export const ShutDownPeC2Columns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
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
  {
    field: 'durationInHrs',
    title: 'Duration (hrs)',
    editable: true,
  },
  {
    field: 'shutdownRate',
    title: 'Shutdown Type',
    type: 'shutdownRateDropdown',
    editable: true,
    minWidth: 200,
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
  },
]
export const ShutDownPeColumnsldpe12 = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
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

// PP Shutdown Columns (same as PE)
export const ShutDownPpColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    locked: true,
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
export const ShutDownPpDtaColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    widthT: 225,
    autoAdjust: false,
    minWidth: 350,
    locked: true,
  },
  {
    field: 'lineDisplayName',
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
    isVisible: false,
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
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
  },
]
export const ShutDownPVCDMDColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
    locked: true,
  },
  {
    field: 'lineId',
    title: 'Line',
    type: 'lineDropdown',
    editable: true,
    width: 130,
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

export const ShutDownPTAColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'discriptionDrpdwn',
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

export const ShutDownPTADMDColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'discriptionDrpdwn',
    locked: true,
  },

  {
    field: 'maintenanceId',
    title: 'Maintenance ID',
    editable: false,
    hidden: true,
    isVisible: false,
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
  },
  {
    field: 'remark',
    title: 'Shutdown Basis',
    editable: true,
  },
]
export const ShutDownChemicalColumns = [
  {
    field: 'discription',
    title: 'Shutdown Desc',
    editable: true,
    type: 'descLimit',
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
