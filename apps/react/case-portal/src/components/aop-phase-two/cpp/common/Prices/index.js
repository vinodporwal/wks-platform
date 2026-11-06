import React from 'react'
import { useSelector } from 'react-redux'
import PriceGrid from './PriceGrid'
import EdRpoRate from './EdRpoRate'
import EdRpoMapping from './EdRpoMapping'
import RpoMonthlyConfig from './RpoMonthlyConfig'

const Prices = () => {
  const siteObject = useSelector((state) => state.dataGridStore?.siteObject)
  const isJmd = siteObject?.name?.toLowerCase() === 'jmd'

  return (
    <div>
      <PriceGrid />
      {isJmd && (
        <>
          <EdRpoRate />
          <RpoMonthlyConfig />
          <EdRpoMapping />
        </>
      )}
    </div>
  )
}

export default Prices