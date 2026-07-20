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
import { setIsReleased } from 'store/reducers/dataGridStore'
import { useSession } from 'SessionStoreContext'
import Notification from 'components/Utilities/Notification'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import React, { useEffect, useState } from 'react'
import { DataService } from 'services/DataService'
import { MockPlantContributionAPILastFourYears } from './mockPlantContributionAPILastFourYears'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import ValueFormatterProductionProductionNormBasis from 'utils/ValueFormatterProduction_ProductionNormBasis'
import { useDispatch, useSelector } from 'react-redux'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
const categories = () => {
  return [
    {
      key: 'ProductMixAndProduction',
      title: 'Plant Contribution Summary (T-22)\nProduct mix and Production',
    },
    { key: 'ByProducts', title: 'By products' },
    { key: 'RawMaterial', title: 'Raw material' },
    { key: 'CatChem', title: 'Cat chem' },
    { key: 'Utilities', title: 'Utilities' },
    { key: 'OtherVariableCost', title: 'Other Variable Cost' },
    { key: 'ProductionCostCalculations', title: 'Cost & Contribution Summary' },
  ]
}

export default function PlantContributionLastFourYears() {
  const keycloak = useSession()
  const dispatch = useDispatch()

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
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const VERTICAL_NAME = verticalObject?.name
  const AOP_YEAR = year?.selectedYear
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear
  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const [loading, setLoading] = useState(false)
  const [reports, setReports] = useState({})
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [modifiedCells, setModifiedCells] = React.useState({})
  const [otherVariableRows, setOtherVariableRows] = useState([])
  const [openReleaseDialogBox, setOpenReleaseDialogBox] = useState(false)
  const [isReleaseDisabled, setIsReleaseDisabled] = useState(true)

  const IS_CRACKER = lowerVertName === 'cracker'

  const formats = IS_CRACKER
    ? {
        dec3: '{0:0.0000}',
        dec2: '{0:0.0000}',
        cost: '{0:0.0000}',
        norms: '{0:0.0000}',
      }
    : {
        dec3:
          lowerVertName === 'elastomer'
            ? '{0:0.000}'
            : lowerVertName === 'pta'
              ? '{0:0.00000}'
              : '{0:0.00}',
        dec2:
          lowerVertName === 'vcm'
            ? '{0:0.000}'
            : lowerVertName === 'pta'
              ? '{0:0.00000}'
              : '{0:0.00}',
        cost:
          lowerVertName === 'elastomer'
            ? '{0:0}'
            : lowerVertName === 'vcm'
              ? '{0:0.000}'
              : lowerVertName === 'pta'
                ? '{0:0.00000}'
                : '{0:0.00}',
        norms: ['meg', 'elastomer', 'pta'].includes(lowerVertName)
          ? '{0:0.00000}'
          : lowerVertName === 'vcm'
            ? '{0:0.000}'
            : '{0:0.00}',
      }

  const FORMAT_VALUES_3_DECIMAL = formats.dec3
  const FORMAT_VALUES_2_DECIMAL = formats.dec2
  const FORMAT_VALUES_COST = formats.cost
  const FORMAT_VALUES_PRICE = '{0:0}'
  const FORMAT_VALUES_NORMS = formats.norms

  const loadAll = async () => {
    setLoading(true)
    const out = {}
    let tempOtherVariableRows = []
    await Promise.all(
      categories().map(async ({ key }) => {
        const { columns, columnGrouping } =
          await MockPlantContributionAPILastFourYears.getReport({
            category: key,
            AOP_YEAR,
            lowerVertName,
            FORMAT_VALUES_3_DECIMAL,
            FORMAT_VALUES_2_DECIMAL,
            FORMAT_VALUES_COST,
            FORMAT_VALUES_PRICE,
            FORMAT_VALUES_NORMS,
          })

        const apiResp = await DataService.plantContributionPlanLastFourYears(
          keycloak,
          key,
          PLANT_ID,
          AOP_YEAR,
        )
        let rows = apiResp.data?.plantProductionData || []
        if (apiResp?.code == 200) {
          const suffixCountMap = {
            ProductMixAndProduction: 4,
            ByProducts: 2,
            RawMaterial: 3,
            ProductionCostCalculations: 6,
            CatChem: 2,
            Utilities: 2,
            OtherVariableCost: 2,
          }
          const suffixCount = suffixCountMap[key] || 0

          rows = apiResp?.data?.plantProductionData.map((item, index, arr) => {
            const isBold =
              lowerVertName !== 'meg' && index >= arr.length - suffixCount
            return {
              ...item,
              id: index,
              actualId: item?.id,
              isEditable: false,
              isdisable: true,
              isBold,
            }
          })
          if (key === 'OtherVariableCost') {
            tempOtherVariableRows = rows
          }
        }
        out[key] = { columns, columnGrouping, rows }
      }),
    )

    setReports(out)
    setOtherVariableRows(tempOtherVariableRows)
    setLoading(false)
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
    loadAll()
    getIsReleased()
  }, [keycloak, AOP_YEAR, PLANT_ID])

  const handleCalculate = () => {
    handleCalculateMonthwiseAndTurnaround()
  }

  const handleRemarkCellClick = (row) => {
    setCurrentRemark(row.Remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = async () => {
    try {
      if (Object.keys(modifiedCells).length === 0) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'No Records to Save!',
          severity: 'info',
        })
        setLoading(false)
        return
      }

      const data = Object.values(modifiedCells)
      const rowsToUpdate = data.map((row) => ({
        id: row?.actualId,
        actualFourYearsAgo: row.actualFourYearsAgo,
        actualThreeYearsAgo: row.actualThreeYearsAgo,
        actualTwoYearsAgo: row.actualTwoYearsAgo,
        actualLastYear: row.actualLastYear,
        budgetCurrent: row.budgetCurrent,
      }))

      const res = await DataService.savePlantContributionlastfourData(
        keycloak,
        rowsToUpdate,
      )

      if (res?.code == 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Successfully!',
          severity: 'success',
        })
        // Optionally reload data here
        loadAll()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Saved Failed!',
          severity: 'error',
        })
      }
    } catch (err) {
      setSnackbarOpen(true)
      setSnackbarData({ message: err.message, severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

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

  return (
    <Box sx={{ width: '100%' }}>
      <LoaderBackdrop open={!!loading} />

      {/* Main Categories Except 'OtherVariableCost' */}
      {categories()
        .filter(
          (item) =>
            item.key !== 'OtherVariableCost' &&
            item.key !== 'ProductionCostCalculations',
        )
        .map(({ key, title }, idx) => {
          const rpt = reports[key] || {}
          return (
            <Box key={key} sx={{ mt: 0 }}>
              <KendoDataTablesReports
                key={IS_RELEASED}
                columns={rpt.columns || []}
                rows={rpt.rows || []}
                title={title}
                setRows={() => {}}
                permissions={{
                  textAlignment: 'center',
                  showCalculate: false,
                  showFinalSubmit: false,
                  showTitle: true,
                }}
                handleRelease={handleRelease}
                isReleaseDisabled={isReleaseDisabled}
              />
            </Box>
          )
        })}

      {/* Separate Grid for 'OtherVariableCost' */}
      {(() => {
        const key = 'OtherVariableCost'
        const rpt = reports[key] || {}
        return (
          <Box key={key} sx={{ mt: 1 }}>
            <KendoDataTablesReports
              key={IS_RELEASED}
              modifiedCells={modifiedCells}
              setRows={setOtherVariableRows}
              columns={rpt.columns || []}
              rows={otherVariableRows || []}
              title={'Other Variable Cost'}
              setRemarkDialogOpen={setRemarkDialogOpen}
              currentRemark={currentRemark}
              setCurrentRemark={setCurrentRemark}
              currentRowId={currentRowId}
              setCurrentRowId={setCurrentRowId}
              loading={loading}
              handleRemarkCellClick={handleRemarkCellClick}
              setModifiedCells={setModifiedCells}
              permissions={{
                customHeight: { mainBox: '32vh', otherBox: '100%' },
                textAlignment: 'center',
                remarksEditable: true,
                showCalculate: false,
                saveBtnForRemark: true,
                saveBtn: false,
                showWorkFlowBtns: true,
                showTitle: true,
              }}
              saveChanges={saveChanges}
            />
          </Box>
        )
      })()}
      {/* Last: Production Cost Calculations */}
      {(() => {
        const key = 'ProductionCostCalculations'
        const rpt = reports[key] || {}
        return (
          <Box key={key} sx={{ mt: 0 }}>
            <KendoDataTablesReports
              key={IS_RELEASED}
              columns={rpt.columns || []}
              rows={rpt.rows || []}
              title={rpt.title || 'Cost & Contribution Summary'}
              setRows={() => {}}
              permissions={{
                textAlignment: 'center',
                showCalculate: false,
                showFinalSubmit: false,
                showTitle: true,
              }}
            />
          </Box>
        )
      })()}

      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />

      <Dialog
        open={openReleaseDialogBox}
        onClose={closeReleaseDialogBox}
        aria-labelledby='alert-dialog-title'
        aria-describedby='alert-dialog-description'
        disableScrollLock
        slotProps={{
          backdrop: { disableScrollLock: true },
        }}
      >
        <DialogTitle id='alert-dialog-title'>
          {'Confirm AOP Release? '}
        </DialogTitle>
        <DialogContent>
          <DialogContentText
            id='alert-dialog-description'
            sx={{ color: 'text.primary' }}
          >
            Warning: User will not be able to edit Production or Norms values
            after this action is completed.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeReleaseDialogBox}>Cancel</Button>
          <Button onClick={submitConfirmation} autoFocus>
            Submit
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
