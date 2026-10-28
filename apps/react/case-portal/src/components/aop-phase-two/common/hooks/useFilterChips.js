import { useMemo, useCallback } from 'react'

/**
 * Custom hook to extract active filters from Kendo's nested filter structure
 * and provide chip removal / clear-all functionality.
 *
 * @param {Object} filter - Kendo filter state object ({ logic, filters: [...] })
 * @param {Function} setFilter - State setter for filter
 * @param {Array} columns - Column definitions array (supports nested children)
 * @returns {{ activeFilters, getColumnTitle, handleRemoveFilter, handleClearAllFilters }}
 */
export const useFilterChips = (filter, setFilter, columns) => {
  // Extract active filters as a flat list from nested Kendo filter structure
  const activeFilters = useMemo(() => {
    const extract = (f) => {
      if (!f || !f.filters || f.filters.length === 0) return []
      return f.filters.flatMap((subFilter) => {
        if (subFilter.filters) {
          return extract(subFilter)
        }
        return [subFilter]
      })
    }
    return extract(filter)
  }, [filter])

  // Get column title for a field by searching nested columns
  const getColumnTitle = useCallback(
    (field) => {
      const findTitle = (cols) => {
        for (const col of cols) {
          if (col.children) {
            const found = findTitle(col.children)
            if (found) return found
          }
          if (col.field === field) {
            return col.title || col.headerName || field
          }
        }
        return null
      }
      return findTitle(columns) || field
    },
    [columns],
  )

  // Remove a specific filter from the filter state
  const handleRemoveFilter = useCallback(
    (field, value, operator) => {
      setFilter((prevFilter) => {
        const removeMatching = (f) => {
          if (!f || !f.filters) return f
          const newFilters = f.filters
            .map((subFilter) => {
              if (subFilter.filters) {
                return removeMatching(subFilter)
              }
              return subFilter
            })
            .filter((subFilter) => {
              if (subFilter.filters) {
                return subFilter.filters.length > 0
              }
              return !(
                subFilter.field === field &&
                subFilter.value === value &&
                subFilter.operator === operator
              )
            })
          return { ...f, filters: newFilters }
        }
        return removeMatching(prevFilter)
      })
    },
    [setFilter],
  )

  // Clear all filters
  const handleClearAllFilters = useCallback(() => {
    setFilter({ logic: 'and', filters: [] })
  }, [setFilter])

  return {
    activeFilters,
    getColumnTitle,
    handleRemoveFilter,
    handleClearAllFilters,
  }
}
