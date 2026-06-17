import React, { useState, useEffect, useCallback } from 'react'
import { Box, Stack } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelName } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { ExclusionDateApiDataService } from 'components/aop-phase-two/services/common/exclusion-date-api-service'

const ExclusionDate = ({ startDate, endDate }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)

  const { plantObject, year, oldYear, siteObject, verticalChange } =
    dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR)

  const EXCEL_NAME = generateExcelName(dataGridStore, 'Exclusion_Dates')

  const [rows, setRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [loading, setLoading] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
    autoHide: true,
  })
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const columns = [
    {
      field: 'exclusionStartDate',
      title: 'From Date',
      type: 'date',
      editable: !READ_ONLY,
      widthT: 150,
      minWidth: 120,
    },
    {
      field: 'exclusionEndDate',
      title: 'To Date',
      type: 'date',
      editable: !READ_ONLY,
      widthT: 150,
      minWidth: 120,
    },
    {
      field: 'remark',
      title: 'Reason',
      type: 'textarea',
      editable: !READ_ONLY,
      widthT: 250,
      minWidth: 200,
    },
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    setLoading(true)
    try {
      const data = await ExclusionDateApiDataService.getExclusionDate(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const modifiedData = (data?.data?.Data || []).map((item, index) => ({
        ...item,
        idFromApi: item?.id,
        id: index,
        originalRemark: item.remark,
        exclusionEndDate: item?.endDate ? new Date(item.endDate) : null,
        exclusionStartDate: item?.startDate ? new Date(item.startDate) : null,
      }))

      setRows(modifiedData)
    } catch (error) {
      console.error('Error fetching exclusion date data:', error)
      setSnackbarData({
        message: 'Failed to load exclusion date data',
        severity: 'error',
        autoHide: true,
      })
      setSnackbarOpen(true)
    } finally {
      setLoading(false)
    }
  }, [PLANT_ID, AOP_YEAR, keycloak])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const validateDates = (newRows) => {
    if (!startDate || !endDate) {
      setSnackbarData({
        message: 'Configuration dates not available',
        severity: 'error',
        autoHide: true,
      })
      return false
    }

    const limitStart = new Date(startDate)
    const limitEnd = new Date(endDate)

    for (let i = 0; i < newRows.length; i++) {
      const row = newRows[i]

      if (!row.exclusionStartDate || !row.exclusionEndDate) {
        setSnackbarData({
          message: 'Both From date and To date are required',
          severity: 'error',
          autoHide: true,
        })
        return false
      }

      const rowStart = new Date(row.exclusionStartDate)
      const rowEnd = new Date(row.exclusionEndDate)

      if (rowStart > rowEnd) {
        setSnackbarData({
          message: 'From date cannot be after To date',
          severity: 'error',
          autoHide: true,
        })
        return false
      }

      const normalizeDate = (date) => {
        const d = new Date(date)
        d.setHours(0, 0, 0, 0)
        return d
      }

      const rs = normalizeDate(rowStart)
      const re = normalizeDate(rowEnd)
      const ls = normalizeDate(limitStart)
      const le = normalizeDate(limitEnd)

      if (rs < ls || re > le) {
        const formatDDMMYYYY = (date) => {
          if (!date) return ''
          const d = new Date(date)
          const day = String(d.getDate()).padStart(2, '0')
          const month = String(d.getMonth() + 1).padStart(2, '0')
          const year = d.getFullYear()
          return `${day}-${month}-${year}`
        }
        setSnackbarData({
          message: `Dates must be between ${formatDDMMYYYY(startDate)} and ${formatDDMMYYYY(endDate)}`,
          severity: 'error',
          autoHide: true,
        })
        return false
      }

      const reason = (row?.remark ?? '').trim()
      const originalRemark = (row?.originalRemark ?? '').trim()

      if (!reason) {
        setSnackbarData({
          message: 'Please add the Reason',
          severity: 'error',
          autoHide: true,
        })
        return false
      }

      if (reason === originalRemark) {
        setSnackbarData({
          message: 'Please update the Reason',
          severity: 'error',
          autoHide: true,
        })
        return false
      }
    }

    return true
  }

  const saveChanges = async () => {
    console.log('*******', modifiedCells)
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
    console.log('modifiedCells******', modifiedCells)
    const rawData = Object.values(modifiedCells)
    const data = rawData.filter((row) => row.inEdit)
    console.log('data****', data)
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }
    console.log('data after length****', data)
    if (!validateDates(data)) {
      setSnackbarOpen(true)
      setLoading(false)
      return
    }
    console.log('data after validation****', data)
    const payloadData = data.map((row) => {
      const toLocalDateOnly = (date) => {
        if (!date) return null
        const d = new Date(date)
        const year = d.getFullYear()
        const month = String(d.getMonth() + 1).padStart(2, '0')
        const day = String(d.getDate()).padStart(2, '0')
        return `${year}-${month}-${day}`
      }

      return {
        id: row?.idFromApi || null,
        startDate: toLocalDateOnly(row?.exclusionStartDate),
        endDate: toLocalDateOnly(row?.exclusionEndDate),
        remark: row?.remark || row?.remarks,
      }
    })
    console.log('payloadData', payloadData)
    console.log('Saving exclusion dates:', payloadData)
    try {
      const response = await ExclusionDateApiDataService.postExclusionDate(
        payloadData,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      console.log('Save response:', response)
      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved ${modifiedData.length} rows changes!`,
        severity: 'success',
      })
      await fetchData()
      return response
    } catch (error) {
      console.error('Error saving exclusion date:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExcelUpload = async (file) => {
    setLoading(true)
    try {
      const response = await ExclusionDateApiDataService.importExclusionDate(
        file,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Uploaded successfully!',
          severity: 'success',
          autoHide: true,
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
        link.setAttribute('download', 'Error File - Exclusion Date.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
          autoHide: true,
        })
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload failed!',
          severity: 'error',
          autoHide: true,
        })
      }
    } catch (error) {
      console.error('Error uploading excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Unexpected error occurred!',
        severity: 'error',
        autoHide: true,
      })
    } finally {
      setLoading(false)
    }
  }

  const handleExcelDownload = async () => {
    setLoading(true)
    try {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download started!',
        severity: 'success',
        autoHide: true,
      })

      const response = await ExclusionDateApiDataService.exportExclusionDate(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )

      return response
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel',
        severity: 'error',
        autoHide: true,
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

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
        await ExclusionDateApiDataService.deleteExclusionDate(
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
        await fetchData()
      }
    } catch (error) {
      console.error('Error deleting Record', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to delete record',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // Permissions (adjust as needed)
  const permissions = {
    showAction: true,
    addButton: true,
    deleteButton: true,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showExport: true,
    ExcelName: EXCEL_NAME,
    showImport: true,
    showTitleNameBusiness: true,
    showTitle: true,
  }

  return (
    <Box>
      <LoaderBackdrop open={loading} />

      <Stack>
        <AdvanceKendoTable
          columns={columns}
          rows={rows}
          setRows={setRows}
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          saveChanges={saveChanges}
          permissions={permissions}
          handleExcelUpload={handleExcelUpload}
          handleExport={handleExcelDownload}
          handleRemarkCellClick={handleRemarkCellClick}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          setCurrentRowId={setCurrentRowId}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          snackbarData={snackbarData}
          setSnackbarData={setSnackbarData}
          deleteRowData={deleteRowData}
        />
      </Stack>
    </Box>
  )
}

export default ExclusionDate
