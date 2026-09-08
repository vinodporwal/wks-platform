import { useEffect, useState, lazy, Suspense } from 'react'
import { ThemeRoutes } from './routes'
import ThemeCustomization from './themes'
import { SessionStoreProvider } from './SessionStoreContext'
import { CaseService, RecordService, FormService } from 'services'
import menuItemsDefs from './menu'
import { RegisterInjectUserSession, RegisteOptions } from './plugins'
import { sessionStore } from './store'
import './App.css'
// import formPayload from './createFormJSON.json'
import dtrPayload from './DTR.json'
import rejectPayload from './Reject.json'
import assetTrainCreateCasePayload from './AssetTrainCreateCase.json'
import caseManagementPayload from './CaseManagement.json'
import Config from './consts'
import { useIframeSso } from './hooks/useIframeSso'

const ScrollTop = lazy(() => import('./components/ScrollTop'))

const App = () => {
  const [keycloak, setKeycloak] = useState({})
  const [authenticated, setAuthenticated] = useState(null)
  const [recordsTypes, setRecordsTypes] = useState([])
  const [casesDefinitions, setCasesDefinitions] = useState([])
  const [menu, setMenu] = useState({ items: [] })
  const [formChecked, setFormChecked] = useState(false)

  const isInIframe = window.self !== window.top

  // Cookie cleanup and cache management
  useEffect(() => {
    const appVersion = process.env.REACT_APP_VERSION || '1.0.0';
    const lastVersion = localStorage.getItem('wks_app_version');
    
    if (lastVersion !== appVersion) {
      console.log(`App version changed from ${lastVersion} to ${appVersion} - performing selective cleanup`);
      
      // === SELECTIVE COOKIE CLEANUP ===
      // Only clear cookies that are known to accumulate and cause header bloat
      // DO NOT clear functional cookies that WKS needs
      const problematicCookies = [
        // APM-specific cookies that accumulate
        'apm_session', 'apm_token', 'apm_auth', 'apm_sso',
        // Generic SSO cookies that grow large
        'sso_session', 'sso_token', 'sso_auth', 'sso_state',
        // Keycloak cookies that can accumulate
        'KC_RESTART', 'AUTH_SESSION_ID', 'AUTH_SESSION_ID_LEGACY',
        // Large session cookies that aren't essential
        'connect.sid', 'session_state', 'legacy_session'
      ];
      
      // Get all cookies and only clear the problematic ones
      document.cookie.split(";").forEach(cookie => {
        const eqPos = cookie.indexOf("=");
        const name = eqPos > -1 ? cookie.substr(0, eqPos).trim() : cookie.trim();
        
        // Only clear cookies that match problematic patterns or names
        const shouldClear = problematicCookies.includes(name) ||
                           name.toLowerCase().includes('apm_') ||
                           name.toLowerCase().includes('_sso_') ||
                           (name.toLowerCase().includes('session') && name.length > 20); // Long session names
        
        if (shouldClear) {
          // Clear cookie for current domain
          document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`;
          document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/cm`;
          document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;domain=${window.location.hostname}`;
          
          console.log(`Cleared problematic cookie: ${name}`);
        }
      });
      
      // === SELECTIVE BROWSER CACHE CLEANUP ===
      // Clear browser caches but preserve service worker caches
      if ('caches' in window) {
        caches.keys().then(cacheNames => {
          cacheNames.forEach(cacheName => {
            // Only clear WKS-specific caches, not all caches
            if (cacheName.includes('wks') || cacheName.includes('case-portal')) {
              caches.delete(cacheName);
              console.log(`Cleared cache: ${cacheName}`);
            }
          });
        });
      }
      
      // === SELECTIVE SESSION STORAGE CLEANUP ===
      // Only remove problematic sessionStorage items, preserve important ones
      const sessionKeysToRemove = [];
      const preservedSessionKeys = ['apmMainAssetInfo']; // Keep APM asset info
      
      for (let i = 0; i < sessionStorage.length; i++) {
        const key = sessionStorage.key(i);
        if (key && !preservedSessionKeys.includes(key)) {
          // Only remove auth-related session items that can accumulate
          if (key.includes('keycloak_') || key.includes('sso_') || 
              key.includes('auth_') || key.includes('token_') ||
              (key.includes('session') && key.length > 15)) {
            sessionKeysToRemove.push(key);
          }
        }
      }
      
      sessionKeysToRemove.forEach(key => {
        sessionStorage.removeItem(key);
        console.log(`Cleared sessionStorage: ${key}`);
      });
      
      // === PRESERVE IMPORTANT LOCALSTORAGE ===
      // DO NOT clear these localStorage items as they are functional:
      // - baseUrl, keycloakToken, userId (auth essentials)
      // - aCaseOwnerEmail, formData1 (case form data)
      // - categoryOptions, faultCategoryOptions, etc. (cached options)
      // - dtrCreated, rejectCreated, etc. (form creation flags)
      
      // Update stored version
      localStorage.setItem('wks_app_version', appVersion);
      
      // Force a single page reload only if this was an actual version change (not first load)
      if (lastVersion && lastVersion !== appVersion) {
        console.log('Reloading page to apply clean state...');
        window.location.reload();
        return; // Exit early to prevent further execution
      }
    }
  }, []); // Empty dependency array - only run on mount

  // Iframe SSO flow: receive token from APM via postMessage
  useIframeSso({
    onSuccess: async (token) => {
      const payload = JSON.parse(atob(token.split('.')[1]))

      // The APM token is not a Keycloak token — call our backend /sso/userinfo
      // which uses the Keycloak admin client to look up the user profile + wks-portal roles
      let userInfo = {}
      try {
        const res = await fetch(`${Config.CaseEngineUrl}/sso/userinfo`, {
          credentials: 'include', // send the WKS_SSO_SESSION cookie set by /sso/login
        })
        if (res.ok) {
          userInfo = await res.json()
          console.log('SSO userinfo fetched from backend:', userInfo)
        } else {
          console.warn('SSO userinfo fetch failed, status:', res.status, '— user will have no roles')
        }
      } catch (e) {
        console.warn('SSO userinfo fetch error:', e)
      }

      // Inject wks-portal roles into the token so buildMenuItems works correctly
      const wksRoles = userInfo.wks_portal_roles || []
      const patchedPayload = {
        ...payload,
        resource_access: {
          ...(payload.resource_access || {}),
          'wks-portal': { roles: wksRoles },
        },
        azp: 'wks-portal',
      }

      const kcMock = {
        token,
        tokenParsed: patchedPayload,
        idTokenParsed: { ...patchedPayload, ...userInfo },
        isTokenExpired: () => payload.exp * 1000 < Date.now(),
        updateToken: () => Promise.resolve(false),
        hasRealmRole: (role) => (patchedPayload.resource_access?.['wks-portal']?.roles || []).includes(role),
        hasResourceRole: (role, clientId) => (patchedPayload.resource_access?.[clientId || 'wks-portal']?.roles || []).includes(role),
      }
      initApp(kcMock, true)
    },
    onFailure: (err) => {
      console.error('SSO iframe login failed:', err)
    },
  })

  function initApp(kc, authenticated) {
    localStorage.setItem('baseUrl', `${Config.CaseEngineUrl}`)
    setKeycloak(kc)
    setAuthenticated(authenticated)

    if (authenticated) {
      localStorage.setItem('keycloakToken', kc.token)
      localStorage.setItem('userId', kc.idTokenParsed.sub)
    }

    buildMenuItems(kc)
    RegisterInjectUserSession(kc)
    RegisteOptions(kc)

    if (!formChecked) {
          // checkAndPostForm(keycloak);
      checkAndPostDTR(kc)
      checkAndPostReject(kc)
      checkAndPostAssetTrainCreateCase(kc)
      checkAndPostCaseManagement(kc)
      setFormChecked(true)
    }
  }

  useEffect(() => {
    // Skip standard Keycloak init when inside iframe — SSO hook handles it
    if (isInIframe) return

    localStorage.setItem('baseUrl', `${Config.CaseEngineUrl}`)

    const { keycloak } = sessionStore.bootstrap()

    keycloak.init({ onLoad: 'login-required', checkLoginIframe: false }).then((authenticated) => {
      if (!authenticated) {
        keycloak.login()
        return
      }
      initApp(keycloak, authenticated)
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
                Math.round(keycloak.tokenParsed.exp + keycloak.timeSkew - new Date().getTime() / 1000) +
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


  async function buildMenuItems(keycloak) {

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

    // Hide the entire management menu group for users without management roles
    // if (!accountStore.isManagerUser(keycloak)) {
    //   menu.items = menu.items.filter(item => item.id !== 'management');
    // }

    if (!isAdmin) {
      menu.items = menu.items.filter(item => item.id !== 'management');

      // Restrict workspace group to Cases only for non-admin users
      menu.items[1].children = menu.items[1].children.filter(
        child => child.id === 'case-list'
      );
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

  // async function checkAndPostForm(keycloak) {
  //   if (localStorage.getItem('formCreated')) {
  //     console.log('Form "Case Management System" already exists.')
  //     return
  //   }
  //   try {
  //     // Use FormService to get all forms
  //     const data = await FormService.getAll(keycloak)

  //     // Check if "EED Case Management System" exists in the list
  //     const formExists = data.some(
  //       (form) => form.title === 'Case Management System',
  //     )

  //     if (formExists) {
  //       console.log('Case Management System" already exists.')
  //     } else {
  //       console.log(
  //         'Form "Case Management System" does not exist. Creating form...',
  //       )
  //       await createForm(keycloak)
  //     }
  //   } catch (error) {
  //     console.error('Error checking form existence:', error)
  //   }
  // }

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




  // async function createForm(keycloak) {
  //   try {
  //     // Use FormService to create a new form with the JSON payload
  //     const response = await FormService.create(keycloak, formPayload)

  //     if (!response.ok) {
  //       throw new Error('Failed to create form')
  //     }
  //     console.log('Form created successfully')
  //     localStorage.setItem('formCreated', 'true')
  //   } catch (error) {
  //     console.error('Error creating form:', error)
  //   }
  // }

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