import { useMemo } from 'react'
import { useSelector } from 'react-redux'
import { normalizeContextValue } from '../utilities/plantContextResolver'

/**
 * Reads and normalizes the active vertical, site, plant, year, and release flags
 * from Redux `dataGridStore` for shared tab components.
 */
const usePlantContext = () => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year, oldYear, isReleased } =
    dataGridStore || {}

  return useMemo(() => {
    const verticalName = verticalObject?.name || ''
    const siteName = siteObject?.name || ''
    const plantName = plantObject?.name || ''

    const vertical = normalizeContextValue(verticalName)
    const site = normalizeContextValue(siteName)
    const plant = normalizeContextValue(plantName)

    const verticalId = verticalObject?.id ?? null
    const siteId = siteObject?.id ?? null
    const plantId = plantObject?.id ?? null
    const aopYear = year?.selectedYear ?? null

    return {
      vertical,
      site,
      plant,
      verticalName,
      siteName,
      plantName,
      verticalId,
      siteId,
      plantId,
      aopYear,
      isOldYear: Boolean(oldYear?.oldYear),
      isReleased: Boolean(isReleased),
      isReady: Boolean(plantId && aopYear),
      contextKey: `${vertical}-${site}-${plantId ?? plant}-${aopYear ?? ''}`,
      dataGridStore,
    }
  }, [
    plantObject,
    siteObject,
    verticalObject,
    year,
    oldYear,
    isReleased,
    dataGridStore,
  ])
}

export default usePlantContext

