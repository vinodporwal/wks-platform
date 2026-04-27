// 2.util.js

const collectItems = (nodes) =>
  nodes.flatMap((node) => {
    if (node.type === 'item') return [node]
    if (node.children) return collectItems(node.children)
    return []
  })

export const shouldShowReleaseButton = (menuItems) => {
  // console.log('menuItems', menuItems)

  const planGroup = menuItems
    ?.flatMap((m) => m.children || [])
    ?.find((c) => c.id === 'production-norms-plan')

  if (!planGroup?.children?.length) return false

  const allItems = collectItems(planGroup.children)
  const lastItem = allItems[allItems.length - 1]
  const lastKey = lastItem?.url?.split('/').pop()

  // console.log('lastKey', lastKey)

  return lastKey !== 'quality-packaging-norms'
}
