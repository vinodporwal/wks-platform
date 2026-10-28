import { useEffect, useRef } from 'react'

/**
 * Custom hook for debouncing a callback function
 * @param {Function} callback - The function to debounce
 * @param {number} delay - Delay in milliseconds (default: 3000ms)
 * @param {Array} dependencies - Dependencies array to trigger debounce
 */
export const useDebounce = (callback, delay = 3000, dependencies = []) => {
  const debounceTimerRef = useRef(null)

  useEffect(() => {
    if (dependencies.some((dep) => dep === undefined || dep === null)) {
      return
    }

    // Clear previous timer
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }

    // Set new timer with specified delay
    debounceTimerRef.current = setTimeout(() => {
      callback()
    }, delay)

    // Cleanup timer on unmount or when dependencies change
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current)
      }
    }
  }, dependencies)
}
