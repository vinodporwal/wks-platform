// src/routes/PrivateRoute.js
import { useMenuContext } from 'menu/menuProvider'
import { Navigate } from 'react-router-dom'
import { useSession } from 'SessionStoreContext'

const findFirstUrlFromMenu = (menu) => {
  for (const group of menu.items) {
    if (!group.children) continue
    for (const child of group.children) {
      if (child.type === 'item' && child.url) {
        return child.url
      } else if (child.children) {
        const firstItem = child.children.find((c) => c.type === 'item' && c.url)
        if (firstItem) return firstItem.url
      }
    }
  }
  return '/not-found'
}

const isRouteIdAllowed = (menu, routeId) => {
  for (const group of menu.items) {
    if (!group.children) continue
    for (const child of group.children || []) {
      if (child.id === routeId) return true
      if (child.children?.map((menu) => menu.id).includes(routeId)) return true
    }
  }
  return false
}

const PrivateRoute = ({ children, routeId }) => {
  const keycloak = useSession()
  const { items: menuItems } = useMenuContext()
  const menu = { items: [...menuItems] }
  const isPlantManager = keycloak?.realmAccess?.roles?.includes('plant_manager')

  const filterMenuByRole = (menuItems, hasPlantManagerRole) => {
    return menuItems.map((item) => {
      if (item.type === 'group' && item.children) {
        const filteredChildren = item.children.filter((child) => {
          if (child.id === 'user-management' && !hasPlantManagerRole) {
            return false
          }
          return true
        })

        return {
          ...item,
          children: filteredChildren,
        }
      }
      return item
    })
  }

  const filteredMenu = {
    ...menu,
    items: filterMenuByRole(menu?.items || [], isPlantManager),
  }
  console.log('PrivateRoute', isRouteIdAllowed(filteredMenu, routeId))
  if (isRouteIdAllowed(filteredMenu, routeId)) {
    return children
  }
  const fallbackUrl = findFirstUrlFromMenu(filteredMenu)
  console.log('fallbackUrl', fallbackUrl)
  return <Navigate to={fallbackUrl} replace />
}

export default PrivateRoute
