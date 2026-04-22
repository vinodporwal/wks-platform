import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { useGridApiRef } from '@mui/x-data-grid'
import { useSession } from 'SessionStoreContext'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import React, { useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'

import { ProductionNormsApiService } from 'services/production-norms-api-service'
import { getRoleName } from 'services/role-service'
import KendoDataTables from './index'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'

const NaphthaLimsDataSet = ({ permissions }) => {
  const [editResetKey, setEditResetKey] = useState(0)
  const [modifiedCellsC2C3R, setModifiedCellsC2C3R] = React.useState({})
  const [
    calculationObjectOtherProduction,
    setCalculationObjectOtherProduction,
  ] = useState([])

  const keycloak = useSession()
  const apiRefC2C3R = useGridApiRef()
  const dataGridStore = useSelector((state) => state.dataGridStore)

  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_NAME = siteObject?.name
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear

  const PLANT_NAME_UC = plantObject?.name?.toUpperCase()
  const SITE_NAME_UC = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_UC = verticalObject?.name?.toUpperCase()

  const EXCEL_NAME_OTHER_PRODUCTION = `${VERTICAL_NAME_UC}_${SITE_NAME_UC}_${PLANT_NAME_UC}_${AOP_YEAR}_LIMS_Tag`

  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const lowerSiteName = SITE_NAME?.toLowerCase()

  const [loading, setLoading] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  // Data States
  const [rows, setRows] = useState([])
  const [columns, setColumns] = useState([])

  // Remark States
  const [remarkDialogOpenC2C3R, setRemarkDialogOpenC2C3R] = useState(false)
  const [currentRemarkC2C3R, setCurrentRemarkC2C3R] = useState('')
  const [currentRowIdC2C3R, setCurrentRowIdC2C3R] = useState(null)

  const IS_NMD = SITE_NAME?.toLowerCase() === 'nmd'
  const IS_VMD = SITE_NAME?.toLowerCase() === 'vmd'

  const FORMATE_VALUE = ValueFormatterProduction()

  /**
   * Fetch primary Naphtha Lims Data
   */
  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      setLoading(true)
      const response = await ProductionNormsApiService.getNaphthaLimsDataSet(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        // Correct path based on your JSON structure
        const rawRows = response?.data?.data || []
        const rawCols = response?.data?.columns || []

        // Map columns to ensure they have the properties Kendo/DataGrid expects
        const mappedCols = rawCols.map((col) => ({
          ...col,
          header: col.title, // Kendo often uses 'header'
          headerName: col.title, // DataGrid uses 'headerName'
          field: col.field, // Unique ID for the data
          widthT: col.field === 'LIMS Tag Name' ? 200 : 100,
          // Ensure numbers are handled correctly
          type: col.type === 'number' ? 'number' : 'text',
          format: FORMATE_VALUE,
        }))

        // Add an 'id' to rows if it doesn't exist (DataGrids usually require a unique id)
        const rowsWithId = rawRows.map((row, index) => ({
          id: index,
          ...row,
          isEditable: false,
        }))

        setRows(rowsWithId)
        setColumns(mappedCols)
      } else {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({
          message:
            response?.message || 'Error fetching data. Please try again.',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error fetching data:', error)
    } finally {
      setLoading(false)
    }
  }

  // Trigger fetch on dependency change
  useEffect(() => {
    fetchData()
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

  const isCellEditable = (params) => params.row.id !== 'total'

  const getAdjustedPermissionsC2C3R = (permissions, isOldYear) => {
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
      showCalculate: false,
      isOldYear: isOldYear,
      showNote: true,
    }
  }

  const adjustedPermissionsForC2C3R = useMemo(
    () =>
      getAdjustedPermissionsC2C3R(
        {
          showAction: true,
          addButton: false,
          deleteButton: false,
          editButton: false,
          showUnit: false,
          saveWithRemark: true,
          showCalculate: IS_NMD || IS_VMD ? false : true,
          allAction: true,
          showNote: true,
          showTitleNameBusiness: false,
          titleName: '',
          saveBtn: false,
          downloadExcelBtnFromUI: true,
          ExcelName: `${EXCEL_NAME_OTHER_PRODUCTION}`,
          showCalculateVisibility:
            calculationObjectOtherProduction &&
            Object.keys(calculationObjectOtherProduction).length > 0,
        },
        IS_OLD_YEAR,
      ),
    [calculationObjectOtherProduction, IS_OLD_YEAR, IS_NMD, IS_VMD],
  )

  return (
    <div>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      <KendoDataTables
        modifiedCells={modifiedCellsC2C3R}
        setModifiedCells={setModifiedCellsC2C3R}
        columns={columns} // Correctly using the 'columns' state
        rows={rows} // Correctly using the 'rows' state
        setRows={setRows} // Correctly using the 'setRows' setter
        title={'Production AOP'}
        isCellEditable={isCellEditable}
        onAddRow={(newRow) => console.log('New Row Added:', newRow)}
        onDeleteRow={(id) => console.log('Row Deleted:', id)}
        onRowUpdate={(updatedRow) => console.log('Row Updated:', updatedRow)}
        paginationOptions={[100, 200, 300]}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        apiRef={apiRefC2C3R}
        fetchData={fetchData} // Changed to fetchData to refresh the current table
        remarkDialogOpen={remarkDialogOpenC2C3R}
        setRemarkDialogOpen={setRemarkDialogOpenC2C3R}
        currentRemark={currentRemarkC2C3R}
        setCurrentRemark={setCurrentRemarkC2C3R}
        currentRowId={currentRowIdC2C3R}
        permissions={adjustedPermissionsForC2C3R}
        selectedUOM={'UOM'}
        note={''}
        resetEditSignal={editResetKey}
        setEditResetKey={setEditResetKey}
        // groupBy={'Type'}
      />
    </div>
  )
}

export default NaphthaLimsDataSet
