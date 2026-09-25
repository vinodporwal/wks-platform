import { useEffect, useState, useMemo, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { EfficiencyAndFuelRatioAPIService } from 'components/aop-phase-two/services/cpp/jmd/efficiencyAndFuelRatioApiService'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useDebounce } from 'hooks/useDebounce'

const Efficiency = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, jmdSelectedPlants, year } = dataGridStore
  const PLANT_ID = plantObject?.id
  const IS_JMD = siteObject?.name?.toLowerCase() === 'jmd'
  const AOP_YEAR = year?.selectedYear

  // For JMD plants we send the full list of selected plants; for non-JMD
  // we send only the currently selected plant.
  const PLANT_ID_LIST = useMemo(
    () =>
      IS_JMD
        ? jmdSelectedPlants?.map((plant) => plant.id) ?? []
        : PLANT_ID
          ? [PLANT_ID]
          : [],
    [plantObject, jmdSelectedPlants, siteObject],
  )

  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [rows, setRows] = useState([])

  const columns = [
    {
      field: 'assetName',
      title: 'Asset Name',
      type: 'text',
      editable: false,
      locked: true,
      minWidth: 200,
    },
    {
      field: 'type',
      title: 'Type',
      type: 'text',
      editable: false,
      minWidth: 100,
    },
    {
      field: 'uom',
      title: 'UOM',
      type: 'text',
      editable: false,
      minWidth: 120,
    },
    {
      field: 'value',
      title: 'Value',
      type: 'number1',
      editable: true,
      minWidth: 120,
    },
    {
      field: 'remarks',
      title: 'Remark',
      widthT: 250,
      type: 'textarea',
      editable: true,
      minWidth: 250,
    },
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID_LIST.length || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await EfficiencyAndFuelRatioAPIService.getEfficiency(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
      )
      const data = response?.data || []

      if (!data || data.length === 0) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({ message: 'No data found', severity: 'info' })
        return
      }

      const rowsWithId = data.map((row, index) => ({
        ...row,
        id: row.id || `row_${index}`,
        remarks: row.remarks || '',
      }))
      setRows(rowsWithId)
    } catch (error) {
      console.error('Error fetching efficiency data:', error)
      setRows([])
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Error fetching data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID_LIST, AOP_YEAR])

  useDebounce(
    () => {
      if (PLANT_ID_LIST?.length && AOP_YEAR) {
        fetchData()
      }
    },
    1000,
    [PLANT_ID_LIST, AOP_YEAR],
  )

  useEffect(() => {
    setModifiedCells({})
  }, [PLANT_ID_LIST, AOP_YEAR])

  const permissions = {
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: true,
    allAction: true,
    showTitle: true,
    titleName: 'Efficiency',
  }

  const saveChanges = async () => {
    setLoading(true)
    const modifiedData = Object.values(modifiedCells)
    if (modifiedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    const data = modifiedData.filter((row) => row.inEdit)
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    try {
      const payload = data.map((item) => {
        const { inEdit, isNew, isEditable, ...rest } = item
        return {
          ...rest,
          id: isNew ? null : rest.id,
          aopYear: AOP_YEAR,
        }
      })

      await EfficiencyAndFuelRatioAPIService.saveEfficiency(
        keycloak,
        PLANT_ID_LIST,
        AOP_YEAR,
        payload,
      )

      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} changes!`,
        severity: 'success',
      })
      fetchData()
    } catch (error) {
      console.error('Error saving efficiency data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
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
        title='Efficiency'
        permissions={permissions}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={() => {}}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        groupBy={'type'}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
      />
    </Box>
  )
}

export default Efficiency
