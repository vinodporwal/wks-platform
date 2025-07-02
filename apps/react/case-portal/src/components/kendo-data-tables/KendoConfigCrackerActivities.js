// DecokingConfig.jsx (refactored to mirror CrackerConfig patterns)
import React, { useState, useCallback, useEffect, useMemo } from 'react'
import {
  Box,
  Tab,
  Tabs,
  Backdrop,
  CircularProgress,
  Typography,
} from '@mui/material'
import KendoDataTables from './index.js'
import { DataService } from 'services/DataService'
import { useSession } from 'SessionStoreContext'

// --- Column Definitions -----------------------------------------------------
import {
  ibrGridOne,
  ibrPlanColumns,
  ibrGridThree,
  runningDurationColumns,
} from './columnDefs'

// --- Sample Data -------------------------------------------------------------
import {
  ibrGridOneRowsSample,
  ibrPlanRowsSample,
  ibrGridThreeRowsSample,
  runningDurationRowsSample,
} from './rowSamples'
import { useSelector } from 'react-redux'

const DecokingConfig = () => {
  const keycloak = useSession()
  const tabs = ['IBR Plan', 'Running Duration']
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { sitePlantChange, verticalChange, yearChanged, oldYear, plantID } =
    dataGridStore
  const isOldYear = oldYear?.oldYear
  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [activeTabIndex, setActiveTabIndex] = useState(0)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const handleRemarkCellClick = (dataItem) => {
    setCurrentRemark(dataItem.remark || '')
    setCurrentRowId(dataItem.id)
    setRemarkDialogOpen(true)
  }

  // --- Rows State Per Tab ----------------------------------------------------
  const [ibrScreen1Rows, setIbrScreen1Rows] = useState([])
  const [ibrScreen2Rows, setIbrScreen2Rows] = useState([])
  const [ibrScreen3Rows, setIbrScreen3Rows] = useState([])
  const [runningDurationRows, setRunningDurationRows] = useState([])
  const [modifiedCells, setModifiedCells] = React.useState({})

  // --- Get/Set Rows by Tab ---------------------------------------------------
  const getRows = useCallback(
    (tab) => {
      if (tab === 'IBR Plan') {
        return { 1: ibrScreen1Rows, 2: ibrScreen2Rows, 3: ibrScreen3Rows }
      }
      if (tab === 'Running Duration') {
        return runningDurationRows
      }
      return []
    },
    [ibrScreen1Rows, ibrScreen2Rows, ibrScreen3Rows, runningDurationRows],
  )

  const setRowsForTab = useCallback((tab, data, screen = 1) => {
    if (tab === 'IBR Plan') {
      if (screen === 1) setIbrScreen1Rows(data)
      if (screen === 2) setIbrScreen2Rows(data)
      if (screen === 3) setIbrScreen3Rows(data)
    } else if (tab === 'Running Duration') {
      setRunningDurationRows(data)
    }
  }, [])

  // --- Fetch Data ------------------------------------------------------------
  const fetchData = useCallback(async () => {
    const currentTab = tabs[activeTabIndex]
    setLoading(true)
    try {
      if (currentTab === 'IBR Plan') {
        // screen 1
        const data1 = await DataService.getIbrScreen1(keycloak)
        if (data1?.code === 200) {
          const processedData = data1.data.map((item, index) => ({
            ...item,
            idFromApi: item.id,
            id: index,
            month:
              item?.month === 'Invalid month' ? 'N/A' : item?.month || 'N/A',
          }))
          setRowsForTab(currentTab, processedData, 1)
        } else {
          setRowsForTab(currentTab, [], 1)
        }
        // screen 2
        // const data2 = await DataService.getIbrScreen2(keycloak)
        setRowsForTab(currentTab, ibrPlanRowsSample, 2)
        // screen 3
        // const data3 = await DataService.getIbrScreen3(keycloak)
        setRowsForTab(currentTab, ibrGridThreeRowsSample, 3)
      } else if (currentTab === 'Running Duration') {
        // const rd = await DataService.getRunningDuration(keycloak)
        setRowsForTab(currentTab, runningDurationRowsSample)
      }
    } catch (err) {
      console.error('Error loading data:', err)
      setSnackbarOpen(true)
      setSnackbarData({ message: 'Failed to load data', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [activeTabIndex, keycloak, setRowsForTab])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const saveChanges = React.useCallback(async () => {
    // setLoading(true)
    try {
      if (Object.keys(modifiedCells).length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        setLoading(false)
        return
      }

      var rawData = Object.values(modifiedCells)
      const data = rawData.filter((row) => row.inEdit)

      if (data.length == 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        setLoading(false)
        return
      }
      console.log(data)

      saveCracker(data)
    } catch (error) {
      console.log('Error saving changes:', error)
    }
    // }, 400)
  }, [modifiedCells])

  const monthMap = {
    January: 1,
    February: 2,
    March: 3,
    April: 4,
    May: 5,
    June: 6,
    July: 7,
    August: 8,
    September: 9,
    October: 10,
    November: 11,
    December: 12,
  }

  const saveCracker = async (newRow) => {
    setLoading(true)
    try {
      var plantId = ''
      const storedPlant = localStorage.getItem('selectedPlant')
      if (storedPlant) {
        const parsedPlant = JSON.parse(storedPlant)
        plantId = parsedPlant.id
      }

      var payload = []

      payload = newRow.map((row) => ({
        days: row?.attributeValue,
        Month: row?.month,
        remarks: row?.remarks,
        isEditable: row?.isEditable,
        normParameterId: row?.normParameterId,
        aopMonth: monthMap[row?.month] || 0,
        id: row?.idFromApi || null,
      }))

      const response = await DataService.saveCracker(plantId, payload, keycloak)
      if (response) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Successfully!',
          severity: 'success',
        })

        setModifiedCells({})
        setLoading(false)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Falied!',
          severity: 'error',
        })
      }

      return response
    } catch (error) {
      console.error('Error saving data:', error)
      setLoading(false)
    } finally {
      // fetchData()
      setLoading(false)
    }
  }

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: true,
      saveBtn: true,
      allAction: true,
    },
    isOldYear,
  )

  const allMonths = [
    'N/A',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
    'January',
    'February',
    'March',
  ].map((month) => ({ value: month, displayName: month }))

  // --- Renderers -------------------------------------------------------------
  const renderIbrPlanTables = () => (
    <>
      {[1].map((screen) => (
        <Box key={screen} sx={{ mt: 2 }}>
          <Typography variant='h6' sx={{ fontWeight: 'bold' }}>
            {screen === 1
              ? 'IBR Plan'
              : screen === 2
                ? 'SD / TA Activities'
                : 'Furnace Run length'}
          </Typography>
          <KendoDataTables
            columns={
              screen === 1
                ? ibrGridOne
                : screen === 2
                  ? ibrPlanColumns
                  : ibrGridThree
            }
            rows={getRows('IBR Plan')[screen]}
            setRows={(data) => setRowsForTab('IBR Plan', data, screen)}
            fetchData={fetchData}
            handleRemarkCellClick={handleRemarkCellClick}
            remarkDialogOpen={remarkDialogOpen}
            currentRemark={currentRemark}
            setCurrentRemark={setCurrentRemark}
            currentRowId={currentRowId}
            snackbarData={snackbarData}
            snackbarOpen={snackbarOpen}
            setSnackbarOpen={setSnackbarOpen}
            setSnackbarData={setSnackbarData}
            modifiedCells={modifiedCells}
            allMonths={allMonths}
            setModifiedCells={setModifiedCells}
            permissions={adjustedPermissions}
            saveChanges={saveChanges}
            setRemarkDialogOpen={setRemarkDialogOpen}
          />
        </Box>
      ))}

      {[2].map((screen) => (
        <Box key={screen} sx={{ mt: 2 }}>
          <Typography variant='h6' sx={{ fontWeight: 'bold' }}>
            {screen === 1
              ? 'IBR Plan'
              : screen === 2
                ? 'SD / TA Activities'
                : 'Furnace Run length'}
          </Typography>
          <KendoDataTables
            columns={
              screen === 1
                ? ibrGridOne
                : screen === 2
                  ? ibrPlanColumns
                  : ibrGridThree
            }
            rows={getRows('IBR Plan')[screen]}
            setRows={(data) => setRowsForTab('IBR Plan', data, screen)}
            fetchData={fetchData}
            handleRemarkCellClick={handleRemarkCellClick}
            remarkDialogOpen={remarkDialogOpen}
            currentRemark={currentRemark}
            setCurrentRemark={setCurrentRemark}
            currentRowId={currentRowId}
            snackbarData={snackbarData}
            snackbarOpen={snackbarOpen}
            setSnackbarOpen={setSnackbarOpen}
            setSnackbarData={setSnackbarData}
            modifiedCells={modifiedCells}
            allMonths={allMonths}
            setModifiedCells={setModifiedCells}
            permissions={adjustedPermissions}
            saveChanges={saveChanges}
            setRemarkDialogOpen={setRemarkDialogOpen}
          />
        </Box>
      ))}

      {[3].map((screen) => (
        <Box key={screen} sx={{ mt: 2 }}>
          <Typography variant='h6' sx={{ fontWeight: 'bold' }}>
            {screen === 1
              ? 'IBR Plan'
              : screen === 2
                ? 'SD / TA Activities'
                : 'Furnace Run length'}
          </Typography>
          <KendoDataTables
            columns={
              screen === 1
                ? ibrGridOne
                : screen === 2
                  ? ibrPlanColumns
                  : ibrGridThree
            }
            rows={getRows('IBR Plan')[screen]}
            setRows={(data) => setRowsForTab('IBR Plan', data, screen)}
            fetchData={fetchData}
            handleRemarkCellClick={handleRemarkCellClick}
            remarkDialogOpen={remarkDialogOpen}
            currentRemark={currentRemark}
            setCurrentRemark={setCurrentRemark}
            currentRowId={currentRowId}
            snackbarData={snackbarData}
            snackbarOpen={snackbarOpen}
            setSnackbarOpen={setSnackbarOpen}
            setSnackbarData={setSnackbarData}
            modifiedCells={modifiedCells}
            allMonths={allMonths}
            setModifiedCells={setModifiedCells}
            permissions={adjustedPermissions}
            saveChanges={saveChanges}
            setRemarkDialogOpen={setRemarkDialogOpen}
          />
        </Box>
      ))}
    </>
  )

  // const renderRunningDurationTable = () => (
  //   <Box sx={{ mt: 2 }}>
  //     <Typography variant='h6'>Running Duration</Typography>
  //     <KendoDataTables
  //       columns={runningDurationColumns}
  //       rows={runningDurationRows}
  //       setRows={setRunningDurationRows}
  //       fetchData={fetchData}
  //       handleRemarkCellClick={handleRemarkCellClick}
  //       remarkDialogOpen={remarkDialog.open}
  //       setRemarkDialogOpen={(open) => setRemarkDialog((v) => ({ ...v, open }))}
  //       currentRemark={remarkDialog.remark}
  //       setCurrentRemark={(r) => setRemarkDialog((v) => ({ ...v, remark: r }))}
  //       currentRowId={remarkDialog.rowId}
  //       snackbarData={snackbarData}
  //       snackbarOpen={snackbarOpen}
  //       setSnackbarOpen={setSnackbarOpen}
  //       setSnackbarData={setSnackbarData}
  //       modifiedCells={modifiedCells}
  //       setModifiedCells={(m) => {
  //         /* implement setter */
  //       }}
  //     />
  //   </Box>
  // )

  return (
    <Box>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      <Tabs value={activeTabIndex} onChange={(e, i) => setActiveTabIndex(i)}>
        {tabs.map((tab) => (
          <Tab key={tab} label={tab} />
        ))}
      </Tabs>

      {activeTabIndex === 0
        ? renderIbrPlanTables()
        : renderRunningDurationTable()}
    </Box>
  )
}

export default DecokingConfig
