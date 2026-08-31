import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Box, Button, Dialog, DialogActions, DialogContent } from '@mui/material'
import ReleaseDialog from 'components/aop-phase-two/common/components/ReleaseDialog'
import { useDispatch, useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { setIsBlocked, setIsReleased } from 'store/reducers/dataGridStore'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import { validateFields } from 'utils/validationUtils'
import { useMenuContext } from 'menu/menuProvider'
import { shouldShowReleaseButton } from 'utils/releaseButtonUtils'
import { DataService } from 'services/DataService'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { OverallAopConsumptionApiService } from 'components/aop-phase-two/services/common/overallAopConsumptionApiService'
import MaterialGroupedSelectionGrid from '../material-grouped-selection/MaterialGroupedSelectionGrid'

const OverallAopConsumption = () => {
  const dispatch = useDispatch()
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
    screenTitle,
    isReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_NAME = siteObject?.name
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = screenTitle?.title
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const isFilament = VERTICAL_NAME?.toLowerCase() === 'filament (pfy)'
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const valueFormat = ValueFormatterConsumption()
  const headerMap = generateHeaderNames(AOP_YEAR)

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [grades, setGrades] = useState([])
  const [gradeId, setGradeId] = useState(null)
  const [calculationObject, setCalculationObject] = useState([])
  const [isReleaseDisabled, setIsReleaseDisabled] = useState(true)
  const [openMaterialGroupedSelectionDialog, setOpenMaterialGroupedSelectionDialog] = useState(false)

  // Release states
  const [openReleaseDialogBox, setOpenReleaseDialogBox] = useState(false)

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const { items: menuItems } = useMenuContext()
  const showReleaseButton = shouldShowReleaseButton(menuItems)

  const monthsConfig = [
    { field: 'april', key: 4, title: 'April' },
    { field: 'may', key: 5, title: 'May' },
    { field: 'june', key: 6, title: 'June' },
    { field: 'july', key: 7, title: 'July' },
    { field: 'aug', key: 8, title: 'August' },
    { field: 'sep', key: 9, title: 'September' },
    { field: 'oct', key: 10, title: 'October' },
    { field: 'nov', key: 11, title: 'November' },
    { field: 'dec', key: 12, title: 'December' },
    { field: 'jan', key: 1, title: 'January' },
    { field: 'feb', key: 2, title: 'February' },
    { field: 'march', key: 3, title: 'March' },
  ]

  const columns = useMemo(() => {
    const defaultColumns = [
      {
        field: 'Particulars',
        title: 'Type',
        editable: false,
        hidden: true,
        minWidth: 100,
      },
      {
        field: 'productName',
        title: 'Particulars',
        editable: false,
        minWidth: 200,
      },
      {
        field: 'sapCode',
        title: 'SAP Code',
        minWidth: 120,
        type: 'text',
        editable: false,
      },
      {
        field: 'UOM',
        title: 'UOM',
        editable: false,
        minWidth: 100,
      },
      ...monthsConfig.map((m) => ({
        field: m.field,
        title: headerMap[m.key] || m.title,
        editable: false,
        type: 'number1',
        format: valueFormat,
        minWidth: 120,
      })),
      // {
      //   field: 'avgOfAllMonths',
      //   title: 'YTD',
      //   editable: false,
      //   type: 'number1',
      //   hidden: true,
      //   format: valueFormat,
      //   minWidth: 120,
      // },
      // {
      //   field: 'aopRemarks',
      //   title: 'Remark',
      //   type: 'textarea',
      //   editable: true,
      //   minWidth: 160,
      // },
    ]
    if (isFilament) {
      defaultColumns.push({
        field: 'ytd',
        title: 'YTD',
        editable: false,
        type: 'number1',
        hidden: false,
        format: valueFormat,
        minWidth: 120,
      })
    }

    return defaultColumns
  }, [isFilament])

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
    getIsReleased()
  }, [getIsReleased])

  const fetchGradeDropdowns = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const response = await OverallAopConsumptionApiService.getGrades(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200 && Array.isArray(response?.data)) {
        setGrades(response.data)
        if (response.data.length > 0) {
          const firstGrade = response.data[0]
          const firstId =
            firstGrade?.id ??
            firstGrade?.gradeId ??
            firstGrade?.gradeFkId ??
            null
          setGradeId(firstId)
        } else {
          setGradeId(null)
        }
      } else {
        setGrades([])
        setGradeId(null)
      }
    } catch (error) {
      setGrades([])
      setGradeId(null)
      console.error('Error fetching grades dropdown:', error)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const fetchGradeDropdownsAfterCalc = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      setGrades([])
      const response = await OverallAopConsumptionApiService.getGrades(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.code === 200 && Array.isArray(response?.data)) {
        setGrades(response.data)
        if (response.data.length === 0) {
          setGradeId(null)
          return
        }
        const firstGrade = response.data[0]
        const firstId =
          firstGrade?.id ?? firstGrade?.gradeId ?? firstGrade?.gradeFkId ?? null
        setGradeId(firstId)
      } else {
        setGrades([])
        setGradeId(null)
      }
    } catch (error) {
      setGrades([])
      setGradeId(null)
      console.error('Error fetching grades after calculation:', error)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR])

  const fetchData = useCallback(
    async () => {
      if (!PLANT_ID || !AOP_YEAR) return
      setLoading(true)
      try {
        const verticalWiseAPI = {
          'staple (psf)':OverallAopConsumptionApiService.getOverallAopConsumption,
          'filament (pfy)':OverallAopConsumptionApiService.getOverallAopConsumptionYTD,
          'pet-py':OverallAopConsumptionApiService.getOverallAopConsumption,
        }
        const response =
          await verticalWiseAPI[VERTICAL_NAME?.toLowerCase()](keycloak, PLANT_ID, AOP_YEAR,
            null,
          )
        if (response?.code === 200) {
          setCalculationObject(response?.data?.aopCalculation || {})
          const monthFields = monthsConfig.map((m) => m.field)
          const formattedData = response?.data?.aopConsumptionNormDTOList?.map(
            (item, index) => {
              const total = monthFields.reduce((sum, month) => {
                const value = parseFloat(item[month]) || 0
                return sum + value
              }, 0)
              const avgOfAllMonths = total / monthFields.length
              return {
                ...item,
                idFromApi: item.id,
                NormParametersId: item.materialFkId?.toLowerCase(),
                originalRemark: item.aopRemarks?.trim() || null,
                id: index,
                isEditable: true,
                Particulars: item.normParameterTypeDisplayName || '',
                avgOfAllMonths,
              }
            },
          )
          setRows(formattedData || [])
        } else {
          setRows([])
        }
      } catch (error) {
        console.error('Error fetching overall AOP consumption data:', error)
        setRows([])
      } finally {
        setLoading(false)
      }
    },
    [keycloak, PLANT_ID, AOP_YEAR],
  )

  // Initial load
  // useEffect(() => {
  //   fetchGradeDropdowns()
  // }, [fetchGradeDropdowns])

  // Fetch data on grade change
  useEffect(() => {
    fetchData()
  }, [fetchData])

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
      const requiredFields = ['aopRemarks']
      const validationMessage = validateFields(data, requiredFields)
      if (validationMessage) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: validationMessage,
          severity: 'error',
        })
        return
      }

      const businessData = data.map((row) => ({
        april: row.april || null,
        may: row.may || null,
        june: row.june || null,
        july: row.july || null,
        aug: row.aug || null,
        sep: row.sep || null,
        oct: row.oct || null,
        nov: row.nov || null,
        dec: row.dec || null,
        jan: row.jan || null,
        feb: row.feb || null,
        march: row.march || null,
        aopRemarks: row.aopRemarks || null,
        aopYear: AOP_YEAR,
        plantFkId: PLANT_ID,
        siteFkId: siteObject?.id,
        verticalFkId: VERTICAL_ID,
        materialFkId: row.NormParametersId,
        id: row.idFromApi || null,
        aopCaseId: '2025-26-NormsAOP',
        aopStatus: 'Saved',
      }))

      setLoading(true)
      const res =
        await OverallAopConsumptionApiService.saveOverallAopConsumption(
          keycloak,
          PLANT_ID,
          businessData,
        )

      if (res?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        dispatch(setIsBlocked(false))
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Save Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error saving overall consumption data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Save failed, please try again!',
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
    dispatch,
    siteObject,
    VERTICAL_ID,
  ])

  const handleCalculate = () => {
    setOpenMaterialGroupedSelectionDialog(true)
  }

  const handleCallCalculate = async () => {
    setLoading(true)
    try {
      const data =
        await OverallAopConsumptionApiService.calculateOverallAopConsumption(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      if (data || data === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Refresh Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
      console.error('Error calculating overall norms:', error)
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
      const verticalWiseAPI = {
        'staple (psf)':OverallAopConsumptionApiService.exportOverallAopConsumption,
        'filament (pfy)':OverallAopConsumptionApiService.exportOverallAopConsumptionYTD,
        'pet-py':OverallAopConsumptionApiService.exportOverallAopConsumption,
      }
      const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}`
      await verticalWiseAPI[VERTICAL_NAME?.toLowerCase()](
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
        SCREEN_NAME,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel downloaded successfully!',
        severity: 'success',
      })
    } catch (error) {
      console.error('Error exporting overall norms excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    }
  }

  const handleRemarkCellClick = useCallback(
    (row) => {
      if (READ_ONLY) return
      setCurrentRemark(row.aopRemarks || '')
      setCurrentRowId(row.id)
      setRemarkDialogOpen(true)
    },
    [READ_ONLY],
  )

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
    showAction: false,
    addButton: false,
    deleteButton: false,
    editButton: false,
    showUnit: false,
    saveBtn: false,
    showCalculate: true,
    calculateDisabled: !(
      calculationObject && Object.keys(calculationObject).length > 0
    ),
    allAction: true,
    showDropdown: false,
    showExport: true,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: `${SCREEN_NAME}`,
  }

  const dropdownConfig = {
    options: grades,
    label: 'Grade',
    placeholder: 'Select Grade',
    valueKey: 'gradeId',
    labelKey: 'displayName',
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />
      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title={SCREEN_NAME}
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
        handleCalculate={handleCallCalculate}
        handleRemarkCellClick={handleRemarkCellClick}
        handleExport={handleExport}
        groupBy='Particulars'
        dropdownConfig={dropdownConfig}
        selectedDropdownValue={gradeId || ''}
        setSelectedDropdownValue={setGradeId}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        snackbarData={snackbarData}
        setSnackbarData={setSnackbarData}
        isReleaseDisabled={isReleaseDisabled}
        handleRelease={handleRelease}
      />

      <ReleaseDialog
        openReleaseDialogBox={openReleaseDialogBox}
        closeReleaseDialogBox={closeReleaseDialogBox}
        submitConfirmation={submitConfirmation}
      />
      <Dialog
        open={openMaterialGroupedSelectionDialog}
        onClose={(event, reason) => {
          if (reason !== 'backdropClick') {
            setOpenMaterialGroupedSelectionDialog(false)
          }
        }}
        maxWidth="md"
        fullWidth
        disableScrollLock
        disableEnforceFocus={true}
        PaperProps={{
          sx: {
            borderRadius: '20px',
            p: 1,
            backdropFilter: 'blur(8px)',
            boxShadow: '0 10px 30px rgba(0,0,0,0.08)',
          },
        }}
      >
        <DialogContent sx={{ p: 1 }}>
          <MaterialGroupedSelectionGrid
            onSaveSuccess={async () => {
              setOpenMaterialGroupedSelectionDialog(false)
              await handleCallCalculate()
            }}
          />
        </DialogContent>
        <DialogActions sx={{ px: 1.5, pb: 1 }}>
          <Button
            onClick={() => setOpenMaterialGroupedSelectionDialog(false)}
            variant="contained"
            className="btn-no"
            sx={{ textTransform: 'none' }}
          >
            Close
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default OverallAopConsumption
