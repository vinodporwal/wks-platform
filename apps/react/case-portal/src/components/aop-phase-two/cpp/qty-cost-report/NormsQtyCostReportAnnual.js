import { useEffect, useMemo, useState } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from 'components/aop-phase-two/common/ValueFormatterPhaseTwo'
import { UtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/utilityPlantApiServiceV2'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateExcelName } from '../../common/utilities/excelNameUtil'
import { generateHeaderNames } from 'components/aop-phase-two/common/utilities/generateHeaders'

const NormsQtyCostReportAnnual = () => {
  const keycloak = useSession()
  // State management
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year, screenTitle } =
    dataGridStore
  const PLANT_ID = plantObject?.id

  const AOP_YEAR = year?.selectedYear

  const EXCEL_NAME = generateExcelName(
    dataGridStore,
    'NORMS_QTY_COST_REPORT_ANNUAL',
  )
  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = ValueFormatterPhaseTwo()
  const valueFormatTwo = customValueFormatterPhaseTwo(2)

  // Quarterly and Annual columns
  const QUARTERLY_COLUMNS = [
    { quarter: 'q1', title: 'Q1 (Apr-Jun)' },
    { quarter: 'q2', title: 'Q2 (Jul-Sep)' },
    { quarter: 'q3', title: 'Q3 (Oct-Dec)' },
    { quarter: 'q4', title: 'Q4 (Jan-Mar)' },
  ]
    .map((q) => ({
      title: q.title,
      children: [
        {
          field: `${q.quarter}.qty`,
          title: 'Gen. Quantity',
          widthT: 130,
          minWidth: 130,
          type: 'number1',
          format: valueFormatTwo,
          editable: false,
        },
        {
          field: `${q.quarter}.norms`,
          title: 'Norms',
          widthT: 130,
          minWidth: 130,
          editable: false,
          type: 'number1',
          format: valueFormat,
        },
        {
          field: `${q.quarter}.quantity`,
          title: 'Quantity',
          widthT: 130,
          minWidth: 130,
          type: 'number1',
          format: valueFormatTwo,
          editable: false,
        },
        {
          field: `${q.quarter}.price`,
          title: 'Price',
          widthT: 130,
          minWidth: 130,
          editable: false,
          type: 'number1',
          format: valueFormatTwo,
        },
        {
          field: `${q.quarter}.amount`,
          title: 'Amount',
          widthT: 130,
          minWidth: 130,
          type: 'number1',
          format: valueFormatTwo,
          editable: false,
        },
      ],
    }))
    .concat([
      {
        title: 'Annual',
        children: [
          {
            field: 'annual.qty',
            title: 'Gen. Quantity',
            widthT: 130,
            minWidth: 130,
            type: 'number1',
            format: valueFormat,
            editable: false,
          },
          {
            field: 'annual.norms',
            title: 'Norms',
            widthT: 130,
            minWidth: 130,
            editable: false,
            type: 'number1',
            format: valueFormat,
          },
          {
            field: 'annual.quantity',
            title: 'Quantity',
            widthT: 130,
            minWidth: 130,
            type: 'number1',
            format: valueFormat,
            editable: false,
          },
          {
            field: 'annual.price',
            title: 'Price',
            widthT: 130,
            minWidth: 130,
            editable: false,
            type: 'number1',
            format: valueFormat,
          },
          {
            field: 'annual.amount',
            title: 'Amount',
            widthT: 130,
            minWidth: 130,
            type: 'number1',
            format: valueFormat,
            editable: false,
          },
        ],
      },
    ])

  const [rows, setRows] = useState([])

  // Base columns (common to all views)
  const baseColumns = [
    //Generating Plant
    {
      field: 'generatingPlantName',
      title: 'Generating Plant',
      widthT: 180,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 180,
    },
    //Utility
    {
      field: 'utilityName',
      title: 'Utility',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: false,
      minWidth: 120,
    },
    // Utility ID
    {
      field: 'utilityId',
      title: 'Utility ID',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: false,
      minWidth: 120,
    },
    //UOM
    {
      field: 'uom',
      title: 'Generation UOM',
      widthT: 180,
      type: 'text',
      editable: false,
      minWidth: 180,
    },
    // Account
    {
      field: 'accountName',
      title: 'Account',
      widthT: 120,
      type: 'text',
      editable: false,
      minWidth: 120,
    },
    // Material
    {
      field: 'materialName',
      title: 'Material',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 120,
    },
    // SAP Code
    {
      field: 'materialId',
      title: 'SAP Code',
      widthT: 120,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 120,
    },
    // Issuing Plant
    {
      field: 'issuingPlantName',
      title: 'Issuing Plant',
      widthT: 150,
      type: 'text',
      editable: false,
      minWidth: 150,
    },
    {
      field: 'issuingUom',
      title: 'Issuing UOM',
      widthT: 150,
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 150,
    },
  ]

  // Column definitions for quarterly and annual view
  const quarterlyColumns = [
    ...baseColumns,
    // Quarterly and Annual columns
    ...QUARTERLY_COLUMNS,
  ]

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchNormsData()
    }
  }, [PLANT_ID, AOP_YEAR])

  const fetchNormsData = async () => {
    setLoading(true)
    try {
      const res =
        await UtilityPlantApiServiceV2.getNormBasedUtilityBudgetSummary(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      console.log('API Response:', res)

      if (res?.data?.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }

      let tempRes = res?.data?.map((item, index) => {
        return {
          ...item,
          id: item.id || index + 1,
        }
      })

      console.log('Fetched data:', tempRes)
      setRows(tempRes)
    } catch (error) {
      console.error('Error fetching norm-based utility budget data:', error)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

  // Permissions for quarterly and annual view (read-only)
  const permissions = useMemo(() => {
    return {
      showAction: true,
      addButton: false,
      deleteButton: false,
      editButton: false,
      saveBtn: false,
      allAction: true,
      showTitleNameBusiness: true,
      titleName: screenTitle?.title,
      showImport: false,
      showTitle: true,
      showExport: true,
      ExcelName: EXCEL_NAME,
    }
  }, [])

  const handleExport = async () => {
    console.log('handleExport called')
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'info',
    })

    try {
      await UtilityPlantApiServiceV2.exportNormBasedUtilityBudgetSummary(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )
      setSnackbarData({
        message: 'Excel download completed successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting Norms data:', error)
      setSnackbarData({
        message: 'Excel download failed. Please try again.',
        severity: 'error',
      })
    }
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        columns={quarterlyColumns}
        rows={rows}
        setRows={setRows}
        title='Norms Qty. Cost Report - Quarterly & Annual'
        permissions={permissions}
        handleExport={handleExport}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        customHeight={80}
        groupBy={['generatingPlantName']}
      />
    </Box>
  )
}

export default NormsQtyCostReportAnnual
