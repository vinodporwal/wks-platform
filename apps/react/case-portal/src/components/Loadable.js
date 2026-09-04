import { Suspense } from 'react'
import PageSkeleton from './PageSkeleton'

function Loadable(Component) {
  return function LoadableComponent(props) {
    return (
      <Suspense fallback={<PageSkeleton />}>
        <Component {...props} />
      </Suspense>
    )
  }
}

export default Loadable
