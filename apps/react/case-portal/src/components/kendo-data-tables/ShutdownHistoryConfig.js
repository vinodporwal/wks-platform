import { useGridApiRef } from '@mui/x-data-grid'
import React, { useEffect, useState, useMemo, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from './index'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { OptimizerDataApiService } from 'services/optimizer-api-service'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { DataService } from 'services/DataService'
import { DtaDataService } from 'services/DtaDataservice'
const ShutdownHistoryConfig = ({ permissions }) => {
  const [modifiedCells, setModifiedCells] = React.useState({})
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name

  const SITE_ID = siteObject?.id
  const SITE_NAME = siteObject?.name

  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name

  const AOP_YEAR = year?.selectedYear
  const vertName = verticalChange?.selectedVertical
  const SCREEN_NAME = screenTitle?.title

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${AOP_YEAR}`

  const lowerVertName = vertName?.toLowerCase()
  const lowerSiteName = SITE_NAME?.toLowerCase()
  const lowerPlantName = PLANT_NAME?.toLowerCase()
  const plantName = plantObject?.name
  const siteName = siteObject?.name
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const [open1, setOpen1] = useState(false)
  const [deleteId, setDeleteId] = useState(null)
  const apiRef = useGridApiRef()

  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const keycloak = useSession()
  const [rows, setRows] = useState()
  const valueFormat = ValueFormatterProduction()
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const [lineDetails, setLineDetails] = useState([])
  const headerMap = generateHeaderNames(AOP_YEAR)
  const [allDescriptionDrpdwn, setAllDescriptionDrpdwn] = useState([])
  const IS_AROMATIC_HMD =
    lowerVertName === 'aromatics' && lowerSiteName === 'hmd'
  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.Remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const columns = [
    {
      field: 'discription',
      title: 'Shutdown Type',
      editable: true,
      type: 'discriptionDrpdwn',
      minWidth: 200,
      locked: true,
    },

    {
      field: 'maintenanceId',
      title: 'Maintenance ID',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'maintStartDateTime',
      title: 'SD - From',
      editable: true,
    },
    {
      field: 'maintEndDateTime',
      title: 'SD - To',
      editable: true,
    },
    {
      field: 'Remarks',
      title: 'Remark',
      editable: true,
    },
  ]
  useEffect(() => {
    if (!PLANT_ID || !AOP_YEAR) return

    const getAllDescriptionDrpdwn = async () => {
      try {
        let data = []

        if (IS_AROMATIC_HMD) {
          data = await DataService.dropdownValuesDMD(
            keycloak,
            PLANT_ID,
            AOP_YEAR,
          )
        } else {
          data = await DataService.dropdownValues(keycloak, PLANT_ID, AOP_YEAR)
        }

        let descriptionObjList = []
        {
          descriptionObjList = data?.data.map((product) => ({
            id: product.Name,
            name: product.Name,
            displayName: product.DisplayName,
          }))
        }
        setAllDescriptionDrpdwn(descriptionObjList)
      } catch (error) {
        console.error('Error fetching products', error)
      }
    }

    if (IS_AROMATIC_HMD) getAllDescriptionDrpdwn()
  }, [oldYear, AOP_YEAR, keycloak, PLANT_ID, lowerVertName])

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setModifiedCells({})
    setLoading(true)
    try {
      const res = await DtaDataService.getShutdownHistoryConfigData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = (res?.data || []).map((item, index) => {
          const descriptionObj = allDescriptionDrpdwn.find(
            (p) => p.name === item.ShutdownType,
          )
          return {
            ...item,
            id: index,
            idFromApi: item.Id,
            isEditable: item?.IsEditable,
            maintStartDateTime: item.FromDate,
            maintEndDateTime: item.ToDate,
            Remarks: item.Remarks,
            discription: descriptionObj
              ? descriptionObj.displayName
              : item.ShutdownType, // fallback so something shows if no dropdown match yet
            originalRemark: item.Remarks,
          }
        })
        setRows(mapped)
      } else {
        setRows([])
      }
    } catch (err) {
      console.error('Error fetching data:', err)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, yearChanged, PLANT_ID, AOP_YEAR, allDescriptionDrpdwn])

  function addTimeOffset(dateTime) {
    if (!dateTime) return null
    const date = new Date(dateTime)
    date.setUTCHours(date.getUTCHours() + 5)
    date.setUTCMinutes(date.getUTCMinutes() + 30)
    return date
  }
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

      // adjust to whichever fields are actually mandatory on this grid
      const requiredFields = ['Remarks']

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

      for (const record of data) {
        // Date required validation (before checking time order)
        const dateRequiredRows = new Set()
        for (const record of data) {
          const startMissing = !record.maintStartDateTime
          const endMissing = !record.maintEndDateTime

          if (startMissing || endMissing) {
            record.isError = true
            dateRequiredRows.add(record.id)
          }
        }

        if (dateRequiredRows.size > 0) {
          setSnackbarOpen(true)
          setSnackbarData({
            message: 'Start Date and End Date are required for all records.',
            severity: 'error',
          })
          return
        }

        if (
          record.maintStartDateTime &&
          record.maintEndDateTime &&
          record.maintStartDateTime.getTime() >=
            record.maintEndDateTime.getTime()
        ) {
          record.isError = true
          setSnackbarOpen(true)
          setSnackbarData({
            message: `Start time must be before end time for "${record.discription || 'this record'}".`,
            severity: 'error',
          })
          return
        }
      }
      var payload = []
      payload = data.map((item) => ({
        ShutdownType: item.discription || item.discriptionDrpdwn,
        FromDate: addTimeOffset(item.maintStartDateTime),
        ToDate: addTimeOffset(item.maintEndDateTime),
        AopYear: AOP_YEAR,
        Id: item.idFromApi || null,
        Remarks: item.Remarks || 'null',
        Plant_FK_Id: PLANT_ID,
        //normParameterFKId: item.normParameterFkId || item.NormParameter_FK_Id,
        id: item.idFromApi || null,
      }))

      const response = await DtaDataService.saveShutdownHistoryConfig(
        keycloak,
        AOP_YEAR,
        PLANT_ID,
        payload,
      )

      if (response) {
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
  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

  const deleteRowData = async (paramsForDelete) => {
    setLoading(true)

    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
        setModifiedCells((prev) => {
          const newModifiedCells = { ...prev }
          delete newModifiedCells[deleteId]
          return newModifiedCells
        })
      }

      if (idFromApi) {
        await DtaDataService.deleteShutdownHistoryConfigData(idFromApi, keycloak)
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        fetchData()
      } else {
        setLoading(false)
      }
    } catch (error) {
      console.error('Error deleting Record', error)
    }
  }
  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: permissions?.showAction ?? true,
      addButton: permissions?.addButton ?? true,
      deleteButton: permissions?.deleteButton ?? true,
      editButton: permissions?.editButton ?? false,
      showUnit: permissions?.showUnit ?? false,
      saveWithRemark: permissions?.saveWithRemark ?? true,
      saveBtn: permissions?.saveBtn ?? true,
      customHeight: permissions?.customHeight,
      allAction: true,
      downloadExcelBtn: false,
      downloadExcelBtnFromUI: true,
      ExcelName: `${EXCEL_EXPORT_TITLE}_Shutdown History Config`,
      uploadExcelBtn: false,
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: 'Shutdown History Config',
      showCalculate: false,
    },
    isOldYear,
  )

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setRows}
        columns={columns}
        rows={rows}
        fetchData={fetchData}
        saveChanges={saveChanges}
        paginationOptions={[100, 200, 300]}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        deleteId={deleteId}
        open1={open1}
        setDeleteId={setDeleteId}
        setOpen1={setOpen1}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        deleteRowData={deleteRowData}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        allDescriptionDrpdwn={allDescriptionDrpdwn}
        currentRowId={currentRowId}
        permissions={adjustedPermissions}
        disableRedHighlight={true}
        screenType='shutdown'
      />
    </div>
  )
}

export default ShutdownHistoryConfig
