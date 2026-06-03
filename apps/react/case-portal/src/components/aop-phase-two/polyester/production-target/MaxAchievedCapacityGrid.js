import React, { useState, useEffect, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { ProductionTargetApiService } from '../../services/polyester/productionTargetApiService'

const MaxAchievedCapacityGrid = ({
  snackbarData,
  setSnackbarData,
  snackbarOpen,
  setSnackbarOpen,
  loading,
  setLoading,
}) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear

  const [rows, setRows] = useState([])

  const valueFormat = ValueFormatterProduction()
  const headerMap = generateHeaderNames(AOP_YEAR)

  const monthsConfig = [
    { field: 'april', key: 4, title: 'Apr' },
    { field: 'may', key: 5, title: 'May' },
    { field: 'june', key: 6, title: 'Jun' },
    { field: 'july', key: 7, title: 'Jul' },
    { field: 'august', key: 8, title: 'Aug' },
    { field: 'september', key: 9, title: 'Sep' },
    { field: 'october', key: 10, title: 'Oct' },
    { field: 'november', key: 11, title: 'Nov' },
    { field: 'december', key: 12, title: 'Dec' },
    { field: 'january', key: 1, title: 'Jan' },
    { field: 'february', key: 2, title: 'Feb' },
    { field: 'march', key: 3, title: 'Mar' },
  ]

  const columns = [
    {
      field: 'materialFKId',
      title: 'Particulars',
      editable: false,
      hidden: true,
      minWidth: 120,
    },
    {
      field: 'productName',
      title: 'Particulars',
      editable: false,
      minWidth: 200,
    },
    ...monthsConfig.map((m) => ({
      field: m.field,
      title: m.title,
      editable: false,
      type: 'number1',
      format: valueFormat,
      minWidth: 100,
    })),
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await ProductionTargetApiService.getMaxAchievedCapacity(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200 && Array.isArray(response?.data)) {
        const formattedData = response.data.map((item, index) => ({
          ...item,
          idFromApi: item.id || null,
          materialFKId: item.materialFKId || null,
          productName: item.productName || item.materialDisplayName || '',
          id: index,
          isEditable: false,
        }))
        setRows(formattedData)
      } else {
        setRows([])
      }
    } catch (error) {
      console.error('Error fetching max achieved capacity data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, setLoading])

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
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Max Achieved Capacity',
    showDropdown: false,
    showUnit: false,
  }

  return (
    <AdvanceKendoTable
      columns={columns}
      rows={rows}
      setRows={setRows}
      title={permissions.titleName}
      permissions={permissions}
      snackbarData={snackbarData}
      snackbarOpen={snackbarOpen}
      setSnackbarOpen={setSnackbarOpen}
      setSnackbarData={setSnackbarData}
      loading={loading}
    />
  )
}

export default MaxAchievedCapacityGrid
