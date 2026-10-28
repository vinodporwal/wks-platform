import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Box, Button, Typography } from '@mui/material'
import DownloadIcon from '@mui/icons-material/Download'
import {
  ExcelExport,
  ExcelExportColumn,
} from '@progress/kendo-react-excel-export'
import KendoDataGrid from 'components/Kendo-Report-DataGrid/index'
import LoaderBackdrop from 'components/Utilities/LoaderBackdrop'
import { useSelector } from 'react-redux'
import { ProductionNormsApiService } from 'services/production-norms-api-service'
import { useSession } from 'SessionStoreContext'
import {
  CustomAccordion,
  CustomAccordionDetails,
  CustomAccordionSummary,
} from 'utils/CustomAccrodian'

const CALL_DELAY_MS = 200

const NaphthaSummary = () => {
  const keycloak = useSession()
  const [dataMap, setDataMap] = useState({})
  const [gridNames, setGridNames] = useState([])
  const [loading, setLoading] = useState(false)
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    plantID,
    yearChanged,
    oldYear,
    plantObject,
    siteObject,
    verticalObject,
    verticalChange,
    year,
  } = dataGridStore

  const timeoutIdsRef = useRef([])
  const activeRequestsRef = useRef(0)
  const isMountedRef = useRef(true)
  const exportRefs = useRef({})

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const vertName = verticalChange?.selectedVertical || verticalObject?.name
  const lowerVertName = vertName?.toLowerCase()
  const lowerSiteName = siteObject?.name?.toLowerCase()
  const lowerPlantName = plantObject?.name?.toLowerCase()

  useEffect(() => {
    return () => {
      isMountedRef.current = false
      timeoutIdsRef.current.forEach((t) => clearTimeout(t))
      timeoutIdsRef.current = []
    }
  }, [])

  function parseDDMMYYYY(dateStr) {
    if (!dateStr) return null
    const [day, month, yr] = dateStr.split('-')
    return new Date(`${yr}-${month}-${day}`)
  }

  const enrichColumns = useCallback(
    (backendCols = []) => {
      const DEFAULT_MIN_WIDTH = 160

      const cols = backendCols.map((col) => {
        const isTextCol = col.type === 'string'
        const isNumberCol = col.type === 'number'
        return {
          ...col,
          title: col.title || col.field,
          filterable: true,
          filter: isTextCol ? 'text' : isNumberCol ? 'numeric' : undefined,
          align: isTextCol ? 'left' : isNumberCol ? 'right' : undefined,
          ...(isNumberCol ? { format: '{0:0.000}' } : {}),
          editable: false,
          isRightAlligned: isNumberCol ? 'numeric' : undefined,
        }
      })

      if (cols.length > 17) {
        return cols.map((c) => ({
          widthT: c.minWidth ?? DEFAULT_MIN_WIDTH,
          ...c,
        }))
      }

      return cols
    },
    [AOP_YEAR, PLANT_ID, keycloak],
  )

  const fetchDataForGrid = useCallback(
    async (reportType) => {
      try {
        const apiResponse = await ProductionNormsApiService.NaphthaSummaryData(
          keycloak,
          reportType,
          PLANT_ID,
          AOP_YEAR,
        )

        if (apiResponse?.code !== 200) {
          return { rows: [], columns: [] }
        }

        const backendCols = apiResponse.data?.columns || []
        const rawData =
          apiResponse.data?.data ||
          (Array.isArray(apiResponse.data) ? apiResponse.data : [])
        const enrichedCols = enrichColumns(backendCols)

        const dateFields = enrichedCols
          .filter((c) => c.type === 'date')
          .map((c) => c.field)
        const numberFields = enrichedCols
          .filter((c) => c.type === 'number')
          .map((c) => c.field)

        const rowsWithId = (rawData || []).map((item, index) => {
          const parsedItem = { ...item }
          dateFields.forEach((f) => {
            parsedItem[f] = item?.[f] ? parseDDMMYYYY(item[f]) : null
          })
          numberFields.forEach((f) => {
            parsedItem[f] =
              item?.[f] !== undefined && item?.[f] !== null
                ? Number(item[f])
                : null
          })
          return { ...parsedItem, id: index, isEditable: false }
        })

        return { rows: rowsWithId, columns: enrichedCols }
      } catch (err) {
        console.error(`Error fetching ${reportType}:`, err)
        return { rows: [], columns: [] }
      }
    },
    [AOP_YEAR, PLANT_ID, keycloak, enrichColumns],
  )

  const scheduleAndRunFetch = useCallback(
    (reportType, delayMs) => {
      const id = setTimeout(async () => {
        activeRequestsRef.current += 1
        if (isMountedRef.current) setLoading(true)

        try {
          const { rows, columns } = await fetchDataForGrid(reportType)

          if (!isMountedRef.current) return
          setDataMap((prev) => ({ ...prev, [reportType]: { rows, columns } }))
        } catch (err) {
          console.error(`Scheduled fetch failed for ${reportType}:`, err)
        } finally {
          activeRequestsRef.current -= 1
          if (activeRequestsRef.current <= 0 && isMountedRef.current) {
            activeRequestsRef.current = 0
            setLoading(false)
          }
        }
      }, delayMs)

      timeoutIdsRef.current.push(id)
    },
    [fetchDataForGrid, keycloak],
  )

  const fetchAllGrids = useCallback(async () => {
    timeoutIdsRef.current.forEach((t) => clearTimeout(t))
    timeoutIdsRef.current = []
    setDataMap({})

    if (!PLANT_ID || !AOP_YEAR) return

    try {
      setLoading(true)

      const typeListResult = await ProductionNormsApiService.NaphthaSummaryData(
        keycloak,
        'TYPE LIST',
        PLANT_ID,
        AOP_YEAR,
      )

      let types = []
      if (typeListResult?.code === 200) {
        const rawTypes =
          typeListResult?.data?.data ?? typeListResult?.data ?? []
        types = rawTypes.map((item) => item?.grid || item?.gridName || item)
      } else {
        setLoading(false)
        return
      }

      const normalized = [...new Set(types)].filter(
        (type) => type && typeof type === 'string' && type !== 'TYPE LIST',
      )
      setGridNames(normalized)

      if (normalized.length === 0) {
        setLoading(false)
        return
      }

      normalized.forEach((type, idx) => {
        const delay = idx * CALL_DELAY_MS
        scheduleAndRunFetch(type, delay)
      })
    } catch (err) {
      console.error('Error fetching TYPE_LIST for Naphtha Summary:', err)
      setLoading(false)
    }
  }, [AOP_YEAR, PLANT_ID, keycloak, scheduleAndRunFetch])

  useEffect(() => {
    fetchAllGrids()
    return () => {
      timeoutIdsRef.current.forEach((t) => clearTimeout(t))
      timeoutIdsRef.current = []
    }
  }, [fetchAllGrids, AOP_YEAR, PLANT_ID, keycloak, oldYear, yearChanged])

  const exportAllGrids = useCallback(() => {
    // Backend Export API (Commented out):
    // const excelFileName = `${lowerVertName || 'meg'}_${lowerSiteName || 'site'}_${lowerPlantName || 'plant'}_Naphtha_Summary.xlsx`
    // ProductionNormsApiService.exportNapthaSummary(
    //   keycloak,
    //   PLANT_ID,
    //   AOP_YEAR,
    //   'LIMS Summary',
    //   excelFileName,
    // )

    // UI Export only:
    const keys = Object.keys(exportRefs.current || {})
    if (!keys.length) return

    const firstKey = keys.find((k) => exportRefs.current[k])
    if (!firstKey) return
    const baseRef = exportRefs.current[firstKey]
    const baseOptions = baseRef?.workbookOptions?.()
    if (!baseOptions) return

    const sheets = gridNames
      .map((name) => {
        const ref = exportRefs.current[name]
        try {
          const opts = ref?.workbookOptions?.()
          return opts?.sheets?.[0] ? { ...opts.sheets[0] } : null
        } catch {
          return null
        }
      })
      .filter(Boolean)

    if (!sheets.length) return

    sheets.forEach((s, idx) => {
      s.title = gridNames[idx] || s.title || `Sheet${idx + 1}`
    })

    baseOptions.sheets = sheets
    baseRef.save(baseOptions)
  }, [gridNames])

  const fileName = `${lowerVertName || 'meg'}_${lowerSiteName || 'site'}_${lowerPlantName || 'plant'}_Naphtha_Summary.xlsx`

  return (
    <div>
      <LoaderBackdrop open={!!loading} />

      {/* Hidden ExcelExport instances for each grid */}
      <div style={{ display: 'none' }}>
        {gridNames.map((name) => {
          const data = dataMap[name] || { rows: [], columns: [] }
          const setRef = (ref) => {
            if (ref) exportRefs.current[name] = ref
          }
          return (
            <ExcelExport
              key={`excel-${name}`}
              data={data.rows}
              ref={setRef}
              fileName={fileName}
            >
              {(data.columns || []).map((col) => (
                <ExcelExportColumn
                  key={col.field}
                  field={col.field}
                  title={col.title || col.field}
                />
              ))}
            </ExcelExport>
          )
        })}
      </div>

      <Box display='flex' justifyContent='flex-end' mb='2px'>
        <Button
          variant='contained'
          onClick={exportAllGrids}
          className='btn-export'
          startIcon={<DownloadIcon fontSize='small' />}
        >
          Export
        </Button>
      </Box>

      <Box display='flex' flexDirection='column' gap={2}>
        {gridNames.length === 0 && !loading && (
          <Typography sx={{ p: 2 }}>No grids available.</Typography>
        )}

        {gridNames.map((name) => {
          const d = dataMap[name] || { rows: [], columns: [] }
          return (
            <div key={name}>
              <CustomAccordion defaultExpanded disableGutters>
                <CustomAccordionSummary
                  aria-controls={`${name}-content`}
                  id={`${name}-header`}
                >
                  <Typography component='span' className='grid-title'>
                    {name}
                  </Typography>
                </CustomAccordionSummary>
                <CustomAccordionDetails>
                  <Box sx={{ width: '100%', margin: 0 }}>
                    <KendoDataGrid
                      rows={d.rows}
                      columns={d.columns}
                      permissions={{ isHeight: d?.rows?.length > 15 }}
                    />
                  </Box>
                </CustomAccordionDetails>
              </CustomAccordion>
            </div>
          )
        })}
      </Box>
    </div>
  )
}

export default NaphthaSummary
