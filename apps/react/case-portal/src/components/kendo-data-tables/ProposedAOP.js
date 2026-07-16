import React, { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { setIsBlocked } from 'store/reducers/dataGridStore'
import { Box } from '@mui/material'
import getEnhancedColDefsProposedAOP from 'components/data-tables/CommonHeader/Kendo_Proposed_AOP_Header'
import { ProposedAopApiService } from 'services/proposed-aop-api-service'
import { ConsumptionNormsApiService } from 'services/consumption-norms-api-service'
import { getRoleName } from 'services/role-service'
import ValueFormatterConsumption from 'utils/ValueFormatterConsumption'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { validateFields } from 'utils/validationUtils'

const ProposedAOP = () => {
  const [modifiedCells, setModifiedCells] = React.useState({})
  const [calculationObject, setCalculationObject] = useState([])
  const keycloak = useSession()

  const [open1, setOpen1] = useState(false)
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
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const SCREEN_NAME = screenTitle?.title || 'Proposed AOP'

  const isPEPP =
    verticalObject?.name?.toLowerCase() === 'pe' ||
    verticalObject?.name?.toLowerCase() === 'pp'
  const isPET = verticalObject?.name?.toLowerCase() === 'pet'

  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [gradeId, setGradeId] = useState(null)
  const [grades, setGrades] = useState([])
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveEditedData = async (newRows) => {
    setLoading(true)
    try {
      const payload = newRows.map((row) => ({
        id: row?.idFromApi ?? null,
        normParameterId: row?.normParameterId ?? null,
        normParameterTypeId: row?.normParameterTypeId ?? null,
        normParameterTypeDisplayName: row?.normParameterTypeDisplayName ?? null,
        productName: row?.productName ?? null,
        uom: row?.uom ?? null,
        lastFY: row?.lastFY ?? null,
        sysGrn: row?.sysGrn ?? null,
        proposed: row?.proposed === '' ? null : (row?.proposed ?? null),
        remarks: row?.remarks ?? null,
        plantId: row?.plantId ?? null,
        aopYear: row?.aopYear ?? null,
        gradeId: row?.gradeId ?? null,
      }))

      const response = await ProposedAopApiService.saveProposedAOP(
        keycloak,
        payload,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData(gradeId)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Save Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      console.error('Error saving data!', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error saving data!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const saveChanges = React.useCallback(async () => {
    const editedData = Object.values(modifiedCells)
    if (editedData.length === 0) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'No changes to save!',
        severity: 'info',
      })
      return
    }

    const requiredFields = ['remarks']
    const validationMessage = validateFields(editedData, requiredFields)
    if (validationMessage) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: validationMessage,
        severity: 'error',
      })
      return
    }

    saveEditedData(editedData)
  }, [modifiedCells, gradeId])

  const fetchGradeDropdowns = async () => {
    try {
      const response =
        await ConsumptionNormsApiService.getConsumptionAOPNormsGrades(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )

      if (response?.code === 200) {
        setGrades(response?.data || [])
        if (response?.data && response?.data.length > 0) {
          const firstGrade = response.data[0]
          const firstId = firstGrade?.gradeId ?? firstGrade?.id ?? null
          setGradeId(firstId)
          fetchData(firstId)
        } else {
          fetchData(null)
        }
      } else {
        setGrades([])
        fetchData(null)
      }
    } catch (error) {
      setGrades([])
      console.error('Error fetching grades:', error)
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

      if (response?.code === 200) {
        setGrades(response?.data || [])
        if (response?.data && response?.data.length > 0) {
          const firstGrade = response.data[0]
          const firstId = firstGrade?.gradeId ?? firstGrade?.id ?? null
          setGradeId(firstId)
          fetchData(firstId)
        } else {
          setGradeId(null)
          fetchData(null)
        }
      } else {
        setGrades([])
        setGradeId(null)
        fetchData(null)
      }
    } catch (error) {
      setGrades([])
      console.error('Error fetching grades after calculation:', error)
    }
  }

  const fetchData = async (currentGradeId) => {
    if (!PLANT_ID || !AOP_YEAR) return
    if ((isPEPP || isPET) && !currentGradeId) return
    setLoading(true)
    try {
      setRows([])
      const response = await ProposedAopApiService.getProposedAOP(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        currentGradeId || null,
      )

      if (response?.code !== 200) {
        setRows([])
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Error fetching data.',
          severity: 'error',
        })
        return
      }

      setCalculationObject(response?.data?.aopCalculation || [])

      const formattedData = (response?.data?.proposedAOP || []).map(
        (item, index) => {
          return {
            ...item,
            idFromApi: item.id,
            originalRemark: item.remarks?.trim() || null,
            id: index,
            Particulars: item.normParameterTypeDisplayName || 'Type',
            UOM: item.uom,
            isEditable: true,
          }
        },
      )

      setRows(formattedData)
    } catch (error) {
      console.error('Error fetching data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching data.',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchGradeDropdowns()
  }, [PLANT_ID, AOP_YEAR, oldYear, yearChanged, keycloak])

  const productionColumns = getEnhancedColDefsProposedAOP({
    valueFormat,
  })

  const handleCalculate = async () => {
    setLoading(true)
    try {
      const response = await ProposedAopApiService.calculateProposedAOP(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })

        if (isPEPP || isPET) {
          fetchGradeDropdownsAfterCalc()
        } else {
          fetchData(null)
        }
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Data Refresh Failed!',
          severity: 'error',
        })
      }
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred during calculation',
        severity: 'error',
      })
      console.error('Calculation Error!', error)
    } finally {
      setLoading(false)
    }
  }

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

  const handleExport = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setSnackbarOpen(true)
    setSnackbarData({
      message: 'Excel download started!',
      severity: 'success',
    })

    try {
      await ProposedAopApiService.exportProposedAOP(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_EXPORT_TITLE,
        SCREEN_NAME,
      )
    } catch (error) {
      console.error('Error downloading Excel:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Failed to download Excel.',
        severity: 'error',
      })
    } finally {
      setSnackbarOpen(true)
    }
  }

  const handleExcelUpload = async (rawFile) => {
    if (!rawFile) return
    setLoading(true)
    try {
      const response = await ProposedAopApiService.importProposedAOP(
        rawFile,
        keycloak,
      )
      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Upload Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        fetchData(gradeId)
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
        link.setAttribute('download', 'Error File - Proposed AOP.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        fetchData(gradeId)
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Upload Failed!',
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

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: true,
      saveBtn: false,
      isOldYear: isOldYear,
      showCalculate: false,
      uploadExcelBtn: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: true,
      saveBtn: true,
      showCalculate: true,
      allAction: true,
      showCalculateVisibility:
        Object.keys(calculationObject || {}).length > 0 ? true : false,
      showRefresh: false,
      noColor: false,
      customHeight: defaultCustomHeight,
      showG: true,
      marginBottom: true,
      dropdownLabel: 'Grade',
      uploadExcelBtn: true,
      showImport: false,
      showExport: true,
      isHeight: rows?.length > 10,
      showTitleNameBusiness: true,
      showTitle: true,
      title: `${SCREEN_NAME}`,
      titleName: `${SCREEN_NAME}`,
    },
    isOldYear,
  )

  const handleGradeChange = (selectedGradeId) => {
    setGradeId(selectedGradeId)
    fetchData(selectedGradeId)
  }

  return (
    <div>
      <LoaderBackdrop open={!!loading} />
      <Box>
        <KendoDataTablesReports
          modifiedCells={modifiedCells}
          setModifiedCells={setModifiedCells}
          columns={productionColumns}
          rows={rows}
          setRows={setRows}
          fetchData={fetchData}
          getRowId={(row) => row.id}
          paginationOptions={[100, 200, 300]}
          saveChanges={saveChanges}
          snackbarData={snackbarData}
          snackbarOpen={snackbarOpen}
          open1={open1}
          setOpen1={setOpen1}
          setSnackbarOpen={setSnackbarOpen}
          setSnackbarData={setSnackbarData}
          handleCalculate={handleCalculate}
          handleRemarkCellClick={handleRemarkCellClick}
          remarkDialogOpen={remarkDialogOpen}
          setRemarkDialogOpen={setRemarkDialogOpen}
          currentRemark={currentRemark}
          setCurrentRemark={setCurrentRemark}
          currentRowId={currentRowId}
          permissions={adjustedPermissions}
          groupBy='Particulars'
          grades={grades}
          handleGradeChange={handleGradeChange}
          plantID={PLANT_ID}
          title={SCREEN_NAME}
          handleExport={handleExport}
          handleExcelUpload={handleExcelUpload}
          isProposedAOP={true}
        />
      </Box>
    </div>
  )
}

export default ProposedAOP
