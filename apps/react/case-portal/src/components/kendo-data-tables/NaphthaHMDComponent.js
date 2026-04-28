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
  const [rows1, setRows1] = useState([])
  const [loading, setLoading] = useState(false)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})
  const [modifiedCells1, setModifiedCells1] = useState({})
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
      widthT: 150,
      editable: false,
      hidden: true,
    },
    {
      field: 'name',
      title: 'Name',
      editable: false,
      widthT: 200,
    },
    {
      field: 'max',
      title: 'Max',
      editable: true,
      type: 'number',
      widthT: 120,
    },
    {
      field: 'min',
      title: 'Min',
      type: 'number',
      editable: true,
      widthT: 120,
    },
    {
      field: 'months',
      title: 'Month',
      editable: true,
      type: 'number',
      widthT: 120,
    },
    {
      field: 'monthsId',
      title: 'monthsId',
      editable: false,
      hidden: true,
    },
    {
      field: 'minId',
      title: 'minId',
      editable: false,
      hidden: true,
    },
    {
      field: 'maxId',
      title: 'maxId',
      editable: false,
      hidden: true,
    },
  ]
  const limpsColumns = [
    {
      field: 'id',
      title: 'ID',
      hidden: true,
      widthT: 50,
      editable: false,
    },
    {
      field: 'name',
      title: 'Name',
      editable: false,
      widthT: 200,
      hidden: true,
    },
    {
      field: 'displayName',
      title: 'DisplayName',
      editable: false,
      widthT: 200,
    },
    {
      field: 'uom',
      title: 'UOM',
      editable: false,
      width: 120,
    },
    {
      field: 'jmd',
      title: 'JMD',
      editable: true,
      width: 120,
    },
    {
      field: 'hpn',
      title: 'HPN',
      editable: true,
      width: 120,
    },
    {
      field: 'heavy',
      title: 'Heavy',
      editable: true,
      width: 120,
    },
    {
      field: 'others',
      title: 'Others',
      editable: true,
      width: 120,
    },
    {
      field: 'blend',
      title: 'Blend',
      editable: true,
      width: 120,
    },
    {
      field: 'jmdId',
      editable: true,
      hidden: true,
    },
    {
      field: 'hpnId',
      editable: true,
      hidden: true,
    },
    {
      field: 'heavyId',
      editable: true,
      hidden: true,
    },
    {
      field: 'othersId',
      editable: true,
      hidden: true,
    },
    {
      field: 'blendId',
      editable: true,
      hidden: true,
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
          maxId: item.maxId,
          minId: item.minId,
          monthsId: item.monthsId,
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

  const fetchLimsData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const res = await ProductionNormsApiService.getLimsData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = res?.data?.Data?.map((item, index) => ({
          id: item.id || index,
          name: item.name,
          displayName: item.displayName,
          uom: item.uom,
          jmd: item.jmd,
          hpn: item.hpn,
          heavy: item.heavy,
          others: item.others,
          blend: item.blend,
          jmdId: item.jmdId,
          hpnId: item.hpnId,
          heavyId: item.heavyId,
          othersId: item.othersId,
          blendId: item.blendId,
        }))
        setRows1(mapped)
      } else {
        setRows1([])
      }
    } catch (err) {
      console.error('fetchLims Data error', err)
      setRows1([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, yearChanged, plantID])

  useEffect(() => {
    fetchLimsData()
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

      //   const validationMessage = validateFields(data, requiredFields)
      //   if (validationMessage) {
      //     setSnackbarOpen(true)
      //     setSnackbarData({
      //       message: validationMessage,
      //       severity: 'error',
      //     })
      //     setLoading(false)
      //     return
      //   }

      const payload = data.map((item) => ({
        //id: item.id || null,
        section: item.section,
        name: item.name,
        max: item.max,
        min: item.min,
        months: item.months,
        maxId: item.maxId,
        minId: item.minId,
        monthsId: item.monthsId,
      }))

      // 3. Save to API
      const response = await ProductionNormsApiService.saveNaphthaHMDData(
        keycloak,
        PLANT_ID,
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
  const saveChangesLimsData = React.useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells1)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        return
      }

      const payload = data.map((item) => ({
        id: item.id || null,
        name: item.name,
        displayName: item.displayName,
        uom: item.uom,
        jmd: item.jmd,
        hpn: item.hpn,
        heavy: item.heavy,
        others: item.others,
        blend: item.blend,
        jmdId: item.jmdId,
        hpnId: item.hpnId,
        heavyId: item.heavyId,
        othersId: item.othersId,
        blendId: item.blendId,
      }))

      // 3. Save to API
      const response = await ProductionNormsApiService.saveLimsData(
        keycloak,
        PLANT_ID,
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
        setModifiedCells1({})
        fetchLimsData()
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
  }, [modifiedCells1, keycloak, PLANT_ID, AOP_YEAR, fetchLimsData])

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
      titleName: 'LIMS Data Extraction Settings',
      adjustedPermissions: true,
      ExcelName: `${lowerVertName}_LIMS Data Extraction Settings_${AOP_YEAR}`,
      //addButton: true,
      //deleteButton: true,
    },
    isOldYear,
  )
  const adjustedPermissions1 = getAdjustedPermissions(
    {
      allAction: true,
      saveBtn: true,
      showTitleNameBusiness: true,
      titleName: 'LIMS Data',
      adjustedPermissions: true,
      ExcelName: `${lowerVertName}_LIMS Data_${AOP_YEAR}`,
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
        title='LIMS Data Extraction Settings'
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
        //handleRemarkCellClick={handleRemarkCellClick}
        //deleteRowData={deleteRowData}
        permissions={adjustedPermissions}
        groupBy='Particulars'
      />

      <KendoDataTables
        columns={limpsColumns}
        rows={rows1}
        setRows={setRows1}
        title='LIMS Data'
        modifiedCells={modifiedCells1}
        setModifiedCells={setModifiedCells1}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        enableSaveAddBtn={enableSaveAddBtn}
        saveChanges={saveChangesLimsData}
        //handleRemarkCellClick={handleRemarkCellClick}
        permissions={adjustedPermissions1}
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
