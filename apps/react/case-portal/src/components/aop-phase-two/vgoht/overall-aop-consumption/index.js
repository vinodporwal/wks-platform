import React, { useState, useEffect, useCallback } from 'react'
import { Box, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import ValueFormatterPhaseTwo, {
  customValueFormatterPhaseTwo,
} from '../../common/ValueFormatterPhaseTwo'
import { OverallAopConsumptionApiService } from '../../services/vgoht/overallAopConsumptionApiService'
import useReleaseAOP from 'components/aop-phase-two/common/hooks/useReleaseAOP'
import { overAllAOpResponse } from '../dummyData'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { generateExcelNameWithoutExt } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import MaterialGroupedSelectionDialog, {
  useMaterialGroupedSelectionPopup,
} from 'components/kendo-data-tables/MaterialGroupedSelectionDialog'

const OverallAopConsumption = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const { isReleaseDisabled, handleRelease, ReleaseDialogComponent } =
    useReleaseAOP({
      setSnackbarOpen,
      setSnackbarData,
    })

  const valueFormat = customValueFormatterPhaseTwo(5)
  const headerMap = generateHeaderNames(AOP_YEAR)
  const EXCEL_EXPORT_TITLE = generateExcelNameWithoutExt(
      dataGridStore,
      'Overall_AOP_Consumption'
    )

  const columns = [
    {
      field: 'sapCode',
      title: 'SAP Mat Code',
      // widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: false,
    },
    {
      field: 'productName',
      title: 'Particulars',
      // widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'normParameterTypeDisplayName',
      title: 'normParameterTypeDisplayName',
      // widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
      hidden: true,
    },
    {
      field: 'UOM',
      title: 'UOM',
      // widthT: 100,
      minWidth: 100,
      type: 'text',
      editable: false,
    },
    {
      field: 'april',
      title: headerMap[4],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'may',
      title: headerMap[5],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'june',
      title: headerMap[6],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'july',
      title: headerMap[7],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'aug',
      title: headerMap[8],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'sep',
      title: headerMap[9],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'oct',
      title: headerMap[10],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'nov',
      title: headerMap[11],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'dec',
      title: headerMap[12],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'jan',
      title: headerMap[1],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'feb',
      title: headerMap[2],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'march',
      title: headerMap[3],
      // widthT: 100,
      minWidth: 120,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
  ]

  useEffect(() => {
    if (PLANT_ID && AOP_YEAR) {
      fetchData()
    }
  }, [PLANT_ID, AOP_YEAR])

  const fetchData = async () => {
    setLoading(true)
    try {
      const response =
        await OverallAopConsumptionApiService.getOverallAopConsumption(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      const data =
        response?.data?.aopConsumptionNormDTOList?.map((item) => {
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

          const monthValues = monthFields.map((field) => {
            const val = item[field]
            return val !== null && val !== undefined && !isNaN(val)
              ? Number(val)
              : 0
          })

          const sum = monthValues.reduce((acc, val) => acc + val, 0)
          const avgNorms = sum / 12

          return {
            ...item,
            avgNorms,
            isEditable: false,
          }
        }) || []
      setRows(data)
    } catch (error) {
      console.error('Error fetching overall AOP consumption data:', error)
    } finally {
      setLoading(false)
    }
  }

  const executeCalculate = async () => {
    setLoading(true)
    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Calculating...',
      severity: 'info',
    })

    try {
      const calculatedData =
        await OverallAopConsumptionApiService.calculateOverallAopConsumption(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      setSnackbarData({
        message: 'Calculation completed successfully!',
        severity: 'success',
      })
      await fetchData()
    } catch (error) {
      console.error('Error calculating overall AOP consumption:', error)
      setSnackbarData({
        message: 'Calculation failed. Please try again.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const { openDialog, handleClose, handleCalculate } =
    useMaterialGroupedSelectionPopup({
      keycloak,
      plantId: PLANT_ID,
      onCalculate: executeCalculate,
    })

  const permissions = {
    showAction: false,
    addButton: false,
    deleteButton: false,
    editButton: false,
    saveBtn: false,
    allAction: true,
    downloadExcelBtnFromUI: true,
    showCalculate: true,
    showReleaseBtn: true,
    isReleaseDisabled: isReleaseDisabled,
    ExcelName: EXCEL_EXPORT_TITLE,
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Overall AOP Consumption (Norm/Quantity)',
    showDropdown: false,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        handleCalculate={handleCalculate}
        handleRelease={handleRelease}
        isReleaseDisabled={isReleaseDisabled}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        customHeight={70}
        groupBy={['normParameterTypeDisplayName']}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />

      {ReleaseDialogComponent}
      <MaterialGroupedSelectionDialog
        open={openDialog}
        onClose={handleClose}
        onSaveSuccess={executeCalculate}
      />
    </Box>
  )
}

export default OverallAopConsumption
