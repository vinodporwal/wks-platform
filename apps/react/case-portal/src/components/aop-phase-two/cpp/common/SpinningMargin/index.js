import SpinningMarginJMD from './SpinningMarginJMD'
import SpinningMarginDMD from './SpinningMarginDMD'
import { useSelector } from 'react-redux'

const SpinningMargin = () => {

    const dataGridStore = useSelector((state) => state.dataGridStore)
    const {
      siteObject,
    } = dataGridStore
    const IS_JMD = siteObject?.name?.toLowerCase() == 'jmd'
  return (
    <div>
      {IS_JMD ? <SpinningMarginJMD /> : <SpinningMarginDMD />}
    </div>
  )
}

export default SpinningMargin