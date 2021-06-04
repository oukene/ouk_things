/**
 *  Repeat Switch (v.0.0.1)
 *
 *  Authors
 *   - oukene
 *  Copyright 2021
 *
 */
definition(
    name: "Repeat Switch",
    namespace: "oukene/smart-switch",
    author: "oukene",
    description: "반복 스위치",
    category: "My Apps",
    
    parent: "oukene/smart-switch/parent:Smart Switch",
    iconUrl: "https://cdn4.iconfinder.com/data/icons/music-player-47/32/repeat_rewind_arrow_interface_refresh-512.png",
    iconX2Url: "https://cdn4.iconfinder.com/data/icons/music-player-47/32/repeat_rewind_arrow_interface_refresh-512.png",
    iconX3Url: "https://cdn4.iconfinder.com/data/icons/music-player-47/32/repeat_rewind_arrow_interface_refresh-512.png"
)

preferences
{
	page(name: "dashBoardPage", install: false, uninstall: true)
	page(name: "actuatorTypePage", install: false, uninstall: true)
    page(name: "switchPage", install: false, uninstall: true)
	page(name: "optionPage", install: true, uninstall: true)
}

def dashBoardPage(){
	dynamicPage(name: "dashBoardPage", title:"[Dash Board]", refreshInterval:1) {
    	if(state.initialize)
        {
        	section("현재 상태") {
				paragraph "- DashBoard", image: "https://cdn4.iconfinder.com/data/icons/finance-427/134/23-512.png"
                paragraph "[ " + (actuatorType ? "$actuatorType - $actuator" : "") + "switch - $main_switch ]"
                //paragraph "[ $actuatorType - $actuator, switch - $main_switch ]"
                paragraph "현재상태: " + main_switch.currentSwitch
				paragraph "" + (main_switch.currentSwitch == "on" ? "종료 예정 시각: " : "작동 예정 시각: ") + new Date(state.next_operator_time).format('yyyy-MM-dd HH:mm:ss.SSS', location.getTimeZone())
			}
     	}          
        section() {
          	href "switchPage", title: "설정", description:"", image: "https://cdn4.iconfinder.com/data/icons/industrial-1-4/48/33-512.png"
       	}
        if(state.initialize)
        {
            section()
            {
                href "optionPage", title: "옵션", description:"", image: "https://cdn4.iconfinder.com/data/icons/multimedia-internet-web/512/Multimedia_Internet_Web-16-512.png"
            }
		}
    }
}

def actuatorTypePage()
{
	dynamicPage(name: "actuatorTypePage", title: "설정", nextPage: "optionPage")
    {
        section()
        {
            input "actuatorType", "enum", title: "동작 조건 선택", multiple: false, required: false, submitOnChange: true, options: [
            	"button": "Button",
                "switch": "Switch"]
        }
        if(actuatorType != null)
        {
            section("$actuatorType 설정") {
                input(name: "actuator", type: "capability.$actuatorType", title: "$actuatorType 에서", required: true)
                input(name: "actuatorAction", type: "enum", title: "다음 동작이 발생하면", options: actuatorValues(actuatorType), required: true)
            }
        }
    }
}

def switchPage()
{
    dynamicPage(name: "switchPage", title: "", nextPage: "actuatorTypePage")
    {
        section("스위치 설정") {
            input(name: "main_switch", type: "capability.switch", title: "이 스위치를 켭니다(메인스위치)", multiple: false, required: true)
            input(name: "sub_switch", type: "capability.switch", title: "이 스위치들을 켭니다(보조스위치)", multiple: true, required: false)
        }
    }
}


def optionPage()
{
    dynamicPage(name: "optionPage", title: "")
    {
        section("그리고 아래 옵션을 적용합니다") {
        	input "onPeriod", "number", required: true, title: "켜짐 유지 시간(초)", defaultValue: "10"
            input "offPeriod", "number", required: true, title: "꺼짐 유지 시간(초)", defaultValue: "10"
            input "enableLog", "bool", title: "로그활성화", required: false, defaultValue: false
        }
        timeInputs()
        
        section("자동화 on/off")
        {
            input "enable", "bool", title: "활성화", required: true, defaultValue: true
        }
        
        if (!overrideLabel) {
            // if the user selects to not change the label, give a default label
            def l = main_switch.displayName + ": Repeat Switch"
            log.debug "will set default label of $l"
            app.updateLabel(l)
        }
        section("자동화 이름") {
        	if (overrideLabel) {
            	label title: "자동화 이름을 입력하세요", defaultValue: app.label, required: false
            }
            else
            {
            	paragraph app.label
            }
            input "overrideLabel", "bool", title: "이름 수정", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

def timeInputs() {
    section {
        input "startTime", "time", title: "자동화 작동시작", required: false
        input "endTime", "time", title: "자동화 작동종료", required: false
    }
}

private actuatorValues(attributeName) {
    switch(attributeName) {
        case "switch":
            return ["on":"켜짐","off":"꺼짐"]
        case "button":
        	return ["push", "double", "held"]
        default:
            return ["UNDEFINED"]
    }
}


def installed()
{
	log("Installed with settings: ${settings}.")
	initialize()
}

def updated()
{
	log("Updated with settings: ${settings}.")
	unsubscribe()
	unschedule()
	initialize()
}

def initialize()
{
	if(!enable) return
    
    log(location)
	
	// if the user did not override the label, set the label to the default
    //if (!overrideLabel) {
    //    app.updateLabel(app.label)
    //}
    subscribe(main_switch, "switch.off", switch_off_handler)
    subscribe(main_switch, "switch.on", switch_on_handler)
	
    if(actuator)
    {
        log("$actuator : $actuatorAction")
        subscribe(actuator, "$actuatorType.$actuatorAction", startRepeatSwitch)
	}
    else
    {
    	log("actuator not selected")
    }
    
    if(startTime != null)
        schedule(startTime, scheduleOnHandler)
        
	if(endTime != null)
        schedule(endTime, scheduleOffHandler)
        
	if(null == actuator && null == startTime)
    {
    	main_switch.off()
        main_switch.on()
    }
    
    state.initialize = true
}


def startRepeatSwitch(evt)
{
	log("$evt.name : $evt.value : $actuatorAction")
    
    state.next_operator_time = now() + (offPeriod * 1000)
    main_switch.off()
    main_switch.on()
}


def switch_on_handler(evt) {
	log.debug "switch_on_handler"
	state.next_operator_time = now() + (onPeriod * 1000)
    if(sub_switch)
    {
    	sub_switch.on()
    }
    runIn(onPeriod, switch_on_off, [overwrite: true])
}

def switch_off_handler(evt) {
	log("switch_off_handler")
    state.next_operator_time = now() + (offPeriod * 1000)
    if(sub_switch)
    {
    	sub_switch.off()
    }
    runIn(offPeriod, switch_on_off, [overwrite: true])
}

def switch_on_off()
{
	if("on" == main_switch.currentSwitch)
    {
    	main_switch.off()
    }
    else
    {
    	def isBetween = true
        if(null != startTime && null != endTime) { isBetween = timeOfDayIsBetween(startTime, endTime, new Date(), location.timeZone) }
        log("between: $isBetween")
    	if(isBetween)
        {
        	main_switch.on()
        }
    }
}

def scheduleOnHandler()
{
	// 동작 조건이 지정되어있지 않을때만 자동 on
	if(null == actuator)
    {
        log("start schedule")
        main_switch.on()
	}
}

def scheduleOffHandler()
{
	log("end schedule")
	main_switch.off()
}

def log(msg)
{
	if(enableLog != null && enableLog == true)
    {
    	log.debug msg
    }
}

