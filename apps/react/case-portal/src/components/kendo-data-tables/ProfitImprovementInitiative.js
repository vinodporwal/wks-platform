import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Box } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { PlantAopReportApiService } from 'services/plant-aop-report-api-service'
import { validateFields } from 'utils/validationUtils'

export default function ProfitImprovementInitiative({ permissions }) {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    year,
    verticalChange,
    yearChanged,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const thisYear = AOP_YEAR
  const SITE_NAME_NO_CASE = siteObject?.name?.toLowerCase()
  const PLANT_NAME_NO_CASE = plantObject?.name?.toLowerCase()

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)

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

  const oldYearLabel = useMemo(() => {
    if (!thisYear || !thisYear.includes('-')) return ''
    const [start, end] = thisYear.split('-').map(Number)
    return `${start - 1}-${(end - 1).toString().slice(-2)}`
  }, [thisYear])

  const columns = useMemo(
    () => [
      {
        field: 'sNo',
        title: 'S.No',
        width: 100,
        editable: false,
        minWidth: 100,
      },
      {
        field: 'initiativeDescription',
        title: 'Initiative Description',
        editable: true,
        widthT: 300,
        minWidth: 100,
      },
      {
        field: 'recommendation',
        title: 'Category',
        editable: true,
        hidden: false,
        isVisible: true,
      },
      {
        field: 'cost',
        title: 'Cost (Rs/Cr)',
        widthT: 80,
        editable: true,
        minWidth: 100,
        type:'number',
        format: '{0:0.000}',
      },
      {
        field: 'outcome',
        title: 'Outcome (Rs/Cr)',
        widthT: 80,
        editable: true,
        minWidth: 100,
        type:'number',
        format: '{0:0.000}',
      },

      {
        field: 'targetDate',
        title: 'Target Date',
        editable: true,
        minWidth: 100,
      },
      {
        field: 'responsibility',
        title: 'Responsibility',
        widthT: 60,
        editable: true,
        minWidth: 100,
      },
    ],
    [PLANT_ID, yearChanged],
  )
  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await PlantAopReportApiService.getProfitImprovementInitiative(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (res?.code === 200) {
        const mapped = (res?.data?.Data || []).map((item, index) => ({
          ...item,
          id: index,
          idFromApi: item?.id,
          sNo: index + 1,
          isEditable: item?.isEditable,
          cost: item.cost,
          responsibility: item.remark,
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
  }, [keycloak, yearChanged, PLANT_ID])

  useEffect(() => {
    fetchData()
  }, [fetchData, yearChanged, PLANT_ID])

  function toLocalDateString(date) {
    if (!date) return null
    const d = new Date(date)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}` // "2026-09-24"
  }

  const saveChanges = useCallback(async () => {
    try {
      setLoading(true)
      const data = Object.values(modifiedCells)
      if (!data.length) {
        setSnackbarData({ message: 'No Records to Save!', severity: 'info' })
        setSnackbarOpen(true)
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
        cost: item.cost != null && item.cost !== '' ? Number(item.cost) : null,
        outcome: item.outcome != null && item.outcome !== '' ? Number(item.outcome) : null,
        recommendation: item.recommendation || null,
        targetDate: toLocalDateString(item.targetDate) || null,
        remark: item.responsibility || null,
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
        await PlantAopReportApiService.saveProfitImprovementInitiative(
          keycloak,
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
    } catch (e) {
      console.log(e)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error while saving!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells])

  const deleteRowData = async (paramsForDelete) => {
    setLoading(true)
    try {
      const { idFromApi, id } = paramsForDelete
      const deleteId = id

      if (!idFromApi) {
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteId))
      }

      if (idFromApi) {
        await PlantAopReportApiService.deleteProfitImprovementInitiative(
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
        verticalObject?.name?.toUpperCase() || vertName?.toUpperCase(),
        siteObject?.name?.toUpperCase(),
        plantObject?.name?.toUpperCase(),
        'Profit_Improvement',
        AOP_YEAR,
      ]
        .filter(Boolean)
        .join('_'),
    [verticalObject, siteObject, plantObject, vertName, AOP_YEAR],
  )

  const downloadExcelForConfiguration = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })
    try {
      const excelTitle = getExcelExportTitle()
      await PlantAopReportApiService.exportProfitImprovementInitiative(
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
        await PlantAopReportApiService.importProfitImprovementInitiative(
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
        link.setAttribute('download', 'Error File - Profit Improvement.xlsx')
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

  const handleCalculate = () => { }


  const getAdjustedPermissionsC = (permissions, isOldYear) => {
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

  const adjustedPermissionsC = getAdjustedPermissionsC(
    {
      allAction: true,
      saveBtn: true,
      addButton: permissions?.addButton ?? true,
      deleteButton: permissions?.deleteButton ?? true,
      showTitleNameBusiness: true,
      titleName: 'Profit Improvement and Operability Improvement Initiative',
      adjustedPermissions: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      ExcelName: getExcelExportTitle(),
      disableColWidth: true,
    },
    isOldYear,
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <KendoDataTables
        rows={rows}
        setRows={setRows}
        columns={columns}
        title='Profit Improvement and Operability Improvement Initiative'
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        enableSaveAddBtn={enableSaveAddBtn}
        saveChanges={saveChanges}
        deleteRowData={deleteRowData}
        handleCalculate={handleCalculate}
        permissions={adjustedPermissionsC}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        handleExcelUpload={handleExcelUpload}
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
