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
  const [startDate, setStartDate] = useState(null)
  const [endDate, setEndDate] = useState(null)

  const isValidDate = (d) => d instanceof Date && !isNaN(d)

  function formatDateDDMMYYYY(date) {
    if (!(date instanceof Date) || isNaN(date)) return ''
    const d = date.getDate().toString().padStart(2, '0')
    const m = (date.getMonth() + 1).toString().padStart(2, '0')
    const y = date.getFullYear()
    return `${d}/${m}/${y}`
  }

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
      field: 'StartDate',
      title: 'Start Date',
      editable: true,
    },
    {
      field: 'EndDate',
      title: 'End Date',
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

  // Financial-year date bounds, clamped to an absolute 2021-07-01 .. 2028-06-30 window
  useEffect(() => {
    const fetchConfigDates = async () => {
      try {
        const data = await DataService.getConfigurationExecutionDetails(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
        const data1 = data?.data
        const startObj = data1?.find((item) => item.Name === 'StartDate')
        const endObj = data1?.find((item) => item.Name === 'EndDate')
        const start = startObj ? new Date(startObj.AttributeValue) : null
        const end = endObj ? new Date(endObj.AttributeValue) : null
        setStartDate(isValidDate(start) ? start : null)
        setEndDate(isValidDate(end) ? end : null)
      } catch (e) {
        setStartDate(null)
        setEndDate(null)
      }
    }
    if (PLANT_ID && AOP_YEAR) fetchConfigDates()
  }, [PLANT_ID, AOP_YEAR, keycloak])

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
            StartDate: item.FromDate ? new Date(item.FromDate) : null,
            EndDate: item.ToDate ? new Date(item.ToDate) : null,
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

  // Date-only formatter for the payload (no time component since picker is dd/mm/yyyy only)
  function formatDateForPayload(date) {
    if (!date) return null
    const d = date instanceof Date ? date : new Date(date)
    if (isNaN(d)) return null
    return d.toLocaleDateString('en-CA') // yyyy-mm-dd
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

      let hasDateError = false

      for (const record of data) {
        const sDate =
          record.StartDate instanceof Date
            ? record.StartDate
            : new Date(record.StartDate)
        const eDate =
          record.EndDate instanceof Date
            ? record.EndDate
            : new Date(record.EndDate)

        // required check
        if (
          !record.StartDate ||
          !record.EndDate ||
          isNaN(sDate) ||
          isNaN(eDate)
        ) {
          record.isError = true
          hasDateError = true
          continue
        }

        // start must be before (or same day as, since this is date-only) end
        if (sDate.getTime() > eDate.getTime()) {
          record.isError = true
          setSnackbarOpen(true)
          setSnackbarData({
            message: `Start date must be before or same as end date for "${record.discription || 'this record'}".`,
            severity: 'error',
          })
          setLoading(false)
          return
        }

        // must fall within the configured financial-year window
        if (
          startDate &&
          endDate &&
          (sDate < startDate ||
            sDate > endDate ||
            eDate < startDate ||
            eDate > endDate)
        ) {
          record.isError = true
          hasDateError = true
        }
      }

      if (hasDateError) {
        setSnackbarOpen(true)
        setSnackbarData({
          message:
            startDate && endDate
              ? `Dates must be between ${formatDateDDMMYYYY(startDate)} and ${formatDateDDMMYYYY(endDate)} for selected year.`
              : 'Start Date and End Date are required for all records.',
          severity: 'error',
        })
        setLoading(false)
        return
      }

      var payload = []
      payload = data.map((item) => ({
        ShutdownType: item.discription || item.discriptionDrpdwn,
        FromDate: formatDateForPayload(item.StartDate),
        ToDate: formatDateForPayload(item.EndDate),
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
  }, [
    modifiedCells,
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    fetchData,
    startDate,
    endDate,
  ])

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
        await DtaDataService.deleteShutdownHistoryConfigData(
          idFromApi,
          keycloak,
        )
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

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      let response
      response = await DtaDataService.exportShutdownHistoryConfig(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
      )
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    } finally {
      setSnackbarOpen(true)
    }
  }
  const importShutdownHistoryConfig = async (rawFile) => {
    setLoading(true)

    try {
      const response = await DtaDataService.ImportShutdownHistoryConfig(
        rawFile,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = Array.from(byteCharacters, (char) =>
          char.charCodeAt(0),
        )
        const byteArray = new Uint8Array(byteNumbers)

        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })

        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute(
          'download',
          'Error File - Shutdown History Config.xlsx',
        )
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
          severity: 'error',
        })
      }

      return response
    } catch (error) {
      console.error('Error uploading Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = (rawFile) => {
    importShutdownHistoryConfig(rawFile)
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
      downloadExcelBtn: true,
      downloadExcelBtnFromUI: false,
      ExcelName: `${EXCEL_EXPORT_TITLE}_Shutdown History Config`,
      uploadExcelBtn: true,
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
        startDate={startDate}
        endDate={endDate}
        permissions={adjustedPermissions}
        handleExcelUpload={handleExcelUpload}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        disableRedHighlight={true}
        screenType='AromaticsShutdownHistoryConfig'
      />
    </div>
  )
}

export default ShutdownHistoryConfig
