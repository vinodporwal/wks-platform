// src/utils/CustomAccordion.js
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import MuiAccordion from '@mui/material/Accordion'
import MuiAccordionDetails from '@mui/material/AccordionDetails'
import MuiAccordionSummary from '@mui/material/AccordionSummary'
import { styled } from '@mui/material/styles'

// Custom Accordion
export const CustomAccordion = styled((props) => (
  <MuiAccordion disableGutters elevation={0} square {...props} />
))(() => ({
  position: 'unset',
  border: 'none',
  boxShadow: 'none',
  margin: '0px',
  '&:before': {
    display: 'none',
  },
}))

// Custom Accordion Summary
export const CustomAccordionSummary = styled((props) => (
  <MuiAccordionSummary expandIcon={<ExpandMoreIcon />} {...props} />
))(({theme}) => ({
  backgroundColor: theme?.palette?.mode === 'dark' ? '#131726' : '#fff',
  padding: '0px 12px',
  minHeight: '36px',
  '& .MuiAccordionSummary-content': {
    margin: '4px 0',
  },
}))

// Custom Accordion Details
export const CustomAccordionDetails = styled(MuiAccordionDetails)(({theme}) => ({
  padding: '0px 0px 12px',
  backgroundColor: theme?.palette?.mode === 'dark' ? '#131726' : '#fff',
}))
