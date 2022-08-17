import { Menu } from 'react-admin';

export const CustomMenu = () => (
    <Menu>
        <Menu.DashboardItem />
        <Menu.Item to="/caseList" primaryText="Case List" />
        <Menu.Item to="/tasklist" primaryText="Task List" />
    </Menu>
);