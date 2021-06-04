/**
 *  Copyright 2021 oukene
 */
metadata {
        definition (name: "virtual counter", namespace: "oukene", author: "oukene") {
        capability "Switch"
        capability "Refresh"
        capability "Switch Level"
    }

	// simulator metadata
	simulator {
	}
}

def parse(String description) {
}

def on() {
	sendEvent(name: "switch", value: "on")
    log.info "Dimmer On"
}

def off() {
	sendEvent(name: "switch", value: "off")
    log.info "Dimmer Off"
}

def setLevel(val){
    log.info "setLevel $val"
    
    if (val <= 0) 
    {
    	val = 0
        off()
	}
    else
    {
    	on()
    }
    sendEvent(name: "level", value: val)
}

def refresh() {
    log.info "refresh"
}