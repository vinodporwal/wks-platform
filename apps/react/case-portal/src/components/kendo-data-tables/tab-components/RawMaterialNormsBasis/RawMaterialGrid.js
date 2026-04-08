import React, { useState, useEffect, useCallback } from 'react'
import { Box, Typography, Backdrop, CircularProgress } from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { useGridApiRef } from '@mui/x-data-grid'
import KendoDataTables from '../../index'
import getEnhancedAOPColDefs from 'components/data-tables/CommonHeader/kendo_ConfigHeader'
import { DataService } from 'services/DataService'
import { getRoleName } from 'services/role-service'
import { RawMaterialNormsBasisApiService } from 'services/raw-material-norms-basis-api-service'

const RawMaterialGrid = ({
  summary,
  summaryEdited,
  setSummaryEdited,
  triggerGrid3Refresh,
}) => {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modifiedCells, setModifiedCells] = useState({})
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)
  const [open1, setOpen1] = useState(false)
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const apiRef = useGridApiRef()

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantObject,
    year,
    oldYear,
    yearChanged,
    verticalObject,
    siteObject,
  } = dataGridStore
  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear
  const IS_OLD_YEAR = oldYear?.oldYear

  const keycloak = useSession()

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)

  const PLANT_NAME_NO_CASE = plantObject?.name?.toUpperCase()
  const SITE_NAME_NO_CASE = siteObject?.name?.toUpperCase()
  const VERTICAL_NAME_NO_CASE = verticalObject?.name?.toUpperCase()
  const EXCEL_EXPORT_TITLE = `${VERTICAL_NAME_NO_CASE}_${SITE_NAME_NO_CASE}_${PLANT_NAME_NO_CASE}_Raw_Material`

  const FORMATE_VALUE = '{0:0.000}'

  const IS_ELASTOMER_JMD_IIR =
    VERTICAL_NAME_NO_CASE === 'ELASTOMER' &&
    SITE_NAME_NO_CASE === 'JMD' &&
    PLANT_NAME_NO_CASE === 'IIR'

  const handleRemarkCellClick = (row) => {
    if (READ_ONLY) return
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
  }

  const productionColumns = getEnhancedAOPColDefs({
    handleRemarkCellClick,
    configType: 'rawMaterial',
    FORMATE_VALUE,
  })

  // TODO: Replace with actual API call when backend is ready
  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return

    setModifiedCells({})

    try {
      setLoading(true)

      const response = await RawMaterialNormsBasisApiService.getRawMaterialData(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        'Configuration',
      )
      const disabledRows = ['Total Fillers', 'Net Rubber']
      const formattedData = response?.data?.gradeWiseNormConfigurationList?.map(
        (row, index) => ({
          id: row.id || index,
          particulars: row.grade,
          uom: row.uom,
          IIR: row.iirR1675,
          CIIR: row.ciirC1139,
          BIIR: row.biirB2232,
          materialFKId: row.materialFKId,
          r1675Id: row.r1675Id,
          c1139Id: row.c1139Id,
          b2232Id: row.b2232Id,
          name: row.name,
          type: row.type || 'Raw Material',
          isEditable: IS_ELASTOMER_JMD_IIR
            ? !disabledRows.includes(row.grade)
            : true,
        }),
      )

      setRows(formattedData || [])
    } catch (error) {
      console.error('Error fetching Raw Material data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Error fetching data',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setModifiedCells({})
    fetchData()
  }, [oldYear, yearChanged, keycloak, PLANT_ID, AOP_YEAR])

  const saveSummary = async (summary) => {
    try {
      const response = await DataService.saveSummaryAOPConsumptionNorm(
        PLANT_ID,
        AOP_YEAR,
        summary,
        keycloak,
      )

      if (response?.code == 200) {
        setSnackbarData({
          message: 'Saved Successfully!',
          severity: 'success',
        })
        setSnackbarOpen(true)
      } else {
        setSnackbarData({
          message: 'Saved Failed!',
          severity: 'error',
        })
      }
      return response
    } catch (error) {
      console.error('Error saving Summary!', error)
    } finally {
      setLoading(false)
    }
  }

  const handleUpdate = async (updatedRows) => {
    setLoading(true)
    try {
      let payload = updatedRows?.map((row) => {
        return {
          name: row.name,
          grade: row.particulars,
          uom: row.uom,
          iirR1675: row.IIR,
          ciirC1139: row.CIIR,
          biirB2232: row.BIIR,
          materialFKId: row.materialFKId,
          r1675Id: row.r1675Id,
          c1139Id: row.c1139Id,
          b2232Id: row.b2232Id,
        }
      })

      const response =
        await RawMaterialNormsBasisApiService.postRawMaterialData(
          keycloak,
          payload,
          PLANT_ID,
          AOP_YEAR,
          'Configuration',
        )

      console.log('payload', payload)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Saved Successfully!',
        severity: 'success',
      })

      await fetchData()
      triggerGrid3Refresh()
      return { code: 200 }
    } catch (error) {
      console.error('Error updating data:', error)
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Data Save failed!',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const downloadExcelForConfiguration = async () => {
    try {
      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Excel download started!',
        severity: 'success',
      })

      const response =
        await RawMaterialNormsBasisApiService.exportRawMaterialExcel(
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          EXCEL_EXPORT_TITLE,
          'rawmaterial',
        )
      return { code: 200 }
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

  const handleExcelUpload = async (rawFile) => {
    setLoading(true)

    try {
      const response =
        await RawMaterialNormsBasisApiService.importRawMaterialExcel(
          rawFile,
          keycloak,
          PLANT_ID,
          AOP_YEAR,
          'rawmaterial',
        )

      if (response?.code === 200) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: response?.message || 'Uploaded Successfully!',
          severity: 'success',
        })
        setModifiedCells({})
        await fetchData()
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
        link.setAttribute('download', 'Error File - Raw Material.xlsx')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Partial data saved. Error file downloaded.',
          severity: 'warning',
        })
        await fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({ message: 'Upload Failed!', severity: 'error' })
      }

      return response
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

  const saveChanges = useCallback(async () => {
    setLoading(true)

    try {
      if (Object.keys(modifiedCells).length === 0) {
        if (summaryEdited) {
          await saveSummary(summary)
          setModifiedCells({})
          setSummaryEdited(false)
        }
        return
      }

      const rawData = Object.values(modifiedCells)
      const data = rawData.filter((row) => row.inEdit)

      if (data.length === 0) {
        setLoading(false)
        return
      }

      await handleUpdate(data)
    } catch (error) {
      console.log('Error saving changes:', error)
    } finally {
      setLoading(false)
    }
  }, [modifiedCells, summaryEdited, summary])

  const getAdjustedPermissions = (permissions, isOldYear) => {
    if (isOldYear != 1) return permissions
    return {
      ...permissions,
      showAction: false,
      addButton: false,
      deleteButton: false,
      downloadExcelBtn: false,
      uploadExcelBtn: false,
      editButton: false,
      showUnit: false,
      saveWithRemark: false,
      saveBtn: false,
      isOldYear: isOldYear,
      allAction: false,
    }
  }

  const adjustedPermissions = getAdjustedPermissions(
    {
      showAction: true,
      saveWithRemark: true,
      saveBtn: true,
      allAction: true,
      downloadExcelBtn: true,
      uploadExcelBtn: true,
      showTitleNameBusiness: true,
      titleName: 'Raw material : Isobutylene & Isoprene norm basis',
    },
    IS_OLD_YEAR,
  )

  return (
    <Box>
      <Backdrop
        sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
        open={!!loading}
      >
        <CircularProgress color='inherit' />
      </Backdrop>

      <KendoDataTables
        modifiedCells={modifiedCells}
        setModifiedCells={setModifiedCells}
        setRows={setRows}
        columns={productionColumns}
        rows={rows}
        paginationOptions={[100, 200, 300]}
        saveChanges={saveChanges}
        snackbarData={snackbarData}
        snackbarOpen={snackbarOpen}
        apiRef={apiRef}
        open1={open1}
        setOpen1={setOpen1}
        setSnackbarOpen={setSnackbarOpen}
        setSnackbarData={setSnackbarData}
        handleRemarkCellClick={handleRemarkCellClick}
        fetchData={fetchData}
        remarkDialogOpen={remarkDialogOpen}
        setRemarkDialogOpen={setRemarkDialogOpen}
        currentRemark={currentRemark}
        setCurrentRemark={setCurrentRemark}
        currentRowId={currentRowId}
        permissions={adjustedPermissions}
        summaryEdited={summaryEdited}
        downloadExcelForConfiguration={downloadExcelForConfiguration}
        handleExcelUpload={handleExcelUpload}
        groupBy={'type'}
        supressGridHeight={true}
      />
    </Box>
  )
}

export default RawMaterialGrid
