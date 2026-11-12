import { useMemo } from 'react'
import { Box } from '@mui/material'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { customValueFormatterPhaseTwo } from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

// Read-only USD view of the JMD Fuel Cost grid. Receives the already-fetched
// INR rows + exchange rate from the parent — no API calls here. USD = INR / rate
// (rate is "1$ = X INR").
const FuelAvailabilityCostUSD = ({ rows = [], exchangeRate }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { year } = dataGridStore
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(dataGridStore, 'Fuel_Availability_Cost_USD')

  const headerMap = generateHeaderNames(AOP_YEAR)
  // 4 decimals — 2 decimals loses too much precision (e.g. 34/93 = 0.3656
  // rounds to 0.37, and 93 * 0.37 = 34.41 instead of ~34).
  const valueFormat = customValueFormatterPhaseTwo(4)

  const rate = useMemo(() => {
    const parsed = parseFloat(exchangeRate)
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null
  }, [exchangeRate])

  const usdRows = useMemo(() => {
    if (!rate) return []
    return rows.map((row) => {
      const converted = { ...row }
      Object.keys(row).forEach((key) => {
        if (key.startsWith('cost') && typeof row[key] === 'number') {
          converted[key] = Math.round((row[key] / rate) * 10000) / 10000
        }
      })
      return converted
    })
  }, [rows, rate])

  const MONTH_TO_INDEX = {
    apr: 4,
    may: 5,
    jun: 6,
    jul: 7,
    aug: 8,
    sep: 9,
    oct: 10,
    nov: 11,
    dec: 12,
    jan: 1,
    feb: 2,
    mar: 3,
  }

  const MONTH_FIELDS = [
    'apr',
    'may',
    'jun',
    'jul',
    'aug',
    'sep',
    'oct',
    'nov',
    'dec',
    'jan',
    'feb',
    'mar',
  ]

  const COST_FIELD = (mon) =>
    `cost${mon.charAt(0).toUpperCase()}${mon.slice(1)}`

  const readonlyCol = (field, title, minWidth, extra = {}) => ({
    field,
    title,
    type: 'text',
    editable: false,
    minWidth,
    ...extra,
  })

  const monthBaseColumnConfig = {
    editable: false,
    widthT: 100,
    minWidth: 100,
    align: 'left',
    headerAlign: 'left',
    type: 'number1',
    format: valueFormat,
  }

  const MONTH_COST_COLUMNS = MONTH_FIELDS.map((mon) => ({
    ...monthBaseColumnConfig,
    field: COST_FIELD(mon),
    title: headerMap[MONTH_TO_INDEX[mon]],
  }))

  const columns = [
    readonlyCol('cppPlantName', 'CPP Plant', 120),
    readonlyCol('fuelDisplayName', 'Fuel', 200),
    ...MONTH_COST_COLUMNS,
    readonlyCol('remarks', 'Remarks', 250),
  ]

  const permissions = {
    showAction: false,
    addButton: false,
    editButton: false,
    saveBtn: false,
    allAction: true,
    showExport: false,
    downloadExcelBtnFromUI: true,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    ExcelName: EXCEL_NAME,
    titleName: `Fuel Cost (In $/MMBTU${rate ? `, 1$ = ${exchangeRate} INR` : ''})`,
  }

  return (
    <Box>
      <AdvanceKendoTable
        columns={columns}
        rows={usdRows}
        setRows={() => {}}
        modifiedCells={{}}
        setModifiedCells={() => {}}
        title={permissions.titleName}
        permissions={permissions}
        groupBy={['cppPlantName']}
        initialFieldValues={{}}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />
    </Box>
  )
}

export default FuelAvailabilityCostUSD
