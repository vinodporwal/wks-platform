import { lazy } from 'react'
import { Navigate } from '../../node_modules/react-router-dom/dist/index'
import Loadable from 'components/Loadable'
import MainLayout from 'layout/MainLayout'
import PrivateRoute from './PrivateRoutes'
import { CaseStatus } from 'common/caseStatus'

// Views
const CaseList = Loadable(lazy(() => import('views/caseList/caseList').then((m) => ({ default: m.CaseList }))))
const CaseDefList = Loadable(lazy(() => import('views/management/caseDef/caseDefList/caseDefList').then((m) => ({ default: m.CaseDefList }))))
const FormList = Loadable(lazy(() => import('views/management/form/formList').then((m) => ({ default: m.FormList }))))
const ProcessDefList = Loadable(lazy(() => import('views/management/processDef/processDefList').then((m) => ({ default: m.ProcessDefList }))))
const QueueList = Loadable(lazy(() => import('views/management/queue/queueList').then((m) => ({ default: m.QueueList }))))
const RecordTypeList = Loadable(lazy(() => import('views/management/recordType/recordTypeList').then((m) => ({ default: m.RecordTypeList }))))
const TaskList = Loadable(lazy(() => import('views/taskList/taskList').then((m) => ({ default: m.TaskList }))))
const ManagamentDefault = Loadable(lazy(() => import('../views/management')))
const DashboardDefault = Loadable(lazy(() => import('../views/dashboard')))

// Lazy loaded table and page components
const WorkFlowMerge = Loadable(lazy(() => import('components/data-tables/AOPWorkFlow/kendo-WorkFlowMerge')))
const AssessmentForm = Loadable(lazy(() => import('components/data-tables/AssesmentForm/AssessmentContext')))
const MonthwiseProduction = Loadable(lazy(() => import('components/data-tables/Reports-kendo/kendo-MonthwiseProduction')))
const MonthwiseRawMaterial = Loadable(lazy(() => import('components/data-tables/Reports-kendo/kendo-MonthwiseRawMaterial')))
const PlantsProductionSummary = Loadable(lazy(() => import('components/data-tables/Reports-kendo/kendo-PlantsProductionData')))
const ProductionVolumeDataBasis = Loadable(lazy(() => import('components/data-tables/Reports-kendo/kendo-ProductionVolumeDataBasis')))
const AnnualAopCost = Loadable(lazy(() => import('components/data-tables/Reports/AnnualAopCost')))
const NormsHistorianBasis = Loadable(lazy(() => import('components/data-tables/Reports/NormsHistorianBasis')))
const BestAchievedNorms = Loadable(lazy(() => import('components/data-tables/Reports/BestAchievedNorms')))
const BusinessDemand = Loadable(lazy(() => import('components/kendo-data-tables/BusinessDemand')))
const ConsumptionNorms = Loadable(lazy(() => import('components/kendo-data-tables/ConsumptionNorms')))
const PackagingConsumables = Loadable(lazy(() => import('components/kendo-data-tables/PackagingConsumables')))
const DecokingConfig = Loadable(lazy(() => import('components/kendo-data-tables/KendoConfigCrackerActivities')))
const CrackerConfig = Loadable(lazy(() => import('components/kendo-data-tables/KendoConfigCrackerInput')))
const CrackerConfigOutput = Loadable(lazy(() => import('components/kendo-data-tables/KendoConfigCrackerOutput')))
const MaintenanceTable = Loadable(lazy(() => import('components/kendo-data-tables/MaintenanceTable')))
const NormalOpNormsScreen = Loadable(lazy(() => import('components/kendo-data-tables/NormalOpNorms')))
const ProductionNorms = Loadable(lazy(() => import('components/kendo-data-tables/ProductionNorms')))
const ConfigurationOtherCost = Loadable(lazy(() => import('components/kendo-data-tables/ConfigurationOtherCost')))
const ProductionvolumeData = Loadable(lazy(() => import('components/kendo-data-tables/ProductionVoluemData')))
const ShutDown = Loadable(lazy(() => import('components/kendo-data-tables/ShutDown')))
const ShutdownNorms = Loadable(lazy(() => import('components/kendo-data-tables/ShutdownNorms')))
const SlowDown = Loadable(lazy(() => import('components/kendo-data-tables/Slowdown')))
const SlowdownNorms = Loadable(lazy(() => import('components/kendo-data-tables/SlowdownNorms')))
const TextSubmitComponent = Loadable(lazy(() => import('components/user-management/TextSubmitComponent')))
const UserForm = Loadable(lazy(() => import('components/user-management/UserForm')))
const UserManagementTable = Loadable(lazy(() => import('components/user-management/UserManagementTable')))
const QualityPackagingNorms = Loadable(lazy(() => import('components/kendo-data-tables/QualityPackagingNorms')))
const ConfigurationTable = Loadable(lazy(() => import('components/kendo-data-tables/KendoConfigurationTable')))
const AopBudget = Loadable(lazy(() => import('components/kendo-data-tables/AopBudget')))
const PlantTeam = Loadable(lazy(() => import('components/kendo-data-tables/PlantTeam')))
const RelPerf = Loadable(lazy(() => import('components/kendo-data-tables/RelPerf')))
const RelPerfPlantWise = Loadable(lazy(() => import('components/kendo-data-tables/RelPerfPlantWise')))
const PlantSafetyPerformanceTarget = Loadable(lazy(() => import('components/kendo-data-tables/PlantSafetyPerformanceTarget')))
const IntermediateValuesDataSet = Loadable(lazy(() => import('components/data-tables/Reports/IntermediateValuesDataSet')))
const RawDataSet = Loadable(lazy(() => import('components/data-tables/Reports/RawDataSet')))
const UtilitiesNormsBasis = Loadable(lazy(() => import('components/data-tables/Reports/UtilitiesNormsBasis')))
const SteadyStateNormsHistorianBasis = Loadable(lazy(() => import('components/data-tables/Reports/SteadyStateNormsHistorianBasis')))
const ConsumptionNormsHistorianBasis = Loadable(lazy(() => import('components/data-tables/Reports/ConsumptionNormsHistorianBasis')))
const BestAchievedIndividualNorms = Loadable(lazy(() => import('components/data-tables/Reports/BestAchievedIndividualNorms')))
const RunLengthDataSet = Loadable(lazy(() => import('components/data-tables/Reports/RunLengthDataSet')))
const MaintenanceSummary = Loadable(lazy(() => import('components/kendo-data-tables/MaintenanceSummary')))
const PlantBudgetSummary = Loadable(lazy(() => import('components/kendo-data-tables/PlantBudgetSummary')))
const SiteAOPReport = Loadable(lazy(() => import('components/kendo-data-tables/SiteAOPReport')))
const AopDesignBasis = Loadable(lazy(() => import('components/kendo-data-tables/AopDesignBasis')))
const ProductionTargetBasis = Loadable(lazy(() => import('components/data-tables/Reports/ProductionTargetBasis')))
const SiteMaintenanceSummary = Loadable(lazy(() => import('components/kendo-data-tables/SiteMaintenanceSummary')))
const FeedStockAvailability = Loadable(lazy(() => import('components/kendo-data-tables/FeedStockavailability')))
const TurnaroundPlanTable = Loadable(lazy(() => import('components/kendo-data-tables/TurnaroundPlanTable')))
const NormComparisonReport = Loadable(lazy(() => import('components/kendo-data-tables/NormComparisonReport')))

// CPP
const Inputs = Loadable(lazy(() => import('components/aop-phase-two/cpp/Inputs/index')))
const PlantRequirement = Loadable(lazy(() => import('components/aop-phase-two/cpp/PlantRequirement')))
const FixedConsumption = Loadable(lazy(() => import('components/aop-phase-two/cpp/FixedConsumption')))
const Norms = Loadable(lazy(() => import('components/aop-phase-two/cpp/Norms')))
const QtyCostReport = Loadable(lazy(() => import('components/aop-phase-two/cpp/qty-cost-report/index')))
const UtilityRate = Loadable(lazy(() => import('components/aop-phase-two/cpp/utility-rate/index')))
const Summary = Loadable(lazy(() => import('components/aop-phase-two/cpp/Summary/index')))
const Outputs = Loadable(lazy(() => import('components/aop-phase-two/cpp/Outputs')))
const TabManagement = Loadable(lazy(() => import('components/aop-phase-two/cpp/common/TabManagement')))

// TCS
const TcsOutput = Loadable(lazy(() => import('components/aop-phase-two/tcs/TcsOutput/index')))
const PimsOutput = Loadable(lazy(() => import('components/aop-phase-two/tcs/PimsOutput/PimsOutput')))
const TcsInput = Loadable(lazy(() => import('components/aop-phase-two/tcs/TcsInput/index')))
const WorkflowDiagram = Loadable(lazy(() => import('components/aop-phase-two/tcs/workflow-diagram/index')))
const AopDashboard = Loadable(lazy(() => import('components/kendo-data-tables/AopDashboard')))
const ProposedConsumptionNorms = Loadable(lazy(() => import('components/kendo-data-tables/ProposedConsumptionNorms')))
const ProposedAOP = Loadable(lazy(() => import('components/kendo-data-tables/ProposedAOP')))

// VGOHT
const ProductionNormsBasis = Loadable(lazy(() => import('components/aop-phase-two/vgoht/production-norms-basis/index')))
const ShutdownActivities = Loadable(lazy(() => import('components/aop-phase-two/vgoht/shutdown-activities/index')))
const SlowdownActivities = Loadable(lazy(() => import('components/aop-phase-two/vgoht/slowdown-activities/index')))
const ProductionTarget = Loadable(lazy(() => import('components/aop-phase-two/vgoht/production-target/index')))
const NetProductionHours = Loadable(lazy(() => import('components/aop-phase-two/vgoht/net-production-hours/index')))
const MonthwiseProductionPlan = Loadable(lazy(() => import('components/aop-phase-two/vgoht/monthwise-production-plan/index')))
const SteadyStateConsumption = Loadable(lazy(() => import('components/aop-phase-two/vgoht/steady-state-consumption/index')))
const ShutdownConsumption = Loadable(lazy(() => import('components/aop-phase-two/vgoht/shutdown-consumption/index')))
const SlowdownConsumption = Loadable(lazy(() => import('components/aop-phase-two/vgoht/slowdown-consumption/index')))
const OverallAopConsumption = Loadable(lazy(() => import('components/aop-phase-two/vgoht/overall-aop-consumption/index')))
const SiteBudgetSummary = Loadable(lazy(() => import('components/kendo-data-tables/SiteBudgetSummary')))
const QualityPackagingBasis = Loadable(lazy(() => import('components/data-tables/Reports/QualityPackagingBasis')))
const EthyleneBalance = Loadable(lazy(() => import('components/kendo-data-tables/EthyleneBalance')))
const PropyleneBalance = Loadable(lazy(() => import('components/kendo-data-tables/PropyleneBalance')))

// Crude
const ProductionNormsBasisCrude = Loadable(lazy(() => import('components/aop-phase-two/crude/production-norms-basis/index')))
const ShutdownActivitiesCrude = Loadable(lazy(() => import('components/aop-phase-two/crude/shutdown-activities/index')))
const SlowdownActivitiesCrude = Loadable(lazy(() => import('components/aop-phase-two/crude/slowdown-activities/index')))
const ProductionTargetCrude = Loadable(lazy(() => import('components/aop-phase-two/crude/production-target/index')))
const NetProductionHoursCrude = Loadable(lazy(() => import('components/aop-phase-two/crude/net-production-hours/index')))
const MonthwiseProductionPlanCrude = Loadable(lazy(() => import('components/aop-phase-two/crude/monthwise-production-plan/index')))
const SteadyStateConsumptionCrude = Loadable(lazy(() => import('components/aop-phase-two/crude/steady-state-consumption/index')))
const ShutdownConsumptionCrude = Loadable(lazy(() => import('components/aop-phase-two/crude/shutdown-consumption/index')))
const SlowdownConsumptionCrude = Loadable(lazy(() => import('components/aop-phase-two/crude/slowdown-consumption/index')))
const OverallAopConsumptionCrude = Loadable(lazy(() => import('components/aop-phase-two/crude/overall-aop-consumption/index')))

// FCC
const ProductionNormsBasisFCC = Loadable(lazy(() => import('components/aop-phase-two/fcc/production-norms-basis/index')))
const OverallAopConsumptionFCC = Loadable(lazy(() => import('components/aop-phase-two/fcc/overall-aop-consumption/index')))
const SteadyStateConsumptionFCC = Loadable(lazy(() => import('components/aop-phase-two/fcc/steady-state-consumption/index')))
const MonthwiseProductionPlanFCC = Loadable(lazy(() => import('components/aop-phase-two/fcc/monthwise-production-plan/index')))
const NetProductionHoursFCC = Loadable(lazy(() => import('components/aop-phase-two/fcc/net-production-hours/index')))

// Coker
const ProductionNormsBasisCoker = Loadable(lazy(() => import('components/aop-phase-two/coker/production-norms-basis/index')))
const OverallAopConsumptionCoker = Loadable(lazy(() => import('components/aop-phase-two/coker/overall-aop-consumption/index')))
const SteadyStateConsumptionCoker = Loadable(lazy(() => import('components/aop-phase-two/coker/steady-state-consumption/index')))
const MonthwiseProductionPlanCoker = Loadable(lazy(() => import('components/aop-phase-two/coker/monthwise-production-plan/index')))
const NetProductionHoursCoker = Loadable(lazy(() => import('components/aop-phase-two/coker/net-production-hours/index')))

// Staple (Polyester)
const BusinessDemandPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/business-demand/index')))
const MonthwiseProductionPlanPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/monthwise-production-plan/index')))
const NetProductionHoursPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/net-production-hours/index')))
const OverallAopConsumptionPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/overall-aop-consumption/index')))
const ProductionNormsBasisPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/production-norms-basis/index')))
const ProductionTargetPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/production-target/index')))
const ProposedAopConsumptionPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/proposed-aop-consumption/index')))
const QualityPackagingNormsPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/quality-packaging-norms/index')))
const ShutdownConsumptionPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/shutdown-consumption/index')))
const ShutdownPlanPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/shutdown-plan/index')))
const SlowdownPlanPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/slowdown-plan/index')))
const SlowdownConsumptionPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/slowdown-consumption/index')))
const SteadyStateConsumptionPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/steady-state-consumption/index')))
const GradeWiseSteadyStateConsumptionPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/grade-wise-steady-state-consumption/index')))
const ConfigurationOtherCostPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/configuration-other-cost/index')))
const ProductGradeSelection = Loadable(lazy(() => import('components/aop-phase-two/polyester/product-grade-selection/index')))
const ProposedSteadyStateConsumption = Loadable(lazy(() => import('components/aop-phase-two/polyester/proposed-steady-state-consumption/index')))
const MaterialGroupedSelectionPolyester = Loadable(lazy(() => import('components/aop-phase-two/polyester/material-grouped-selection/index')))

// Other
const OtherProduction = Loadable(lazy(() => import('components/kendo-data-tables/other-production/index')))
const SapBasedRefNorms = Loadable(lazy(() => import('components/data-tables/Reports-kendo/SapBasedRefNorms')))
const SpecificConsumptionCalculation = Loadable(lazy(() => import('components/kendo-data-tables/SpecificConsumptionCalculation')))
const ProductionOptimizer = Loadable(lazy(() => import('components/kendo-data-tables/ProductionOptimizer')))
const CausticSodaLyeBasis = Loadable(lazy(() => import('components/data-tables/Reports/CausticSodaLyeBasis')))
const MaterialBalance = Loadable(lazy(() => import('components/kendo-data-tables/MaterialBalance')))
const CatalystChecmicalsCalculation = Loadable(lazy(() => import('components/kendo-data-tables/CatalystChecmicalsCalculation')))
const CausticSodaLyeBasisCatChem = Loadable(lazy(() => import('components/data-tables/Reports/CausticSodaLyeBasisCatChem')))
const MaterialGroupedSelection = Loadable(lazy(() => import('components/kendo-data-tables/MaterialGroupedSelection')))

// Vertical MEROX
const SteadyStateConsumptionMerox = Loadable(lazy(() => import('components/aop-phase-two/merox/steady-state-consumption')))
const OverallAopConsumptionMerox = Loadable(lazy(() => import('components/aop-phase-two/merox/overall-aop-consumption')))
const ProductionNormsBasisMerox = Loadable(lazy(() => import('components/aop-phase-two/merox/production-norms-basis')))
const EtheleneStock = Loadable(lazy(() => import('components/data-tables/Reports/EtheleneStock')))

// Vertical ALKYLATION
const SteadyStateConsumptionAlkylation = Loadable(lazy(() => import('components/aop-phase-two/alkylation/steady-state-consumption')))
const OverallAopConsumptionAlkylation = Loadable(lazy(() => import('components/aop-phase-two/alkylation/overall-aop-consumption')))
const ProductionNormsBasisAlkylation = Loadable(lazy(() => import('components/aop-phase-two/alkylation/production-norms-basis')))

// PCG
const SteadyStateConsumptionPCG = Loadable(lazy(() => import('components/aop-phase-two/pcg/steady-state-consumption')))
const OverallAopConsumptionPCG = Loadable(lazy(() => import('components/aop-phase-two/pcg/overall-aop-consumption')))
const ProductionNormsBasisPCG = Loadable(lazy(() => import('components/aop-phase-two/pcg/production-norms-basis')))
const NetProductionHoursPCG = Loadable(lazy(() => import('components/aop-phase-two/pcg/net-production-hours/index')))
const MonthwiseProductionPlanPCG = Loadable(lazy(() => import('components/aop-phase-two/pcg/monthwise-production-plan/index')))
const ShutdownActivitiesPCG = Loadable(lazy(() => import('components/aop-phase-two/pcg/shutdown-activities/index')))
const ShutdownConsumptionPCG = Loadable(lazy(() => import('components/aop-phase-two/pcg/shutdown-consumption/index')))

// Vertical Refinery Utility
const SteadyStateConsumptionRefUtil = Loadable(lazy(() => import('components/aop-phase-two/refineryUtility/steady-state-consumption')))
const ShutdownConsumptionRefinery = Loadable(lazy(() => import('components/aop-phase-two/refineryUtility/shutdown-consumption/index')))
const OverallAopConsumptionRefUtil = Loadable(lazy(() => import('components/aop-phase-two/refineryUtility/overall-aop-consumption')))
const ProductionNormsBasisRefUtil = Loadable(lazy(() => import('components/aop-phase-two/refineryUtility/production-norms-basis')))
const PlantAOPReport = Loadable(lazy(() => import('components/kendo-data-tables/PlantAOPReport')))
const ShutdownPlanRefinery = Loadable(lazy(() => import('components/aop-phase-two/refineryUtility/shutdown-plan/index')))

// Naphthasplitter & Refinery AOP Budget
const SteadyStateConsumptionNS = Loadable(lazy(() => import('components/aop-phase-two/naphthasplitter/steady-state-consumption')))
const OverallAopConsumptionNS = Loadable(lazy(() => import('components/aop-phase-two/naphthasplitter/overall-aop-consumption')))
const ProductionNormsBasisNS = Loadable(lazy(() => import('components/aop-phase-two/naphthasplitter/production-norms-basis')))
const GradeMixOptimizer = Loadable(lazy(() => import('components/kendo-data-tables/GradeMixOptimizer')))
const VcmAvailability = Loadable(lazy(() => import('components/kendo-data-tables/VcmAvailability')))
const PlantCapacities = Loadable(lazy(() => import('components/aop-phase-two/refineryAopBudget/PlantCapacities')))
const ShutdownSchedule = Loadable(lazy(() => import('components/aop-phase-two/refineryAopBudget/shutdown')))
const SlowdownSchedule = Loadable(lazy(() => import('components/aop-phase-two/refineryAopBudget/slowdown')))
const ProductionScheduling = Loadable(lazy(() => import('components/kendo-data-tables/ProductionScheduling')))
const OtherDocumentUpload = Loadable(lazy(() => import('components/aop-phase-two/refineryAopBudget/OtherDocumentUpload/index')))
const JwBudgetScreen = Loadable(lazy(() => import('components/aop-phase-two/refineryAopBudget/JwBudget/index')))
const JwBudgetScreenDTA = Loadable(lazy(() => import('components/aop-phase-two/refineryAopBudget/JwBudgetDTA/index')))
const ThroughputNormsScreen = Loadable(lazy(() => import('components/aop-phase-two/refineryAopBudget/ThroughputNorms.js/index')))
const JwUnitScreen = Loadable(lazy(() => import('components/aop-phase-two/refineryAopBudget/JwUnit/index')))
const FixedBedAndLabCostScreen = Loadable(lazy(() => import('components/aop-phase-two/refineryAopBudget/FixedBedAndLabCost/index')))

export const MainRoutes = (
  keycloak,
  authenticated,
  // recordsTypes,
  casesDefinitions,
) => {
  let routes = {
    path: '/',
    element: <MainLayout keycloak={keycloak} authenticated={authenticated} />,
    children: [
      {
        path: '/',
        element: <Navigate to='/dashboard' />,
      },

      {
        path: 'home',
        element: (
          <PrivateRoute routeId='home'>
            <DashboardDefault />
          </PrivateRoute>
        ),
      },

      {
        path: 'case-list',
        children: [
          {
            path: 'cases',
            element: (
              <PrivateRoute routeId='case-list'>
                <CaseList />
              </PrivateRoute>
            ),
          },
          {
            path: 'wip-cases',
            element: (
              <PrivateRoute routeId='case-list'>
                <CaseList status={CaseStatus.WipCaseStatus.description} />
              </PrivateRoute>
            ),
          },
          {
            path: 'closed-cases',
            element: (
              <PrivateRoute routeId='case-list'>
                <CaseList status={CaseStatus.ClosedCaseStatus.description} />
              </PrivateRoute>
            ),
          },
          {
            path: 'archived-cases',
            element: (
              <PrivateRoute routeId='case-list'>
                <CaseList status={CaseStatus.ArchivedCaseStatus.description} />
              </PrivateRoute>
            ),
          },
        ],
      },
      {
        path: 'task-list',
        element: (
          <PrivateRoute routeId='task-list'>
            <TaskList />
          </PrivateRoute>
        ),
      },
      {
        path: 'system',
        children: [
          {
            path: 'look-and-feel',
            element: <ManagamentDefault />,
          },
        ],
      },

      {
        path: 'case-life-cycle',
        children: [
          {
            path: 'process-definition',
            element: (
              <PrivateRoute routeId='process-definition'>
                <ProcessDefList />
              </PrivateRoute>
            ),
          },
          {
            path: 'case-definition',
            element: (
              <PrivateRoute routeId='case-definition'>
                <CaseDefList />
              </PrivateRoute>
            ),
          },
          {
            path: 'record-type',
            element: (
              <PrivateRoute routeId='record-type'>
                <RecordTypeList />
              </PrivateRoute>
            ),
          },
          {
            path: 'form',
            element: (
              <PrivateRoute routeId='form'>
                <FormList />
              </PrivateRoute>
            ),
          },
          {
            path: 'queue',
            element: (
              <PrivateRoute routeId='queue'>
                <QueueList />
              </PrivateRoute>
            ),
          },
        ],
      },

      {
        path: 'tcs',
        children: [
          //TCS Started
          {
            path: 'tcs-input',
            element: (
              <PrivateRoute routeId='tcs-input'>
                <TcsInput />
              </PrivateRoute>
            ),
          },
          {
            path: 'tcs-output',
            element: (
              <PrivateRoute routeId='tcs-output'>
                <TcsOutput />
              </PrivateRoute>
            ),
          },
          {
            path: 'workflow-design',
            element: (
              <PrivateRoute routeId='workflow-design'>
                <WorkflowDiagram />
              </PrivateRoute>
            ),
          },
          {
            path: 'pims-output',
            element: (
              <PrivateRoute routeId='pims-output'>
                <PimsOutput />
              </PrivateRoute>
            ),
          },
        ],
        //TCS Ended],
      },

      {
        path: 'refinery-aop-budget',
        children: [
          //TCS Started
          {
            path: 'plant-capacities',
            element: (
              <PrivateRoute routeId='plant-capacities'>
                <PlantCapacities />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown',
            element: (
              <PrivateRoute routeId='shutdown'>
                <ShutdownSchedule />
              </PrivateRoute>
            ),
          },
          {
            path: 'slowdown',
            element: (
              <PrivateRoute routeId='slowdown'>
                <SlowdownSchedule />
              </PrivateRoute>
            ),
          },
          {
            path: 'other-document-upload',
            element: (
              <PrivateRoute routeId='other-document-upload'>
                <OtherDocumentUpload />
              </PrivateRoute>
            ),
          },
        ],
        // REFINERY AOP BUDGET Ended],
      },
      {
        path: 'jw-budget',
        children: [
          //TCS Started
          {
            path: 'jw-budget-source',
            element: (
              <PrivateRoute routeId='jw-budget-source'>
                <JwBudgetScreen />
              </PrivateRoute>
            ),
          },
          {
            path: 'jw-budget-source-dta',
            element: (
              <PrivateRoute routeId='jw-budget-source-dta'>
                <JwBudgetScreenDTA />
              </PrivateRoute>
            ),
          },
          {
            path: 'throughput-norms',
            element: (
              <PrivateRoute routeId='throughput-norms'>
                <ThroughputNormsScreen />
              </PrivateRoute>
            ),
          },
          {
            path: 'jw-unit',
            element: (
              <PrivateRoute routeId='jw-unit'>
                <JwUnitScreen />
              </PrivateRoute>
            ),
          },
          {
            path: 'fixed-bed-and-lab-cost',
            element: (
              <PrivateRoute routeId='fixed-bed-and-lab-cost'>
                <FixedBedAndLabCostScreen />
              </PrivateRoute>
            ),
          },
        ],
        // REFINERY AOP BUDGET Ended],
      },

      {
        path: 'production-norms-plan',
        children: [
          {
            path: 'business-demand',
            element: (
              <PrivateRoute routeId='business-demand'>
                <BusinessDemand />
              </PrivateRoute>
            ),
          },
          {
            path: 'production-optimizer',
            element: (
              <PrivateRoute routeId='production-optimizer'>
                <ProductionOptimizer />
              </PrivateRoute>
            ),
          },
          {
            path: 'production-scheduling',
            element: (
              <PrivateRoute routeId='production-scheduling'>
                <ProductionScheduling />
              </PrivateRoute>
            ),
          },
          {
            path: 'aop-design-basis',
            element: (
              <PrivateRoute routeId='aop-design-basis'>
                <AopDesignBasis />
              </PrivateRoute>
            ),
          },
          {
            path: 'material-balance',
            element: (
              <PrivateRoute routeId='material-balance'>
                <MaterialBalance />
              </PrivateRoute>
            ),
          },

          {
            path: 'configuration',
            element: (
              <PrivateRoute routeId='configuration'>
                <ConfigurationTable />
                {/* <Configuration /> */}
              </PrivateRoute>
            ),
            // element: <SelectivityData />,
          },
          {
            path: 'configuration-other-cost',
            element: (
              <PrivateRoute routeId='configuration-other-cost'>
                <ConfigurationOtherCost />
              </PrivateRoute>
            ),
          },
          {
            path: 'spyro-menu',
            children: [
              {
                path: 'spyro-input',
                element: (
                  <PrivateRoute routeId='spyro-input'>
                    <CrackerConfig keycloak={keycloak} />
                  </PrivateRoute>
                ),
              },
              {
                path: 'spyro-output',
                element: (
                  <PrivateRoute routeId='spyro-output'>
                    <CrackerConfigOutput />
                  </PrivateRoute>
                ),
              },
              {
                path: 'decoking-activities',
                element: (
                  <PrivateRoute routeId='decoking-activities'>
                    <DecokingConfig />
                  </PrivateRoute>
                ),
              },
            ],
          },
          {
            path: 'cat-chem-calculation',
            element: (
              <PrivateRoute routeId='cat-chem-calculation'>
                <CatalystChecmicalsCalculation />
              </PrivateRoute>
            ),
          },
          {
            path: 'production-volume-data',
            element: (
              <PrivateRoute routeId='production-volume-data'>
                <ProductionvolumeData />
              </PrivateRoute>
            ),
          },
          {
            path: 'maintenance-details',
            element: (
              <PrivateRoute routeId='maintenance-details'>
                <MaintenanceTable />
              </PrivateRoute>
            ),
          },
          {
            path: 'combined-production-norms',
            element: (
              <PrivateRoute routeId='combined-production-norms'>
                <OtherProduction />
              </PrivateRoute>
            ),
          },
          {
            path: 'specific-consumption-c3',
            element: (
              <PrivateRoute routeId='specific-consumption-c3'>
                <SpecificConsumptionCalculation />
              </PrivateRoute>
            ),
          },
          {
            path: 'consumption-aop',
            element: (
              <PrivateRoute routeId='consumption-aop'>
                <ConsumptionNorms />
              </PrivateRoute>
            ),
          },

          {
            path: 'proposed-aop-consumption',
            element: (
              <PrivateRoute routeId='proposed-aop-consumption'>
                <ProposedConsumptionNorms />
              </PrivateRoute>
            ),
          },

          {
            path: 'proposed-aop',
            element: (
              <PrivateRoute routeId='proposed-aop'>
                <ProposedAOP />
              </PrivateRoute>
            ),
          },

          {
            path: 'packaging-consumables',
            element: (
              <PrivateRoute routeId='packaging-consumables'>
                <PackagingConsumables />
              </PrivateRoute>
            ),
          },
          {
            path: 'production-aop',
            element: (
              <PrivateRoute routeId='production-aop'>
                <ProductionNorms />
              </PrivateRoute>
            ),
          },
          {
            path: 'normal-op-norms',
            element: (
              <PrivateRoute routeId='normal-op-norms'>
                <NormalOpNormsScreen />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown-norms',
            element: (
              <PrivateRoute routeId='shutdown-norms'>
                <ShutdownNorms />
              </PrivateRoute>
            ),
          },
          {
            path: 'slowdown-norms',
            element: (
              <PrivateRoute routeId='slowdown-norms'>
                <SlowdownNorms />
              </PrivateRoute>
            ),
          },
          // {
          //   path: 'slowdown-norms',
          //   element: <SlowdownNorms />,
          // },

          {
            path: 'shutdown-plan',
            element: (
              <PrivateRoute routeId='shutdown-plan'>
                <ShutDown />
              </PrivateRoute>
            ),
          },
          {
            path: 'slowdown-plan',
            element: (
              <PrivateRoute routeId='slowdown-plan'>
                <SlowDown />
              </PrivateRoute>
            ),
          },
          {
            path: 'turnaround-plan',
            element: (
              <PrivateRoute routeId='turnaround-plan'>
                <TurnaroundPlanTable />
              </PrivateRoute>
            ),
          },
          {
            path: 'feed-stock-availability',
            element: (
              <PrivateRoute routeId='feed-stock-availability'>
                <FeedStockAvailability />
              </PrivateRoute>
            ),
          },

          //Vertical VGOHT Started
          {
            path: 'production-norms-basis',
            element: (
              <PrivateRoute routeId='production-norms-basis'>
                <ProductionNormsBasis />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown-activities',
            element: (
              <PrivateRoute routeId='shutdown-activities'>
                <ShutdownActivities />
              </PrivateRoute>
            ),
          },
          {
            path: 'slowdown-activities',
            element: (
              <PrivateRoute routeId='slowdown-activities'>
                <SlowdownActivities />
              </PrivateRoute>
            ),
          },
          {
            path: 'production-target',
            element: (
              <PrivateRoute routeId='production-target'>
                <ProductionTarget />
              </PrivateRoute>
            ),
          },
          {
            path: 'net-production-hrs',
            element: (
              <PrivateRoute routeId='net-production-hrs'>
                <NetProductionHours />
              </PrivateRoute>
            ),
          },
          {
            path: 'monthwise-production-plan',
            element: (
              <PrivateRoute routeId='monthwise-production-plan'>
                <MonthwiseProductionPlan />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-consumption',
            element: (
              <PrivateRoute routeId='steady-state-consumption'>
                <SteadyStateConsumption />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown-consumption',
            element: (
              <PrivateRoute routeId='shutdown-consumption'>
                <ShutdownConsumption />
              </PrivateRoute>
            ),
          },
          {
            path: 'slowdown-consumption',
            element: (
              <PrivateRoute routeId='slowdown-consumption'>
                <SlowdownConsumption />
              </PrivateRoute>
            ),
          },
          {
            path: 'overall-aop-consumption',
            element: (
              <PrivateRoute routeId='overall-aop-consumption'>
                <OverallAopConsumption />
              </PrivateRoute>
            ),
          },
          {
            path: 'grade-mix-optimizer',
            element: (
              <PrivateRoute routeId='grade-mix-optimizer'>
                <GradeMixOptimizer />
              </PrivateRoute>
            ),
          },
          {
            path: 'vcm-availability',
            element: (
              <PrivateRoute routeId='vcm-availability'>
                <VcmAvailability />
              </PrivateRoute>
            ),
          },
          {
            path: 'material-grouped-selection',
            element: (
              <PrivateRoute routeId='material-grouped-selection'>
                <MaterialGroupedSelection />
              </PrivateRoute>
            ),
          },
          //Vertical VGOHT Ended
          {
            path: 'quality-packaging-norms',
            element: (
              <PrivateRoute routeId='quality-packaging-norms'>
                <QualityPackagingNorms />
              </PrivateRoute>
            ),
          },

          //Vertical CRUDE Started
          {
            path: 'production-norms-basis-crude',
            element: (
              <PrivateRoute routeId='production-norms-basis-crude'>
                <ProductionNormsBasisCrude />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown-activities-crude',
            element: (
              <PrivateRoute routeId='shutdown-activities-crude'>
                <ShutdownActivitiesCrude />
              </PrivateRoute>
            ),
          },
          {
            path: 'slowdown-activities-crude',
            element: (
              <PrivateRoute routeId='slowdown-activities-crude'>
                <SlowdownActivitiesCrude />
              </PrivateRoute>
            ),
          },
          {
            path: 'production-target-crude',
            element: (
              <PrivateRoute routeId='production-target-crude'>
                <ProductionTargetCrude />
              </PrivateRoute>
            ),
          },
          {
            path: 'net-production-hrs-crude',
            element: (
              <PrivateRoute routeId='net-production-hrs-crude'>
                <NetProductionHoursCrude />
              </PrivateRoute>
            ),
          },
          {
            path: 'monthwise-production-plan-crude',
            element: (
              <PrivateRoute routeId='monthwise-production-plan-crude'>
                <MonthwiseProductionPlanCrude />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-consumption-crude',
            element: (
              <PrivateRoute routeId='steady-state-consumption-crude'>
                <SteadyStateConsumptionCrude />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown-consumption-crude',
            element: (
              <PrivateRoute routeId='shutdown-consumption-crude'>
                <ShutdownConsumptionCrude />
              </PrivateRoute>
            ),
          },
          {
            path: 'slowdown-consumption-crude',
            element: (
              <PrivateRoute routeId='slowdown-consumption-crude'>
                <SlowdownConsumptionCrude />
              </PrivateRoute>
            ),
          },
          {
            path: 'overall-aop-consumption-crude',
            element: (
              <PrivateRoute routeId='overall-aop-consumption-crude'>
                <OverallAopConsumptionCrude />
              </PrivateRoute>
            ),
          },
          //Vertical CRUDE Ended

          //Vertical FCC Start******************************************************************
          {
            path: 'production-norms-basis-fcc',
            element: (
              <PrivateRoute routeId='production-norms-basis-fcc'>
                <ProductionNormsBasisFCC />
              </PrivateRoute>
            ),
          },
          {
            path: 'net-production-hrs-fcc',
            element: (
              <PrivateRoute routeId='net-production-hrs-fcc'>
                <NetProductionHoursFCC />
              </PrivateRoute>
            ),
          },
          {
            path: 'monthwise-production-plan-fcc',
            element: (
              <PrivateRoute routeId='monthwise-production-plan-fcc'>
                <MonthwiseProductionPlanFCC />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-consumption-fcc',
            element: (
              <PrivateRoute routeId='steady-state-consumption-fcc'>
                <SteadyStateConsumptionFCC />
              </PrivateRoute>
            ),
          },

          {
            path: 'overall-aop-consumption-fcc',
            element: (
              <PrivateRoute routeId='overall-aop-consumption-fcc'>
                <OverallAopConsumptionFCC />
              </PrivateRoute>
            ),
          },
          //Vertical FCC Ended ****************************

          //Vertical Coker Start******************************************************************
          {
            path: 'production-norms-basis-coker',
            element: (
              <PrivateRoute routeId='production-norms-basis-coker'>
                <ProductionNormsBasisCoker />
              </PrivateRoute>
            ),
          },
          {
            path: 'net-production-hrs-coker',
            element: (
              <PrivateRoute routeId='net-production-hrs-coker'>
                <NetProductionHoursCoker />
              </PrivateRoute>
            ),
          },
          {
            path: 'monthwise-production-plan-coker',
            element: (
              <PrivateRoute routeId='monthwise-production-plan-coker'>
                <MonthwiseProductionPlanCoker />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-consumption-coker',
            element: (
              <PrivateRoute routeId='steady-state-consumption-coker'>
                <SteadyStateConsumptionCoker />
              </PrivateRoute>
            ),
          },

          {
            path: 'overall-aop-consumption-coker',
            element: (
              <PrivateRoute routeId='overall-aop-consumption-coker'>
                <OverallAopConsumptionCoker />
              </PrivateRoute>
            ),
          },
          //Vertical Coker Ended ****************************

          //Vertical STAPLE (Polyester) Start******************************************************************
          {
            path: 'production-norms-basis-polyester',
            element: (
              <PrivateRoute routeId='production-norms-basis-polyester'>
                <ProductionNormsBasisPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'net-production-hrs-polyester',
            element: (
              <PrivateRoute routeId='net-production-hrs-polyester'>
                <NetProductionHoursPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'monthwise-production-plan-polyester',
            element: (
              <PrivateRoute routeId='monthwise-production-plan-polyester'>
                <MonthwiseProductionPlanPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-consumption-polyester',
            element: (
              <PrivateRoute routeId='steady-state-consumption-polyester'>
                <SteadyStateConsumptionPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'grade-wise-steady-state-consumption-polyester',
            element: (
              <PrivateRoute routeId='grade-wise-steady-state-consumption-polyester'>
                <GradeWiseSteadyStateConsumptionPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'overall-aop-consumption-polyester',
            element: (
              <PrivateRoute routeId='overall-aop-consumption-polyester'>
                <OverallAopConsumptionPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'production-target-polyester',
            element: (
              <PrivateRoute routeId='production-target-polyester'>
                <ProductionTargetPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'business-demand-polyester',
            element: (
              <PrivateRoute routeId='business-demand-polyester'>
                <BusinessDemandPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'proposed-aop-consumption-polyester',
            element: (
              <PrivateRoute routeId='proposed-aop-consumption-polyester'>
                <ProposedAopConsumptionPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown-plan-polyester',
            element: (
              <PrivateRoute routeId='shutdown-plan-polyester'>
                <ShutdownPlanPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown-consumption-polyester',
            element: (
              <PrivateRoute routeId='shutdown-consumption-polyester'>
                <ShutdownConsumptionPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'slowdown-plan-polyester',
            element: (
              <PrivateRoute routeId='slowdown-plan-polyester'>
                <SlowdownPlanPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'slowdown-consumption-polyester',
            element: (
              <PrivateRoute routeId='slowdown-consumption-polyester'>
                <SlowdownConsumptionPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'quality-packaging-norms-polyester',
            element: (
              <PrivateRoute routeId='quality-packaging-norms-polyester'>
                <QualityPackagingNormsPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'material-grouped-selection-polyester',
            element: (
              <PrivateRoute routeId='material-grouped-selection-polyester'>
                <MaterialGroupedSelectionPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'configuration-other-cost-polyester',
            element: (
              <PrivateRoute routeId='configuration-other-cost-polyester'>
                <ConfigurationOtherCostPolyester />
              </PrivateRoute>
            ),
          },
          {
            path: 'product-grade-selection-polyester',
            element: (
              <PrivateRoute routeId='product-grade-selection-polyester'>
                <ProductGradeSelection />
              </PrivateRoute>
            ),
          },
          {
            path: 'proposed-steady-state-consumption-polyester',
            element: (
              <PrivateRoute routeId='proposed-steady-state-consumption-polyester'>
                <ProposedSteadyStateConsumption />
              </PrivateRoute>
            ),
          },

          //Vertical STAPLE (Polyester) Ended ****************************

          //Vertical MEROX Started
          {
            path: 'production-norms-basis-merox',
            element: (
              <PrivateRoute routeId='production-norms-basis-merox'>
                <ProductionNormsBasisMerox />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-consumption-merox',
            element: (
              <PrivateRoute routeId='steady-state-consumption-merox'>
                <SteadyStateConsumptionMerox />
              </PrivateRoute>
            ),
          },
          {
            path: 'overall-aop-consumption-merox',
            element: (
              <PrivateRoute routeId='overall-aop-consumption-merox'>
                <OverallAopConsumptionMerox />
              </PrivateRoute>
            ),
          },
          //Vertical MEROX Ended

          //Vertical ALKYLATION Started
          {
            path: 'production-norms-basis-alkylation',
            element: (
              <PrivateRoute routeId='production-norms-basis-alkylation'>
                <ProductionNormsBasisAlkylation />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-consumption-alkylation',
            element: (
              <PrivateRoute routeId='steady-state-consumption-alkylation'>
                <SteadyStateConsumptionAlkylation />
              </PrivateRoute>
            ),
          },
          {
            path: 'overall-aop-consumption-alkylation',
            element: (
              <PrivateRoute routeId='overall-aop-consumption-alkylation'>
                <OverallAopConsumptionAlkylation />
              </PrivateRoute>
            ),
          },
          //Vertical ALKYLATION Ended

          //Vertical PCG Started
          {
            path: 'production-norms-basis-pcg',
            element: (
              <PrivateRoute routeId='production-norms-basis-pcg'>
                <ProductionNormsBasisPCG />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-consumption-pcg',
            element: (
              <PrivateRoute routeId='steady-state-consumption-pcg'>
                <SteadyStateConsumptionPCG />
              </PrivateRoute>
            ),
          },
          {
            path: 'overall-aop-consumption-pcg',
            element: (
              <PrivateRoute routeId='overall-aop-consumption-pcg'>
                <OverallAopConsumptionPCG />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown-activities-pcg',
            element: (
              <PrivateRoute routeId='shutdown-activities-pcg'>
                <ShutdownActivitiesPCG />
              </PrivateRoute>
            ),
          },
          {
            path: 'net-production-hrs-pcg',
            element: (
              <PrivateRoute routeId='net-production-hrs-pcg'>
                <NetProductionHoursPCG />
              </PrivateRoute>
            ),
          },
          {
            path: 'monthwise-production-plan-pcg',
            element: (
              <PrivateRoute routeId='monthwise-production-plan-pcg'>
                <MonthwiseProductionPlanPCG />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown-consumption-pcg',
            element: (
              <PrivateRoute routeId='shutdown-consumption-pcg'>
                <ShutdownConsumptionPCG />
              </PrivateRoute>
            ),
          },

          //Vertical PCG Ended
          //Vertical Refinery utility Started
          {
            path: 'production-norms-basis-refinery',
            element: (
              <PrivateRoute routeId='production-norms-basis-refinery'>
                <ProductionNormsBasisRefUtil />
              </PrivateRoute>
            ),
          },
          {

            path: 'shutdown-plan-refinery',
            element: (
              <PrivateRoute routeId='shutdown-plan-refinery'>
                <ShutdownPlanRefinery />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-consumption-refinery',
            element: (
              <PrivateRoute routeId='steady-state-consumption-refinery'>
                <SteadyStateConsumptionRefUtil />
              </PrivateRoute>
            ),
          },
          {
            path: 'shutdown-consumption-refinery',
            element: (
              <PrivateRoute routeId='shutdown-consumption-refinery'>
                <ShutdownConsumptionRefinery />
              </PrivateRoute>
            ),
          },
          {
            path: 'overall-aop-consumption-refinery',
            element: (
              <PrivateRoute routeId='overall-aop-consumption-refinery'>
                <OverallAopConsumptionRefUtil />
              </PrivateRoute>
            ),
          },
          //Vertical Refinery utility Ended
          //Vertical Naphthasplitter Started
          {
            path: 'production-norms-basis-naphthasplitter',
            element: (
              <PrivateRoute routeId='production-norms-basis-naphthasplitter'>
                <ProductionNormsBasisNS />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-consumption-naphthasplitter',
            element: (
              <PrivateRoute routeId='steady-state-consumption-naphthasplitter'>
                <SteadyStateConsumptionNS />
              </PrivateRoute>
            ),
          },
          {
            path: 'overall-aop-consumption-naphthasplitter',
            element: (
              <PrivateRoute routeId='overall-aop-consumption-naphthasplitter'>
                <OverallAopConsumptionNS />
              </PrivateRoute>
            ),
          },
          //Vertical Naphthasplitter Ended
        ],
      },

      {
        path: 'functions',
        children: [
          { path: 'safety', element: <DashboardDefault /> },
          { path: 'reliability', element: <TextSubmitComponent /> },
        ],
      },

      {
        path: 'reports',
        children: [
          {
            path: 'production-target-basis',
            element: (
              <PrivateRoute routeId='production-target-basis'>
                <ProductionTargetBasis />
              </PrivateRoute>
            ),
          },
          {
            path: 'ethelene-stock',
            element: (
              <PrivateRoute routeId='ethelene-stock'>
                <EtheleneStock />
              </PrivateRoute>
            ),
          },
          {
            path: 'aop-annual-cost-report',
            element: (
              <PrivateRoute routeId='aop-annual-cost-report'>
                <AnnualAopCost />
              </PrivateRoute>
            ),
          },
          {
            path: 'production-volume-basis',
            element: (
              <PrivateRoute routeId='production-volume-basis'>
                <ProductionVolumeDataBasis />
              </PrivateRoute>
            ),
          },
          {
            path: 'sap-based-ref-norms',
            element: (
              <PrivateRoute routeId='sap-based-ref-norms'>
                <SapBasedRefNorms />
              </PrivateRoute>
            ),
          },
          {
            path: 'norms-historian-basis',
            element: (
              <PrivateRoute routeId='norms-historian-basis'>
                <NormsHistorianBasis />
              </PrivateRoute>
            ),
          },
          {
            path: 'steady-state-norms-historian-basis',
            element: (
              <PrivateRoute routeId='steady-state-norms-historian-basis'>
                <SteadyStateNormsHistorianBasis />
              </PrivateRoute>
            ),
          },

          {
            path: 'consumption-norms-historian-basis',
            element: (
              <PrivateRoute routeId='consumption-norms-historian-basis'>
                <ConsumptionNormsHistorianBasis />
              </PrivateRoute>
            ),
          },

          {
            path: 'best-achieved-basis',
            element: (
              <PrivateRoute routeId='best-achieved-basis'>
                <BestAchievedNorms />
              </PrivateRoute>
            ),
          },

          {
            path: 'best-achieved-individual-basis',
            element: (
              <PrivateRoute routeId='best-achieved-individual-basis'>
                <BestAchievedIndividualNorms />
              </PrivateRoute>
            ),
          },

          {
            path: 'plants-production',
            element: (
              <PrivateRoute routeId='plants-production'>
                <PlantsProductionSummary />
              </PrivateRoute>
            ),
          },
          {
            path: 'monthwise-production',
            element: (
              <PrivateRoute routeId='monthwise-production'>
                <MonthwiseProduction />
              </PrivateRoute>
            ),
          },

          {
            path: 'intermediate-values',
            element: (
              <PrivateRoute routeId='intermediate-values'>
                <IntermediateValuesDataSet />
              </PrivateRoute>
            ),
          },

          {
            path: 'raw-data',
            element: (
              <PrivateRoute routeId='raw-data'>
                <RawDataSet />
              </PrivateRoute>
            ),
          },
          {
            path: 'utilities-norms-basis',
            element: (
              <PrivateRoute routeId='utilities-norms-basis'>
                <UtilitiesNormsBasis />
              </PrivateRoute>
            ),
          },
          {
            path: 'run-length',
            element: (
              <PrivateRoute routeId='run-length'>
                <RunLengthDataSet />
              </PrivateRoute>
            ),
          },

          {
            path: 'quality-packaging-basis',
            element: (
              <PrivateRoute routeId='quality-packaging-basis'>
                <QualityPackagingBasis />
              </PrivateRoute>
            ),
          },

          {
            path: 'caustic-soda-lye-basis',
            element: (
              <PrivateRoute routeId='caustic-soda-lye-basis'>
                <CausticSodaLyeBasis />
              </PrivateRoute>
            ),
          },

          {
            path: 'caustic-soda-lye-basis-cat-chem',
            element: (
              <PrivateRoute routeId='caustic-soda-lye-basis-cat-chem'>
                <CausticSodaLyeBasisCatChem />
              </PrivateRoute>
            ),
          },

          {
            path: 'monthwise-raw-material',
            element: <MonthwiseRawMaterial />,
          },
          { path: 'previous-fy-aop-result', element: <DashboardDefault /> },
          {
            path: 'mat-bal-sheet',
            element: (
              <PrivateRoute routeId='mat-bal-sheet'>
                <DashboardDefault />
              </PrivateRoute>
            ),
          },
        ],
      },
      {
        path: 'utilityPlant',
        children: [
          {
            path: 'norms',
            element: (
              <PrivateRoute routeId='norms'>
                <Norms />
              </PrivateRoute>
            ),
          },
          {
            path: 'qty-cost-report',
            element: (
              <PrivateRoute routeId='qty-cost-report'>
                <QtyCostReport />
              </PrivateRoute>
            ),
          },
          {
            path: 'utility-rate',
            element: (
              <PrivateRoute routeId='utility-rate'>
                <UtilityRate />
              </PrivateRoute>
            ),
          },
          {
            path: 'plant-requirement',
            element: (
              <PrivateRoute routeId='plant-requirement'>
                <PlantRequirement />
              </PrivateRoute>
            ),
          },
          {
            path: 'fixed-consumption',
            element: (
              <PrivateRoute routeId='fixed-consumption'>
                <FixedConsumption />
              </PrivateRoute>
            ),
          },
          {
            path: 'inputs',
            element: (
              <PrivateRoute routeId='inputs'>
                <Inputs />
              </PrivateRoute>
            ),
          },
          {
            path: 'summary',
            element: (
              <PrivateRoute routeId='summary'>
                <Summary />
              </PrivateRoute>
            ),
          },
          {
            path: 'outputs',
            element: (
              <PrivateRoute routeId='outputs'>
                <Outputs />
              </PrivateRoute>
            ),
          },
          // ...other utilityPlant routes...
        ],
      },

      {
        path: 'functional-aop',
        children: [
          {
            path: 'aop-budget',
            element: (
              <PrivateRoute routeId='aop-budget'>
                <AopBudget />
              </PrivateRoute>
            ),
          },

          {
            path: 'reliability-performance',
            element: (
              <PrivateRoute routeId='reliability-performance'>
                <RelPerf />
              </PrivateRoute>
            ),
          },
          {
            path: 'plant-reliability-performance',
            element: (
              <PrivateRoute routeId='plant-reliability-performance'>
                <RelPerfPlantWise />
              </PrivateRoute>
            ),
          },
        ],
      },

      {
        path: 'functional-reports',
        children: [
          {
            path: 'maintenance-summary',
            element: (
              <PrivateRoute routeId='maintenance-summary'>
                <MaintenanceSummary />
              </PrivateRoute>
            ),
          },

          {
            path: 'site-maintenance-summary',
            element: (
              <PrivateRoute routeId='site-maintenance-summary'>
                <SiteMaintenanceSummary />
              </PrivateRoute>
            ),
          },

          {
            path: 'plant-budget-summary',
            element: (
              <PrivateRoute routeId='plant-budget-summary'>
                <PlantBudgetSummary />
              </PrivateRoute>
            ),
          },
          {
            path: 'site-aop-report',
            element: (
              <PrivateRoute routeId='site-aop-report'>
                <SiteAOPReport />
              </PrivateRoute>
            ),
          },
          {
            path: 'plant-aop-report',
            element: (
              <PrivateRoute routeId='plant-aop-report'>
                <PlantAOPReport />
              </PrivateRoute>
            ),
          },

          {
            path: 'site-budget-summary',
            element: (
              <PrivateRoute routeId='site-budget-summary'>
                <SiteBudgetSummary />
              </PrivateRoute>
            ),
          },

          {
            path: 'norm-comparison-report',
            element: (
              <PrivateRoute routeId='norm-comparison-report'>
                <NormComparisonReport />
              </PrivateRoute>
            ),
          },

          {
            path: 'ethylene-balance',
            element: (
              <PrivateRoute routeId='ethylene-balance'>
                <EthyleneBalance />
              </PrivateRoute>
            ),
          },

          {
            path: 'propylene-balance',
            element: (
              <PrivateRoute routeId='propylene-balance'>
                <PropyleneBalance />
              </PrivateRoute>
            ),
          },
        ],
      },

      {
        path: 'plant-team',
        element: (
          <PrivateRoute routeId='plant-team'>
            <PlantTeam />
          </PrivateRoute>
        ),
      },

      {
        path: 'dashboard',
        element: (
          // <PrivateRoute routeId='dashboard'>
          <AopDashboard />
          // </PrivateRoute>
        ),
      },

      {
        path: 'plant-safety-performance-target',
        element: (
          <PrivateRoute routeId='plant-safety-performance-target'>
            <PlantSafetyPerformanceTarget />
          </PrivateRoute>
        ),
      },

      {
        path: 'workflow',
        element: (
          <PrivateRoute routeId='workflow'>
            <WorkFlowMerge />
          </PrivateRoute>
        ),
        // element: <FiveTables />,
      },
      {
        path: 'tab-management',
        element: (
          <PrivateRoute routeId='tab-management'>
            <TabManagement keycloak={keycloak} />
          </PrivateRoute>
        ),
      },
      {
        path: 'user-management',
        element: (
          <PrivateRoute routeId='user-management'>
            <UserManagementTable keycloak={keycloak} />
          </PrivateRoute>
        ),
      },

      {
        path: 'user-form',
        element: <UserForm keycloak={keycloak} />,
      },
      {
        path: 'assessment-form',
        element: (
          <PrivateRoute routeId='assessment-form'>
            <AssessmentForm />
          </PrivateRoute>
        ),
      },
      {
        path: '*',
        element: <Navigate to='/dashboard' replace />,
      },
    ],
  }

  casesDefinitions?.forEach((element) => {
    routes.children.push({
      path: 'case-list/' + element.id,
      element: <CaseList caseDefId={element.id} />,
    })
  })

  // recordsTypes.forEach((element) => {
  //   routes.children.push({
  //     path: 'record-list/' + element.id,
  //     element: <RecordList recordTypeId={element.id} />,
  //   })
  // })

  return routes
}
