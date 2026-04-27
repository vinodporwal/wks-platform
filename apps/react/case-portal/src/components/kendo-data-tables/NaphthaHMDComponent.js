import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import { Backdrop, Box, CircularProgress } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import { DataService } from 'services/DataService'
import { SiteReportDataService } from 'services/SiteReportDataService'
import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import { add } from 'lodash'
import { validateFields } from 'utils/validationUtils'
import { ProductionNormsApiService } from 'services/production-norms-api-service'
import { Place } from '@mui/icons-material'
export default function NaphthaHMDComponent() {
  const keycloak = useSession()
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

  const SCREEN_NAME = screenTitle?.title
  const AOP_YEAR = year?.selectedYear
  const thisYear = AOP_YEAR
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})
  const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()

  const headerMap = generateHeaderNames(AOP_YEAR)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const unsavedChangesRef = useRef({ unsavedRows: {}, rowsBeforeChange: {} })

  const naphthaColumns = [
    {
      field: 'id',
      title: 'ID',
      widthT: 50,
      editable: false,
      hidden: true,
    },
    {
      field: 'section',
      title: 'Section',
      widthT: 120,
      editable: false,
      hidden: true,
    },
    {
      field: 'name',
      title: 'Name',
      editable: false,
    },
    {
      field: 'max',
      title: 'Max',
      editable: true,
      type: 'number',
    },
    {
      field: 'min',
      title: 'Min',
      type: 'number',
      editable: true,
    },
    {
      field: 'months',
      title: 'Month',
      editable: true,
      type: 'number',
    },
    
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const res = await ProductionNormsApiService.getNaphthaHMDData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = res?.data?.Data?.map((item, index) => ({
          id: item.id || index,
          section: item.section,
          name: item.name,
          max: item.max,
          min: item.min,
          months: item.months,
          Particulars: item.section,
        }))
        setRows(mapped)
      } else {
        setRows([])
      }
    } catch (err) {
      console.error('fetchData error', err)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, yearChanged, plantID])

  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

  const saveChanges = React.useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        return
      }

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

      const payload = data.map((item) => ({
        id: item.id || null,
        plant: item.plant,
        noOfShutdownDays: item.noOfShutdownDays,
        noOfSlowdownDays: item.noOfSlowdownDays,
        monthPlan: item.monthPlan,
        shutdownSlowdownPlan: item.shutdownSlowdownPlan,
        remarks: item.remarks || 'system generated',
        siteId: SITE_ID,
        aopYear: AOP_YEAR,
        updatedBy: keycloak?.userName || 'system',
        updatedDate: new Date().toISOString(),
      }))

      // 3. Save to API
      const response = await ProductionNormsApiService.saveNaphthaHMDData(
        keycloak,
        SITE_ID,
        AOP_YEAR,
        payload,
      )

      // 4. Handle API response
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Save failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchData])

  //   const deleteRowData = async (paramsForDelete) => {
  //     setLoading(true)

  //     try {
  //       const { idFromApi, id } = paramsForDelete
  //       const deleteId = id

  //       if (!idFromApi) {
  //         setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
  //       }

  //       if (idFromApi) {
  //         await ProductionNormsApiService.deleteNaphthaHMDData(idFromApi, keycloak)
  //         setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
  //         setSnackbarOpen(true)
  //         setSnackbarData({
  //           message: 'Record Deleted successfully!',
  //           severity: 'success',
  //         })
  //         fetchData()
  //       } else {
  //         setLoading(false)
  //       }
  //     } catch (error) {
  //       console.error('Error deleting Record!', error)
  //     }
  //   }

  const handleExcelUpload = (type) => (rawFile) => {
    uploadPeopleDetails(rawFile, type)
  }

  const handleRemarkCellClick = useCallback((row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }, [])

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
      allAction: true,
      saveBtn: true,
      showTitleNameBusiness: true,
      titleName: 'Naphtha HMD Data',
      adjustedPermissions: true,
      ExcelName: `${lowerVertName}_Naphtha_HMD_Data_${AOP_YEAR}`,
      //addButton: true,
      //deleteButton: true,
    },
    isOldYear,
  )

  return (
    <Box>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      <KendoDataTables
        columns={naphthaColumns}
        rows={rows}
        setRows={setRows}
        title='Naphtha HMD Data'
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        enableSaveAddBtn={enableSaveAddBtn}
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        //deleteRowData={deleteRowData}
        permissions={adjustedPermissions}
        groupBy='Particulars'
      />
      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </Box>
  )
}
