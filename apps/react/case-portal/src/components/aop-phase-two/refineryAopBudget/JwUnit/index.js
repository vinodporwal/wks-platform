import React, { useState, useEffect, useCallback } from 'react'
import { Box } from '@mui/material'
import { useSelector, useDispatch } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { setIsReleased } from 'store/reducers/dataGridStore'
import { getRoleName } from 'services/role-service'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { generateHeaderNames } from '../../common/utilities/generateHeaders'
import { customValueFormatterPhaseTwo } from '../../common/ValueFormatterPhaseTwo'
import { generateExcelNameWithoutExt } from '../../common/utilities/excelNameUtil'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import ReleaseDialog from '../../common/components/ReleaseDialog'
import { JwUnitApiService } from 'components/aop-phase-two/services/crude/jwUnitApiService'
import ReleaseAPIService from 'components/aop-phase-two/services/common/releaseAPIService'

const MONTH_FIELDS = [
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

const JwUnitScreen = () => {
  const keycloak = useSession()
  const dispatch = useDispatch()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, year, oldYear, isReleased } = dataGridStore

  const PLANT_ID = plantObject?.id || plantObject?.value || plantObject?.plantId
  const SITE_ID = siteObject?.id || siteObject?.value || plantObject?.siteId || plantObject?.siteFKId
  const AOP_YEAR = year?.selectedYear || year

  const IS_OLD_YEAR = oldYear?.oldYear || oldYear?.isOldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // Release state
  const [openReleaseDialogBox, setOpenReleaseDialogBox] = useState(false)
  const [isReleaseDisabled, setIsReleaseDisabled] = useState(true)

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  const valueFormat = customValueFormatterPhaseTwo(5)
  const headerMap = generateHeaderNames(AOP_YEAR)

  const columns = [
    {
      field: 'productName',
      title: 'Particulars',
      widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
    },
    {
      field: 'normParameterTypeDisplayName',
      title: 'Type',
      widthT: 250,
      minWidth: 200,
      type: 'text',
      editable: false,
      locked: true,
      hidden: true,
    },
    {
      field: 'uom',
      title: 'UOM',
      widthT: 120,
      minWidth: 100,
      type: 'text',
      editable: false,
    },
    {
      field: 'april',
      title: headerMap[4],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'may',
      title: headerMap[5],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'june',
      title: headerMap[6],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'july',
      title: headerMap[7],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'aug',
      title: headerMap[8],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'sep',
      title: headerMap[9],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'oct',
      title: headerMap[10],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'nov',
      title: headerMap[11],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'dec',
      title: headerMap[12],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'jan',
      title: headerMap[1],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'feb',
      title: headerMap[2],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'march',
      title: headerMap[3],
      widthT: 120,
      minWidth: 120,
      type: 'number1',
      editable: !READ_ONLY,
      format: valueFormat,
    },
    {
      field: 'remarks',
      title: 'Remarks',
      widthT: 200,
      minWidth: 150,
      type: 'text',
      editable: !READ_ONLY,
    },
  ]

  const getIsReleasedStatus = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const response = await ReleaseAPIService.getReleaseAOPStatus(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      const hasReleaseRecord = Array.isArray(response?.data)
        ? response.data.length > 0
        : response?.data && typeof response.data === 'object' && Object.keys(response.data).length > 0

      const isReleasedVal =
        hasReleaseRecord || response?.data?.isReleased === 1 || response?.isReleased === 1 ? 1 : 0

      if (isReleasedVal === 1) {
        setIsReleaseDisabled(true)
        dispatch(setIsReleased({ isReleased: 1 }))
      } else {
        setIsReleaseDisabled(false)
        dispatch(setIsReleased({ isReleased: 0 }))
      }
    } catch (error) {
      console.error('Error fetching release status:', error)
      setIsReleaseDisabled(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, dispatch])

  const fetchData = useCallback(async () => {
    if (!SITE_ID || !AOP_YEAR) return
    setLoading(true)
    try {
      const response = await JwUnitApiService.getJwUnitData(
        keycloak,
        SITE_ID,
        AOP_YEAR,
      )
      const rawList = Array.isArray(response?.data)
        ? response.data
        : response?.data?.aopConsumptionNormDTOList || response?.result || []

      const formattedData = rawList.map((item) => {
        const uomVal = item.uom || item.UOM || ''
        return {
          ...item,
          id: item.id || item.normParameterFkId,
          normParameterFkId: item.normParameterFkId || item.id,
          uom: uomVal,
          UOM: uomVal,
          isEditable: READ_ONLY ? false : item.isEditable !== undefined ? item.isEditable : true,
        }
      })

      setRows(formattedData)
      await getIsReleasedStatus()
    } catch (error) {
      console.error('Error fetching JW Unit data:', error)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [keycloak, SITE_ID, AOP_YEAR, READ_ONLY, getIsReleasedStatus])

  useEffect(() => {
    if (SITE_ID && AOP_YEAR) {
      fetchData()
    }
  }, [SITE_ID, AOP_YEAR, fetchData])

  const handleCustomItemChange = (e) => {
    if (READ_ONLY) return
    const { dataItem, field, value } = e
    if (!dataItem) return

    const rowId = dataItem.id

    const updatedRow = {
      ...dataItem,
      [field]: value,
      inEdit: true,
    }

    setRows((prevRows) =>
      prevRows.map((r) => (r.id === rowId ? updatedRow : r)),
    )

    setModifiedCells((prev) => ({
      ...prev,
      [rowId]: {
        ...(prev[rowId] || {}),

        ...updatedRow,
      },
    }))
  }

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remarks || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const saveChanges = async () => {
    if (READ_ONLY) return
    setLoading(true)
    try {
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

      const payload = modifiedData.map((row) => ({
        id: row.id,
        normParameterFkId: row.normParameterFkId || row.id,
        siteFkId: SITE_ID,
        aopYear: AOP_YEAR,
        april: Number(row.april || 0),
        may: Number(row.may || 0),
        june: Number(row.june || 0),
        july: Number(row.july || 0),
        aug: Number(row.aug || 0),
        sep: Number(row.sep || 0),
        oct: Number(row.oct || 0),
        nov: Number(row.nov || 0),
        dec: Number(row.dec || 0),
        jan: Number(row.jan || 0),
        feb: Number(row.feb || 0),
        march: Number(row.march || 0),
        remarks: row.remarks || '',
      }))

      await JwUnitApiService.saveJwUnitData(
        keycloak,
        payload,
        AOP_YEAR,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Data saved successfully!',
        severity: 'success',
      })
      setModifiedCells({})
      await fetchData()
    } catch (error) {
      console.error('Error saving JW Unit:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error saving data!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  // Release handlers
  const handleRelease = () => {
    if (READ_ONLY) return
    setOpenReleaseDialogBox(true)
  }

  const closeReleaseDialogBox = () => {
    setOpenReleaseDialogBox(false)
  }

  const submitConfirmation = async () => {
    setOpenReleaseDialogBox(false)
    setLoading(true)
    try {
      await ReleaseAPIService.releaseAOPReport(
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
    showAction: true,
    addButton: false,
    deleteButton: false,
    editButton: true,
    saveBtn: true,
    allAction: true,
    showExport: false,
    downloadExcelBtnFromUI: false,
    showCalculate: false,
    ExcelName: generateExcelNameWithoutExt(dataGridStore, 'Job_Work_Unit'),
    showImport: false,
    showTitleNameBusiness: true,
    showTitle: true,
    titleName: 'Job Work Unit',
    showDropdown: false,
    remarksEditable: !READ_ONLY,
    showReleaseBtn: true,
  }

  return (
    <Box>
      <LoaderBackdrop open={!!loading} />

      <AdvanceKendoTable
        columns={columns}
        rows={rows}
        setRows={setRows}
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        customItemChange={handleCustomItemChange}
        title={permissions.showTitle ? permissions.titleName : ''}
        permissions={permissions}
        saveChanges={saveChanges}
        isReleaseDisabled={isReleaseDisabled || READ_ONLY}
        handleRelease={handleRelease}
        handleRemarkCellClick={handleRemarkCellClick}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        setCurrentRowId={setCurrentRowId}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        groupBy={['normParameterTypeDisplayName']}
        customHeight={70}
        paginationConfig={{
          threshold: 100,
          buttonCount: 5,
          pageSizes: [10, 20, 50, 100],
          defaultPageSize: 100,
        }}
      />

      <ReleaseDialog
        openReleaseDialogBox={openReleaseDialogBox}
        closeReleaseDialogBox={closeReleaseDialogBox}
        submitConfirmation={submitConfirmation}
      />
    </Box>
  )
}

export default JwUnitScreen
