/**
 * APM Context Management Utility
 * 
 * Handles persistence of APM iframe context parameters (assetName, hierarchyName, eventIds, sourceSystem)
 * using sessionStorage + URL fallback approach for Windows authentication environment.
 * 
 * Key features:
 * - Stores parameters in sessionStorage for persistence across navigation
 * - Falls back to URL parameters for initial load
 * - Provides unified access to context parameters
 * - Handles parameter cleanup and updates
 */

const APM_CONTEXT_KEY = 'apm_context_params';

/**
 * Get APM context parameters from sessionStorage or URL fallback
 * Priority: sessionStorage -> URL parameters -> defaults
 * 
 * @returns {object} Context parameters { assetName, hierarchyName, eventIds, sourceSystem }
 */
export const getApmContext = () => {
  // First, try to get from sessionStorage
  const storedContext = getStoredContext();
  
  // Then, get current URL parameters
  const urlParams = getUrlParameters();
  
  // If URL has parameters (initial iframe load or direct navigation), 
  // update sessionStorage and return URL params
  if (hasUrlParameters(urlParams)) {
    const mergedContext = {
      ...storedContext, // Keep existing stored context as base
      ...urlParams      // Override with URL parameters
    };
    
    // Only store non-empty values
    const contextToStore = Object.fromEntries(
      Object.entries(mergedContext).filter(([key, value]) => value && value.trim() !== '')
    );
    
    if (Object.keys(contextToStore).length > 0) {
      storeContext(contextToStore);
      console.log('APM Context: Updated from URL parameters', contextToStore);
    }
    
    return mergedContext;
  }
  
  // If no URL parameters but we have stored context, use stored context
  if (storedContext && Object.keys(storedContext).length > 0) {
    console.log('APM Context: Using stored context', storedContext);
    return storedContext;
  }
  
  // Fallback to defaults
  const defaultContext = {
    assetName: '',
    hierarchyName: '',
    eventIds: '',
    sourceSystem: ''
  };
  
  console.log('APM Context: Using default context');
  return defaultContext;
};

/**
 * Get URL parameters from current location
 * @returns {object} URL parameters
 */
const getUrlParameters = () => {
  const searchParams = new URLSearchParams(window.location.search);
  return {
    assetName: searchParams.get('assetName') || '',
    hierarchyName: searchParams.get('hierarchyName') || '',
    eventIds: searchParams.get('eventIds') || '',
    sourceSystem: searchParams.get('sourceSystem') || ''
  };
};

/**
 * Check if URL has any APM parameters
 * @param {object} urlParams URL parameters object
 * @returns {boolean} True if URL has APM parameters
 */
const hasUrlParameters = (urlParams) => {
  return Object.values(urlParams).some(value => value && value.trim() !== '');
};

/**
 * Get stored context from sessionStorage
 * @returns {object|null} Stored context or null if not found
 */
const getStoredContext = () => {
  try {
    const stored = sessionStorage.getItem(APM_CONTEXT_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      
      // Validate stored data structure
      if (typeof parsed === 'object' && parsed !== null) {
        return {
          assetName: parsed.assetName || '',
          hierarchyName: parsed.hierarchyName || '',
          eventIds: parsed.eventIds || '',
          sourceSystem: parsed.sourceSystem || ''
        };
      }
    }
  } catch (error) {
    console.warn('APM Context: Error reading from sessionStorage', error);
  }
  
  return null;
};

/**
 * Store context in sessionStorage
 * @param {object} context Context parameters to store
 */
const storeContext = (context) => {
  try {
    const cleanContext = {
      assetName: context.assetName || '',
      hierarchyName: context.hierarchyName || '',
      eventIds: context.eventIds || '',
      sourceSystem: context.sourceSystem || ''
    };
    
    sessionStorage.setItem(APM_CONTEXT_KEY, JSON.stringify(cleanContext));
  } catch (error) {
    console.warn('APM Context: Error storing to sessionStorage', error);
  }
};

/**
 * Manually update stored APM context
 * Useful when context changes programmatically
 * 
 * @param {object} updates Partial context updates
 */
export const updateApmContext = (updates) => {
  const currentContext = getStoredContext() || {};
  const newContext = {
    ...currentContext,
    ...updates
  };
  
  // Only store non-empty values
  const contextToStore = Object.fromEntries(
    Object.entries(newContext).filter(([key, value]) => value && value.trim() !== '')
  );
  
  storeContext(contextToStore);
  console.log('APM Context: Manually updated', contextToStore);
};

/**
 * Clear stored APM context
 * Useful for debugging or when context becomes invalid
 */
export const clearApmContext = () => {
  try {
    sessionStorage.removeItem(APM_CONTEXT_KEY);
    console.log('APM Context: Cleared stored context');
  } catch (error) {
    console.warn('APM Context: Error clearing sessionStorage', error);
  }
};

/**
 * Get APM context with backward compatibility for existing getUrlParams() usage
 * Returns the same structure as the original getUrlParams() function
 * 
 * @returns {object} { assetName, hierarchyName }
 */
export const getUrlParamsCompat = () => {
  const context = getApmContext();
  return {
    assetName: context.assetName || '',
    hierarchyName: context.hierarchyName || ''
  };
};

/**
 * Get full APM context for services that need eventIds and sourceSystem
 * 
 * @returns {object} { assetName, hierarchyName, eventIds, sourceSystem }
 */
export const getFullApmContext = () => {
  return getApmContext();
};

/**
 * Debug utility to log current APM context state
 */
export const debugApmContext = () => {
  const urlParams = getUrlParameters();
  const storedContext = getStoredContext();
  const finalContext = getApmContext();
  
  console.group('APM Context Debug');
  console.log('URL Parameters:', urlParams);
  console.log('Stored Context:', storedContext);
  console.log('Final Context:', finalContext);
  console.log('Storage Key:', APM_CONTEXT_KEY);
  console.groupEnd();
};