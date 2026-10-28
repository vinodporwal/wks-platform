import React, { useEffect, useState } from 'react'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'
import { useSelector } from 'react-redux'
import getEnhancedProductionColDefs from '../../data-tables/CommonHeader/Kendo_ProductionVolumeHeader'
import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { useDispatch } from 'react-redux'
import { setIsBlocked } from 'store/reducers/dataGridStore'
import KendoDataTables from '../index'
import { validateFields } from 'utils/validationUtils'
import { ProductionVolumeDataApiService } from 'services/production-volume-data-api-service'
import { DataService } from 'services/DataService'
import { getColDefsNonEditable } from '../Utilities-Kendo/productionTargetColDefs'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import { getRoleName } from 'services/role-service'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'

const ProposedOperatingCapacityAvg = ({ permissions }) => {
  const [modifiedCells, setModifiedCells] = useState({})
  const [enableSaveAddBtn, setEnableSaveAddBtn] = useState(false)
  const [editResetKey, setEditResetKey] = useState(0)

  const keycloak = useSession()
  const apiRef = useGridApiRef()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    oldYear,
    yearChanged,
    plantObject,
    siteObject,
    verticalObject,
    year,
  } = dataGridStore

  const IS_OLD_YEAR = oldYear?.oldYear

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const PLANT_ID = plantObject?.id
  const VERTICAL_ID = verticalObject?.id
  const SITE_ID = siteObject?.id
  const AOP_YEAR = year?.selectedYear

  const PLANT_NAME_NO_CASE = plantObject?.name
  const SITE_NAME_NO_CASE = siteObject?.name
  const VERTICAL_NAME_NO_CASE = 'PP'
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}`

  const VERTICAL_NAME = 'pp'
  const SITE_NAME = siteObject?.name?.toLowerCase()

  const headerMap = generateHeaderNames(AOP_YEAR)

  // Dummy data for testing
  const dummyData = [
    {
      id: 0,
      idFromApi: null,
      productName: 'Product A',
      normParametersFKId: 'product_a',
      april: 120.5,
      may: 125.3,
      june: 118.7,
      july: 130.2,
      august: 128.9,
      september: 122.4,
      october: 135.6,
      november: 127.8,
      december: 131.2,
      january: 129.5,
      february: 124.6,
      march: 126.3,
      remarks: 'Sample remark for Product A',
      originalRemark: 'Sample remark for Product A',
      isEditable: false,
    },
    {
      id: 1,
      idFromApi: null,
      productName: 'Product B',
      normParametersFKId: 'product_b',
      april: 95.2,
      may: 98.7,
      june: 92.3,
      july: 101.5,
      august: 99.8,
      september: 96.1,
      october: 103.4,
      november: 97.9,
      december: 100.2,
      january: 98.5,
      february: 94.8,
      march: 96.7,
      remarks: 'Sample remark for Product B',
      originalRemark: 'Sample remark for Product B',
      isEditable: false,
    },
    {
      id: 2,
      idFromApi: null,
      productName: 'Product C',
      normParametersFKId: 'product_c',
      april: 150.8,
      may: 155.2,
      june: 148.6,
      july: 160.3,
      august: 157.9,
      september: 152.7,
      october: 165.1,
      november: 158.4,
      december: 161.8,
      january: 159.2,
      february: 154.3,
      march: 156.9,
      remarks: 'Sample remark for Product C',
      originalRemark: 'Sample remark for Product C',
      isEditable: false,
    },
    {
      id: 3,
      idFromApi: null,
      productName: 'Product D',
      normParametersFKId: 'product_d',
      april: 85.4,
      may: 88.9,
      june: 82.1,
      july: 91.7,
      august: 89.3,
      september: 86.5,
      october: 93.2,
      november: 87.8,
      december: 90.4,
      january: 88.6,
      february: 84.9,
      march: 86.2,
      remarks: 'Sample remark for Product D',
      originalRemark: 'Sample remark for Product D',
      isEditable: false,
    },
    {
      id: 4,
      idFromApi: null,
      productName: 'Product E',
      normParametersFKId: 'product_e',
      april: 110.3,
      may: 114.7,
      june: 107.9,
      july: 118.5,
      august: 116.2,
      september: 112.8,
      october: 121.4,
      november: 115.6,
      december: 119.1,
      january: 117.3,
      february: 113.5,
      march: 115.8,
      remarks: 'Sample remark for Product E',
      originalRemark: 'Sample remark for Product E',
      isEditable: false,
    },
  ]

  const [rows, setRows] = useState([])

  const valueFormat_ = ValueFormatterProduction()

  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [selectedUnit, setSelectedUnit] = useState('TPH')
  const [loading, setLoading] = useState(false)

  const fetchData = async (unit = selectedUnit) => {
    if (!PLANT_ID || !SITE_ID || !VERTICAL_ID || !AOP_YEAR) return

    setModifiedCells({})
    try {
      setLoading(true)
      const response =
        await ProductionVolumeDataApiService.getProposedOperatingCapacityAvg(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
        )
      if (response?.code != 200) {
        setRows([])
        setLoading(false)
        return
      }
      var formattedData = response?.data?.aopMCCalculatedDataDTOList.map(
        (item, index) => {
          const isTPD = unit == 'TPD'
          return {
            ...item,
            idFromApi: item?.id || null,
            normParametersFKId: item?.materialFKId?.toLowerCase(),
            productName: item?.materialDisplayName,
            remarks: item?.remarks?.trim() || null,
            originalRemark: item?.remarks?.trim() || null,

            id: index,

            ...(isTPD && {
              april: item.april ? item.april * 24 : item.april || 0,
              may: item.may ? item.may * 24 : item.may || 0,
              june: item.june ? item.june * 24 : item.june || 0,
              july: item.july ? item.july * 24 : item.july || 0,
              august: item.august ? item.august * 24 : item.august || 0,
              september: item.september
                ? item.september * 24
                : item.september || 0,
              october: item.october ? item.october * 24 : item.october || 0,
              november: item.november ? item.november * 24 : item.november || 0,
              december: item.december ? item.december * 24 : item.december || 0,
              january: item.january ? item.january * 24 : item.january || 0,
              february: item.february ? item.february * 24 : item.february || 0,
              march: item.march ? item.march * 24 : item.march || 0,
            }),
          }
        },
      )

      formattedData = formattedData.map((item) => ({
        ...item,
        remarks: item.remarks ? item.remarks.trim() : '',
        isEditable: false,
      }))
      setRows(formattedData)
      setLoading(false)
    } catch (error) {
      console.error('Error fetching data:', error)
      setLoading(false)
    }
  }

  const valueFormat = valueFormat_

  const colDefs_non_editable = getColDefsNonEditable(headerMap, valueFormat)

  useEffect(() => {
    setModifiedCells({})
    fetchData()
  }, [oldYear, yearChanged, keycloak, selectedUnit, PLANT_ID])

  const colDefs_editable = getEnhancedProductionColDefs({
    headerMap,
    valueFormat,
  })

  const handleUnitChangeMaxCapacity = (unit) => {
    setSelectedUnit(unit)
  }

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      allAction: true,
      showAction: false,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      showCalculate: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: false,
      allAction: true,
      addButton: false,
      deleteButton: false,
      editButton: false,
      showUnit: true,
      saveWithRemark: false,
      showRefreshBtn: false,
      saveBtn: false,
      units: ['TPH', 'TPD'],
      showCalculate: false,
      showCalculateVisibility: false,
      downloadExcelBtnFromUI: true,
      ExcelName: `${EXCEL_EXPORT_TITLE}_Production_Target_${AOP_YEAR}`,
      showTitleAndInformation: false,
      showTitleNameBusiness: true,
      titleName: 'Production Target',
    },
    IS_OLD_YEAR,
  )

  const colDefs_current_operating_capacity = colDefs_non_editable

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        enableSaveAddBtn={enableSaveAddBtn}
        setRows={setRows}
        columns={colDefs_current_operating_capacity}
        rows={rows}
        paginationOptions={[100, 200, 300]}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        apiRef={apiRef}
        fetchData={fetchData}
        handleUnitChange={handleUnitChangeMaxCapacity}
        experimentalFeatures={{ newEditingApi: true }}
        permissions={adjustedPermissions}
        selectedUnit={selectedUnit}
        setSelectedUnit={setSelectedUnit}
        supressGridHeight={true}
        resetEditSignal={editResetKey}
        setEditResetKey={setEditResetKey}
      />
    </div>
  )
}

export default ProposedOperatingCapacityAvg
