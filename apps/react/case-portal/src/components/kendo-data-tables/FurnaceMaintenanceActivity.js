import { useSession } from 'SessionStoreContext'
import Notification from 'components/Utilities/Notification'
import React, { useEffect, useState } from 'react'
import { DataService } from 'services/DataService'
import KendoDataTables from 'components/kendo-data-tables/index'
import { validateFields } from 'utils/validationUtils'
import moment from 'moment'
import { DropDownList } from '@progress/kendo-react-dropdowns'
import { DatePicker } from '@progress/kendo-react-dateinputs'
import { useSelector } from 'react-redux'
import { getRoleName } from 'services/role-service'
const FurnaceMaintenanceActivity = () => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const [rows, setRows] = useState()
  const [furnaceDropdownData, setFurnaceDropdownData] = useState([])
  const [maintenanceActivityData, setMaintenanceActivityData] = useState([])

  const [loading, setLoading] = useState(false)
  const keycloak = useSession()

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const columnsGrid = [
    {
      field: 'furnace',
      title: 'Furnace',
      width: 150,
      editable: true,
      type: 'dynamicDropdown',
    },
    {
      field: 'maintenanceActivity',
      title: 'Maintenance Activity',
      width: 150,
      editable: true,
      type: 'dynamicDropdown',
    },
    {
      field: 'maintStartDateTime',
      title: 'Start Date',
      widthT: 200,
      editable: true,
    },
    {
      field: 'maintEndDateTime',
      title: 'End Date',
      widthT: 200,
      editable: true,
    },
    {
      field: 'duration',
      title: 'Duration (Days)',
      widthT: 150,
      type: 'number',
      editable: false,
      align: 'right',
      headerAlign: 'right',
    },
    {
      field: 'remarks',
      title: 'Remark',
      widthT: 300,
      editable: true,
    },
  ]

  const mapData = (data, tag) =>
    (data?.data?.furnaceMaintenanceActivity || []).map((item, i) => ({
      ...item,
      idFromApi: item?.Id,
      id: i,
      idRow: `${tag}-${i}`,
      originalRemark: item?.remarks ?? '',
      isEditable: true,
    }))

  const fetchFurnaceDropdownData = async () => {
    try {
      // const response = await DataService.getFurnaceDropdownData(keycloak)
      // if (response?.code === 200) {
      //   setFurnaceDropdownData(response?.data)
      // }
      const furnaceOptions = [
        { value: 'H101-SF', name: 'H101-SF' },
        { value: 'H102-SF', name: 'H102-SF' },
        { value: 'H103-SF', name: 'H103-SF' },
        { value: 'H104-SF', name: 'H104-SF' },
        { value: 'H105-SF', name: 'H105-SF' },
      ]
      setFurnaceDropdownData(furnaceOptions)
    } catch (e) {
      console.error('Error loading dropdown data:', e)
    }
  }

  const fetchMaintenanceActivityData = async () => {
    try {
      // const response = await DataService.getMaintenanceActivityData(keycloak)
      // if (response?.code === 200) {
      //   setMaintenanceActivityData(response?.data)
      // }
      const maintenanceActivityOptions = [
        { value: 'TLE', name: 'TLE' },
        { value: 'Coile Replacement', name: 'Coile Replacement' },
        { value: 'IBR', name: 'IBR' },
        { value: 'Plan Shutdown', name: 'Plan Shutdown' },
      ]
      setMaintenanceActivityData(maintenanceActivityOptions)
    } catch (e) {
      console.error('Error loading dropdown data:', e)
    }
  }

  const fetchData = async () => {
    setLoading(true)
    try {
      // const response = await DataService.getFurnaceMaintenanceActivity(
      //   keycloak,
      //   PLANT_ID,
      //   AOP_YEAR,
      // )
      // MOCK DATA FETCH
      const mockResult = {
        code: 200,
        data: {
          furnaceMaintenanceActivity: [
            {
              Id: 1,
              furnace: 'H101-SF',
              maintenanceActivity: 'TLE',
              maintStartDateTime: new Date('2024-04-01').toISOString(),
              maintEndDateTime: new Date('2024-04-05').toISOString(),
              duration: 5,
              remarks: 'Mock data 1',
            },
            {
              Id: 2,
              furnace: 'H103-SF',
              maintenanceActivity: 'IBR',
              maintStartDateTime: new Date('2024-05-10').toISOString(),
              maintEndDateTime: new Date('2024-05-20').toISOString(),
              duration: 11,
              remarks: 'Mock data 2',
            },
          ],
        },
      }

      await new Promise((resolve) => setTimeout(resolve, 800)) // delay to simulate network

      if (mockResult?.code === 200) {
        setRows(mapData(mockResult, 'VMD'))
      } else {
        setRows([])
      }
    } catch (e) {
      console.error('Error loading previous year:', e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchFurnaceDropdownData()
    fetchMaintenanceActivityData()
  }, [])

  useEffect(() => {
    fetchData()
  }, [keycloak, AOP_YEAR, PLANT_ID])

  const saveChanges = async () => {
    try {
      const data = Object.values(modifiedCells)
      if (data.length == 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        setLoading(false)
        return
      }

      const formatIfDate = (value) => {
        if (!value) return ''
        const parsed = moment.utc(
          value,
          ['MMM D, YYYY', 'MMM D, YYYY, h:mm:ss A'],
          true,
        )
        return parsed.isValid()
          ? new Date(parsed.add(1, 'day').format('YYYY-MM-DD'))
          : value
      }

      const rowsToUpdate = data.map((row) => ({
        id: row.Id || null,
        startDate: formatIfDate(row.fromDate),
        endDate: formatIfDate(row.toDate),
        maintenanceActivity: row.maintenanceActivity,
        furnace: row.furnace,
        duration: row.duration,
        remark: row.remarks,
      }))
      const requiredFields = ['remarks']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        setLoading(false)
        return
      }

      // Date Validation for Save Action
      const invalidDateRow = rowsToUpdate.find(
        (r) =>
          r.startDate &&
          r.endDate &&
          moment(r.endDate).isBefore(moment(r.startDate), 'day'),
      )
      if (invalidDateRow) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'End Date cannot be before Start Date for some records',
          severity: 'error',
        })
        setLoading(false)
        return
      }

      // const response = await DataService.saveFurnaceMaintenanceActivity(
      //   keycloak,
      //   PLANT_ID,
      //   AOP_YEAR,
      //   rowsToUpdate,
      // )
      // MOCK SAVE API
      await new Promise((resolve) => setTimeout(resolve, 800))
      const res = { code: 200 }

      if (res?.code == 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Successfully!',
          severity: 'success',
        })
        fetchData()
        setModifiedCells({})
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Failed!',
          severity: 'error',
        })
      }
    } catch (err) {
      console.error('Error while save', err)
      setSnackbarOpen(true)
      setSnackbarData({ message: err.message, severity: 'error' })
    } finally {
      setSnackbarOpen(true)
    }
  }

  const deleteRowData = async (paramsForDelete) => {
    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRows2((prevRows) => prevRows.filter((row) => row.id !== deleteId))
      }

      if (idFromApi) {
        await DataService.deleteFurnaceMaintenanceActivity(idFromApi, keycloak)
        setRows2((prevRows) => prevRows.filter((row) => row.id !== deleteId))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        fetchData()
      }
    } catch (error) {
      console.error('Error deleting Record!', error)
    }
  }

  return (
    <React.Fragment>
      <KendoDataTables
        modifiedCells={modifiedCells}
        rows={rows}
        setRows={setRows}
        columns={columnsGrid}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        loading={loading}
        fetchData={fetchData}
        setModifiedCells={setModifiedCells}
        deleteRowData={deleteRowData}
        permissions={{
          remarksEditable: true,
          saveBtn: !isOldYear,
          saveBtnForRemark: true,
          addButton: !isOldYear,
          allAction: true,
          deleteButton: true,
          dynamicDropdownOptions: {
            furnace: furnaceDropdownData,
            maintenanceActivity: maintenanceActivityData,
          },
        }}
      />
      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </React.Fragment>
  )
}
export default FurnaceMaintenanceActivity
