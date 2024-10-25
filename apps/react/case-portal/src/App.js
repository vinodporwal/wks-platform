import { useEffect, useState, lazy, Suspense } from 'react'
import { ThemeRoutes } from './routes'
import ThemeCustomization from './themes'
import { SessionStoreProvider } from './SessionStoreContext'
import { CaseService, RecordService } from 'services'
import menuItemsDefs from './menu'
import { RegisterInjectUserSession, RegisteOptions } from './plugins'
import { accountStore, sessionStore } from './store'
import './App.css'

const ScrollTop = lazy(() => import('./components/ScrollTop'))

const App = () => {
  const [keycloak, setKeycloak] = useState({})
  const [authenticated, setAuthenticated] = useState(null)
  const [recordsTypes, setRecordsTypes] = useState([])
  const [casesDefinitions, setCasesDefinitions] = useState([])
  const [menu, setMenu] = useState({ items: [] })

  useEffect(() => {

    localStorage.setItem('baseUrl', 'https://wkspwr.dev.connectedplant.honeywell.com:8902');
    const { keycloak } = sessionStore.bootstrap()

    const storedToken = localStorage.getItem('keycloakToken')
    if (storedToken) {
      keycloak.token = storedToken
    }

    keycloak.init({ onLoad: 'login-required' }).then((authenticated) => {
      setKeycloak(keycloak)
      setAuthenticated(authenticated)

      if (authenticated) {
        localStorage.setItem('keycloakToken', keycloak.token)
      }

      buildMenuItems(keycloak)
      RegisterInjectUserSession(keycloak)
      RegisteOptions(keycloak)
      forceLogoutIfUserNoMinimalRoleForSystem(keycloak)
    })

    keycloak.onAuthRefreshError = () => {
      window.location.reload()
    }

    keycloak.onTokenExpired = () => {
      keycloak
        .updateToken(70)
        .then((refreshed) => {
          if (refreshed) {
            console.info('Token refreshed: ' + refreshed)
            RegisterInjectUserSession(keycloak)
            RegisteOptions(keycloak)

            localStorage.setItem('keycloakToken', keycloak.token)
          } else {
            console.info(
              'Token not refreshed, valid for ' +
                Math.round(
                  keycloak.tokenParsed.exp +
                    keycloak.timeSkew -
                    new Date().getTime() / 1000,
                ) +
                ' seconds',
            )
          }
        })
        .catch(() => {
          console.error('Failed to refresh token')
          localStorage.removeItem('keycloakToken')
        })
    }
  }, [])

  async function forceLogoutIfUserNoMinimalRoleForSystem(keycloak) {
    if (!accountStore.hasAnyRole(keycloak)) {
      localStorage.removeItem('keycloakToken')
      return keycloak.logout({ redirectUri: window.location.origin })
    }
  }

  // async function buildMenuItems(keycloak) {
  //   const menu = {
  //     items: [...menuItemsDefs.items],
  //   }
  //   console.log('menuItemsDefs', menuItemsDefs)

  //   await RecordService.getAllRecordTypes(keycloak).then((data) => {
  //     setRecordsTypes(data)

  //     data.forEach((element) => {
  //       menu.items[1].children
  //         .filter((menu) => menu.id === 'record-list')[0]
  //         .children.push({
  //           id: element.id,
  //           title: element.id,
  //           type: 'item',
  //           url: '/record-list/' + element.id,
  //           breadcrumbs: true,
  //         })
  //     })
  //   })

  //   await CaseService.getCaseDefinitions(keycloak).then((data) => {
  //     setCasesDefinitions(data)

  //     data.forEach((element) => {
  //       menu.items[1].children
  //         .filter((menu) => menu.id === 'case-list')[0]
  //         .children.push({
  //           id: element.id,
  //           title: element.name,
  //           type: 'item',
  //           url: '/case-list/' + element.id,
  //           breadcrumbs: true,
  //         })
  //     })
  //   })

  //   if (!accountStore.isManagerUser(keycloak)) {
  //     delete menu.items[2]
  //   }

  //   return setMenu(menu)
  // }

  async function buildMenuItems(keycloak) {
    const menu = {
      items: [...menuItemsDefs.items],
    };
  
    console.log('menuItemsDefs:', menuItemsDefs);
  

    if (keycloak.hasRealmRole('admin')) {
      menu.items = menu.items.filter((item) => item.id === 'management');
    } 
    // else if (keycloak.hasRealmRole('user')) {
    //   menu.items = menu.items.filter((item) => item.id === 'utilities');
    // }
    // else if (keycloak.hasRealmRole('manager')) {
    //   menu.items = menu.items.filter((item) => item.id === 'dashboard');
    // } 
    else {
      await RecordService.getAllRecordTypes(keycloak).then((data) => {
        setRecordsTypes(data);
  
        data.forEach((element) => {
          menu.items[1].children
            .filter((menu) => menu.id === 'record-list')[0]
            .children.push({
              id: element.id,
              title: element.id,
              type: 'item',
              url: '/record-list/' + element.id,
              breadcrumbs: true,
            });
        });
      });
  
      await CaseService.getCaseDefinitions(keycloak).then((data) => {
        setCasesDefinitions(data);
  
        data.forEach((element) => {
          menu.items[1].children
            .filter((menu) => menu.id === 'case-list')[0]
            .children.push({
              id: element.id,
              title: element.name,
              type: 'item',
              url: '/case-list/' + element.id,
              breadcrumbs: true,
            });
        });
      });
  
      if (!accountStore.isManagerUser(keycloak)) {
        delete menu.items[2];
      }
    }
  
    return setMenu(menu);
  }
  
  

  return (
    keycloak &&
    authenticated && (
      <ThemeCustomization>
        <Suspense fallback={<div>Loading...</div>}>
          <ScrollTop>
            <SessionStoreProvider value={{ keycloak, menu }}>
              <ThemeRoutes
                keycloak={keycloak}
                authenticated={authenticated}
                recordsTypes={recordsTypes}
                casesDefinitions={casesDefinitions}
              />
            </SessionStoreProvider>
          </ScrollTop>
        </Suspense>
      </ThemeCustomization>
    )
  )
}

export default App
