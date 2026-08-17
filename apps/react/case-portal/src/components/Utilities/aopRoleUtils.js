/**
 * Standalone AOP Role Formatter Utilities
 * Replaces underscores with spaces and capitalizes the 1st character of each word.
 */

export const getRoleTitle = (role) => {
  if (!role || typeof role !== 'string') return ''
  return role
    .trim()
    .replace(/_/g, ' ')
    .split(' ')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export const formatRoleWithLabel = (role) => {
  if (!role || typeof role !== 'string') return ''
  const clean = role.trim()
  const title = getRoleTitle(clean)
  return `${clean} - ${title}`
}
