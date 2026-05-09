import React, { useEffect, useRef, useState } from 'react'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
  CamundaPlatformPropertiesProviderModule,
} from 'bpmn-js-properties-panel'
import camundaModdleDescriptor from 'camunda-bpmn-moddle/resources/camunda.json'

import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'
import '@bpmn-io/properties-panel/dist/assets/properties-panel.css'
import 'components/aop-phase-two/css/workflow.css'

import {
  Button,
  Stack,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Drawer,
} from '@mui/material'
import { ChevronLeft, Settings } from '@mui/icons-material'

function ProcessModeler() {
  const containerRef = useRef(null)
  const propertiesPanelRef = useRef(null)
  const modelerRef = useRef(null)
  const [openDialog, setOpenDialog] = useState(false)
  const [generatedXML, setGeneratedXML] = useState('')
  const [propertiesPanelOpen, setPropertiesPanelOpen] = useState(false)

  useEffect(() => {
    const modeler = new BpmnModeler({
      container: containerRef.current,
      keyboard: {
        bindTo: window,
      },
      propertiesPanel: {
        parent: propertiesPanelRef.current,
      },
      additionalModules: [
        BpmnPropertiesPanelModule,
        BpmnPropertiesProviderModule,
        CamundaPlatformPropertiesProviderModule,
      ],
      moddleExtensions: {
        camunda: camundaModdleDescriptor,
      },
    })

    modelerRef.current = modeler

    async function createNewDiagram() {
      //     const xml = `<?xml version="1.0" encoding="UTF-8"?>
      // <bpmn:definitions
      //   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      //   xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
      //   xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
      //   xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
      //   xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
      //   xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
      //   id="Definitions_1"
      //   targetNamespace="http://example.com/eps">

      //   <bpmn:process id="TCS_APPROVAL_PROCESS" isExecutable="true">

      //     <bpmn:startEvent id="StartEvent" name="Start"/>

      //     <bpmn:userTask id="SubmitPlantData" name="Submit Plant Data">
      //       <bpmn:multiInstanceLoopCharacteristics
      //         camunda:collection="\${plantList}"
      //         camunda:elementVariable="plant"/>
      //     </bpmn:userTask>

      //     <bpmn:userTask id="EPS_Approval" name="EPS Engineer Approval"
      //       camunda:candidateGroups="EPS_ENGINEER">
      //       <bpmn:multiInstanceLoopCharacteristics
      //         camunda:collection="\${plantList}"
      //         camunda:elementVariable="plant"/>
      //     </bpmn:userTask>

      //     <bpmn:exclusiveGateway id="Gateway_AllApproved" name="All Approved?"/>

      //     <bpmn:userTask id="EPS_HEAD" name="EPS Head Approval"
      //       camunda:candidateGroups="EPS_HEAD"/>

      //     <bpmn:exclusiveGateway id="Gateway_EPS_HEAD"/>

      //     <bpmn:userTask id="CLUSTER_HEAD" name="Cluster Head Approval"
      //       camunda:candidateGroups="CLUSTER_HEAD"/>

      //     <bpmn:exclusiveGateway id="Gateway_CLUSTER"/>

      //     <bpmn:endEvent id="EndEvent" name="Completed"/>

      //     <!-- FLOWS -->
      //     <bpmn:sequenceFlow id="flow1" sourceRef="StartEvent" targetRef="SubmitPlantData"/>
      //     <bpmn:sequenceFlow id="flow2" sourceRef="SubmitPlantData" targetRef="EPS_Approval"/>
      //     <bpmn:sequenceFlow id="flow3" sourceRef="EPS_Approval" targetRef="Gateway_AllApproved"/>

      //     <bpmn:sequenceFlow id="flow4" sourceRef="Gateway_AllApproved" targetRef="EPS_HEAD">
      //       <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
      //         <![CDATA[ allApproved == true ]]>
      //       </bpmn:conditionExpression>
      //     </bpmn:sequenceFlow>

      //     <bpmn:sequenceFlow id="flow5" sourceRef="Gateway_AllApproved" targetRef="SubmitPlantData">
      //       <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
      //         <![CDATA[ allApproved == false ]]>
      //       </bpmn:conditionExpression>
      //     </bpmn:sequenceFlow>

      //     <bpmn:sequenceFlow id="flow6" sourceRef="EPS_HEAD" targetRef="Gateway_EPS_HEAD"/>

      //     <bpmn:sequenceFlow id="flow7" sourceRef="Gateway_EPS_HEAD" targetRef="CLUSTER_HEAD">
      //       <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
      //         <![CDATA[ status == 'APPROVED' ]]>
      //       </bpmn:conditionExpression>
      //     </bpmn:sequenceFlow>

      //     <bpmn:sequenceFlow id="flow8" sourceRef="Gateway_EPS_HEAD" targetRef="EPS_Approval">
      //       <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
      //         <![CDATA[ status == 'REJECTED' ]]>
      //       </bpmn:conditionExpression>
      //     </bpmn:sequenceFlow>

      //     <bpmn:sequenceFlow id="flow9" sourceRef="CLUSTER_HEAD" targetRef="Gateway_CLUSTER"/>

      //     <bpmn:sequenceFlow id="flow10" sourceRef="Gateway_CLUSTER" targetRef="EndEvent">
      //       <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
      //         <![CDATA[ status == 'APPROVED' ]]>
      //       </bpmn:conditionExpression>
      //     </bpmn:sequenceFlow>

      //     <bpmn:sequenceFlow id="flow11" sourceRef="Gateway_CLUSTER" targetRef="EPS_HEAD">
      //       <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
      //         <![CDATA[ status == 'REJECTED' ]]>
      //       </bpmn:conditionExpression>
      //     </bpmn:sequenceFlow>

      //   </bpmn:process>

      //   <!-- DIAGRAM (THIS FIXES YOUR ERROR) -->
      //   <bpmndi:BPMNDiagram id="BPMNDiagram_1">
      //     <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="TCS_APPROVAL_PROCESS">

      //       <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
      //         <dc:Bounds x="100" y="150" width="36" height="36"/>
      //       </bpmndi:BPMNShape>

      //       <bpmndi:BPMNShape id="SubmitPlantData_di" bpmnElement="SubmitPlantData">
      //         <dc:Bounds x="200" y="120" width="140" height="80"/>
      //       </bpmndi:BPMNShape>

      //       <bpmndi:BPMNShape id="EPS_Approval_di" bpmnElement="EPS_Approval">
      //         <dc:Bounds x="380" y="120" width="160" height="80"/>
      //       </bpmndi:BPMNShape>

      //       <bpmndi:BPMNShape id="Gateway_AllApproved_di" bpmnElement="Gateway_AllApproved" isMarkerVisible="true">
      //         <dc:Bounds x="580" y="140" width="50" height="50"/>
      //       </bpmndi:BPMNShape>

      //       <bpmndi:BPMNShape id="EPS_HEAD_di" bpmnElement="EPS_HEAD">
      //         <dc:Bounds x="680" y="120" width="160" height="80"/>
      //       </bpmndi:BPMNShape>

      //       <bpmndi:BPMNShape id="CLUSTER_HEAD_di" bpmnElement="CLUSTER_HEAD">
      //         <dc:Bounds x="880" y="120" width="160" height="80"/>
      //       </bpmndi:BPMNShape>

      //       <bpmndi:BPMNShape id="EndEvent_di" bpmnElement="EndEvent">
      //         <dc:Bounds x="1080" y="150" width="36" height="36"/>
      //       </bpmndi:BPMNShape>

      //       <!-- EDGES -->
      //       <bpmndi:BPMNEdge id="flow1_di" bpmnElement="flow1">
      //         <di:waypoint x="136" y="168"/>
      //         <di:waypoint x="200" y="168"/>
      //       </bpmndi:BPMNEdge>

      //       <bpmndi:BPMNEdge id="flow2_di" bpmnElement="flow2">
      //         <di:waypoint x="340" y="168"/>
      //         <di:waypoint x="380" y="168"/>
      //       </bpmndi:BPMNEdge>

      //       <bpmndi:BPMNEdge id="flow3_di" bpmnElement="flow3">
      //         <di:waypoint x="540" y="168"/>
      //         <di:waypoint x="580" y="168"/>
      //       </bpmndi:BPMNEdge>

      //     </bpmndi:BPMNPlane>
      //   </bpmndi:BPMNDiagram>

      // </bpmn:definitions>`

      const xml = `<?xml version="1.0" encoding="UTF-8"?>
          <bpmn:definitions
              xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
              xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
              xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
              xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
              xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
              id="Definitions_1"
              targetNamespace="http://example.com/eps">

          <bpmn:process id="TCS_APPROVAL_PROCESS"
                          name="EPS Multi-Form Approval"
                          isExecutable="true"
                          camunda:historyTimeToLive="180">

              <bpmn:startEvent id="StartEvent" name="Process Initiated"/>

              <bpmn:userTask id="SubmitPlantData" name="Submit Plant Data">
              <bpmn:multiInstanceLoopCharacteristics
                  camunda:collection="\${plantList}"
                  camunda:elementVariable="plant"/>
              </bpmn:userTask>

              <bpmn:userTask id="EPS_Approval"
                          name="EPS_ENGINEER Approval"
                          camunda:candidateGroups="EPS_ENGINEER"/>

              <bpmn:userTask id="CTS_APPROVAL"
                          name="CTS_HEAD / EPS_HEAD Approval"
                          camunda:candidateGroups="CTS_ENGINEER,EPS_Head_ENGINEER"/>

              <bpmn:userTask id="Cluster_Head_APPROVAL"
                          name="CLUSTER_HEAD Approval"
                          camunda:candidateGroups="Cluster_Head_ENGINEER"/>

              <bpmn:userTask id="Pending_state"
                          name="Pending"
                          camunda:candidateGroups="Head_ENGINEER"/>

              <bpmn:endEvent id="EndEvent" name="Process Completed"/>

              <!-- Sequence Flows -->
              <bpmn:sequenceFlow id="flow1" sourceRef="StartEvent" targetRef="SubmitPlantData"/>
              <bpmn:sequenceFlow id="flow2" sourceRef="SubmitPlantData" targetRef="EPS_Approval"/>
              <bpmn:sequenceFlow id="flow3" sourceRef="EPS_Approval" targetRef="CTS_APPROVAL"/>
              <bpmn:sequenceFlow id="flow4" sourceRef="CTS_APPROVAL" targetRef="Cluster_Head_APPROVAL"/>
              <bpmn:sequenceFlow id="flow5" sourceRef="Cluster_Head_APPROVAL" targetRef="Pending_state"/>

              <bpmn:sequenceFlow id="flow6" sourceRef="Pending_state" targetRef="EndEvent"/>

          </bpmn:process>

          <!-- DIAGRAM SECTION -->
          <bpmndi:BPMNDiagram id="BPMNDiagram_1">
              <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="TCS_APPROVAL_PROCESS">

              <!-- Shapes -->
              <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
                  <dc:Bounds x="100" y="120" width="36" height="36"/>
              </bpmndi:BPMNShape>

              <bpmndi:BPMNShape id="SubmitPlantData_di" bpmnElement="SubmitPlantData">
                  <dc:Bounds x="200" y="100" width="140" height="80"/>
              </bpmndi:BPMNShape>

              <bpmndi:BPMNShape id="EPS_Approval_di" bpmnElement="EPS_Approval">
                  <dc:Bounds x="380" y="100" width="140" height="80"/>
              </bpmndi:BPMNShape>

              <bpmndi:BPMNShape id="CTS_APPROVAL_di" bpmnElement="CTS_APPROVAL">
                  <dc:Bounds x="560" y="100" width="140" height="80"/>
              </bpmndi:BPMNShape>

              <bpmndi:BPMNShape id="Cluster_Head_APPROVAL_di" bpmnElement="Cluster_Head_APPROVAL">
                  <dc:Bounds x="740" y="100" width="160" height="80"/>
              </bpmndi:BPMNShape>

              <bpmndi:BPMNShape id="EndEvent_di" bpmnElement="EndEvent">
                  <dc:Bounds x="940" y="120" width="36" height="36"/>
              </bpmndi:BPMNShape>

              <!-- Edges -->
              <bpmndi:BPMNEdge id="flow1_di" bpmnElement="flow1">
                  <di:waypoint x="136" y="138"/>
                  <di:waypoint x="200" y="138"/>
              </bpmndi:BPMNEdge>

              <bpmndi:BPMNEdge id="flow2_di" bpmnElement="flow2">
                  <di:waypoint x="340" y="138"/>
                  <di:waypoint x="380" y="138"/>
              </bpmndi:BPMNEdge>

              <bpmndi:BPMNEdge id="flow3_di" bpmnElement="flow3">
                  <di:waypoint x="520" y="138"/>
                  <di:waypoint x="560" y="138"/>
              </bpmndi:BPMNEdge>

              <bpmndi:BPMNEdge id="flow4_di" bpmnElement="flow4">
                  <di:waypoint x="700" y="138"/>
                  <di:waypoint x="740" y="138"/>
              </bpmndi:BPMNEdge>

              <bpmndi:BPMNEdge id="flow5_di" bpmnElement="flow5">
                  <di:waypoint x="900" y="138"/>
                  <di:waypoint x="940" y="138"/>
              </bpmndi:BPMNEdge>

              <!-- REQUIRED FOR FRONTEND VIEWER -->
              <!-- Uncomment if viewer throws "no diagram to display" -->
              <!--
              <bpmndi:BPMNEdge id="flow6_di" bpmnElement="flow6">
                  <di:waypoint x="900" y="138"/>
                  <di:waypoint x="940" y="138"/>
              </bpmndi:BPMNEdge>
              -->

              </bpmndi:BPMNPlane>
          </bpmndi:BPMNDiagram>

          </bpmn:definitions>`

      try {
        await modeler.importXML(xml)
        const canvas = modeler.get('canvas')
        canvas.zoom('fit-viewport')
      } catch (err) {
        console.error('Import error:', err)
      }
    }

    createNewDiagram()

    return () => modeler.destroy()
  }, [])

  const handleSave = async () => {
    try {
      const { xml } = await modelerRef.current.saveXML({ format: true })
      console.log('Saved BPMN XML:', xml)
      // TODO: Send to backend API
    } catch (err) {
      console.error('Error saving diagram:', err)
    }
  }

  const handleShowXML = async () => {
    try {
      const { xml } = await modelerRef.current.saveXML({ format: true })
      setGeneratedXML(xml)
      setOpenDialog(true)
    } catch (err) {
      console.error('Error generating XML:', err)
    }
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
  }

  const handleCopyXML = () => {
    navigator.clipboard.writeText(generatedXML)
  }

  return (
    <div style={{ height: '600px', display: 'flex', flexDirection: 'column' }}>
      <Stack
        direction='row'
        spacing={2}
        sx={{
          padding: '10px',
          borderBottom: '1px solid #ccc',
          backgroundColor: '#f5f5f5',
          justifyContent: 'flex-end',
        }}
      >
        {/* <Button variant='contained' color='secondary' onClick={handleShowXML}>
          Show XML
        </Button> */}
        <IconButton
          onClick={() => setPropertiesPanelOpen(!propertiesPanelOpen)}
          color='primary'
          title='Toggle Properties Panel'
        >
          <Settings />
        </IconButton>
      </Stack>
      <div style={{ position: 'relative', flex: 1, overflow: 'hidden' }}>
        <div
          ref={containerRef}
          style={{
            width: '100%',
            height: '100%',
            border: '1px solid #ccc',
            backgroundColor: '#fff',
          }}
        />
        <Drawer
          anchor='right'
          open={propertiesPanelOpen}
          onClose={() => setPropertiesPanelOpen(false)}
          variant='persistent'
          sx={{
            width: propertiesPanelOpen ? 350 : 0,
            flexShrink: 0,
            '& .MuiDrawer-paper': {
              width: 350,
              position: 'absolute',
              height: '100%',
              boxSizing: 'border-box',
              borderLeft: '1px solid #ccc',
              backgroundColor: '#f8f8f8',
            },
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              padding: '8px',
              borderBottom: '1px solid #ccc',
              backgroundColor: '#fff',
            }}
          >
            <IconButton
              onClick={() => setPropertiesPanelOpen(false)}
              size='small'
            >
              <ChevronLeft />
            </IconButton>
            <span style={{ marginLeft: '8px', fontWeight: 'bold' }}>
              Properties Panel
            </span>
          </div>
          <div
            ref={propertiesPanelRef}
            style={{
              flex: 1,
              overflow: 'auto',
              padding: '10px',
            }}
          />
        </Drawer>
      </div>

      <Dialog
        open={openDialog}
        onClose={handleCloseDialog}
        maxWidth='md'
        fullWidth
      >
        <DialogTitle>Generated BPMN XML</DialogTitle>
        <DialogContent>
          <TextField
            multiline
            fullWidth
            rows={20}
            value={generatedXML}
            variant='outlined'
            InputProps={{
              readOnly: true,
              sx: { fontFamily: 'monospace', fontSize: '12px' },
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCopyXML} variant='outlined'>
            Copy to Clipboard
          </Button>
          <Button onClick={handleCloseDialog} variant='contained'>
            Close
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  )
}

export default ProcessModeler
