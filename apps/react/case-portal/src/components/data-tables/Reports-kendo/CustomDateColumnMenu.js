
import React from 'react';
import {
  GridColumnMenuFilter,
} from '@progress/kendo-react-grid';

export const CustomDateColumnMenu = (props) => {
  return (
    <div>
      <GridColumnMenuFilter
        {...props}
        expanded = {true}
        filterUISettings={{
          operators: {
            date: {
              after: 'is after',
              afterEq: 'is after or equal to',
              before: 'is before',
              beforeEq: 'is before or equal to',
            },
          },
        }}
      />
    </div>
  );
};

CustomDateColumnMenu.displayName = 'CustomDateColumnMenu';
