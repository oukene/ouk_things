/**
 *  Smart Switch (v.0.0.1)
 *
 *  Authors
 *   - oukene
 *  Copyright 2021
 *
 */
definition(
    name: "Smart Switch",
    namespace: "oukene/smart-switch/parent",
    author: "oukene",
    description: "스위치 자동화",
    pausable: true,
    category: "My Apps",
    iconUrl: "https://cdn4.iconfinder.com/data/icons/scenarium-vol-15/128/015_smart_home_switch_on-512.png",
    iconX2Url: "https://cdn4.iconfinder.com/data/icons/scenarium-vol-15/128/015_smart_home_switch_on-512.png",
    iconX3Url: "https://cdn4.iconfinder.com/data/icons/scenarium-vol-15/128/015_smart_home_switch_on-512.png")


preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "[생성된 자동화 목록]", install: true, uninstall: true,submitOnChange: true) {
        section("[자동화 생성]") {
            app(name: "Sensor Switch", appName: "Sensor Switch", namespace: "oukene/smart-switch", title: "Sensor Switch", multiple: true, image: "https://cdn4.iconfinder.com/data/icons/basic-ui-element-2-3-filled-outline/512/Basic_UI_Elements_-_2.3_-_Filled_Outline_-_44-29-512.png")
            app(name: "Counter Switch", appName: "Counter Switch", namespace: "oukene/smart-switch", title: "Counter Switch", multiple: true, image: "https://cdn4.iconfinder.com/data/icons/cue/72/rotate_counter_clockwise-512.png")
            app(name: "Timer Switch", appName: "Timer Switch", namespace: "oukene/smart-switch", title: "Timer Switch", multiple: true, image: "https://cdn4.iconfinder.com/data/icons/business-271/135/50-512.png")
            app(name: "Repeat Switch", appName: "Repeat Switch", namespace: "oukene/smart-switch", title: "Repeat Switch", multiple: true, image: "https://cdn4.iconfinder.com/data/icons/music-player-47/32/repeat_rewind_arrow_interface_refresh-512.png")
		}
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    // nothing needed here, since the child apps will handle preferences/subscriptions
    // this just logs some messages for demo/information purposes
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
        log.debug "child app: ${child.label}"
    }
}