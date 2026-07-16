import React, { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { BusinessDemandApiService } from '../../services/polyester/businessDemandApiService'

const ProductionVolumeReferenceGrid = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const headerMap = generateHeaderNames(AOP_YEAR)
  const valueFormat = ValueFormatterProduction()

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [selectedUnit, setSelectedUnit] = useState('TPH')
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const monthsConfig = [
    { field: 'april', key: 4, title: 'April' },
    { field: 'may', key: 5, title: 'May' },
    { field: 'june', key: 6, title: 'June' },
    { field: 'july', key: 7, title: 'July' },
    { field: 'august', key: 8, title: 'August' },
    { field: 'september', key: 9, title: 'September' },
    { field: 'october', key: 10, title: 'October' },
    { field: 'november', key: 11, title: 'November' },
    { field: 'december', key: 12, title: 'December' },
    { field: 'january', key: 1, title: 'January' },
    { field: 'february', key: 2, title: 'February' },
    { field: 'march', key: 3, title: 'March' },
  ]

  const columns = [
    {
      field: 'Particulars',
      title: 'Type',
      editable: false,
      hidden: true,
      minWidth: 100,
    },
    {
      field: 'productName',
      title: 'Particulars',
      editable: false,
      minWidth: 200,
    },
    ...monthsConfig.map((m) => ({
      field: m.field,
      title: headerMap[m.key] || m.title,
      editable: false,
      type: 'number1',
      format: valueFormat,
      minWidth: 110,
    })),
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await BusinessDemandApiService.getProductionTarget(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (
        response?.code === 200 &&
        response?.data?.aopMCCalculatedDataDTOList
      ) {
        const isTPD = selectedUnit === 'TPD'
        const formattedData = response.data.aopMCCalculatedDataDTOList.map(
          (item, index) => ({
            ...item,
            idFromApi: item?.id || null,
            normParametersFKId: item?.materialFKId?.toLowerCase(),
            id: index,
            Particulars: item?.materialDisplayName,
            productName: item?.materialDisplayName,
            isEditable: false,
            april: isTPD && item.april ? item.april * 24 : item.april || null,
            may: isTPD && item.may ? item.may * 24 : item.may || null,
            june: isTPD && item.june ? item.june * 24 : item.june || null,
            july: isTPD && item.july ? item.july * 24 : item.july || null,
            august:
              isTPD && item.august ? item.august * 24 : item.august || null,
            september:
              isTPD && item.september
                ? item.september * 24
                : item.september || null,
            october:
              isTPD && item.october ? item.october * 24 : item.october || null,
            november:
              isTPD && item.november
                ? item.november * 24
                : item.november || null,
            december:
              isTPD && item.december
                ? item.december * 24
                : item.december || null,
            january:
              isTPD && item.january ? item.january * 24 : item.january || null,
            february:
              isTPD && item.february
                ? item.february * 24
                : item.february || null,
            march: isTPD && item.march ? item.march * 24 : item.march || null,
          }),
        )
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching reference production target data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, selectedUnit])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const handleUnitChange = (unit) => {
    setSelectedUnit(unit)
  }

  const permissions = {
    showAction: false,
    addButton: false,
    deleteButton: false,
    editButton: false,
    showUnit: true,
    units: ['TPH', 'TPD'],
    saveWithRemark: false,
    showCalculate: false,
    saveBtn: false,
    allAction: true,
    showDropdown: false,
    showExport: false,
    showImport: false,
    showTitle: false,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        loading={loading}
        permissions={permissions}
        handleUnitChange={handleUnitChange}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        snackbarData={snackbarData}
        setSnackbarData={setSnackbarData}
      />
    </Box>
  )
}

export default ProductionVolumeReferenceGrid
