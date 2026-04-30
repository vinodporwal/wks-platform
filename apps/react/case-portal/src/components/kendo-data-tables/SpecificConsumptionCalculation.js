import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { Box } from '@mui/material'
import React, { useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { SpecificConsumptionService } from 'services/SpecificConsumptionService'
import KendoDataTables from './index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const SpecificConsumptionCalculation = () => {
  const [loading, setLoading] = useState(false)
  const [rows1, setRows1] = useState([])
  const [calculationColumns, setCalculationColumns] = useState([])
  const [detailColumns, setDetailColumns] = useState([])
  const [rows2, setRows2] = useState([])
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [selectedUnit, setSelectedUnit] = useState('MT')
  const [editResetKey, setEditResetKey] = useState(0)
  const keycloak = useSession()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, siteObject, verticalObject } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()

  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

  const isOldYear = false

  // Computes a totals row by summing all numeric fields across data rows.
  const computeTotalsRow = (data, cols, labelField = 'Name') => {
    if (!data?.length || !cols?.length) return null
    const totals = { [labelField]: 'Total', isTotal: true, id: '__total__' }
    cols.forEach((col) => {
      if (col.field === labelField || col.type === 'string') return
      const sum = data.reduce((acc, row) => {
        const val = parseFloat(row[col.field])
        return acc + (isNaN(val) ? 0 : val)
      }, 0)
      totals[col.field] = Math.round(sum * 10000) / 10000
    })
    return totals
  }

  const fetchGrid1Data = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await SpecificConsumptionService.getCombinedMCU(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.data) {
        const dataSet = response.data?.data || []
        const cols = (response.data?.columns || [])
          .filter((col) => col.field?.toLowerCase() !== 'id')
          .map((col) => ({
            ...col,
            format: col.type === 'number' ? '{0:0.000}' : col.format,
          }))

        const data = dataSet.map((item, index) => {
          const isKiloTon = selectedUnit === 'KT'
          const transformedItem = {
            ...item,
            idFromApi: item.id,
            uom: selectedUnit || 'MT',
            id: index,
            isEditable: false,
          }

          // Apply conversion to dynamic month columns (e.g., 'Apr-26')
          cols.forEach((col) => {
            if (col.type === 'number' && col.field !== 'Name') {
              const val = item[col.field]
              transformedItem[col.field] =
                isKiloTon && val && index !== 0 && index !== 2
                  ? val / 1000
                  : val
            }
          })

          // Calculate Total (row total) using transformed values
          const rowTotal = cols
            .filter((col) => col.type === 'number' && col.field !== 'Name')
            .reduce(
              (sum, col) => sum + (parseFloat(transformedItem[col.field]) || 0),
              0,
            )

          return {
            ...transformedItem,
            Total: rowTotal,
            _displayNameLower: String(transformedItem.Name || '').toLowerCase(),
          }
        })

        const [startYear, endYear] = AOP_YEAR.split('-').map((y) =>
          y.trim().slice(-2),
        )
        const monthsShort = [
          'Jan',
          'Feb',
          'Mar',
          'Apr',
          'May',
          'Jun',
          'Jul',
          'Aug',
          'Sep',
          'Oct',
          'Nov',
          'Dec',
        ]

        const updatedCols = cols.map((col) => {
          if (monthsShort.includes(col.field)) {
            const isNextYear = ['Jan', 'Feb', 'Mar'].includes(col.field)
            const displayYear = isNextYear ? endYear : startYear
            return {
              ...col,
              title: `${col.field}-${displayYear}`,
              headerAttributes: { style: { textAlign: 'center' } },
              attributes: { style: { textAlign: 'right' } },
            }
          }
          return col
        })

        updatedCols.push({
          field: 'Total',
          editable: false,
          title: 'Total',
          type: 'number',
          format: '{0:0.000}',
          headerAttributes: { style: { textAlign: 'center' } },
          attributes: { style: { textAlign: 'right' } },
        })

        // const totalsRow = computeTotalsRow(data, cols, 'Name')
        // setRows1(totalsRow ? [...data, totalsRow] : data)
        setRows1(data)
        setCalculationColumns(updatedCols)
      } else {
        setRows1([])
        setCalculationColumns([])
      }
    } catch (error) {
      console.error('Error fetching Combined MCU:', error)
      setRows1([])
      setCalculationColumns([])
    } finally {
      setLoading(false)
    }
  }

  const fetchGrid2Data = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await SpecificConsumptionService.getCombinedMCUDetails(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (response?.data) {
        const dataSet = response.data?.data || []
        const cols = (response.data?.columns || [])
          .filter((col) => col.field?.toLowerCase() !== 'id')
          .map((col) => ({
            ...col,
            format: col.type === 'number' ? '{0:0.000}' : col.format,
            editable: false,
            widthT: col.field === 'Value' ? 350 : 250,
          }))

        const data = dataSet.map((item, index) => {
          const transformedItem = {
            ...item,
            id: index,
            isEditable: false,
          }

          return {
            ...transformedItem,
          }
        })
        // const totalsRow = computeTotalsRow(data, cols, 'Name')
        setRows2(data)
        setDetailColumns(cols)
      } else {
        setRows2([])
        setDetailColumns([])
      }
    } catch (error) {
      console.error('Error fetching Details:', error)
      setRows2([])
      setDetailColumns([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchGrid1Data()
  }, [PLANT_ID, AOP_YEAR, selectedUnit])

  useEffect(() => {
    fetchGrid2Data()
  }, [PLANT_ID, AOP_YEAR])

  // Dynamically build totalRowConfiguration from whichever columns the API returns
  const totalRowConfiguration = calculationColumns
    .filter((col) => col.type === 'number')
    .map((col) => ({ field: col.field, aggregate: 'sum' }))

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
      allAction: false,
    }
  }

  const adjustedPermissionsCombined = getAdjustedPermissions(
    {
      showAction: false,
      addButton: false,
      allAction: true,
      adjustedPermissions: true,
      units: ['MT', 'KT'],
      dropdownLabel: 'UOM',
      showUnit: true,
      isTotalFooterActive: false,
      downloadExcelBtnFromUI: true,
      showTitleNameBusiness: true,
      titleName: 'Total Production With Propylene Availability',
      ExcelName: `${EXCEL_EXPORT_TITLE}_Total Production With Propylene Availability_${AOP_YEAR}`,
    },
    isOldYear,
  )

  const adjustedPermissionsDetails = getAdjustedPermissions(
    {
      showAction: false,
      addButton: false,
      allAction: true,
      adjustedPermissions: true,
      showTitleNameBusiness: true,
      titleName: 'Supporting parameters',
      ExcelName: `${EXCEL_EXPORT_TITLE}_Supporting parameters_${AOP_YEAR}`,
    },
    isOldYear,
  )

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <Box sx={{ mb: 1, mt: 1 }}>
        <KendoDataTables
          rows={rows2}
          columns={detailColumns}
          setRows={setRows2}
          title='Supporting parameters'
          fetchData={fetchGrid2Data}
          permissions={adjustedPermissionsDetails}
        />
      </Box>

      <Box sx={{ mb: 1 }}>
        <KendoDataTables
          rows={rows1}
          columns={calculationColumns}
          setRows={setRows1}
          title='Total Production with Propylene Availability'
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          fetchData={fetchGrid1Data}
          permissions={adjustedPermissionsCombined}
          selectedUnit={selectedUnit}
          handleUnitChange={setSelectedUnit}
          resetEditSignal={editResetKey}
          setEditResetKey={setEditResetKey}
        />
      </Box>
    </Box>
  )
}

export default SpecificConsumptionCalculation
