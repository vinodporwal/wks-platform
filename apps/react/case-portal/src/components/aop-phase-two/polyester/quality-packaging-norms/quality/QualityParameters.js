import React, { useCallback, useEffect, useState } from 'react'
import {
  Box,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Button,
} from '@mui/material'
import { useDispatch, useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { DataService } from 'services/DataService'
import { setIsReleased } from 'store/reducers/dataGridStore'
import { shouldShowReleaseButton } from 'utils/releaseButtonUtils'
import { useMenuContext } from 'menu/menuProvider'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { validateFields } from 'utils/validationUtils'
import { QualityPackagingNormsApiService } from '../../../services/polyester/qualityPackagingNormsApiService'
import AdvanceKendoTable from '../../../common/AdvanceKendoTable'

const QualityParameters = () => {
  const dispatch = useDispatch()
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    oldYear,
    plantObject,
    siteObject,
    year,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_NAME = siteObject?.name
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased

  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const valueFormat = ValueFormatterConsumption()

  const { items: menuItems } = useMenuContext()
  const showReleaseButton = shouldShowReleaseButton(menuItems)

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Release states
  const [openReleaseDialogBox, setOpenReleaseDialogBox] = useState(false)
  const [isReleaseDisabled, setIsReleaseDisabled] = useState(true)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  function getPreviousYear(aopYear) {
    if (!aopYear) return ''
    const [start, end] = aopYear.split('-').map((s) => s.trim())
    const prevStart = parseInt(start, 10) - 1
    const prevEnd = (parseInt(end, 10) === 0 ? 99 : parseInt(end, 10) - 1)
      .toString()
      .padStart(2, '0')
    return `${prevStart}-${prevEnd}`
  }
  const previousYear = getPreviousYear(AOP_YEAR)

  const columns = [
    {
      field: 'id',
      title: 'ID',
      editable: false,
      hidden: true,
    },
    {
      field: 'sno',
      title: 'S.no',
      minWidth: 60,
      editable: false,
      type: 'number',
    },
    {
      field: 'name',
      title: 'Name',
      editable: false,
    },
    {
      field: 'unit',
      title: 'Unit',
      editable: false,
      minWidth: 80,
    },
    {
      field: 'budget',
      title: `Budget ${previousYear}`,
      editable: false,
      type: 'number',
      format: valueFormat,
    },
    {
      field: 'actual',
      title: `Actual ${previousYear}`,
      editable: true,
      type: 'number',
      format: valueFormat,
    },
    {
      field: 'proposedNorm',
      title: `Proposed Norm ${AOP_YEAR}`,
      minWidth: 150,
      editable: true,
      type: 'number',
      format: valueFormat,
    },
    {
      field: 'remark',
      title: 'Remark',
      type: 'textarea',
      editable: true,
    },
  ]

  const fetchQualityParameters = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) {
      setRows([])
      return
    }
    setLoading(true)
    try {
      const res = await QualityPackagingNormsApiService.getQualityNorms(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (res?.code === 200 && Array.isArray(res?.data?.Data)) {
        const mappedRows = res.data.Data.map((item, idx) => ({
          id: item.id || idx + 1,
          sno: idx + 1,
          materialId: item.materialId,
          name: item.displayName,
          unit: item.uom,
          budget: item.prevBudget,
          actual: item.prevActual,
          proposedNorm: item.proposedNorm,
          normParameterTypeName: item.normParameterTypeName,
          isEditable: item.isEditable !== false,
          Particulars: item.normParameterTypeName,
          remark: item.remark,
          originalRemark: item.remark,
        }))
        setRows(mappedRows)
      } else {
        setRows([])
      }
    } catch (err) {
      console.error('fetchQualityParameters error', err)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const getIsReleased = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const response = await DataService.getReleaseAOPStatus(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.data && Object.keys(response.data).length > 0) {
        setIsReleaseDisabled(true)
      } else {
        setIsReleaseDisabled(false)
      }
    } catch (error) {
      console.error('Error fetching release status:', error)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    fetchQualityParameters()
    getIsReleased()
  }, [fetchQualityParameters, getIsReleased])

  const saveChanges = useCallback(async () => {
    try {
      const data = Object.values(modifiedCells)
      if (data.length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        return
      }
      const requiredFields = ['remark']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        return
      }

      const qualityParameterDTOList = data.map((row) => ({
        id: typeof row.id === 'number' ? null : row.id,
        materialId: row.materialId,
        displayName: row.name,
        uom: row.unit,
        prevBudget: row.budget,
        prevActual: row.actual,
        proposedNorm: row.proposedNorm,
        plantId: PLANT_ID,
        aopYear: AOP_YEAR,
        remark: row.remark || '',
        normParameterTypeName: 'Quality',
      }))

      setLoading(true)
      const res = await QualityPackagingNormsApiService.saveQualityNorms(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        qualityParameterDTOList,
      )

      if (res?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchQualityParameters()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Save Failed!',
          severity: 'error',
        })
      }
    } catch (err) {
      console.error('Error while save', err)
      setSnackbarOpen(true)
      setSnackbarData({ message: err.message, severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, keycloak, PLANT_ID, AOP_YEAR, fetchQualityParameters])

  const handleExcelUpload = async (rawFile) => {
    setLoading(true)
    try {
      const response = await QualityPackagingNormsApiService.importQualityNorms(
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
        fetchQualityParameters()
      } else if (response?.code === 400 && response?.data) {
        const byteCharacters = atob(response.data)
        const byteNumbers = Array.from(byteCharacters, (char) => char.charCodeAt(0))
        const byteArray = new Uint8Array(byteNumbers)
        const blob = new Blob([byteArray], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `Error File - Quality.xlsx`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchQualityParameters()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Upload Failed!',
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

  const handleExport = async () => {
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })
    try {
      const EXCEL_EXPORT_TITLE = `${vertName}_${SITE_NAME}_${PLANT_NAME}_Quality_Parameters`
      await QualityPackagingNormsApiService.exportQualityNorms(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel downloaded successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    }
  }

  const handleRemarkCellClick = useCallback((row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }, [READ_ONLY])

  const handleRelease = () => {
    setOpenReleaseDialogBox(true)
  }

  const closeReleaseDialogBox = () => {
    setOpenReleaseDialogBox(false)
  }

  const submitConfirmation = async () => {
    setOpenReleaseDialogBox(false)
    setLoading(true)
    try {
      await DataService.releaseAOPReport(keycloak, PLANT_ID, AOP_YEAR)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Released Successfully!',
        severity: 'success',
      })
      setIsReleaseDisabled(true)
      dispatch(setIsReleased({ isReleased: 1 }))
    } catch (error) {
      console.error('Error releasing report:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Release Failed!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const permissions = {
    allAction: true,
    saveBtn: true,
    showTitleNameBusiness: true,
    titleName: 'Quality Parameters',
    showExport: true,
    showImport: true,
    ExcelName: `${lowerVertName}_Quality_Parameters`,
    addButton: false,
    deleteButton: false,
    showTitle: true,
    showReleaseBtn: !showReleaseButton,
  }

  return (
    <Box>
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title='Quality Parameters'
        loading={loading}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        permissions={permissions}
        saveChanges={saveChanges}
        handleRemarkCellClick={handleRemarkCellClick}
        handleExport={handleExport}
        handleExcelUpload={handleExcelUpload}
        groupBy='Particulars'
        isReleaseDisabled={isReleaseDisabled}
        handleRelease={handleRelease}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        snackbarData={snackbarData}
        setSnackbarData={setSnackbarData}
      />

      <Dialog
        open={openReleaseDialogBox}
        onClose={closeReleaseDialogBox}
        disableScrollLock
        PaperProps={{
          sx: {
            borderRadius: '20px',
            p: 2,
            width: 400,
            backdropFilter: 'blur(8px)',
            boxShadow: '0 10px 30px rgba(0,0,0,0.08)',
          },
        }}
      >
        <DialogTitle sx={{ fontWeight: 700, fontSize: '1.2rem', pb: 0.5 }}>
          Confirm Release
        </DialogTitle>
        <DialogContent sx={{ pt: 1 }}>
          <DialogContentText sx={{ fontSize: '0.9rem', color: '#4b5563', lineHeight: 1.5 }}>
            Please confirm that <b style={{ color: '#16a34a' }}>Production</b>,{' '}
            <b style={{ color: '#16a34a' }}>Norms</b>, and{' '}
            <b style={{ color: '#16a34a' }}>Reports</b> are verified before releasing for review.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 2, pb: 1.5, gap: 1 }}>
          <Button
            onClick={closeReleaseDialogBox}
            variant='text'
            sx={{
              textTransform: 'none',
              fontWeight: 600,
              color: '#6b7280',
              '&:hover': { background: 'rgba(0,0,0,0.04)' },
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={submitConfirmation}
            variant='contained'
            className='btn-save'
            sx={{ textTransform: 'none', px: 2.5 }}
          >
            Confirm
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default QualityParameters
