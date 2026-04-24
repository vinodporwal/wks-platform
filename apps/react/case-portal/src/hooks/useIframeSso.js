import { useEffect, useRef } from 'react'
import Config from '../consts'

const APM_ORIGIN = process.env.REACT_APP_APM_ORIGIN || 'http://localhost:3000'

/**
 * Handles SSO token exchange when WKS is loaded inside an APM iframe.
 * Flow:
 *   1. WKS signals APM it is ready via postMessage
 *   2. APM sends token back
 *   3. WKS POSTs token to backend /api/sso/login
 *   4. Backend validates token and creates HttpOnly session
 *   5. onSuccess callback fires so App.js can proceed to render
 */
export function useIframeSso({ onSuccess, onFailure }) {
  const isInIframe = window.self !== window.top
  const handledRef = useRef(false)

  useEffect(() => {
    if (!isInIframe) return

    function handleMessage(event) {
      // Strict origin check
      console.log("event.origin:::::", event.origin)
      console.log("env.APM_ORIGIN:::::", APM_ORIGIN)
      if (!event.origin.startsWith(APM_ORIGIN)) return
      if (handledRef.current) return

      const { type, token } = event.data || {}

      if (type === 'SSO_TOKEN') {
        if (!token) {
          onFailure && onFailure('No token received from APM')
          return
        }
        handledRef.current = true
        exchangeToken(token)
      }

      if (type === 'LOGOUT') {
        fetch(`${Config.CaseEngineUrl}/sso/logout`, {
          method: 'POST',
          credentials: 'include',
        }).finally(() => {
          window.location.reload()
        })
      }

      // APM refreshed its token — update our session
      if (type === 'TOKEN_REFRESH') {
        if (!token) return
        exchangeToken(token)
      }
    }

    window.addEventListener('message', handleMessage)

    // Signal APM that WKS is ready to receive the token
    window.parent.postMessage({ type: 'WKS_READY' }, APM_ORIGIN)

    return () => window.removeEventListener('message', handleMessage)
  }, [isInIframe])

  async function exchangeToken(token) {
    try {
      const res = await fetch(`${Config.CaseEngineUrl}/sso/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include', // send/receive cookies
        body: JSON.stringify({ token }),
      })
      console.log("SSO login by APM Response:::", res)
      if (!res.ok) {
        const text = await res.text()
        onFailure && onFailure(`SSO login failed: ${text}`)
        return
      }

      const data = await res.json()
      console.log("SSO login by APM Response JSON :::", data);
      onSuccess && onSuccess(token, data)
    } catch (err) {
      onFailure && onFailure(`SSO login error: ${err.message}`)
    }
  }

  return { isInIframe }
}
