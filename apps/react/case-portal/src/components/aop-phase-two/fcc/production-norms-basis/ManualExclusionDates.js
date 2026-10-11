import React, { useState, useEffect, useCallback } from 'react'
import { Box, Stack } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import AdvanceKendoTable from 'components/aop-phase-two/common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { ManualExclusionDateApiService } from 'components/aop-phase-two/services/fcc/manualExclusionDateApiService'

const ManualExclusionDates = ({ startDate, endDate }) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, oldYear } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR)

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
  const [normParameterFKId, setNormParameterFKId] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const columns = [
    {
      field: 'srNo',
      title: 'Sr',
      editable: false,
      widthT: 80,
      minWidth: 50,
      type: 'text',
    },
    {
      field: 'targetDate',
      title: 'Date',
      type: 'date',
      editable: !READ_ONLY,
      widthT: 150,
      minWidth: 120,
    },
    {
      field: 'remarks',
      title: 'Remarks',
      type: 'textarea',
      editable: !READ_ONLY,
      widthT: 350,
      minWidth: 250,
    },
  ]

  const fetchData = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setModifiedCells({})
    setLoading(true)
    try {
      const response = await ManualExclusionDateApiService.getManualExclusionDate(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      const responseData = Array.isArray(response?.data) ? response.data : (response?.data?.Data || [])
      
      const modifiedData = responseData.map((item, index) => ({
        ...item,
        idFromApi: item?.Id || item?.id,
        id: index,
        srNo: index + 1,
        targetDate: item?.date || null,
        remarks: item?.remarks || '',
        originalRemarks: item?.remarks || '',
      }))
      if(responseData[0]?.normParameterFKId){
        setNormParameterFKId(responseData[0]?.normParameterFKId)
      }
      setRows(modifiedData)
    } catch (error) {
      console.error('Error fetching manual exclusion date data:', error)
      setSnackbarData({
        message: 'Failed to load manual exclusion date data',
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

  const validateData = (newRows) => {
    for (let i = 0; i < newRows.length; i++) {
      const row = newRows[i]
      if (!row.targetDate) {
        setSnackbarData({
          message: 'Date is required for all rows',
          severity: 'error',
          autoHide: true,
        })
        return false
      }

      // if (startDate && endDate) {
      //   const rowDate = new Date(row.targetDate)
      //   const limitStart = new Date(startDate)
      //   const limitEnd = new Date(endDate)
        
      //   rowDate.setHours(0, 0, 0, 0)
      //   limitStart.setHours(0, 0, 0, 0)
      //   limitEnd.setHours(0, 0, 0, 0)

      //   if (rowDate < limitStart || rowDate > limitEnd) {
      //     const formatDDMMYYYY = (date) => {
      //       if (!date) return ''
      //       const d = new Date(date)
      //       return `${String(d.getDate()).padStart(2, '0')}-${String(d.getMonth() + 1).padStart(2, '0')}-${d.getFullYear()}`
      //     }
      //     setSnackbarData({
      //       message: `Dates must be between ${formatDDMMYYYY(startDate)} and ${formatDDMMYYYY(endDate)}`,
      //       severity: 'error',
      //       autoHide: true,
      //     })
      //     return false
      //   }
      // }
      
      const remarks = (row?.remarks ?? '').trim()
      if (!remarks) {
        setSnackbarData({
          message: 'Please add Remarks for all rows',
          severity: 'error',
          autoHide: true,
        })
        return false
      }

      const originalRow = rows.find((r) => r.id === row.id)
      if (originalRow && originalRow.idFromApi) {
        const originalRemarks = (originalRow.originalRemarks ?? '').trim()
        if (remarks === originalRemarks) {
          setSnackbarData({
            message: 'Remarks is required and must be different',
            severity: 'error',
            autoHide: true,
          })
          return false
        }
      }
    }
    return true
  }

  const saveChanges = async () => {
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
    
    const data = modifiedData.filter((row) => row.inEdit)
    if (data.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No Records to Save!',
        severity: 'info',
      })
      setLoading(false)
      return
    }

    if (!validateData(data)) {
      setSnackbarOpen(true)
      setLoading(false)
      return
    }

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
        ...row,
        id: row?.idFromApi || null,
        date: toLocalDateOnly(row?.targetDate),
        remarks: row?.remarks,
        normParameterFKId: normParameterFKId
      }
    })

    try {
      await ManualExclusionDateApiService.postManualExclusionDate(
        payloadData,
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      
      setModifiedCells({})
      setSnackbarOpen(true)
      setSnackbarData({
        message: `Successfully saved changes!`,
        severity: 'success',
      })
      await fetchData()
    } catch (error) {
      console.error('Error saving manual exclusion date:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to save changes. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const deleteRowData = async (paramsForDelete) => {
    try {
      const { idFromApi, id } = paramsForDelete
      const deleteIdLocal = id
      setLoading(true)
      if (!idFromApi) {
        setRows((prevRows) => prevRows.filter((row) => row.id !== deleteIdLocal).map((row, idx) => ({...row, srNo: idx + 1})))
        setModifiedCells((prev) => {
          const newModified = { ...prev }
          delete newModified[deleteIdLocal]
          return newModified
        })
        setLoading(false)
      } else {
        const payload = [{ Id: idFromApi }]
        await ManualExclusionDateApiService.deleteManualExclusionDate(
          keycloak,
          payload
        )
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Record Deleted successfully!',
          severity: 'success',
        })
        await fetchData()
      }
    } catch (error) {
      setLoading(false)
      console.error('Error deleting Record', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to delete record',
        severity: 'error',
      })
    } 
  }
  
  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const permissions = {
    showAction: true,
    addButton: !READ_ONLY,
    deleteButton: !READ_ONLY,
    editButton: !READ_ONLY,
    saveBtn: !READ_ONLY,
    allAction: true,
    showExport: false,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Manual Exclusion Dates',
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
          deleteRowData={deleteRowData}
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
        />
      </Stack>
    </Box>
  )
}

export default ManualExclusionDates
