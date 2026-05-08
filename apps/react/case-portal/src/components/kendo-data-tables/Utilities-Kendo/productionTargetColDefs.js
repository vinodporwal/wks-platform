export function getColDefsPercentageSummary(headerMap = {}, valueFormat) {
  return [
    { field: 'idFromApi', title: 'ID', hidden: true, isVisible: false },
    {
      field: 'aopCaseId',
      title: 'Case ID',
      editable: false,
      hidden: true,
      isVisible: false,
      minWidth: 100,
    },
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: false,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 120,
      editable: false,
      minWidth: 120,
    },
    ...generateMonthColumnsFixedWidth(headerMap, false, valueFormat),
    {
      field: 'avgTph',
      title: 'AVG',
      editable: false,
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
    //add here
  ]
}
export function getColDefsPercentageSummaryElastomerJMD(
  headerMap = {},
  valueFormat,
) {
  return [
    {
      field: 'idFromApi',
      title: 'ID',
      hidden: true,
      isVisible: false,
      minWidth: 100,
    },
    {
      field: 'aopCaseId',
      title: 'Case ID',
      editable: false,
      hidden: true,
      isVisible: false,
      minWidth: 100,
    },
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 200,
      editable: false,
      autoAdjust: false,
      minWidth: 120,
    },
    {
      field: 'april',
      title: 'Value',
      widthT: 100,
      editable: false,
      type: 'number',
      format: valueFormat,
      align: 'left',
      headerAlign: 'left',
      minWidth: 100,
    },
    {
      field: 'avgTph',
      title: 'AVG',
      editable: false,
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
}
export function getColDefsPercentageSummaryPEPP(headerMap = {}, valueFormat) {
  return [
    { field: 'idFromApi', title: 'ID', hidden: true, isVisible: false },
    {
      field: 'aopCaseId',
      title: 'Case ID',
      editable: false,
      hidden: true,
      isVisible: false,
      minWidth: 100,
    },
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: false,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 120,
      editable: false,
      minWidth: 120,
    },
    ...generateMonthColumnsPercentageSummaryPPE(headerMap, false, valueFormat),
    {
      field: 'avgTph',
      title: 'AVG',
      editable: false,
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
    //add here
  ]
}

export function getColDefsDesignCapacity(headerMap = {}, valueFormat) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: true,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 120,
      editable: false,
      minWidth: 120,
    },
    ...generateMonthColumns(headerMap, true, valueFormat),
    {
      field: 'remarks',
      title: 'Remark',
      editable: true,
      align: 'left',
      headerAlign: 'left',
      widthT: 100,
      minWidth: 100,
    },
  ]
}

export function getColDefsDesignCapacityPTA(headerMap = {}, valueFormat) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: true,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 120,
      editable: false,
      minWidth: 120,
    },
    ...generateMonthColumnsPTA(headerMap, true, valueFormat),
    {
      field: 'remarks',
      title: 'Remark',
      editable: true,
      align: 'left',
      headerAlign: 'left',
      widthT: 100,
      minWidth: 100,
    },
  ]
}
export function getColDefsDesignCapacityPTADMD(headerMap = {}, valueFormat) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: true,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 120,
      editable: false,
      minWidth: 120,
    },
    ...generateMonthColumnsPTA(headerMap, true, valueFormat),
    // {
    //   field: 'remarks',
    //   title: 'Remark',
    //   editable: true,
    //   align: 'left',
    //   headerAlign: 'left',
    //   widthT: 90,
    // },
  ]
}
export function getColDefsDesignCapacityAROMATICS(headerMap = {}, valueFormat) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: true,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 200,
      editable: false,
      autoAdjust: false,
      minWidth: 120,
    },
    {
      field: 'april',
      title: 'PAREX#1',
      editable: true,
      align: 'left',
      widthT: 110,
      headerAlign: 'left',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'may',
      title: 'PAREX#2',
      editable: true,
      align: 'left',
      widthT: 110,
      headerAlign: 'left',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'june',
      title: 'PAREX#3',
      editable: true,
      align: 'left',
      widthT: 110,
      headerAlign: 'left',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'total',
      title: 'Total',
      editable: false,
      align: 'left',
      widthT: 110,
      headerAlign: 'left',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'remarks',
      title: 'Remark',
      editable: true,
      align: 'left',
      headerAlign: 'left',
      autoAdjust: false,
      minWidth: 100,
    },
  ]
}
export function getColDefsDesignCapacityELASTOMERJMD(
  headerMap = {},
  valueFormat,
) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: true,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 250,
      editable: false,
      autoAdjust: false,
      minWidth: 120,
    },
    {
      field: 'april',
      title: 'Value',
      editable: false,
      align: 'left',
      widthT: 100,
      headerAlign: 'left',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
  ]
}

export function getColDefsDesignCapacityPEPP(headerMap = {}, valueFormat) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: false,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 250,
      editable: false,
      minWidth: 120,
    },
    ...generateMonthColumnsForPEPP(headerMap, false, valueFormat, true),
  ]
}

export function getColDefsMaxAchievedCapacity(headerMap = {}, valueFormat) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: true,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 120,
      editable: false,
      minWidth: 120,
    },
    ...generateMonthColumnsFixedWidth(headerMap, true, valueFormat),
  ]
}

export function getColDefsMaxAchievedCapacityPTA(headerMap = {}, valueFormat) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: true,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 120,
      editable: false,
      minWidth: 120,
    },
    ...generateMonthColumnsFixedWidthPTA(headerMap, true, valueFormat),
  ]
}

export function getColDefsMaxAchievedCapacityPEPP(headerMap = {}, valueFormat) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: true,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 120,
      editable: false,
      minWidth: 120,
    },
    ...generateMonthColumnsFixedWidthPEPP(headerMap, true, valueFormat),
  ]
}

export function getColDefsMaxAchievedCapacityAROMATICS(
  headerMap = {},
  valueFormat,
) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: true,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 200,
      editable: false,
      autoAdjust: false,
      minWidth: 120,
    },
    {
      field: 'april',
      title: 'PAREX#1',
      align: 'left',
      widthT: 110,
      editable: true,
      headerAlign: 'left',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'may',
      title: 'PAREX#2',
      align: 'left',
      widthT: 110,
      editable: true,
      headerAlign: 'left',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'june',
      title: 'PAREX#3',
      editable: true,
      widthT: 110,
      align: 'left',
      headerAlign: 'left',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'total',
      title: 'Total',
      editable: false,
      align: 'left',
      widthT: 110,
      headerAlign: 'left',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
    {
      field: 'remarks',
      title: 'Remark',
      editable: true,
      align: 'left',
      headerAlign: 'left',
      autoAdjust: false,
      minWidth: 100,
    },
  ]
}
export function getColDefsMaxAchievedCapacityELASTOMERJMD(
  headerMap = {},
  valueFormat,
) {
  return [
    {
      field: 'materialFKId',
      title: 'Particulars',
      widthT: 100,
      editable: true,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 250,
      editable: false,
      autoAdjust: false,
      minWidth: 120,
    },
    {
      field: 'april',
      title: 'Value',
      align: 'left',
      widthT: 100,
      editable: false,
      headerAlign: 'left',
      type: 'number',
      format: valueFormat,
      minWidth: 100,
    },
  ]
}

export function getColDefsNonEditable(headerMap = {}, valueFormat) {
  return [
    {
      field: 'idFromApi',
      title: 'ID',
      hidden: true,
      isVisible: false,
      minWidth: 100,
    },
    {
      field: 'aopCaseId',
      title: 'Case ID',
      hidden: true,
      isVisible: false,
      minWidth: 100,
    },
    {
      field: 'normParametersFKId',
      title: 'Particulars',
      widthT: 100,
      editable: false,
      hidden: true,
      isVisible: false,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 120,
      editable: false,
      minWidth: 120,
    },
    ...generateMonthColumns(headerMap, false, valueFormat),
    {
      field: 'avgTph',
      title: 'AVG',
      editable: false,
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
}

function generateMonthColumns(
  headerMap = {},
  editable = true,
  valueFormat,
  isPEPP,
) {
  const monthOrder = [4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3]

  return monthOrder.map((month) => {
    const monthName = getMonthName(month)

    return {
      field: getMonthName(month).toLowerCase(),
      title: headerMap[month],
      format: valueFormat,
      editable,
      align: 'left',
      headerAlign: 'left',
      type: 'number',
      widthT: monthName === 'March' ? (isPEPP ? 200 : 110) : undefined,
      minWidth: 100,
    }
  })
}

function generateMonthColumnsPTA(
  headerMap = {},
  editable = true,
  valueFormat,
  isPEPP,
) {
  const monthOrder = [4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3]

  return monthOrder.map((month) => {
    const fullMonthName = getMonthName(month)
    const monthName = getMonthName(month)
    const monthNameTitle = fullMonthName.slice(0, 3) // Jan, Feb, Mar...

    return {
      field: getMonthName(month).toLowerCase(),
      title: monthNameTitle,
      format: valueFormat,
      editable,
      align: 'left',
      headerAlign: 'left',
      type: 'number',
      widthT: monthName === 'March' ? (isPEPP ? 200 : 110) : undefined,
      minWidth: 100,
    }
  })
}

function generateMonthColumnsForPEPP(
  headerMap = {},
  editable = true,
  valueFormat,
  isPEPP,
) {
  const monthOrder = [4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3]

  return monthOrder.map((month) => {
    const fullMonthName = getMonthName(month)
    const monthName = getMonthName(month)
    const monthNameTitle = fullMonthName.slice(0, 3) // Jan, Feb, Mar...

    return {
      field: monthName.toLowerCase(),
      title: monthNameTitle,
      format: valueFormat,
      editable,
      align: 'left',
      headerAlign: 'left',
      type: 'number',
      minWidth: 100,
      // widthT: fullMonthName === 'March' ? (isPEPP ? 200 : 110) : undefined,
    }
  })
}

function generateMonthColumnsFixedWidth(
  headerMap = {},
  editable = true,
  valueFormat,
) {
  const monthOrder = [4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3]

  return monthOrder.map((month) => {
    const monthName = getMonthName(month)

    return {
      field: monthName.toLowerCase(),
      title: headerMap[month],
      format: valueFormat,
      editable,
      align: 'left',
      headerAlign: 'left',
      type: 'number',
      widthT: monthName === 'March' ? 200 : undefined,
      minWidth: 100,
    }
  })
}

function generateMonthColumnsFixedWidthPTA(
  headerMap = {},
  editable = true,
  valueFormat,
) {
  const monthOrder = [4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3]

  return monthOrder.map((month) => {
    const fullMonthName = getMonthName(month)
    const monthName = getMonthName(month)
    const monthNameTitle = fullMonthName.slice(0, 3) // Jan, Feb, Mar...

    return {
      field: monthName.toLowerCase(),
      title: monthNameTitle,
      format: valueFormat,
      editable,
      align: 'left',
      headerAlign: 'left',
      type: 'number',
      widthT: monthName === 'March' ? 200 : undefined,
      minWidth: 100,
    }
  })
}

function generateMonthColumnsFixedWidthPEPP(
  headerMap = {},
  editable = true,
  valueFormat,
) {
  const monthOrder = [4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3]

  return monthOrder.map((month) => {
    const fullMonthName = getMonthName(month)
    const monthName = getMonthName(month)
    const monthNameTitle = fullMonthName.slice(0, 3) // Jan, Feb, Mar...

    return {
      field: monthName.toLowerCase(),
      title: monthNameTitle,
      format: valueFormat,
      editable,
      align: 'left',
      headerAlign: 'left',
      type: 'number',
      minWidth: 100,
      //widthT: monthName === 'March' ? 200 : undefined,
    }
  })
}

function generateMonthColumnsPercentageSummaryPPE(
  headerMap = {},
  editable = true,
  valueFormat,
) {
  const monthOrder = [4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3]

  return monthOrder.map((month) => {
    const monthName = getMonthName(month)

    return {
      field: monthName.toLowerCase(),
      title: headerMap[month],
      format: valueFormat,
      editable,
      align: 'left',
      headerAlign: 'left',
      type: 'number',
      minWidth: 100,
      //widthT: monthName === 'March' ? 200 : undefined,
    }
  })
}

function getMonthName(num) {
  const months = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ]
  return months[num - 1]
}
