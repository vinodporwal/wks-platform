import { lazy } from 'react';
import Loadable from 'components/Loadable';
import MainLayout from 'layout/MainLayout';
import { CaseStatus } from 'common/caseStatus';
import { CaseList } from 'views/caseList/caseList';
import { RecordList } from 'views/record/recordList';
import { TaskList } from 'views/taskList/taskList';
import { BpmEngineList } from 'views/management/bpmEngine/bpmEngineList/bpmEngineList';
import { CaseDefList } from 'views/management/caseDef/caseDefList/caseDefList';
import { FormList } from 'views/management/form/formList';
import { RecordTypeList } from 'views/management/recordType/recordTypeList';

const ManagamentDefault = Loadable(lazy(() => import('views/management')));
const DashboardDefault = Loadable(lazy(() => import('views/dashboard')));

export const MainRoutes = (keycloak, authenticated, recordsTypes, casesDefinitions) => {
    let routes = {
        path: '/',
        element: <MainLayout keycloak={keycloak} authenticated={authenticated} />,
        children: [
            {
                path: '/',
                element: <DashboardDefault />
            },

            {
                path: 'home',
                element: <DashboardDefault />
            },
            {
                path: 'case-list',
                children: [
                    {
                        path: 'cases',
                        element: <CaseList keycloak={keycloak} />
                    },
                    {
                        path: 'wip-cases',
                        element: (
                            <CaseList
                                status={CaseStatus.WipCaseStatus.description}
                                keycloak={keycloak}
                            />
                        )
                    },
                    {
                        path: 'closed-cases',
                        element: (
                            <CaseList
                                status={CaseStatus.ClosedCaseStatus.description}
                                keycloak={keycloak}
                            />
                        )
                    },
                    {
                        path: 'archived-cases',
                        element: (
                            <CaseList
                                status={CaseStatus.ArchivedCaseStatus.description}
                                keycloak={keycloak}
                            />
                        )
                    }
                ]
            },
            {
                path: 'task-list',
                element: <TaskList keycloak={keycloak} />
            },
            {
                path: 'system',
                children: [
                    {
                        path: 'look-and-feel',
                        element: <ManagamentDefault />
                    }
                ]
            },
            {
                path: 'case-life-cycle',
                children: [
                    {
                        path: 'case-definition',
                        element: <CaseDefList />
                    },
                    {
                        path: 'record-type',
                        element: <RecordTypeList />
                    },
                    {
                        path: 'process-engine',
                        element: <BpmEngineList />
                    },
                    {
                        path: 'form',
                        element: <FormList />
                    }
                ]
            }
        ]
    };

    casesDefinitions.forEach((element) => {
        routes.children.push({
            path: 'case-list/' + element.id,
            element: <CaseList caseDefId={element.id} keycloak={keycloak} />
        });
    });

    recordsTypes.forEach((element) => {
        routes.children.push({
            path: 'record-list/' + element.id,
            element: <RecordList recordTypeId={element.id} />
        });
    });

    return routes;
};
