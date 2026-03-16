import Backdrop from '@mui/material/Backdrop'
import CircularProgress from '@mui/material/CircularProgress'
import { Box, Typography } from '@mui/material'
import React, { useEffect, useState, useMemo } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { SpecificConsumptionService } from 'services/SpecificConsumptionService'
import KendoDataTables from './index'
import getEnhancedColDefs from '../data-tables/CommonHeader/Kendo_ProductionAopHeader'
import { generateHeaderNames } from 'components/Utilities/generateHeaders'

const SpecificConsumptionCalculation = () => {
  const [loading, setLoading] = useState(false)
  const [rows1, setRows1] = useState([])
  const [rows2, setRows2] = useState([])
  const keycloak = useSession()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, year, oldYear, } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear

  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const headerMap = useMemo(() => generateHeaderNames(AOP_YEAR), [AOP_YEAR])
  const columns = getEnhancedColDefs({ headerMap, valueFormat: '{0:n2}' })

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
        setRows1(response.data.map((item, index) => ({ ...item, id: index })))
      } else {
        setRows1([])
      }
    } catch (error) {
      console.error('Error fetching Combined MCU:', error)
      setRows1([])
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
        setRows2(response.data.map((item, index) => ({ ...item, id: index })))
      } else {
        setRows2([])
      }
    } catch (error) {
      console.error('Error fetching Combined MCU Details:', error)
      setRows2([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchGrid1Data()
    fetchGrid2Data()
  }, [PLANT_ID, AOP_YEAR])

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
      allAction: false,
      showTitleNameBusiness: false,
      titleName: 'Combined MCU',
    },
    isOldYear,
  )

  const adjustedPermissionsDetails = getAdjustedPermissions(
    {
      showAction: false,
      addButton: false,
      allAction: false,
      showTitleNameBusiness: false,
      titleName: "MCU Details",
    },
    isOldYear,
  )

  return (
    <Box sx={{ p: 2 }}>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={loading}
      >
        <CircularProgress color="inherit" />
      </Backdrop>

      <Box sx={{ mb: 4 }}>
        <KendoDataTables
          rows={rows1}
          columns={columns}
          setRows={setRows1}
          loading={loading}
          gridName="CombinedMCU"
          titleName="Combined MCU"
          fetchData={fetchGrid1Data}
          permissions={adjustedPermissionsCombined}
        />
      </Box>

      <Box sx={{ mb: 4 }}>
        <KendoDataTables
          rows={rows2}
          columns={columns}
          setRows={setRows2}
          loading={loading}
          gridName="CombinedMCUDetails"
          titleName="Combined MCU Details"
          fetchData={fetchGrid2Data}
          permissions={adjustedPermissionsDetails}
        />
      </Box>
    </Box>
  )
}

export default SpecificConsumptionCalculation
