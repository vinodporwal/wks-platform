/* eslint-disable no-unused-vars */
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
  const [keycloak, setKeycloak] = useState(null)
  const [authenticated, setAuthenticated] = useState(false)
  const [recordsTypes, setRecordsTypes] = useState([])
  const [casesDefinitions, setCasesDefinitions] = useState([])
  const [menu, setMenu] = useState({ items: [] })
  const [formChecked, setFormChecked] = useState(false)

  const tokenParsed = {
    exp: 1730721251,
    iat: 1730720951,
    auth_time: 1730720950,
    jti: 'dccfcb23-11e8-463d-8deb-b05ca64eb5f3',
    iss: 'http://localhost:8082/realms/localhost',
    aud: 'wks-portal',
    sub: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
    typ: 'Bearer',
    azp: 'wks-portal',
    nonce: '2adc31a8-7986-482d-8038-33a30ea9117a',
    session_state: '6eaa842c-b82b-4295-a7c9-1f18e4cbc891',
    acr: '1',
    sid: '6eaa842c-b82b-4295-a7c9-1f18e4cbc891',
    email_verified: false,
    org: 'localhost',
    name: 'demo demo',
    preferred_username: 'demo',
    given_name: 'demo',
    family_name: 'demo',
    email: 'demo@demo.com',
  }

  const username = 'demo'
  const password = 'demo'
  const clientId = 'wks-portal'
  const realm = 'localhost'
  const baseUrl = Config.LoginUrl

  useEffect(() => {
    const storedToken = localStorage.getItem('keycloakToken')
    if (storedToken) {
      setupKeycloak(storedToken)
      // fetchCaseStatusOptions(storedToken)
    } else {
      fetchToken()
    }
  }, [])

  const fetchCaseStatusOptions = async (token) => {
    try {
      const data = await CaseService.getCaseStatus({ token })
      const options = data.map((item) => ({
        label: item.name,
        value: item.id,
      }))
      localStorage.setItem('caseStatusOptions', JSON.stringify(options))
    } catch (error) {
      console.error('Error fetching case status options:', error)
    }
  }

  const fetchToken = () => {
    const loginData = {
      client_id: clientId,
      grant_type: 'password',
      username: username,
      password: password,
    }

    fetch(`${baseUrl}/realms/${realm}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams(loginData),
    })
      .then((response) => response.json())
      .then((data) => {
        if (data.access_token) {
          // Store token and refresh token in local storage
          localStorage.setItem('keycloakToken', data.access_token)
          localStorage.setItem('refreshToken', data.refresh_token)
          localStorage.setItem('idToken', data.id_token)

          // Set up Keycloak object and proceed with authenticated flow
          setupKeycloak(data.access_token, data)
        } else {
          console.error('Login failed:', data)
        }
      })
      .catch((error) => {
        console.error('Error logging in:', error)
      })
  }

  const setupKeycloak = (token, data) => {
    const keycloakData = {
      token: token,
      refreshToken: data
        ? data.refresh_token
        : localStorage.getItem('refreshToken'),
      idToken: data ? data.id_token : localStorage.getItem('idToken'),
      authenticated: true,
      idTokenParsed: tokenParsed, // Always set this value as tokenParsed
      hasRealmRole: () => true,
      isTokenExpired: () => {
        const currentTime = Math.floor(Date.now() / 1000)
        return currentTime >= tokenParsed.exp
      },
      refreshTokenIfNeeded: () => {
        if (keycloakData.isTokenExpired()) {
          fetchToken() // Fetch a new token if expired
        }
      },
      updateToken: (minValidity) => {
        return new Promise((resolve, reject) => {
          if (keycloakData.isTokenExpired()) {
            console.warn('Token expired, fetching a new token...')
            fetchToken() // Function to fetch a new token
            resolve(true) // Indicate the token was refreshed
          } else {
            console.warn('Token still valid, no refresh needed.')
            resolve(false) // Indicate no refresh was needed
          }
        })
      },
      logout: () => {
        localStorage.clear()
        window.location.href = window.location.origin
      },
    }

    setAuthenticated(true)
    setKeycloak(keycloakData)
    proceedAfterAuth(keycloakData)
  }

  const proceedAfterAuth = (keycloakData) => {
    buildMenuItems(keycloakData)
    RegisterInjectUserSession(keycloakData)
    RegisteOptions(keycloakData)

    if (!formChecked) {
      checkAndPostForm(keycloakData)
      setFormChecked(true)
    }
  }

  async function buildMenuItems(keycloakData) {
    const menu = { items: [...menuItemsDefs.items] }

    // Fetch all record types and case definitions, add them to the menu
    await RecordService.getAllRecordTypes(keycloakData).then((data) => {
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

    await CaseService.getCaseDefinitions(keycloakData).then((data) => {
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

    setMenu(menu)
  }

  async function checkAndPostForm(keycloakData) {
    if (localStorage.getItem('formCreated')) {
      console.log('Form "EED Case Management System" already exists.')
      return
    }
    try {
      const data = await FormService.getAll(keycloakData)
      const formExists = data.some(
        (form) => form.title === 'EED Case Management System',
      )

      if (formExists) {
        console.log('Form "EED Case Management System" already exists.')
      } else {
        await createForm(keycloakData)
      }
    } catch (error) {
      console.error('Error checking form existence:', error)
    }
  }

  async function createForm(keycloakData) {
    try {
      const response = await FormService.create(keycloakData, formPayload)
      if (!response.ok) {
        throw new Error('Failed to create form')
      }
      console.log('Form created successfully')
      localStorage.setItem('formCreated', 'true')
    } catch (error) {
      console.error('Error creating form:', error)
    }
  }

  return authenticated && keycloak ? (
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
  ) : (
    <div>Loading authentication...</div>
  )
}

export default App
