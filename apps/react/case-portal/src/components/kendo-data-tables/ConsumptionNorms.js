import CircularProgress from '@mui/material/CircularProgress'
import { useGridApiRef } from '@mui/x-data-grid'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import React, { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { setIsBlocked, setIsReleased } from 'store/reducers/dataGridStore'
import { validateFields } from 'utils/validationUtils'
import getEnhancedColDefs from '../data-tables/CommonHeader/kendoconsumptionHeader'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import {
  Backdrop,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material'
import KendoDataTables from './index'
import { ConsumptionNormsApiService } from 'services/consumption-norms-api-service'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { DataService } from 'services/DataService'
import { shouldShowReleaseButton } from 'utils/releaseButtonUtils'
import { useMenuContext } from 'menu/menuProvider'
import MaterialGroupedSelection from './MaterialGroupedSelection'

const ConsumptionNorms = () => {
  const [modifiedCells, setModifiedCells] = React.useState({})
  const [calculationObject, setCalculationObject] = useState([])
  const keycloak = useSession()

  const [open1, setOpen1] = useState(false)
  const [openMaterialGroupedSelectionDialog, setOpenMaterialGroupedSelectionDialog] = useState(false)
  const valueFormat = ValueFormatterConsumption()

  const defaultCustomHeight = { mainBox: '55vh', otherBox: '112%' }

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
    // setIsReleased,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = screenTitle?.title
  const headerMap = generateHeaderNames(AOP_YEAR)

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const lowerPlantName = plantObject?.name?.toLowerCase()

  const [loading, setLoading] = useState(false)
  const apiRef = useGridApiRef()
  const [rows, setRows] = useState()
  const [selectedUnit, setSelectedUnit] = useState('TPH')
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [calculatebtnClicked, setCalculatebtnClicked] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [_plantID, set_PlantID] = useState('')
  const dispatch = useDispatch()
  const [gradeId, setGradeId] = useState(null)
  const [grades, setGrades] = useState([])
  const [openReleaseDialogBox, setOpenReleaseDialogBox] = useState(false)
  const [isReleaseDisabled, setIsReleaseDisabled] = useState(true)

  const { items: menuItems } = useMenuContext()
  const showReleaseButton = shouldShowReleaseButton(menuItems)

  // console.log('showReleaseButton', showReleaseButton)

  // const { setIsReleased } = dataGridStore

  const isPEPP = lowerVertName === 'pe' || lowerVertName === 'pp'
  const isPET = lowerVertName === 'pet'
  const IS_PVC_VMD = lowerVertName === 'pvc' && lowerSiteName === 'vmd'
  const IS_ELASTOMER_HMD_SBR =
    VERTICAL_NAME_NO_CASE === 'ELASTOMER' &&
    SITE_NAME_NO_CASE === 'HMD' &&
    PLANT_NAME_NO_CASE === 'SBR'

  const IS_ELASTOMER_JMD_HIIR =
    VERTICAL_NAME_NO_CASE === 'ELASTOMER' &&
    SITE_NAME_NO_CASE === 'JMD' &&
    PLANT_NAME_NO_CASE === 'HIIR'

  const IS_PVC_DMD = lowerVertName === 'pvc' && lowerSiteName === 'dmd'
  const IS_PVC_HMD = lowerVertName === 'pvc' && lowerSiteName === 'hmd'
  const unsavedChangesRef = React.useRef({
    unsavedRows: {},
    rowsBeforeChange: {},
  })

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.aopRemarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const getIsReleased = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    try {
      const response = await DataService.getReleaseAOPStatus(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      // If response has data, disable the button (already released)
      // If no data, enable the button (not yet released)
      if (response?.data && Object.keys(response.data).length > 0) {
        setIsReleaseDisabled(true)
      } else {
        setIsReleaseDisabled(false)
      }
    } catch (error) {
      console.error('Error fetching release status:', error)
    }
  }
  useEffect(() => {
    getIsReleased()
  }, [keycloak, AOP_YEAR, PLANT_ID])
  const saveEditedData = async (newRows) => {
    setLoading(true)
    try {
      let plantId = PLANT_ID
      let siteID = SITE_ID
      let verticalId = VERTICAL_ID
      const businessData = newRows.map((row) => ({
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
        plantFkId: plantId,
        siteFkId: siteID,
        verticalFkId: verticalId,
        materialFkId: row.NormParametersId,
        id: row.idFromApi || null,
        aopCaseId: '2025-26-NormsAOP',
        aopStatus: 'Saved',
      }))
      const response = await ConsumptionNormsApiService.saveAOPConsumptionNorm(
        PLANT_ID,
        businessData,
        keycloak,
      )
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully!',
        severity: 'success',
      })
      //

      setLoading(false)
      setModifiedCells({})

      unsavedChangesRef.current = {
        unsavedRows: {},
        rowsBeforeChange: {},
      }
      fetchData(gradeId)
      dispatch(setIsBlocked(false))

      return response
    } catch (error) {
      console.error('Error saving data!', error)
    } finally {
      //
      setLoading(false)
    }
  }

  const saveChanges = React.useCallback(async () => {
    setLoading(true)

    setTimeout(() => {
      if (lowerVertName == 'meg') {
        try {
          var data = Object.values(modifiedCells)
          if (data.length == 0) {
            setSnackbarOpen(true)
            setSnackbarData({
              message: 'No Records to Save!',
              severity: 'info',
            })
            //
            setLoading(false)

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
            //
            setLoading(false)
            return
          }

          saveEditedData(data)
        } catch (error) {
          console.log('Error saving changes:', error)
          //
          setLoading(false)
        }
      }

      if (
        lowerVertName == 'pe' ||
        IS_ELASTOMER_JMD_HIIR ||
        lowerVertName == 'pp' ||
        lowerVertName == 'pet' ||
        IS_PVC_VMD ||
        IS_PVC_DMD ||
        IS_PVC_HMD
      ) {
        try {
          setLoading(true)

          var editedData = Object.values(modifiedCells)

          const requiredFields = ['aopRemarks']

          const validationMessage = validateFields(editedData, requiredFields)
          if (validationMessage) {
            setSnackbarOpen(true)
            setSnackbarData({
              message: validationMessage,
              severity: 'error',
            })

            setLoading(false)
            return
          }

          if (calculatebtnClicked == false) {
            // if (editedData.length === 0) {
            //   setSnackbarOpen(true)
            //   setSnackbarData({
            //     message: 'No Records to Save!',
            //     severity: 'info',
            //   })
            //   setLoading(false)

            //   setCalculatebtnClicked(false)
            //   return
            // }
            //UNCOMMNET THIS IF saveBtn IS SET TO --> TRUE
            saveEditedData(editedData)

            // setLoading(false)
            setCalculatebtnClicked(false)
            // saveEditedData(editedData)
          } else {
            saveEditedData(editedData)
          }
        } catch (error) {
          setLoading(false)
          console.log('Error saving changes:', error)
          setCalculatebtnClicked(false)
        }
      }
    }, 400)
  }, [apiRef, selectedUnit, modifiedCells, calculatebtnClicked])

  const fetchGradeDropdowns = async () => {
    try {
      const response =
        await ConsumptionNormsApiService.getConsumptionAOPNormsGrades(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      if (response?.code == 200) {
        setGrades(response?.data)
      }

      fetchData(response?.data[0]?.gradeId)
    } catch (error) {
      setGrades([])
      console.error('Error fetching data:', error)
    }
  }

  const fetchGradeDropdownsAfterCalc = async () => {
    try {
      setGrades([])
      const response =
        await ConsumptionNormsApiService.getConsumptionAOPNormsGrades(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      if (response?.code == 200) {
        setGrades(response?.data)
      }

      if (response?.data?.length === 0) {
        setGradeId(null)
        await fetchData(null)
        return
      }

      const firstGrade = response?.data[0]
      const firstId =
        firstGrade?.id ?? firstGrade?.gradeId ?? firstGrade?.gradeFkId ?? null

      setGradeId(firstId)

      fetchData(firstId)
    } catch (error) {
      setGrades([])
      console.error('Error fetching Business Demand data:', error)
    }
  }

  const fetchData = async (gradeId) => {
    if (!PLANT_ID || !AOP_YEAR) return
    if (
      (isPEPP ||
        isPET ||
        IS_ELASTOMER_HMD_SBR ||
        IS_ELASTOMER_JMD_HIIR ||
        IS_PVC_VMD ||
        IS_PVC_DMD ||
        IS_PVC_HMD) &&
      !gradeId
    )
      return
    setLoading(true)
    try {
      var response
      setRows([])
      if (
        lowerVertName === 'pe' ||
        lowerVertName === 'pp' ||
        lowerVertName === 'pet' ||
        IS_ELASTOMER_HMD_SBR ||
        IS_ELASTOMER_JMD_HIIR ||
        IS_PVC_VMD ||
        IS_PVC_DMD ||
        IS_PVC_HMD
      ) {
        response = await ConsumptionNormsApiService.getConsumptionNormsData(
          keycloak,
          gradeId,
          PLANT_ID,
          AOP_YEAR,
        )
      } else {
        response = await ConsumptionNormsApiService.getConsumptionNormsData(
          keycloak,
          null,
          PLANT_ID,
          AOP_YEAR,
        )
      }

      if (response?.code != 200) {
        setRows([])
        setLoading(false)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Error fetching data. Please try again.',
          severity: 'error',
        })

        return
      }
      setCalculationObject(response?.data?.aopCalculation)

      const monthFields = [
        'april',
        'may',
        'june',
        'july',
        'aug',
        'sep',
        'oct',
        'nov',
        'dec',
        'jan',
        'feb',
        'march',
      ]

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
            isEditable: false,
            Particulars: item.normParameterTypeDisplayName || 'Type',
            avgOfAllMonths,
          }
        },
      )

      setRows(formattedData)
      setLoading(false)
      setCalculatebtnClicked(false)
    } catch (error) {
      console.error('Error fetching data:', error)
      setLoading(false)
      setCalculatebtnClicked(false)
    }
  }

  useEffect(() => {
    // fetchData(gradeId)
    if (
      lowerVertName === 'pe' ||
      lowerVertName === 'pp' ||
      lowerVertName === 'pet' ||
      IS_ELASTOMER_HMD_SBR ||
      IS_ELASTOMER_JMD_HIIR ||
      IS_PVC_VMD ||
      IS_PVC_DMD ||
      IS_PVC_HMD
    ) {
      fetchGradeDropdowns()
    } else {
      fetchData(null)
    }
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

  const productionColumns = getEnhancedColDefs({
    headerMap,
    lowerVertName,
    lowerSiteName,
    lowerPlantName,
    valueFormat,
  })

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
      const response = await DataService.releaseAOPReport(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Released Successfully!',
        severity: 'success',
      })
      setIsReleaseDisabled(true)
      let isReleased = 1
      dispatch(setIsReleased({ isReleased }))
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

  const handleUnitChange = (unit) => {
    setSelectedUnit(unit)
  }

  const handleCalculate = () => {
    if (lowerVertName === 'pe' && lowerSiteName === 'c2') {
      setOpenMaterialGroupedSelectionDialog(true)
    } else {
      handleCalculateMeg()
    }
  }

  const handleCalculateMeg = async () => {
    try {
      const data =
        await ConsumptionNormsApiService.handleCalculateConsumptionNorms(
          PLANT_ID,
          AOP_YEAR,
          keycloak,
        )

      if (data || data == 0) {
        // dispatch(setIsBlocked(true))
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })

        if (
          lowerVertName === 'pe' ||
          lowerVertName === 'pp' ||
          lowerVertName === 'pet' ||
          IS_ELASTOMER_HMD_SBR ||
          IS_ELASTOMER_JMD_HIIR ||
          IS_PVC_VMD ||
          IS_PVC_DMD ||
          IS_PVC_HMD
        ) {
          fetchGradeDropdownsAfterCalc()
        } else {
          fetchData(null)
        }
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Refresh Falied!',
          severity: 'error',
        })
      }

      return data
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
      console.error('Error!', error)
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
      if (
        lowerVertName === 'pe' ||
        lowerVertName === 'pp' ||
        lowerVertName === 'pet' ||
        IS_ELASTOMER_HMD_SBR ||
        IS_ELASTOMER_JMD_HIIR ||
        IS_PVC_VMD ||
        IS_PVC_DMD ||
        IS_PVC_HMD
      ) {
        response =
          await ConsumptionNormsApiService.OverallConsumptionPEPPExport(
            keycloak,
            PLANT_ID,
            AOP_YEAR,
            EXCEL_EXPORT_TITLE,
            SCREEN_NAME,
          )
      }
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
      showCalculate: true,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      units: ['TPH', 'TPD'],
      saveWithRemark: true,
      saveBtn: false,
      showCalculate: true,
      allAction: true,
      dontClearGradeOnCalculate: true,
      showCalculateVisibility:
        Object.keys(calculationObject || {}).length > 0 ? true : false,
      showRefresh: false,
      noColor: false,
      customHeight: defaultCustomHeight,
      showG:
        lowerVertName === 'pe' ||
          lowerVertName === 'pp' ||
          lowerVertName === 'pet' ||
          IS_ELASTOMER_HMD_SBR ||
          IS_ELASTOMER_JMD_HIIR ||
          IS_PVC_VMD ||
          IS_PVC_DMD ||
          IS_PVC_HMD
          ? true
          : false,
      marginBottom:
        lowerVertName === 'pe' ||
          lowerVertName === 'pp' ||
          lowerVertName === 'pet' ||
          IS_ELASTOMER_HMD_SBR ||
          IS_ELASTOMER_JMD_HIIR ||
          IS_PVC_VMD ||
          IS_PVC_DMD ||
          IS_PVC_HMD
          ? true
          : false,
      dropdownLabel: 'Grade',
      downloadExcelBtnFromUI:
        lowerVertName === 'pe' ||
          lowerVertName === 'pp' ||
          lowerVertName === 'pet' ||
          IS_ELASTOMER_HMD_SBR ||
          IS_ELASTOMER_JMD_HIIR ||
          IS_PVC_VMD ||
          IS_PVC_DMD ||
          IS_PVC_HMD
          ? false
          : true,
      downloadExcelBtn:
        lowerVertName === 'pe' ||
          lowerVertName === 'pp' ||
          lowerVertName === 'pet' ||
          IS_ELASTOMER_HMD_SBR ||
          IS_ELASTOMER_JMD_HIIR ||
          IS_PVC_VMD ||
          IS_PVC_DMD ||
          IS_PVC_HMD
          ? true
          : false,
      ExcelName: `${EXCEL_EXPORT_TITLE}_${SCREEN_NAME}`,
      isHeight: lowerVertName !== 'meg' && rows?.length > 10,
      showTitleNameBusiness: true,
      showReleaseBtn: showReleaseButton ? true : false,
      titleName: `${SCREEN_NAME}`,
    },
    isOldYear,
  )

  const handleGradeChange = (gradeId) => {
    setGradeId(gradeId)
    fetchData(gradeId)
  }

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      <div>
        {
          <Box>
            <KendoDataTables
              autoHeight={true}
              modifiedCells={modifiedCells}
              setModifiedCells={setModifiedCells}
              columns={productionColumns}
              rows={rows}
              setRows={setRows}
              getRowId={(row) => row.id}
              title='Consumption AOP'
              paginationOptions={[100, 200, 300]}
              saveChanges={saveChanges}
              snackbarData={snackbarData}
              snackbarOpen={snackbarOpen}
              apiRef={apiRef}
              open1={open1}
              setOpen1={setOpen1}
              setSnackbarOpen={setSnackbarOpen}
              setSnackbarData={setSnackbarData}
              handleCalculate={handleCalculate}
              handleRemarkCellClick={handleRemarkCellClick}
              // fetchData={fetchData}

              handleUnitChange={handleUnitChange}
              remarkDialogOpen={remarkDialogOpen}
              setRemarkDialogOpen={setRemarkDialogOpen}
              currentRemark={currentRemark}
              setCurrentRemark={setCurrentRemark}
              currentRowId={currentRowId}
              permissions={adjustedPermissions}
              groupBy='Particulars'
              grades={grades}
              handleGradeChange={handleGradeChange}
              calculatebtnClicked={calculatebtnClicked}
              downloadExcelForConfiguration={downloadExcelForConfiguration}
              plantID={PLANT_ID}
              isReleaseDisabled={isReleaseDisabled}
              handleRelease={handleRelease}
            />
          </Box>
        }
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
          <DialogTitle
            sx={{
              fontWeight: 700,
              fontSize: '1.2rem',

              pb: 0.5,
            }}
          >
            Confirm Release
          </DialogTitle>

          <DialogContent sx={{ pt: 1 }}>
            <DialogContentText
              sx={{
                fontSize: '0.9rem',
                color: '#4b5563',
                lineHeight: 1.5,
              }}
            >
              Please confirm that <b style={{ color: '#16a34a' }}>Production</b>
              , <b style={{ color: '#16a34a' }}>Norms</b>, and{' '}
              <b style={{ color: '#16a34a' }}>Reports</b> are verified before
              releasing for review.
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
              sx={{
                textTransform: 'none',
                px: 2.5,
              }}
            >
              Confirm
            </Button>
          </DialogActions>
        </Dialog>{' '}
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
            <MaterialGroupedSelection
              onSaveSuccess={async () => {
                setOpenMaterialGroupedSelectionDialog(false)
                await handleCalculateMeg()
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
      </div>
    </div>
  )
}

export default ConsumptionNorms
