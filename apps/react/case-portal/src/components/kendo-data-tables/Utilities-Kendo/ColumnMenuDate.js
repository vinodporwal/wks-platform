
import { getColumnMenuCheckboxFilter } from '../../data-tables/Reports-kendo/ColumnMenu1'
import { CustomDateColumnMenu } from '../../data-tables/Reports-kendo/CustomDateColumnMenu';

const dateFields = [
  'maintStartDateTime',
  'maintEndDateTime',
  'endDateTA',
  'startDateTA',
  'endDateSD',
  'startDateSD',
  'endDateIBR',
  'startDateIBR',
];

export const getColumnMenu = (field, data) => {
  return dateFields.includes(field)
    ? CustomDateColumnMenu
    : getColumnMenuCheckboxFilter(data);
};
