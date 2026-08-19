/**
 * Standalone AOP Role Formatter Utilities
 * Capitalizes role words and converts 3-character prefixes/acronyms (e.g. tcs, gms, eps, aom, cts) to all uppercase (e.g. TCS Head, GMS Head).
 */

export const aopRoleDisplayName = (role) => {
  if (!role || typeof role !== 'string') return ''
  return role
    .trim()
    .replace(/_/g, ' ')
    .replace(/-/g, ' ')
    .split(' ')
    .map((word) => {
      if (!word) return ''
      // If 3 chars (like tcs, gms, eps, aom, cts, fca), make all caps
      if (word.length === 3) {
        return word.toUpperCase()
      }
      return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
    })
    .join(' ')
}

export const getRoleTitle = aopRoleDisplayName

export const formatRoleWithLabel = (role) => {
  if (!role || typeof role !== 'string') return ''
  const clean = role.trim()
  const title = aopRoleDisplayName(clean)
  return `${clean} - ${title}`
}
