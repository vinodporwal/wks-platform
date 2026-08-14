import { useGridApiRef } from '@mui/x-data-grid'
import React, { useEffect, useState, useMemo, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { validateFields } from 'utils/validationUtils'
import KendoDataTables from './index'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const PlantAOPReport = ({ permissions }) => {
  const [_plantID, set_PlantID] = useState('')
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

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

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
  const keycloak = useSession()
  const [rows, setRows] = useState()
  const [tabIndex, setTabIndex] = useState(0)
  const valueFormat = ValueFormatterConsumption()
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const columns = [
    {
      field: 'id',
      title: 'Id',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'initiativeDescription',
      title: 'Initiative Description',
      editable: true,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'recommendation',
      title: 'Category',
      editable: true,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'outcome',
      title: 'Outcome',
      editable: true,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'targetDate',
      title: 'Target Date',
      editable: true,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'responsibility',
      title: 'Responsibility',
      editable: true,
      hidden: false,
      isVisible: true,
    },
    {
      field: 'aopYear',
      title: 'AOP Year',
      editable: true,
      hidden: true,
      isVisible: true,
    },
    {
      field: 'plant_FK_Id',
      title: 'Plant',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'createdOn',
      title: 'Created On',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'modifiedOn',
      title: 'Modified On',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'updatedBy',
      title: 'Updated By',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'isVisible',
      title: 'Is Visible',
      editable: false,
      hidden: true,
      isVisible: false,
    },
    {
      field: 'displayOrder',
      title: 'Display Order',
      editable: true,
      hidden: true,
      isVisible: true,
    },
  ]
  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    setLoading(true)
    try {
      const res = await PlantAopReportApiService.getSafetyImprovementInitiative(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = (res?.data.Data || []).map((item, index) => ({
          ...item,
          id: index,
          idFromApi: item.id || null,
          isEditable: item?.isEditable,
          responsibility: item.remark,
        }))
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
  }, [keycloak, yearChanged, PLANT_ID, AOP_YEAR])

  function toLocalDateString(date) {
    if (!date) return null
    const d = new Date(date)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}` // "2026-09-24"
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
      const requiredFields = [
        'initiativeDescription',
      ]

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
        id: item.idFromApi || null,
        initiativeDescription: item.initiativeDescription,
        outcome: item.outcome,
        recommendation: item.recommendation,
        targetDate: toLocalDateString(item.targetDate),
        remark: item.responsibility,
        aopYear: AOP_YEAR,
        plantFkId: PLANT_ID,
        isEditable:
          item.isEditable === '' ||
            item.isEditable === undefined ||
            item.isEditable === null
            ? true
            : !!item.isEditable,
        isVisible:
          item.isVisible === '' ||
            item.isVisible === undefined ||
            item.isVisible === null
            ? true
            : !!item.isVisible,
        displayOrder:
          item.displayOrder === '' ||
            item.displayOrder === undefined ||
            item.displayOrder === null
            ? 0
            : Number(item.displayOrder),
      }))

      const response =
        await PlantAopReportApiService.saveSafetyImprovementInitiative(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
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
  }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchData])
  useEffect(() => {
    if (tabIndex === 0) {
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak, tabIndex])

  const deleteRowData = async (paramsForDelete) => {
    setLoading(true)
    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
      }

      if (idFromApi) {
        await PlantAopReportApiService.deleteSafetyImprovementInitiative(
          keycloak,
          idFromApi,
        )
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Deleted Successfully!',
          severity: 'success',
        })
      }
    } catch (error) {
      console.error('Error deleting:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Delete failed!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const getExcelExportTitle = useCallback(
    () =>
      [
        VERTICAL_NAME_NO_CASE || vertName?.toUpperCase(),
        SITE_NAME_NO_CASE,
        PLANT_NAME_NO_CASE,
        'Safety_Improvement',
        AOP_YEAR,
      ]
        .filter(Boolean)
        .join('_'),
    [VERTICAL_NAME_NO_CASE, SITE_NAME_NO_CASE, PLANT_NAME_NO_CASE, vertName, AOP_YEAR],
  )

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })
    try {
      const excelTitle = getExcelExportTitle()
      await PlantAopReportApiService.exportPlantSafetyImprovement(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        excelTitle,
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

  const handleExcelUpload = async (rawFile) => {
    setLoading(true)
    try {
      const response =
        await PlantAopReportApiService.importPlantSafetyImprovement(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          rawFile,
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
        link.setAttribute('download', 'Error File - Safety Improvement.xlsx')
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
          message: response?.message || 'Upload Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error uploading excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
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
      showUnit: permissions?.showUnit ?? false,
      saveWithRemark: permissions?.saveWithRemark ?? true,
      saveBtn: permissions?.saveBtn ?? true,
      customHeight: permissions?.customHeight,
      allAction: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      ExcelName: getExcelExportTitle(),
      showNoteWhileDeleting: false,
      showTitleNameBusiness: true,
      titleName: 'Safety Improvement Initiative',
      disableColWidth: true,
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
        deleteRowData={deleteRowData}
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
        permissions={adjustedPermissions}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        handleExcelUpload={handleExcelUpload}
        disableRedHighlight={true}
        screenType='shutdown'
      />
    </div>
  )
}

export default PlantAOPReport
