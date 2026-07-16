import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { useGridApiRef } from '@mui/x-data-grid'
import { useCallback, useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { CatalystChangeOverApiDataService } from 'services/catalyst-changeover-api-service'
import { DataService } from 'services/DataService'
import { getRoleName } from 'services/role-service'
import { useSession } from 'SessionStoreContext'
import KendoDataTables from './index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { validateFields } from 'utils/validationUtils'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'

const CatalystChangeOver = ({
  permissions,
  revision,
  loadBtnClicked,
  summary,
  summaryEdited,
  setSummaryEdited,
}) => {
  const [modifiedCells, setModifiedCells] = useState({})
  const dataGridStore = useSelector((state) => state.dataGridStore)

  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantObject,
    siteObject,
    year,
    screenTitle,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalChange?.selectedVertical?.toUpperCase()
  const revisionName = revision ? `REV_${revision}` : ''
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_${revisionName}`
  const AOP_YEAR = year?.selectedYear
  const [open1, setOpen1] = useState(false)
  const [deleteId, setDeleteId] = useState(null)
  const apiRef = useGridApiRef()
  const [rows, setRows] = useState([])
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
  const IS_OLD_YEAR = false
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const valueFormat = ValueFormatterProduction()
  const CatalystChangeOverColumns = [
    {
      field: 'id',
      hidden: true,
      isVisible: false,
    },
    {
      field: 'parameter',
      title: 'Parameter',
      editable: true,
      width: 200,
      type: 'dynamicDropdownshared',
      dropdownOptions: [
        { name: 'DeH-15 ', value: 'DeH-15' },
        { name: 'DeH-201', value: 'DeH-201' },
      ],
    },
    {
      field: 'date',
      title: 'Date',
      editable: true,
      fixedWidth: '200px',
    },
    {
      field: 'remarks',
      title: 'Remarks',
      editable: true,
      fixedWidth: '200px',
    },
    {
      field: 'originalRemarks',
      hidden: true,
      isVisible: false,
    },
  ]

  const colDefs = CatalystChangeOverColumns

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    try {
      setLoading(true)
      const data = await CatalystChangeOverApiDataService.getCatalystChangeOver(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (data && data.code === 200) {
        const modifiedData = (data?.data || []).map((item, index) => ({
          ...item,
          idFromApi: item?.id,
          id: index,
          originalRemarks: item.remarks,
          originalRemark: item.remarks,
          parameter: item.parameter || '',
          remarks: item.remarks || '',
          date: item?.date ? new Date(item.date) : null,
        }))
        setRows(modifiedData)
      } else {
        setRows([])
        let errorMsg = 'Failed to fetch data.'
        if (data instanceof Response) {
          try {
            const errJson = await data.json()
            errorMsg =
              errJson?.message || `Error: ${data.status} ${data.statusText}`
          } catch (e) {
            errorMsg = `Error: ${data.status} ${data.statusText}`
          }
        } else if (data?.message) {
          errorMsg = data.message
        }
        setSnackbarOpen(true)
        setSnackbarData({
          message: errorMsg,
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error fetching data:', error)
      setRows([])
      setSnackbarOpen(true)
      setSnackbarData({
        message: error?.message || 'Error fetching data.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setModifiedCells({})
    fetchData()

    // console.log('CatalystChangeOver rendered', revision)
    // console.log('loadBtnClicked', loadBtnClicked)
  }, [
    oldYear,
    yearChanged,
    keycloak,
    PLANT_ID,
    AOP_YEAR,
    revision,
    loadBtnClicked,
  ])

  const deleteRowData = async (paramsForDelete) => {
    setLoading(true)
    try {
      const { idFromApi, id } = paramsForDelete
      const deleteIdLocal = id
      if (!idFromApi) {
        setRows((prevRows) =>
          prevRows.filter((row) => row.id !== deleteIdLocal),
        )
      } else {
        await CatalystChangeOverApiDataService.deleteCatalystChangeOver(
          idFromApi,
          keycloak,
        )
        setRows((prevRows) =>
          prevRows.filter((row) => row.id !== deleteIdLocal),
        )
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        // refresh list
        await fetchData()
      }
    } catch (error) {
      console.error('Error deleting Record', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: error?.message || 'Error deleting Record.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    try {
      let response

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download started!',
        severity: 'success',
      })

      response =
        await CatalystChangeOverApiDataService.exportCatalystChangeOver(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          EXCEL_EXPORT_TITLE,
        )

      return response
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

  const importExcel = async (rawFile) => {
    setLoading(true)

    try {
      let response

      response =
        await CatalystChangeOverApiDataService.importCatalystChangeOver(
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
        await fetchData()
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
        link.setAttribute('download', 'Error File - Catalyst Changeover.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        await fetchData()
      } else {
        let errorMsg = 'Upload Failed!'
        if (response instanceof Response) {
          try {
            const errJson = await response.json()
            errorMsg =
              errJson?.message ||
              `Error: ${response.status} ${response.statusText}`
          } catch (e) {
            errorMsg = `Error: ${response.status} ${response.statusText}`
          }
        } else if (response?.message) {
          errorMsg = response.message
        }
        setSnackbarOpen(true)
        setSnackbarData({ message: errorMsg, severity: 'error' })
      }

      return response
    } catch (error) {
      console.error('Error uploading excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: error?.message || 'Unexpected error occurred!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = (rawFile) => {
    importExcel(rawFile)
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
      showTitleNameBusiness: true,
      titleName: 'Catalyst Changeover',
      uploadExcelBtn: true,
      reasonText: true,
    },
    IS_OLD_YEAR,
  )

  const saveSummary = async (summary) => {
    try {
      const response = await DataService.saveSummaryAOPConsumptionNorm(
        PLANT_ID,
        AOP_YEAR,
        summary,
        keycloak,
      )

      if (response?.code == 200) {
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })

        setSnackbarOpen(true)
        // setIsEdited(false)
      } else {
        let errorMsg = 'Saved Failed!'
        if (response instanceof Response) {
          try {
            const errJson = await response.json()
            errorMsg =
              errJson?.message ||
              `Error: ${response.status} ${response.statusText}`
          } catch (e) {
            errorMsg = `Error: ${response.status} ${response.statusText}`
          }
        } else if (response?.message) {
          errorMsg = response.message
        }
        setSnackbarData({
          message: errorMsg,
          severity: 'error',
        })
        setSnackbarOpen(true)
      }
      return response
    } catch (error) {
      console.error('Error saving Summary!', error)
      setSnackbarData({
        message: error?.message || 'Error saving Summary!',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }

  const saveAPI = async (newRows) => {
    // --- 1. Basic Structure Validation ---

    if (!newRows || newRows.length === 0) return

    const requiredFields = ['parameter', 'remarks']
    const validationMessage = validateFields(newRows, requiredFields)

    if (validationMessage) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validationMessage,
        severity: 'error',
      })
      return
    }

    try {
      const payloadData = []

      for (let i = 0; i < newRows.length; i++) {
        const row = newRows[i]

        // --- 2. Validation: Date must be present ---
        if (!row.date) {
          setSnackbarData({
            message: 'Date is required.',
            severity: 'error',
          })
          setSnackbarOpen(true)
          return // Stop execution
        }

        const rowDate = new Date(row.date)

        // --- 3. Validation: Within AOP Year Range ---
        let limitStart, limitEnd
        const parts = (AOP_YEAR || '').split('-')
        if (parts.length === 2) {
          const startYear = parseInt(parts[0], 10)
          const endYear = startYear + 1
          limitStart = new Date(startYear, 3, 1) // April 1st
          limitEnd = new Date(endYear, 2, 31) // March 31st
        } else {
          const yearNum = parseInt(AOP_YEAR || new Date().getFullYear(), 10)
          limitStart = new Date(yearNum, 0, 1) // Jan 1st
          limitEnd = new Date(yearNum, 11, 31) // Dec 31st
        }

        const normalizeDate = (date) => {
          const d = new Date(date)
          d.setHours(0, 0, 0, 0)
          return d
        }

        const rd = normalizeDate(rowDate)
        const ls = normalizeDate(limitStart)
        const le = normalizeDate(limitEnd)

        const formatDDMMYYYY = (date) => {
          if (!date) return ''
          const d = new Date(date)
          const day = String(d.getDate()).padStart(2, '0')
          const month = String(d.getMonth() + 1).padStart(2, '0')
          const year = d.getFullYear()
          return `${day}-${month}-${year}`
        }

        if (rd < ls || rd > le) {
          setSnackbarData({
            message: `Date must be within AOP Year ${AOP_YEAR} (between ${formatDDMMYYYY(limitStart)} and ${formatDDMMYYYY(limitEnd)}).`,
            severity: 'error',
          })
          setSnackbarOpen(true)
          return // Stop execution
        }

        const toLocalDateOnly = (date) => {
          if (!date) return null

          const d = new Date(date)
          const year = d.getFullYear()
          const month = String(d.getMonth() + 1).padStart(2, '0')
          const day = String(d.getDate()).padStart(2, '0')

          return `${year}-${month}-${day}` // YYYY-MM-DD (LOCAL)
        }

        // If valid, push to payload
        payloadData.push({
          id: row?.idFromApi || null,
          parameter: row?.parameter,
          date: toLocalDateOnly(row?.date),
          remarks: row?.remarks || '',
          plantId: PLANT_ID,
          aopYear: AOP_YEAR,
        })
      }

      // --- 4. Proceed to API Call ---
      const response =
        await CatalystChangeOverApiDataService.postCatalystChangeOver(
          payloadData,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      if (response && response.code === 200) {
        if (summaryEdited) {
          await saveSummary(summary)
          setSummaryEdited(false)
        }

        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData()
      } else {
        let errorMsg = 'Failed to save data.'
        if (response instanceof Response) {
          try {
            const errJson = await response.json()
            errorMsg =
              errJson?.message ||
              `Error: ${response.status} ${response.statusText}`
          } catch (e) {
            errorMsg = `Error: ${response.status} ${response.statusText}`
          }
        } else if (response?.message) {
          errorMsg = response.message
        }
        setSnackbarOpen(true)
        setSnackbarData({ message: errorMsg, severity: 'error' })
      }
      return response
    } catch (error) {
      console.error('Error in saving data!', error)
      setSnackbarData({
        message: error?.message || 'Failed to save data.',
        severity: 'error',
      })
      setSnackbarOpen(true)
    }
  }

  const saveChanges = useCallback(async () => {
    setLoading(true)

    try {
      // CASE 1: only summary edited
      if (Object.keys(modifiedCells).length === 0) {
        if (summaryEdited) {
          await saveSummary(summary)
          setModifiedCells({})
          setSummaryEdited(false)
        }
        return
      }

      const rawData = Object.values(modifiedCells)
      const data = rawData.filter((row) => row.inEdit)
      if (data.length === 0) {
        setLoading(false)
        return
      }

      await saveAPI(data)
    } catch (error) {
      console.log('Error saving changes:', error)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, summaryEdited, summary])

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setRows}
        columns={colDefs}
        rows={rows}
        paginationOptions={[100, 200, 300]}
        saveChanges={saveChanges}
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
        fetchData={fetchData}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        deleteRowData={deleteRowData}
        permissions={adjustedPermissions}
        handleExcelUpload={handleExcelUpload}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        summaryEdited={summaryEdited}
        revision={revision}
      />
    </div>
  )
}

export default CatalystChangeOver
