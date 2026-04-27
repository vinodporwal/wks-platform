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
import dtrPayload from './DTR.json'
import rejectPayload from './Reject.json'
import assetTrainCreateCasePayload from './AssetTrainCreateCase.json'
import caseManagementPayload from './CaseManagement.json'
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
        localStorage.setItem('userId', keycloak.idTokenParsed.sub)
      }

      buildMenuItems(keycloak)
      RegisterInjectUserSession(keycloak)
      RegisteOptions(keycloak)
      forceLogoutIfUserNoMinimalRoleForSystem(keycloak)

      if (!formChecked) {
          checkAndPostForm(keycloak);
          checkAndPostDTR(keycloak);
          checkAndPostReject(keycloak);
          checkAndPostAssetTrainCreateCase(keycloak); 
          checkAndPostCaseManagement(keycloak);
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
    // if (!accountStore.hasAnyRole(keycloak)) {
    //   console.log('User dont have required roles.');
    //   localStorage.removeItem('keycloakToken')
    //   return keycloak.logout({ redirectUri: window.location.origin })
    // }
  }

  
   async function buildMenuItems(keycloak, userGroups = []) {

    const token = keycloak.tokenParsed;
    const clientId = token?.azp || token?.client_id; 
    const clientRoles = token?.resource_access?.[clientId]?.roles || [];
    console.log("*** buildMenuItems clientRoles : ", clientRoles);
   const isAdmin = clientRoles.includes('admin');
    console.log("*** buildMenuItems isAdmin : ", isAdmin);
    const menu = {
      items: [...menuItemsDefs.items],
    };
    console.log('menuItemsDefs', menuItemsDefs);

    const isLinkCaseUrl = location.pathname.endsWith('/link');

    if(isLinkCaseUrl) {
      menu.items = menu.items.filter(item => item.id !== 'dashboard');
    }

    // Hide the entire management menu group for users without management roles
    // if (!accountStore.isManagerUser(keycloak)) {
    //   menu.items = menu.items.filter(item => item.id !== 'management');
    // }

    if (!isAdmin) {
      menu.items = menu.items.filter(item => item.id !== 'management');
    }
  

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

    // if (!accountStore.isManagerUser(keycloak)) {
    //   delete menu.items[2]
    // }

    if (!isAdmin) {
      delete menu.items[2]
    }

    return setMenu(menu)
  }

  async function checkAndPostForm(keycloak) {
    if (localStorage.getItem('formCreated')) {
      console.log('Form "XOM Case Management System" already exists.')
      return
    }
    try {
      // Use FormService to get all forms
      const data = await FormService.getAll(keycloak)

      // Check if "EED Case Management System" exists in the list
      const formExists = data.some(
        (form) => form.title === 'XOM Case Management System',
      )

      if (formExists) {
        console.log('XOM Case Management System" already exists.')
      } else {
        console.log(
          'Form "XOM Case Management System" does not exist. Creating form...',
        )
        await createForm(keycloak)
      }
    } catch (error) {
      console.error('Error checking form existence:', error)
    }
  }

  async function checkAndPostDTR(keycloak) {
  if (localStorage.getItem('dtrCreated')) {
    console.log('DTR "Daily Time Record" form already exists.');
    return;
  }

  try {
    const data = await FormService.getAll(keycloak);

    const dtrExists = data.some(
      (form) => form.title === 'Daily Time Record'
    );

    if (dtrExists) {
      console.log('"DTR Case Management System" already exists.');
    } else {
      console.log('"DTR Case Management System" does not exist. Creating now...');
      await createDTR(keycloak);
    }
  } catch (error) {
    console.error('Error checking DTR existence:', error);
  }
}


async function checkAndPostReject(keycloak) {
  if (localStorage.getItem('rejectCreated')) {
    console.log('Reject "Reject Case Management System" already exists.');
    return;
  }

  try {
    const data = await FormService.getAll(keycloak);

    const rejectExists = data.some(
      (form) => form.title === 'Reject Case Management System'
    );

    if (rejectExists) {
      console.log('"Reject Case Management System" already exists.');
    } else {
      console.log('"Reject Case Management System" does not exist. Creating now...');
      await createReject(keycloak);
    }
  } catch (error) {
    console.error('Error checking Reject existence:', error);
  }
}

async function createReject(keycloak) {
  try {
    console.log('Reject Payload:', rejectPayload);

    const response = await FormService.create(keycloak, rejectPayload);

    if (!response.ok) {
      throw new Error('Failed to create Reject');
    }

    console.log('Reject created successfully');
    localStorage.setItem('rejectCreated', 'true');
  } catch (error) {
    console.error('Error creating Reject:', error);
  }
}


async function checkAndPostAssetTrainCreateCase(keycloak) {
  if (localStorage.getItem('assetTrainCaseCreated')) {
    console.log('AssetTrainCreateCase "Reject Case Management System" already exists.');
    return;
  }

  try {
    const data = await FormService.getAll(keycloak);

    const assetTrainCaseExists = data.some(
      (form) => form.title === 'Asset Train Create Case'
    );

    if (assetTrainCaseExists) {
      console.log('"Reject Case Management System" already exists.');
    } else {
      console.log('"Reject Case Management System" does not exist. Creating now...');
      await createAssetTrainCreateCase(keycloak);
    }
  } catch (error) {
    console.error('Error checking AssetTrainCreateCase existence:', error);
  }
}


async function checkAndPostCaseManagement(keycloak) {
  if (localStorage.getItem('caseManagementCreated')) {
    console.log('Case Management "Case Management System" already exists.');
    return;
  }

  try {
    const data = await FormService.getAll(keycloak);

    const caseManagementExists = data.some(
      (form) => form.title === 'Case Management System'
    );

    if (caseManagementExists) {
      console.log('"Case Management System" already exists.');
    } else {
      console.log('"Case Management System" does not exist. Creating now...');
      await createCaseManagement(keycloak);
    }
  } catch (error) {
    console.error('Error checking Case Management existence:', error);
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

async function createDTR(keycloak) {
  try {
    console.log('DTR Payload:', dtrPayload);  // Debug: make sure payload is loaded

    const response = await FormService.create(keycloak, dtrPayload);

    if (!response.ok) {
      throw new Error('Failed to create DTR');
    }

    console.log('DTR created successfully');
    localStorage.setItem('dtrCreated', 'true');
  } catch (error) {
    console.error('Error creating DTR:', error);
  }
}

async function createAssetTrainCreateCase(keycloak) {
  try {
    console.log('AssetTrainCreateCase Payload:', assetTrainCreateCasePayload);

    const response = await FormService.create(keycloak, assetTrainCreateCasePayload);

    if (!response.ok) {
      throw new Error('Failed to create Asset Train Create Case');
    }

    console.log('Asset Train Create Case created successfully');
    localStorage.setItem('assetTrainCaseCreated', 'true');
  } catch (error) {
    console.error('Error creating Asset Train Create Case:', error);
  }
}


async function createCaseManagement(keycloak) {
  try {
    console.log('Case Management Payload:', caseManagementPayload);

    const response = await FormService.create(keycloak, caseManagementPayload);

    if (!response.ok) {
      throw new Error('Failed to create Case Management');
    }

    console.log('Case Management created successfully');
    localStorage.setItem('caseManagementCreated', 'true');
  } catch (error) {
    console.error('Error creating Case Management:', error);
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