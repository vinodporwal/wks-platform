import { useState, useEffect, useCallback, useMemo } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { customValueFormatterPhaseTwo } from '../../common/ValueFormatterPhaseTwo'
import { PCGShutdownTaApiService } from 'components/aop-phase-two/services/pcg/pcgShutdownTaApiService'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'

const AOPProductionNormsPCG = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, screenTitle } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const EXCEL_NAME = generateExcelName(
    dataGridStore,
    'AOP_Production_Norms',
  )

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const valueFormat = customValueFormatterPhaseTwo(4)

  const columns = useMemo(
    () => [
      {
        field: 'particulars',
        title: 'Particulars',
        widthT: 220,
        minWidth: 180,
        type: 'text',
        editable: false,
      },
      {
        field: 'value',
        title: 'Value',
        widthT: 120,
        minWidth: 100,
        type: 'number1',
        editable: false,
        format: valueFormat,
      },
    ],
    [valueFormat],
  )

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setRows([])
    setLoading(true)
    try {
      const response = await PCGShutdownTaApiService.getAopProductionNorms(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const arr = Array.isArray(response)
        ? response
        : response?.data || []

      const formattedData = arr.map((item, index) => ({
        ...item,
        id: index,
        particulars: item?.particulars || '',
        value: item?.value != null ? Number(item.value) : '',
        isEditable: false,
      }))

      setRows(formattedData)
    } catch (error) {
      console.error('Error fetching aop production norms:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const permissions = {
    showAction: false,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: false,
    allAction: true,
    showExport: false,
    downloadExcelBtnFromUI: true,
    ExcelName: EXCEL_NAME,
    showImport: false,
    showCalculate: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: screenTitle?.title || 'AOP Production Norms',
    showDropdown: false,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        customHeight={70}
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

export default AOPProductionNormsPCG
