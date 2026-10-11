import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Box, Button } from '@mui/material'
import { FileExportIcon } from 'assets/images/icons'
import { ExcelExport } from '@progress/kendo-react-excel-export'
import { useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import { useSession } from 'SessionStoreContext'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import SingleReportGrid from './SingleReportGrid'
import { inferColumnsFromRows } from './reportGridHelpers'

const REPORT_TYPE_FOR_ALL = 'ProductionTarget'

const ProductionVolumeDataBasisPeNew = () => {
  const keycloak = useSession()
  const [dataMap, setDataMap] = useState({})
  const [gridNames, setGridNames] = useState([])
  const [loading, setLoading] = useState(false)
  const [isExporting, setIsExporting] = useState(false)

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    yearChanged,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    year,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const AOP_YEAR = year?.selectedYear

  const workbookRef = useRef(null)
  const excelExportRef = useRef(null)

  const fileName = `${verticalObject?.name?.toUpperCase() || ''}_${siteObject?.name?.toUpperCase() || ''}_${plantObject?.name?.toUpperCase() || ''}-Production_Target_${AOP_YEAR}.xlsx`

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

      const apiResponse = await DataService.getProductionVolDataBasisPe(
        keycloak,
        REPORT_TYPE_FOR_ALL,
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

      const names = gridsArray.map((g) => g.gridName)

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
  }, [keycloak, PLANT_ID, AOP_YEAR])

  useEffect(() => {
    fetchAllGrids()
  }, [fetchAllGrids, PLANT_ID, AOP_YEAR, oldYear, yearChanged])

  // ===================== EXCEL EXPORT =====================
  const exportAllGrids = useCallback(() => {
    if (!gridNames.length || isExporting) return
    setIsExporting(true)

    setTimeout(async () => {
      try {
        const cellBorder = { size: 1, color: '#000000' }

        const headerStyle = {
          background: '#D9D9D9',
          bold: true,
          color: '#000000',
          borderTop: cellBorder,
          borderBottom: cellBorder,
          borderLeft: cellBorder,
          borderRight: cellBorder,
        }

        const dataCellStyle = {
          borderTop: cellBorder,
          borderBottom: cellBorder,
          borderLeft: cellBorder,
          borderRight: cellBorder,
        }

        const sheets = gridNames
          .map((gridName) => {
            const d = dataMap[gridName] || { rows: [], columns: [] }
            if (!d.rows.length) return null

            const cols = (d.columns || []).filter((c) => c.field !== 'GRID_TYPE')
            return {
              title: gridName.slice(0, 31).replace(/[/\\?*:[\]]/g, ' '),
              columns: cols.map((c) => ({
                title: c.title || c.field,
                width: 140,
              })),
              rows: [
                {
                  cells: cols.map((c) => ({
                    value: c.title || c.field,
                    ...headerStyle,
                  })),
                },
                ...d.rows.map((r) => ({
                  cells: cols.map((c) => ({
                    value: r[c.field] ?? '',
                    ...dataCellStyle,
                  })),
                })),
              ],
            }
          })
          .filter(Boolean)

        if (!sheets.length) {
          setIsExporting(false)
          return
        }

        const workbook = { sheets }

        if (excelExportRef.current) {
          if (typeof excelExportRef.current.toDataURL === 'function') {
            const dataUrl = await excelExportRef.current.toDataURL(workbook)
            const base64 = dataUrl.split(',')[1]
            const byteString = atob(base64)
            const mimeString = dataUrl.split(',')[0].split(':')[1].split(';')[0]
            const ab = new ArrayBuffer(byteString.length)
            const ia = new Uint8Array(ab)
            for (let i = 0; i < byteString.length; i++) {
              ia[i] = byteString.charCodeAt(i)
            }
            const blob = new Blob([ab], { type: mimeString })
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = fileName
            document.body.appendChild(a)
            a.click()
            a.remove()
            URL.revokeObjectURL(url)
          } else if (typeof excelExportRef.current.save === 'function') {
            excelExportRef.current.save(workbook)
          }
        }
      } catch (err) {
        console.error('Export failed:', err)
      } finally {
        setIsExporting(false)
      }
    }, 150)
  }, [gridNames, dataMap, fileName, isExporting])

  return (
    <div>
      <LoaderBackdrop open={loading || isExporting} />

      <div style={{ display: 'none' }}>
        <ExcelExport
          data={[]}
          ref={(r) => (excelExportRef.current = r)}
          fileName={fileName}
        />
      </div>

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
        {gridNames.map((name) => {
          const gridData = dataMap[name]
          if (!gridData) return null

          return (
            <SingleReportGrid
              key={name}
              name={name}
              rawData={gridData.rows}
              rawColumns={gridData.columns}
              aopYear={AOP_YEAR}
              defaultExpanded={true}
            />
          )
        })}
      </Box>
    </div>
  )
}

export default ProductionVolumeDataBasisPeNew
