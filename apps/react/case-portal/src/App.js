import { useEffect, useState, lazy, Suspense } from 'react'
import { ThemeRoutes } from './routes'
import ThemeCustomization from './themes'
import { SessionStoreProvider } from './SessionStoreContext'
import { CaseService, RecordService, FormService } from 'services'
import menuItemsDefs from './menu'
import { RegisterInjectUserSession, RegisteOptions } from './plugins'
import { accountStore, sessionStore } from './store'
import './App.css'
import formPayload from './createFormJSON.json'
import Config from './consts'

const ScrollTop = lazy(() => import('./components/ScrollTop'))

const App = () => {
  const [keycloak, setKeycloak] = useState({})
  const [authenticated, setAuthenticated] = useState(null)
  const [recordsTypes, setRecordsTypes] = useState([])
  const [casesDefinitions, setCasesDefinitions] = useState([])
  const [menu, setMenu] = useState({ items: [] })
  const [formChecked, setFormChecked] = useState(false)

  useEffect(() => {
    localStorage.setItem('baseUrl', `${Config.CaseEngineUrl}`)

    const { keycloak } = sessionStore.bootstrap()

    const storedToken = localStorage.getItem('keycloakToken')
    if (storedToken) {
      keycloak.token = storedToken;
    }

    keycloak.init({ onLoad: 'login-required', checkLoginIframe: true }).then((authenticated) => {
    // keycloak.init({ onLoad: 'check-sso', checkLoginIframe: true }).then((authenticated) => {
      if(!authenticated){
        keycloak.login();
      }
      setKeycloak(keycloak)
      setAuthenticated(authenticated)

      if (authenticated) {
        localStorage.setItem('keycloakToken', keycloak.token)
        localStorage.setItem('keycloak', JSON.stringify(keycloak))
      }

      buildMenuItems(keycloak)
      RegisterInjectUserSession(keycloak)
      RegisteOptions(keycloak)
      forceLogoutIfUserNoMinimalRoleForSystem(keycloak)

      if (!formChecked) {
        checkAndPostForm(keycloak)
        setFormChecked(true) // Ensure it runs only once per session
      }
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
      console.log('User dont have required roles.');
      localStorage.removeItem('keycloakToken')
      return keycloak.logout({ redirectUri: window.location.origin })
    }
  }

  async function buildMenuItems(keycloak) {
    const menu = {
      items: [...menuItemsDefs.items],
    }
    console.log('menuItemsDefs', menuItemsDefs)

    await RecordService.getAllRecordTypes(keycloak).then((data) => {
      setRecordsTypes(data)

      data.forEach((element) => {
        menu.items[1].children
          .filter((menu) => menu.id === 'record-list')[0]
          .children.push({
            id: element.id,
            title: element.id,
            type: 'item',
            url: '/record-list/' + element.id,
            breadcrumbs: true,
          })
      })
    })

    await CaseService.getCaseDefinitions(keycloak).then((data) => {
      setCasesDefinitions(data)

      data.forEach((element) => {
        menu.items[1].children
          .filter((menu) => menu.id === 'case-list')[0]
          .children.push({
            id: element.id,
            title: element.name,
            type: 'item',
            url: '/case-list/' + element.id,
            breadcrumbs: true,
          })
      })
    })

    if (!accountStore.isManagerUser(keycloak)) {
      delete menu.items[2]
    }

    return setMenu(menu)
  }

  async function checkAndPostForm(keycloak) {
    if (localStorage.getItem('formCreated')) {
      console.log('Form "EED Case Management System" already exists.')
      return
    }
    try {
      // Use FormService to get all forms
      const data = await FormService.getAll(keycloak)

      // Check if "EED Case Management System" exists in the list
      const formExists = data.some(
        (form) => form.title === 'EED Case Management System',
      )

      if (formExists) {
        console.log('Form "EED Case Management System" already exists.')
      } else {
        console.log(
          'Form "EED Case Management System" does not exist. Creating form...',
        )
        await createForm(keycloak)
      }
    } catch (error) {
      console.error('Error checking form existence:', error)
    }
  }

  async function createForm(keycloak) {
    try {
      // Use FormService to create a new form with the JSON payload
      const response = await FormService.create(keycloak, formPayload)

      if (!response.ok) {
        throw new Error('Failed to create form')
      }
      console.log('Form created successfully')
      localStorage.setItem('formCreated', 'true')
    } catch (error) {
      console.error('Error creating form:', error)
    }
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
