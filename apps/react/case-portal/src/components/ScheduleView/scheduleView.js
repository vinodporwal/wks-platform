import { Fragment, useEffect, useRef, useState, useCallback } from 'react'
import {
  ButtonComponent,
  CheckBoxComponent,
} from '@syncfusion/ej2-react-buttons'
import { TimePickerComponent } from '@syncfusion/ej2-react-calendars'
import {
  DropDownListComponent,
  MultiSelectComponent,
  CheckBoxSelection,
} from '@syncfusion/ej2-react-dropdowns'
import { UploaderComponent } from '@syncfusion/ej2-react-inputs'
import {
  ToolbarComponent,
  ItemsDirective,
  ItemDirective,
  AppBarComponent,
} from '@syncfusion/ej2-react-navigations'
import {
  ScheduleComponent,
  Day,
  Week,
  WorkWeek,
  Month,
  Year,
  TimelineViews,
  TimelineMonth,
  TimelineYear,
  ViewsDirective,
  ViewDirective,
  ResourcesDirective,
  ResourceDirective,
  Inject,
  Resize,
  DragAndDrop,
  Agenda,
  Print,
  ExcelExport,
  ICalendarImport,
  ICalendarExport,
  Timezone,
} from '@syncfusion/ej2-react-schedule'
import { DropDownButtonComponent } from '@syncfusion/ej2-react-splitbuttons'
import {
  addClass,
  Browser,
  extend,
  Internationalization,
  removeClass,
  compile,
} from '@syncfusion/ej2-base'
import { Predicate, Query } from '@syncfusion/ej2-data'
import { registerLicense } from '@syncfusion/ej2-base';
import { tz } from 'moment-timezone'
import moment from 'moment'
import './scheduleView.css'

export const ScheduleView = ({
  cases,
  caseDefId,
  setACase,
  handleClose,
  setOpenCaseForm
}) => {
  registerLicense(
    'ORg4AjUWIQA/Gnt2VVhkQlFadVdJXGFWfVJpTGpQdk5xdV9DaVZUTWY/P1ZhSXxQdkdiX39adXNWRGZYVkw=',
  );
  const [currentView, setCurrentView] = useState('Week')
  const [isTimelineView, setIsTimelineView] = useState(false)
  let scheduleObj = useRef(null)
  let timeBtn = useRef(null)
  let intl = new Internationalization()
  let workWeekObj = useRef(null)
  let resourceObj = useRef(null)
  const weekDays = [
    { text: 'Sunday', value: 0 },
    { text: 'Monday', value: 1 },
    { text: 'Tuesday', value: 2 },
    { text: 'Wednesday', value: 3 },
    { text: 'Thursday', value: 4 },
    { text: 'Friday', value: 5 },
    { text: 'Saturday', value: 6 },
  ]
  const exportItems = [
    { text: 'iCalendar', iconCss: 'e-icons e-export' },
    { text: 'Excel', iconCss: 'e-icons e-export-excel' },
  ]
  const calendarCollections = [
    { CalendarText: 'My Calendar', CalendarId: 1, CalendarColor: '#c43081' }
  ]
  const timezoneData = [
    { text: 'UTC -12:00', value: 'Etc/GMT+12' },
    { text: 'UTC -11:00', value: 'Etc/GMT+11' },
    { text: 'UTC -10:00', value: 'Etc/GMT+10' },
    { text: 'UTC -09:00', value: 'Etc/GMT+9' },
    { text: 'UTC -08:00', value: 'Etc/GMT+8' },
    { text: 'UTC -07:00', value: 'Etc/GMT+7' },
    { text: 'UTC -06:00', value: 'Etc/GMT+6' },
    { text: 'UTC -05:00', value: 'Etc/GMT+5' },
    { text: 'UTC -04:00', value: 'Etc/GMT+4' },
    { text: 'UTC -03:00', value: 'Etc/GMT+3' },
    { text: 'UTC -02:00', value: 'Etc/GMT+2' },
    { text: 'UTC -01:00', value: 'Etc/GMT+1' },
    { text: 'UTC +00:00', value: 'Etc/GMT' },
    { text: 'UTC +01:00', value: 'Etc/GMT-1' },
    { text: 'UTC +02:00', value: 'Etc/GMT-2' },
    { text: 'UTC +03:00', value: 'Etc/GMT-3' },
    { text: 'UTC +04:00', value: 'Etc/GMT-4' },
    { text: 'UTC +05:00', value: 'Etc/GMT-5' },
    { text: 'UTC +05:30', value: 'Asia/Calcutta' },
    { text: 'UTC +06:00', value: 'Etc/GMT-6' },
    { text: 'UTC +07:00', value: 'Etc/GMT-7' },
    { text: 'UTC +08:00', value: 'Etc/GMT-8' },
    { text: 'UTC +09:00', value: 'Etc/GMT-9' },
    { text: 'UTC +10:00', value: 'Etc/GMT-10' },
    { text: 'UTC +11:00', value: 'Etc/GMT-11' },
    { text: 'UTC +12:00', value: 'Etc/GMT-12' },
    { text: 'UTC +13:00', value: 'Etc/GMT-13' },
    { text: 'UTC +14:00', value: 'Etc/GMT-14' },
  ]
  const majorSlotData = [
    { Name: '1 hour', Value: 60 },
    { Name: '1.5 hours', Value: 90 },
    { Name: '2 hours', Value: 120 },
    { Name: '2.5 hours', Value: 150 },
    { Name: '3 hours', Value: 180 },
    { Name: '3.5 hours', Value: 210 },
    { Name: '4 hours', Value: 240 },
    { Name: '4.5 hours', Value: 270 },
    { Name: '5 hours', Value: 300 },
    { Name: '5.5 hours', Value: 330 },
    { Name: '6 hours', Value: 360 },
    { Name: '6.5 hours', Value: 390 },
    { Name: '7 hours', Value: 420 },
    { Name: '7.5 hours', Value: 450 },
    { Name: '8 hours', Value: 480 },
    { Name: '8.5 hours', Value: 510 },
    { Name: '9 hours', Value: 540 },
    { Name: '9.5 hours', Value: 570 },
    { Name: '10 hours', Value: 600 },
    { Name: '10.5 hours', Value: 630 },
    { Name: '11 hours', Value: 660 },
    { Name: '11.5 hours', Value: 690 },
    { Name: '12 hours', Value: 720 },
  ]
  const minorSlotData = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
  const timeFormatData = [
    { Name: '12 hours', Value: 'hh:mm a' },
    { Name: '24 hours', Value: 'HH:mm' },
  ]
  const weekNumberData = [
    { Name: 'Off', Value: 'Off' },
    { Name: 'First Day of Year', Value: 'FirstDay' },
    { Name: 'First Full Week', Value: 'FirstFullWeek' },
    { Name: 'First Four-Day Week', Value: 'FirstFourDayWeek' },
  ]
  const tooltipData = [
    { Name: 'Off', Value: 'Off' },
    { Name: 'On', Value: 'On' },
  ]
  const importTemplateFn = (data) => {
    const template =
      '<div class="e-template-btn"><span class="e-btn-icon e-icons e-upload-1 e-icon-left"></span>${text}</div>'
    return compile(template.trim())(data)
  }
  const updateLiveTime = () => {
    let scheduleTimezone = (scheduleObj && scheduleObj.current) 
      ? scheduleObj.current.timezone
      : 'Asia/Calcutta'
    let liveTime
    let isAdaptive = (scheduleObj && scheduleObj.current && scheduleObj.current.isAdaptive) 
    if (isAdaptive) {
      liveTime = new Date().toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit',
        timeZone: scheduleTimezone,
      })
    } else {
      liveTime = new Date().toLocaleTimeString('en-US', {
        timeZone: scheduleTimezone,
      })
    }
    if(timeBtn.current) timeBtn.current.innerHTML = liveTime;
  }
  const onImportClick = (args) => {
    scheduleObj.current.importICalendar(args.event.target.files[0])
  }
  const onPrint = () => {
    scheduleObj.current.print()
  }
  const onExportClick = (args) => {
    if (args.item.text === 'Excel') {
      let exportDatas = []
      let eventCollection = scheduleObj.current.getEvents()
      let resourceCollection = scheduleObj.current.getResourceCollections()
      let resourceData = resourceCollection[0].dataSource
      for (let resource of resourceData) {
        let data = eventCollection.filter(
          (e) => e.CalendarId === resource.CalendarId,
        )
        exportDatas = exportDatas.concat(data)
      }
      scheduleObj.current.exportToExcel({
        exportType: 'xlsx',
        customData: exportDatas,
        fields: ['Id', 'Subject', 'StartTime', 'EndTime', 'CalendarId'],
      })
    } else {
      scheduleObj.current.exportToICalendar()
    }
  }
  const getEventData = () => {
    const date = scheduleObj.current.selectedDate
    return {
      Id: scheduleObj.current.getEventMaxID(),
      Subject: '',
      StartTime: new Date(
        date.getFullYear(),
        date.getMonth(),
        date.getDate(),
        new Date().getHours(),
        0,
        0,
      ),
      EndTime: new Date(
        date.getFullYear(),
        date.getMonth(),
        date.getDate(),
        new Date().getHours() + 1,
        0,
        0,
      ),
      Location: '',
      Description: '',
      IsAllDay: false,
      CalendarId: 1,
    }
  }
  const onToolbarItemClicked = (args) => {
    switch (args.item.text) {
      case 'Day':
        setCurrentView(isTimelineView ? 'TimelineDay' : 'Day')
        break
      case 'Week':
        setCurrentView(isTimelineView ? 'TimelineWeek' : 'Week')
        break
      case 'WorkWeek':
        setCurrentView(isTimelineView ? 'TimelineWorkWeek' : 'WorkWeek')
        break
      case 'Month':
        setCurrentView(isTimelineView ? 'TimelineMonth' : 'Month')
        break
      case 'Year':
        setCurrentView(isTimelineView ? 'TimelineYear' : 'Year')
        break
      case 'Agenda':
        setCurrentView('Agenda')
        break
      /*case 'New Event': {
        let eventData = getEventData()
        scheduleObj.current.openEditor(eventData, 'Add', true)
        break
      }
      case 'New Recurring Event': {
        let recEventData = getEventData()
        scheduleObj.current.openEditor(recEventData, 'Add', true, 1)
        break
      }*/
    }
  }
  useEffect(() => {
    let updatedView = currentView
    switch (currentView) {
      case 'Day':
      case 'TimelineDay':
        updatedView = isTimelineView ? 'TimelineDay' : 'Day'
        break
      case 'Week':
      case 'TimelineWeek':
        updatedView = isTimelineView ? 'TimelineWeek' : 'Week'
        break
      case 'WorkWeek':
      case 'TimelineWorkWeek':
        updatedView = isTimelineView ? 'TimelineWorkWeek' : 'WorkWeek'
        break
      case 'Month':
      case 'TimelineMonth':
        updatedView = isTimelineView ? 'TimelineMonth' : 'Month'
        break
      case 'Year':
      case 'TimelineYear':
        updatedView = isTimelineView ? 'TimelineYear' : 'Year'
        break
      case 'Agenda':
        updatedView = 'Agenda'
        break
    }
    scheduleObj.current.currentView = updatedView
  }, [isTimelineView])
  const onChange = (args) => {
    setIsTimelineView(args.checked)
  }
  const timelineTemplate = useCallback(() => {
    return (
      <div className='template'>
        <div className='icon-child'>
          <CheckBoxComponent
            id='timeline_views'
            checked={isTimelineView}
            change={onChange}
          />
        </div>
        <div className='text-child'>Timeline Views</div>
      </div>
    )
  }, [])
  const groupTemplate = useCallback(() => {
    return (
      <div className='template'>
        <div className='icon-child'>
          <CheckBoxComponent
            id='grouping'
            checked={true}
            change={(args) => {
              scheduleObj.current.group.resources = args.checked
                ? ['Calendars']
                : []
            }}
          />
        </div>
        <div className='text-child'>Grouping</div>
      </div>
    )
  }, [])
  const gridlineTemplate = useCallback(() => {
    return (
      <div className='template'>
        <div className='icon-child'>
          <CheckBoxComponent
            id='timeSlots'
            checked={true}
            change={(args) => {
              scheduleObj.current.timeScale.enable = args.checked
            }}
          />
        </div>
        <div className='text-child'>Gridlines</div>
      </div>
    )
  }, [])
  const autoHeightTemplate = useCallback(() => {
    return (
      <div className='template'>
        <div className='icon-child'>
          <CheckBoxComponent
            id='row_auto_height'
            checked={false}
            change={(args) => {
              scheduleObj.current.rowAutoHeight = args.checked
            }}
          />
        </div>
        <div className='text-child'>Row Auto Height</div>
      </div>
    )
  }, [])
  const getDateHeaderDay = (value) => {
    return intl.formatDate(value, { skeleton: 'E' })
  }
  const getDateHeaderDate = (value) => {
    return intl.formatDate(value, { skeleton: 'd' })
  }
  const dateHeaderTemplate = (props) => {
    return (
      <Fragment>
        <div>{getDateHeaderDay(props.date)}</div>
        <div>{getDateHeaderDate(props.date)}</div>
      </Fragment>
    )
  }
  const onResourceChange = (args) => {
    let resourcePredicate
    for (let value of args.value) {
      if (resourcePredicate) {
        resourcePredicate = resourcePredicate.or(
          new Predicate('CalendarId', 'equal', value),
        )
      } else {
        resourcePredicate = new Predicate('CalendarId', 'equal', value)
      }
    }
    scheduleObj.current.resources[0].query = resourcePredicate
      ? new Query().where(resourcePredicate)
      : new Query().where('CalendarId', 'equal', 1)
  }
  let onEventClickOrDblClick = (args) => {
    console.log('Event clicked:', args);
    args.cancel = true; 
    setACase({
      businessKey: args.event.Id,
      caseDefinitionId: caseDefId,
    });
	handleClose();
    setOpenCaseForm(true);
  };
  let generateEvents = () => {
    let eventData = []
	cases.forEach(function (c) {
      const attr = (c && c.attributes && c.attributes[0]) ? JSON.parse(c.attributes[0].value) : null;
	  const caseDueDate = (attr && attr.dueDate && attr.dueDate !== "") ? moment(attr.dueDate) : null;
      if(caseDueDate) {
		const calStartDateTime = caseDueDate.subtract(1, 'hour');
		const caseTitle = `${c.businessKey} : ${caseDefId} - ${attr.caseTitle}`;
		const caseDescription = attr.caseDescription;
        eventData.push({
          Id: c.businessKey, 
          Subject: caseTitle,
          StartTime: calStartDateTime.toDate(), //Created
          EndTime: caseDueDate.toDate(), //Due
          Location: '',
          Description: caseDescription, 
          RecurrenceRule: null,
          IsAllDay: false,
          IsReadonly: false,
          CalendarId: 1
	    });
      }		
	});
    
	/*if (Browser.isIE) {
      Timezone.prototype.offset = (date, timezone) =>
        tz.zone(timezone).utcOffset(date.getTime())
    }*/
    let overviewEvents = extend([], eventData, undefined, true);
    /*let timezone = new Timezone()
    console.warn(timezone);
    let currentTimezone = timezone.getLocalTimezoneName()
    for (let event of overviewEvents) {
      event.StartTime = timezone.convert(
        event.StartTime,
        'UTC',
        currentTimezone,
      )
      event.EndTime = timezone.convert(event.EndTime, 'UTC', currentTimezone)
    }*/
    return overviewEvents;
  }
  const createUpload = () => {
    const element = document.querySelector('.calendar-import .e-css.e-btn')
    element.classList.add('e-inherit')
  }
  const btnClick = () => {
    let settingsPanel = document.querySelector('.overview-content .right-panel')
    if (settingsPanel.classList.contains('hide')) {
      removeClass([settingsPanel], 'hide')
      workWeekObj.current.refresh()
      resourceObj.current.refresh()
    } else {
      addClass([settingsPanel], 'hide')
    }
    scheduleObj.current.refreshEvents()
  }
  const timezoneChange = (args) => {
    scheduleObj.current.timezone = args.value
    updateLiveTime()
    document.querySelector('.schedule-overview #timezoneBtn').innerHTML =
      args.itemData.text
  }
  const weekNumberChange = (args) => {
    if (args.value == 'Off') {
      scheduleObj.current.showWeekNumber = false
    } else {
      scheduleObj.current.showWeekNumber = true
      scheduleObj.current.weekRule = args.value
    }
  }
  const tooltipChange = (args) => {
    if (args.value === 'Off') {
      scheduleObj.current.eventSettings.enableTooltip = false
    } else {
      scheduleObj.current.eventSettings.enableTooltip = true
    }
  }
  return (
    <div className='schedule-control-section'>
      <div className='col-lg-12 control-section'>
        <div className='content-wrapper'>
          <div className='schedule-overview'>
            <AppBarComponent colorMode='Primary'>
              <span className='time e-icons e-time-zone'></span>
              <span id='timezoneBtn' className='time '>
                UTC
              </span>
              <span className='time e-icons e-clock'></span>
              <span
                id='timeBtn'
                className='time current-time'
                ref={timeBtn}
              ></span>
              <div className='e-appbar-spacer'></div>
              <div className='control-panel calendar-export'>
                <ButtonComponent
                  id='printBtn'
                  cssClass='title-bar-btn e-inherit'
                  iconCss='e-icons e-print'
                  onClick={onPrint}
                  content='Print'
                />
              </div>
              <div className='control-panel calendar-export'>
                <DropDownButtonComponent
                  id='exportBtn'
                  content='Export'
                  cssClass='e-inherit'
                  items={exportItems}
                  select={onExportClick}
                />
              </div>
              <ButtonComponent
                id='settingsBtn'
                cssClass='overview-toolbar-settings e-inherit'
                iconCss='e-icons e-settings'
                iconPosition='Top'
                content=''
                onClick={btnClick}
              />
            </AppBarComponent>
            <ToolbarComponent
              id='toolbarOptions'
              cssClass='overview-toolbar'
              width='100%'
              height={70}
              overflowMode='Scrollable'
              scrollStep={100}
              created={() =>
                 setInterval(() => {
                   updateLiveTime()
                 }, 60000)
              }
              clicked={onToolbarItemClicked}
            >
              <ItemsDirective>
                {/* <ItemDirective
                  prefixIcon='e-icons e-plus'
                  tooltipText='New Event'
                  text='New Event'
                  tabIndex={0}
                />
                <ItemDirective
                  prefixIcon='e-icons e-repeat'
                  tooltipText='New Recurring Event'
                  text='New Recurring Event'
                  tabIndex={0}
                />
                <ItemDirective type='Separator' /> */}
                <ItemDirective
                  prefixIcon='e-icons e-day'
                  tooltipText='Day'
                  text='Day'
                  tabIndex={0}
                />
                <ItemDirective
                  prefixIcon='e-icons e-week'
                  tooltipText='Week'
                  text='Week'
                  tabIndex={0}
                />
                <ItemDirective
                  prefixIcon='e-icons e-week'
                  tooltipText='WorkWeek'
                  text='WorkWeek'
                  tabIndex={0}
                />
                <ItemDirective
                  prefixIcon='e-icons e-month'
                  tooltipText='Month'
                  text='Month'
                  tabIndex={0}
                />
                <ItemDirective
                  prefixIcon='e-icons e-month'
                  tooltipText='Year'
                  text='Year'
                  tabIndex={0}
                />
                <ItemDirective
                  prefixIcon='e-icons e-agenda-date-range'
                  tooltipText='Agenda'
                  text='Agenda'
                  tabIndex={0}
                />
                <ItemDirective
                  tooltipText='Timeline Views'
                  text='Timeline Views'
                  template={timelineTemplate}
                />
                <ItemDirective type='Separator' />
                <ItemDirective
                  tooltipText='Grouping'
                  text='Grouping'
                  template={groupTemplate}
                />
                <ItemDirective
                  tooltipText='Timme Slots'
                  text='Timme Slots'
                  template={gridlineTemplate}
                />
                <ItemDirective
                  tooltipText='Auto Fit Rows'
                  text='Auto Fit Rows'
                  template={autoHeightTemplate}
                />
              </ItemsDirective>
            </ToolbarComponent>
            <div className='overview-content'>
              <div className='left-panel'>
                <div className='overview-scheduler'>
                  <ScheduleComponent
                    id='scheduler'
                    cssClass='schedule-overview'
                    ref={scheduleObj}
                    width='100%'
                    height='100%'
                    currentView={currentView}
                    group={{ resources: ['Calendars'] }}
                    timezone='UTC'
                    eventSettings={{ dataSource: generateEvents() }}
					eventClick={onEventClickOrDblClick}
					eventDoubleClick={onEventClickOrDblClick}
                    dateHeaderTemplate={dateHeaderTemplate}
                  >
                    <ResourcesDirective>
                      <ResourceDirective
                        field='CalendarId'
                        title='Calendars'
                        name='Calendars'
                        dataSource={calendarCollections}
                        query={new Query().where('CalendarId', 'equal', 1)}
                        textField='CalendarText'
                        idField='CalendarId'
                        colorField='CalendarColor'
                      />
                    </ResourcesDirective>
                    <ViewsDirective>
                      <ViewDirective option='Day' />
                      <ViewDirective option='Week' />
                      <ViewDirective option='WorkWeek' />
                      <ViewDirective option='Month' />
                      <ViewDirective option='Year' />
                      <ViewDirective option='Agenda' />
                      <ViewDirective option='TimelineDay' />
                      <ViewDirective option='TimelineWeek' />
                      <ViewDirective option='TimelineWorkWeek' />
                      <ViewDirective option='TimelineMonth' />
                      <ViewDirective option='TimelineYear' />
                    </ViewsDirective>
                    <Inject
                      services={[
                        Day,
                        Week,
                        WorkWeek,
                        Month,
                        Year,
                        Agenda,
                        TimelineViews,
                        TimelineMonth,
                        TimelineYear,
                        DragAndDrop,
                        Resize,
                        Print,
                        ExcelExport,
                        ICalendarImport,
                        ICalendarExport,
                      ]}
                    />
                  </ScheduleComponent>
                </div>
              </div>
              <div className='right-panel hide'>
                <div className='control-panel e-css'>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Calendar
                      </label>
                    </div>
                    <div className='col-right'>
                      <MultiSelectComponent
                        id='resources'
                        cssClass='schedule-resource'
                        ref={resourceObj}
                        dataSource={calendarCollections}
                        mode='CheckBox'
                        fields={{ text: 'CalendarText', value: 'CalendarId' }}
                        enableSelectionOrder={false}
                        showClearButton={false}
                        showDropDownIcon={true}
                        popupHeight={300}
                        value={[1]}
                        change={onResourceChange}
                      >
                        <Inject services={[CheckBoxSelection]} />
                      </MultiSelectComponent>
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        First Day of Week
                      </label>
                    </div>
                    <div className='col-right'>
                      <DropDownListComponent
                        id='weekFirstDay'
                        dataSource={weekDays}
                        fields={{ text: 'text', value: 'value' }}
                        value={0}
                        popupHeight={400}
                        change={(args) => {
                          scheduleObj.current.firstDayOfWeek = args.value
                        }}
                      />
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Work week
                      </label>
                    </div>
                    <div className='col-right'>
                      <MultiSelectComponent
                        id='workWeekDays'
                        cssClass='schedule-workweek'
                        ref={workWeekObj}
                        dataSource={weekDays}
                        mode='CheckBox'
                        fields={{ text: 'text', value: 'value' }}
                        enableSelectionOrder={false}
                        showClearButton={false}
                        showDropDownIcon={true}
                        value={[1, 2, 3, 4, 5]}
                        change={(args) =>
                          (scheduleObj.current.workDays = args.value)
                        }
                      >
                        <Inject services={[CheckBoxSelection]} />
                      </MultiSelectComponent>
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Timezone
                      </label>
                    </div>
                    <div className='col-right'>
                      <DropDownListComponent
                        id='timezone'
                        dataSource={timezoneData}
                        fields={{ text: 'text', value: 'value' }}
                        value='Asia/Calcutta'
                        popupHeight={150}
                        change={timezoneChange}
                      />
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Day Start Hour
                      </label>
                    </div>
                    <div className='col-right'>
                      <TimePickerComponent
                        id='dayStartHour'
                        showClearButton={false}
                        value={new Date(new Date().setHours(0, 0, 0))}
                        change={(args) =>
                          (scheduleObj.current.startHour = intl.formatDate(
                            args.value,
                            { skeleton: 'Hm' },
                          ))
                        }
                      />
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Day End Hour
                      </label>
                    </div>
                    <div className='col-right'>
                      <TimePickerComponent
                        id='dayEndHour'
                        showClearButton={false}
                        value={new Date(new Date().setHours(23, 59, 59))}
                        change={(args) =>
                          (scheduleObj.current.endHour = intl.formatDate(
                            args.value,
                            { skeleton: 'Hm' },
                          ))
                        }
                      />
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Work Start Hour
                      </label>
                    </div>
                    <div className='col-right'>
                      <TimePickerComponent
                        id='workHourStart'
                        showClearButton={false}
                        value={new Date(new Date().setHours(9, 0, 0))}
                        change={(args) =>
                          (scheduleObj.current.workHours.start =
                            intl.formatDate(args.value, { skeleton: 'Hm' }))
                        }
                      />
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Work End Hour
                      </label>
                    </div>
                    <div className='col-right'>
                      <TimePickerComponent
                        id='workHourEnd'
                        showClearButton={false}
                        value={new Date(new Date().setHours(18, 0, 0))}
                        change={(args) =>
                          (scheduleObj.current.workHours.end = intl.formatDate(
                            args.value,
                            { skeleton: 'Hm' },
                          ))
                        }
                      />
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Slot Duration
                      </label>
                    </div>
                    <div className='col-right'>
                      <DropDownListComponent
                        id='slotDuration'
                        dataSource={majorSlotData}
                        fields={{ text: 'Name', value: 'Value' }}
                        value={60}
                        popupHeight={150}
                        change={(args) => {
                          scheduleObj.current.timeScale.interval = args.value
                        }}
                      />
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Slot Interval
                      </label>
                    </div>
                    <div className='col-right'>
                      <DropDownListComponent
                        id='slotInterval'
                        dataSource={minorSlotData}
                        value={2}
                        popupHeight={150}
                        change={(args) => {
                          scheduleObj.current.timeScale.slotCount = args.value
                        }}
                      />
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Time Format
                      </label>
                    </div>
                    <div className='col-right'>
                      <DropDownListComponent
                        id='timeFormat'
                        dataSource={timeFormatData}
                        fields={{ text: 'Name', value: 'Value' }}
                        value={'hh:mm a'}
                        popupHeight={150}
                        change={(args) => {
                          scheduleObj.current.timeFormat = args.value
                        }}
                      />
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Week Numbers
                      </label>
                    </div>
                    <div className='col-right'>
                      <DropDownListComponent
                        id='weekNumber'
                        dataSource={weekNumberData}
                        fields={{ text: 'Name', value: 'Value' }}
                        value={'Off'}
                        popupHeight={150}
                        change={weekNumberChange}
                      />
                    </div>
                  </div>
                  <div className='col-row'>
                    <div className='col-left'>
                      <label style={{ lineHeight: '34px', margin: '0' }}>
                        Tooltip
                      </label>
                    </div>
                    <div className='col-right'>
                      <DropDownListComponent
                        id='tooltip'
                        dataSource={tooltipData}
                        fields={{ text: 'Name', value: 'Value' }}
                        value={'Off'}
                        popupHeight={150}
                        change={tooltipChange}
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}