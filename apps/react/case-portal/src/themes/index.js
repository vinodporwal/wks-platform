import PropTypes from 'prop-types'
import { useMemo } from 'react'

// material-ui
import { CssBaseline, StyledEngineProvider } from '@mui/material'
import { createTheme, ThemeProvider } from '@mui/material/styles'
import { useSelector } from 'react-redux'

// project import
import Palette from './palette'
import Typography from './typography'
import CustomShadows from './shadows'
import componentsOverride from './overrides'

// ==============================|| THEME HELPER ||============================== //

export const useCustomTheme = (mode) => {
  const theme = useMemo(() => Palette(mode, 'default'), [mode])

  const themeTypography = useMemo(
    () => Typography("'Honeywell Sans Web','Open Sans','Public Sans', sans-serif"),
    [],
  )

  const themeCustomShadows = useMemo(() => CustomShadows(theme), [theme])

  const themeOptions = useMemo(
    () => ({
      breakpoints: {
        values: {
          xs: 0,
          sm: 768,
          md: 1024,
          lg: 1266,
          xl: 1536,
        },
      },
      direction: 'ltr',
      mixins: {
        toolbar: {
          minHeight: 55,
          paddingTop: 8,
          paddingBottom: 8,
        },
      },
      palette: theme.palette,
      customShadows: themeCustomShadows,
      typography: themeTypography,
    }),
    [theme, themeTypography, themeCustomShadows],
  )

  const themes = useMemo(() => {
    const t = createTheme(themeOptions)
    t.components = componentsOverride(t)
    return t
  }, [themeOptions])

  return themes
}

// ==============================|| DEFAULT THEME - MAIN  ||============================== //

export default function ThemeCustomization({ children }) {
  const { mode } = useSelector((state) => state.theme)
  const themes = useCustomTheme(mode)

  return (
    <StyledEngineProvider injectFirst>
      <ThemeProvider theme={themes}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </StyledEngineProvider>
  )
}

ThemeCustomization.propTypes = {
  children: PropTypes.node,
}

// ==============================|| SCOPED THEME - DRAWER ||============================== //

export function DrawerThemeCustomization({ children }) {
  const { mode } = useSelector((state) => state.theme)
  const themes = useCustomTheme(mode)

  return <ThemeProvider theme={themes}>{children}</ThemeProvider>
}

DrawerThemeCustomization.propTypes = {
  children: PropTypes.node,
}
