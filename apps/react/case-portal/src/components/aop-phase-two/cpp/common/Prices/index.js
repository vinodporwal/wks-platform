import React from 'react'
import PriceGrid from './PriceGrid'
import EdRpoRate from './EdRpoRate'
import EdRpoMapping from './EdRpoMapping'

const Prices = () => {
  return (
    <div>
      <PriceGrid />
      <EdRpoRate />
      <EdRpoMapping />
    </div>
  )
}

export default Prices