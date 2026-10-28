export const getRoleName = (keycloak, oldYear, IS_RELEASED) => {
  // return keycloak?.realmAccess?.roles?.includes('cts_admin')
  // console.log('oldYear role sevice', oldYear)
  // return keycloak?.realmAccess?.roles?.includes('read_only') || oldYear
  return keycloak?.realmAccess?.roles?.includes('read_only') || IS_RELEASED
  // return false
}
