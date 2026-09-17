import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Box, Button } from '@mui/material'
import { FileExportIcon } from 'assets/images/icons'
import { ExcelExport } from '@progress/kendo-react-excel-export'
import { useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import { useSession } from 'SessionStoreContext'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import {
  SingleReportGrid,
  inferColumnsFromRows,
} from 'components/Utilities/DynamicReportGrid'

const REPORT_TYPE_FOR_ALL = 'NormsHistorian'

const NormsHistorianBasisPe = () => {
  const keycloak = useSession()
  const [dataMap, setDataMap] = useState({})
  const [gridNames, setGridNames] = useState([])
  const [loading, setLoading] = useState(false)
  const [isExporting, setIsExporting] = useState(false)

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
  const AOP_YEAR = year?.selectedYear
  const lowerVertName = verticalChange?.selectedVertical?.toLowerCase()
  const lowerSiteName = siteObject?.name?.toLowerCase()

  const workbookRef = useRef(null)
  const excelExportRef = useRef(null)

  const IS_AROMATICS_HMD =
    lowerVertName === 'aromatics' && lowerSiteName === 'hmd'

  const fileName = `${verticalObject?.name?.toUpperCase()}_${siteObject?.name?.toUpperCase()}_${plantObject?.name?.toUpperCase()}-Norms_Historian_${AOP_YEAR}.xlsx`

  const fetchAllGrids = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    setGridNames([])
    setDataMap({})
    setLoading(true)

    try {
      const configData = await DataService.getConfigurationExecutionDetails(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
      )
      if (configData?.code !== 200) {
        setLoading(false)
        return
      }

      const StartDate = configData.data.find(
        (d) => d.Name === 'StartDate',
      )?.AttributeValue
      const EndDate = configData.data.find(
        (d) => d.Name === 'EndDate',
      )?.AttributeValue

      if (!StartDate || !EndDate) {
        setLoading(false)
        return
      }

      const phaseTwoVertical = ['crude', 'vgoht', 'hydrotreater', 'alkylation']
      const apiResponse = await DataService.getProductionVolDataBasisPe(
        keycloak,
        phaseTwoVertical.includes(lowerVertName)
          ? 'PlantWiseNormsHistorian'
          : REPORT_TYPE_FOR_ALL,
        StartDate,
        EndDate,
        null,
        PLANT_ID,
        AOP_YEAR,
      )

      if (apiResponse?.code !== 200) {
        setLoading(false)
        return
      }

      const gridsArray = Array.isArray(apiResponse.data)
        ? apiResponse.data
        : Array.isArray(apiResponse.data?.data)
          ? apiResponse.data.data
          : []

      const names = gridsArray
        .filter((g) => g.data?.[0]?.GRID_TYPE !== 'GENERAL_NOTES')
        .map((g) => g.gridName)

      setGridNames(names)

      const map = {}
      gridsArray.forEach((g) => {
        const rawRows = Array.isArray(g.data) ? g.data : []
        const rawCols =
          Array.isArray(g.columns) && g.columns.length
            ? g.columns
            : inferColumnsFromRows(rawRows)

        const rowsWithId = rawRows.map((r, i) => ({
          ...r,
          id: i + 1,
        }))

        map[g.gridName] = {
          rows: rowsWithId,
          columns: rawCols,
        }
      })

      setDataMap(map)
    } catch (err) {
      console.error('Error fetching report grids:', err)
    } finally {
      setLoading(false)
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, lowerVertName])

  useEffect(() => {
    fetchAllGrids()
  }, [fetchAllGrids, PLANT_ID, AOP_YEAR, oldYear, yearChanged])

  // ===================== EXCEL EXPORT =====================
  const exportAllGrids = useCallback(() => {
    const sheets = gridNames
      .map((gridName) => {
        const d = dataMap[gridName] || { rows: [], columns: [] }
        if (!d.rows.length) return null

        const cols = (d.columns || []).filter((c) => c.field !== 'GRID_TYPE')
        return {
          title: gridName.slice(0, 31).replace(/[/\\?*:[\]]/g, ' '),
          columns: cols.map((c) => ({
            title: c.title || c.field,
            width: 120,
          })),
          rows: [
            {
              cells: cols.map((c) => ({
                value: c.title || c.field,
                background: '#D9D9D9',
                bold: true,
              })),
            },
            ...d.rows.map((r) => ({
              cells: cols.map((c) => ({
                value: r[c.field] ?? '',
              })),
            })),
          ],
        }
      })
      .filter(Boolean)

    if (!sheets.length) return
    workbookRef.current = { sheets }
    setIsExporting(true)
  }, [gridNames, dataMap])

  useEffect(() => {
    if (!isExporting) return
    if (excelExportRef.current && workbookRef.current) {
      excelExportRef.current.save(workbookRef.current)
      setIsExporting(false)
    }
  }, [isExporting])

  return (
    <div>
      <LoaderBackdrop open={loading || isExporting} />

      {isExporting && (
        <div style={{ display: 'none' }}>
          <ExcelExport
            data={[]}
            ref={(r) => (excelExportRef.current = r)}
            fileName={fileName}
          />
        </div>
      )}

      {/* Top Action Bar */}
      <Box
        display='flex'
        justifyContent='flex-end'
        sx={{ marginBottom: '8px', mt: '5px' }}
      >
        <Button
          variant='contained'
          className='btn-export'
          startIcon={
            <Box component='img' src={FileExportIcon} className='w16-icon' />
          }
          onClick={exportAllGrids}
          disabled={loading || isExporting || gridNames.length === 0}
        >
          Export
        </Button>
      </Box>

      {/* Renders each grid inside its isolated, high-performance container */}
      <Box display='flex' flexDirection='column'>
        {gridNames.map((name, index) => {
          const gridData = dataMap[name]
          if (!gridData) return null

          return (
            <SingleReportGrid
              key={name}
              name={name}
              rawData={gridData.rows}
              rawColumns={gridData.columns}
              isAromaticsHmd={IS_AROMATICS_HMD}
              aopYear={AOP_YEAR}
              defaultExpanded={true}
            />
          )
        })}
      </Box>
    </div>
  )
}

export default NormsHistorianBasisPe
