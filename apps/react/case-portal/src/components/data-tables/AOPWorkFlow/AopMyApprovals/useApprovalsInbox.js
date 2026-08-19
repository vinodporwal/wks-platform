import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { AopApprovalService } from 'services/AopApprovalService'
import { DataService } from 'services/DataService'
import { setVerticalChangeFromDashboard } from 'store/reducers/dataGridStore'

/**
 * Custom hook managing state & operations for the Approvals Inbox table
 */
export default function useApprovalsInbox(onClose) {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const keycloak = useSession()

  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [sitesData, setSitesData] = useState(null)
  const [modifiedCells, setModifiedCells] = useState({})
  const [navigatingId, setNavigatingId] = useState(null)
  const [expandedRows, setExpandedRows] = useState({})
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })

  const toggleRowExpand = useCallback((rowId) => {
    setExpandedRows((prev) => ({
      ...prev,
      [rowId]: !prev[rowId],
    }))
  }, [])

  const handleExpandChange = useCallback(
    (e) => {
      const dataItem = e.dataItem
      if (!dataItem) return
      const rowId = dataItem.id || dataItem.taskId
      if (rowId) {
        toggleRowExpand(rowId)
      }
    },
    [toggleRowExpand],
  )

  // Load pending approvals list from backend
  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await AopApprovalService.getMyPending(keycloak)
      const rawList = Array.isArray(data) ? data : []
      const itemsWithId = rawList.map((item, index) => {
        const rowKey =
          item.id ||
          item.taskId ||
          (item.plantId && item.year
            ? `${item.plantId}_${item.year}`
            : `approval_row_${index}`)
        return {
          ...item,
          id: rowKey,
        }
      })
      setItems(itemsWithId)
    } catch (e) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: e.message || 'Failed to load approvals',
        severity: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [keycloak])

  useEffect(() => {
    load()
  }, [load])

  // Pre-fetch sites hierarchy to map plant -> site -> vertical IDs accurately
  useEffect(() => {
    let active = true
    const fetchSites = async () => {
      try {
        const res = await DataService.getAllSites(keycloak)
        if (active && Array.isArray(res)) {
          setSitesData(res)
        }
      } catch (err) {
        console.warn('Failed to pre-fetch sites hierarchy:', err)
      }
    }
    fetchSites()
    return () => {
      active = false
    }
  }, [keycloak])

  // Filter items dynamically based on quick search term
  const filteredItems = useMemo(() => {
    const list = items || []
    const term = searchTerm.trim().toLowerCase()

    const matched = !term
      ? list
      : list.filter((item) => {
          const pName = String(item.plantName || item.plant || '').toLowerCase()
          const sName = String(item.siteName || item.site || '').toLowerCase()
          const vName = String(
            item.verticalName || item.vertical || '',
          ).toLowerCase()
          const year = String(item.year || '').toLowerCase()
          const stage = String(
            item.gateDisplayName || item.gateName || '',
          ).toLowerCase()
          const role = String(item.assignedRole || '').toLowerCase()
          const dateStr = String(item.actionTakenDate || '').toLowerCase()
          const modeStr =
            item.actions?.mode === 'ACTION'
              ? 'approval pending action required'
              : 'in progress tracked'
          return (
            pName.includes(term) ||
            sName.includes(term) ||
            vName.includes(term) ||
            year.includes(term) ||
            stage.includes(term) ||
            role.includes(term) ||
            dateStr.includes(term) ||
            modeStr.includes(term)
          )
        })

    return matched.map((item) => {
      const rowId = item.id

      let sid = item.siteId || item.sid || item.sId || item.site_id || ''
      let v_id =
        item.verticalId || item.v_id || item.vid || item.vertical_id || ''

      if ((!sid || !v_id) && Array.isArray(sitesData)) {
        const pid = item.plantId || item.pid || item.plant_id
        for (const vertical of sitesData) {
          for (const site of vertical.sites || []) {
            for (const plant of site.plants || []) {
              const matchesId =
                pid &&
                String(plant.id).toUpperCase() === String(pid).toUpperCase()
              const matchesName =
                item.plantName &&
                (String(plant.id).toUpperCase() ===
                  String(item.plantName).toUpperCase() ||
                  String(
                    plant.displayName || plant.name || '',
                  ).toUpperCase() === String(item.plantName).toUpperCase())
              if (matchesId || matchesName) {
                if (!sid) sid = site.id
                if (!v_id) v_id = vertical.id
                break
              }
            }
          }
        }
      }

      const isCompleted =
        item.status === 'completed' ||
        item.gateName === 'COMPLETED' ||
        String(item.gateDisplayName || '').toLowerCase().includes('approved')
      const isAction = item.actions?.mode === 'ACTION'
      const statusModeStr = isCompleted
        ? 'Approved'
        : isAction
        ? 'Approval Pending'
        : 'In Progress'
      const stageStr = isCompleted
        ? 'All Approved'
        : item.gateDisplayName || item.gateName || 'Pending'

      return {
        ...item,
        siteId: sid,
        verticalId: v_id,
        expanded: Boolean(expandedRows[rowId]),
        statusMode: statusModeStr,
        gateDisplayName: stageStr,
        formattedActionDate: item.actionTakenDate
          ? new Date(item.actionTakenDate).toLocaleString(undefined, {
              year: 'numeric',
              month: 'short',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            })
          : '-',
      }
    })
  }, [items, searchTerm, expandedRows, sitesData])

  // Look up hierarchy & navigate to AOP workflow page for selected row
  const handleGoToPlant = useCallback(
    async (row) => {
      const rowId = row.id || row.plantId || row.plantName
      setNavigatingId(rowId)

      let targetPlantId = row.plantId || row.pid || row.plant_id || row.id || ''
      let targetPlantName = row.plantName || row.plant || ''
      let targetSiteName = row.siteName || row.site || ''
      let targetVerticalName = row.verticalName || row.vertical || ''

      let pid = targetPlantId
      let sid = row.siteId || row.sid || row.sId || row.site_id || ''
      let v_id = row.verticalId || row.v_id || row.vid || row.vertical_id || ''

      let hierarchy = sitesData
      if (!hierarchy) {
        try {
          hierarchy = await DataService.getAllSites(keycloak)
          if (Array.isArray(hierarchy)) setSitesData(hierarchy)
        } catch (e) {
          console.error('Error fetching sites hierarchy for plant lookup:', e)
        }
      }

      if (Array.isArray(hierarchy) && hierarchy.length > 0) {
        let found = false

        // Strategy 1: Exact UUID match for plant.id across all verticals & sites
        if (targetPlantId) {
          for (const vertical of hierarchy) {
            for (const site of vertical.sites || []) {
              for (const plant of site.plants || []) {
                if (
                  String(plant.id).toUpperCase() ===
                  String(targetPlantId).toUpperCase()
                ) {
                  pid = plant.id
                  sid = site.id
                  v_id = vertical.id
                  found = true
                  break
                }
              }
              if (found) break
            }
            if (found) break
          }
        }

        // Strategy 2: Exact Name match (verticalName + siteName + plantName)
        if (!found && targetPlantName) {
          for (const vertical of hierarchy) {
            const vNameMatch =
              !targetVerticalName ||
              String(vertical.id).toUpperCase() ===
                String(targetVerticalName).toUpperCase() ||
              String(vertical.displayName || vertical.name || '').toUpperCase() ===
                String(targetVerticalName).toUpperCase()

            if (!vNameMatch) continue

            for (const site of vertical.sites || []) {
              const sNameMatch =
                !targetSiteName ||
                String(site.id).toUpperCase() ===
                  String(targetSiteName).toUpperCase() ||
                String(site.displayName || site.name || '').toUpperCase() ===
                  String(targetSiteName).toUpperCase()

              if (!sNameMatch) continue

              for (const plant of site.plants || []) {
                const pNameMatch =
                  String(plant.id).toUpperCase() ===
                    String(targetPlantName).toUpperCase() ||
                  String(plant.displayName || plant.name || '').toUpperCase() ===
                    String(targetPlantName).toUpperCase()

                if (pNameMatch) {
                  pid = plant.id
                  sid = site.id
                  v_id = vertical.id
                  found = true
                  break
                }
              }
              if (found) break
            }
            if (found) break
          }
        }

        // Strategy 3: Fallback Plant Name match anywhere in hierarchy
        if (!found && targetPlantName) {
          for (const vertical of hierarchy) {
            for (const site of vertical.sites || []) {
              for (const plant of site.plants || []) {
                if (
                  String(plant.displayName || plant.name || '').toUpperCase() ===
                  String(targetPlantName).toUpperCase()
                ) {
                  pid = plant.id
                  sid = site.id
                  v_id = vertical.id
                  found = true
                  break
                }
              }
              if (found) break
            }
            if (found) break
          }
        }
      }

      const missingParams = []
      if (!pid) missingParams.push('Plant ID (pid)')
      if (!sid) missingParams.push('Site ID (sid)')
      if (!v_id) missingParams.push('Vertical ID (v_id)')

      if (missingParams.length > 0) {
        setNavigatingId(null)
        setSnackbarOpen(true)
        setSnackbarData({
          message: `Cannot navigate: Missing required parameters (${missingParams.join(', ')}).`,
          severity: 'error',
        })
        return
      }

      // Update Redux state with active context
      dispatch(
        setVerticalChangeFromDashboard({
          v_id,
          sid,
          pid,
          trigger: Date.now(),
        }),
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: `Redirecting to AOP Report for ${row.plantName || pid}...`,
        severity: 'success',
      })

      if (typeof onClose === 'function') {
        onClose()
      }

      setTimeout(() => {
        navigate('/workflow')
      }, 400)
    },
    [sitesData, keycloak, dispatch, navigate, onClose],
  )

  const [auditRow, setAuditRow] = useState(null)

  const handleOpenAudit = useCallback((row) => {
    setAuditRow(row)
  }, [])

  const handleCloseAudit = useCallback(() => {
    setAuditRow(null)
  }, [])

  return {
    items,
    setItems,
    loading,
    searchTerm,
    setSearchTerm,
    filteredItems,
    modifiedCells,
    setModifiedCells,
    navigatingId,
    snackbarOpen,
    setSnackbarOpen,
    snackbarData,
    setSnackbarData,
    load,
    handleGoToPlant,
    expandedRows,
    toggleRowExpand,
    handleExpandChange,
    auditRow,
    handleOpenAudit,
    handleCloseAudit,
  }
}
